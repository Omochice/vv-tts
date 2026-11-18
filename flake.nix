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
            settings.global.excludes = [ ];
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
        # A setup hook keeps ANDROID_HOME next to the SDK it points at.
        # Setting it on mkShell would pull the SDK into every dev shell, including the ones that only lint.
        androidSdkHome = pkgs.makeSetupHook { name = "android-sdk-home"; } (
          pkgs.writeText "android-sdk-home.sh" ''
            export ANDROID_HOME=${androidSdk}/libexec/android-sdk
            # The Android Gradle plugin refuses to build when this names another SDK, and GitHub's runners set it to their own.
            export ANDROID_SDK_ROOT=$ANDROID_HOME
          ''
        );
        # VOICEVOX CORE publishes its Java bindings only as a zipped Maven repository on GitHub releases, not on Maven Central.
        voicevoxCoreMaven = pkgs.fetchzip {
          url = "https://github.com/VOICEVOX/voicevox_core/releases/download/0.17.1/java_packages.zip";
          hash = "sha256-FLnV7p9SoPmRrPVSC8yFegoDjvpQzwraDXULgyxMLqM=";
          stripRoot = false;
        };
        voicevoxCoreMavenRepo = pkgs.makeSetupHook { name = "voicevox-core-maven-repo"; } (
          pkgs.writeText "voicevox-core-maven-repo.sh" ''
            export VOICEVOX_CORE_MAVEN_REPO=${voicevoxCoreMaven}
          ''
        );
        devPackages = rec {
          # keep-sorted start block=yes
          actions = with pkgs; [
            actionlint
            ghalint
            zizmor
          ];
          android = [
            androidSdkHome
            pkgs.gradle_9
            pkgs.jdk21
            voicevoxCoreMavenRepo
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
