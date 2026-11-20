{
  description = "VOICEVOX as an Android text-to-speech engine";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    nur-packages = {
      url = "github:Omochice/nur-packages";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    git-hooks = {
      url = "github:cachix/git-hooks.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    {
      self,
      nixpkgs,
      treefmt-nix,
      flake-utils,
      nur-packages,
      git-hooks,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
          overlays = [
            nur-packages.overlays.default
          ];
        };
        treefmt = treefmt-nix.lib.evalModule pkgs (
          { ... }:
          let
            rumdlConfig = (pkgs.formats.toml { }).generate "rumdl.toml" {
              # keep-sorted start
              MD004.style = "dash";
              MD007.indent = 4;
              MD007.style = "fixed";
              MD041.enabled = false;
              MD049.style = "underscore";
              MD050.style = "asterisk";
              MD055.style = "leading-and-trailing";
              MD060.enabled = true;
              MD060.style = "aligned";
              MD077.enabled = false;
              global.line_length = 0;
              # keep-sorted end
            };
          in
          {
            # .git is a file in a linked worktree, so the default .git/config would make treefmt format the main checkout instead.
            projectRootFile = "flake.nix";
            settings.global.excludes = [ "_sources/*" ];
            settings.formatter.rumdl-format.options = [
              "--config"
              (toString rumdlConfig)
            ];
            programs = {
              # keep-sorted start block=yes
              keep-sorted.enable = true;
              ktfmt.enable = true;
              nixfmt.enable = true;
              rumdl-format.enable = true;
              toml-sort.enable = true;
              yamlfmt = {
                enable = true;
                settings = {
                  formatter = {
                    type = "basic";
                    retain_line_breaks_single = true;
                  };
                };
              };
              # keep-sorted end
            };
          }
        );
        pre-commit-check = git-hooks.lib.${system}.run {
          src = ./.;
          package = pkgs.prek;
          hooks = {
            # Claude Code sets CLAUDECODE; humans are covered by the pre-push hook
            gitleaks-commit = {
              enable = true;
              name = "gitleaks (claude commit)";
              entry = pkgs.lib.getExe (
                pkgs.writeShellApplication {
                  name = "gitleaks-when-claude";
                  runtimeInputs = [ pkgs.gitleaks ];
                  text = ''
                    if [ -z "''${CLAUDECODE:-}" ]; then
                      exit 0
                    fi
                    gitleaks git --pre-commit --staged --no-banner --redact
                  '';
                }
              );
              pass_filenames = false;
              stages = [ "pre-commit" ];
            };
            gitleaks-push = {
              enable = true;
              name = "gitleaks";
              entry = "${pkgs.lib.getExe pkgs.gitleaks} git --no-banner --redact";
              pass_filenames = false;
              stages = [ "pre-push" ];
            };
            treefmt = {
              enable = true;
              packageOverrides.treefmt = treefmt.config.build.wrapper;
              stages = [ "pre-push" ];
            };
          };
        };
        androidSdk =
          (pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "37.0" ];
            buildToolsVersions = [ "36.0.0" ];
            includeCmake = false;
          }).androidsdk;
        # A setup hook keeps a variable next to the store path it points at.
        # Setting it on mkShell would pull that path into every dev shell, including the ones that only lint.
        exportInDevShell =
          name: value:
          pkgs.makeSetupHook { name = "export-${name}"; } (
            pkgs.writeText "export-${name}.sh" ''
              export ${name}=${value}
            ''
          );
        sources = pkgs.callPackage ./_sources/generated.nix { };
        # VOICEVOX CORE publishes its Java bindings only as a zipped Maven repository on GitHub releases, not on Maven Central.
        # Not fetched with nvfetcher's fetch.tarball, because it strips the single top-level directory jp/, which is part of the Maven layout.
        voicevoxCoreMaven =
          pkgs.runCommand "voicevox-core-maven-${sources.voicevox-core-java-packages.version}"
            { nativeBuildInputs = [ pkgs.unzip ]; }
            ''
              unzip -q ${sources.voicevox-core-java-packages.src} -d $out
            '';
        # Laid out as a Java resources root, so that Gradle adds it as a source directory instead of copying files by path.
        termsResources =
          let
            onnxRuntime = sources.voicevox-onnxruntime-android-arm64;
            archiveName = "voicevox_onnxruntime-android-arm64-${onnxRuntime.version}";
          in
          pkgs.runCommand "vv-tts-terms-resources"
            {
              nativeBuildInputs = [ pkgs.perl ];
              catalog = ./app/src/main/resources/io/github/omochice/vvtts/assets/assets.tsv;
            }
            ''
              # The terms shown must belong to the archive the app downloads, and that archive is pinned in assets.tsv, not here.
              expected=$(perl -F'\t' -lane 'print $F[3] if $F[1] eq "${archiveName}.tgz"' "$catalog")
              if [ -z "$expected" ]; then
                echo "assets.tsv has no line for ${archiveName}.tgz, the ONNX Runtime archive pinned in nvfetcher.toml" >&2
                exit 1
              fi
              actual=$(sha256sum ${onnxRuntime.src} | perl -lane 'print $F[0]')
              if [ "$actual" != "$expected" ]; then
                echo "${archiveName}.tgz pinned in nvfetcher.toml has sha256 $actual, but assets.tsv pins $expected" >&2
                exit 1
              fi

              dir=$out/io/github/omochice/vvtts/assets/terms
              mkdir -p "$dir"
              cp ${sources.voicevox-vvm-readme.src} "$dir/README.txt"
              cp ${sources.voicevox-vvm-terms.src} "$dir/TERMS.txt"
              tar -xzOf ${onnxRuntime.src} ${archiveName}/TERMS.txt >"$dir/ONNXRUNTIME_TERMS.txt"
            '';
        devPackages = rec {
          # keep-sorted start block=yes
          actions = with pkgs; [
            actionlint
            ghalint
            zizmor
          ];
          android = [
            (exportInDevShell "ANDROID_HOME" "${androidSdk}/libexec/android-sdk")
            # The Android Gradle plugin refuses to build when this names another SDK, and GitHub's runners set it to their own.
            (exportInDevShell "ANDROID_SDK_ROOT" "${androidSdk}/libexec/android-sdk")
            (exportInDevShell "VOICEVOX_CORE_MAVEN_REPO" voicevoxCoreMaven)
            (exportInDevShell "VV_TTS_TERMS_RESOURCES" termsResources)
            pkgs.gradle_9
            pkgs.jdk21
          ];
          # keep-sorted end
          default = [
            treefmt.config.build.wrapper
          ]
          ++ actions
          ++ android;
        };
      in
      {
        # keep-sorted start block=yes
        apps.nvfetcher = {
          type = "app";
          program = pkgs.lib.getExe pkgs.nvfetcher;
        };
        checks = {
          # keep-sorted start
          actions =
            pkgs.runCommand "check-actions"
              {
                buildInputs = with pkgs; [
                  actionlint
                  ghalint
                  zizmor
                ];
                src = self;
              }
              ''
                cd $src
                actionlint .github/**/*.{yaml,yml}
                ghalint run
                zizmor .github
                touch $out
              '';
          formatting = treefmt.config.build.check self;
          pre-commit = pre-commit-check;
          renovate =
            pkgs.runCommand "validate-renovate-config"
              {
                buildInputs = with pkgs; [
                  renovate
                ];
                src = self;
              }
              ''
                cd $src
                renovate-config-validator --strict renovate.json5
                touch $out
              '';
          # keep-sorted end
        };
        devShells = pkgs.lib.pipe devPackages [
          (pkgs.lib.attrsets.mapAttrs (
            name: buildInputs:
            pkgs.mkShell {
              buildInputs = buildInputs ++ pre-commit-check.enabledPackages;
              inherit (pre-commit-check) shellHook;
            }
          ))
        ];
        formatter = treefmt.config.build.wrapper;
        # keep-sorted end
      }
    );
}
