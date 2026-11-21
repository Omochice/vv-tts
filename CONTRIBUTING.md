# Contributing

## Development environment

Every tool comes from the Nix flake.

```console
nix develop
```

## Checks

```console
# Lint, static analysis and unit tests
gradle check

# Formatting, GitHub Actions lint, Renovate config and Git hooks
nix flake check
```

## Formatting

```console
nix fmt
```

## Updating VOICEVOX releases

```console
nix run .#nvfetcher
```

## Commits and pull requests

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

`fixup!` and `squash!` commits are welcome during review, but they must be squashed before merging.
