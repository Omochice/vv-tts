# vv-tts

An Android text-to-speech engine that speaks Japanese with [VOICEVOX](https://voicevox.hiroshiba.jp/) voices, on the device.

## Overview

vv-tts registers itself as a system text-to-speech engine, so that apps such as e-book readers can read aloud with it.\
Speech is synthesized on the device by [VOICEVOX CORE](https://github.com/VOICEVOX/voicevox_core).

The settings screen downloads voice models, sets the default voice, and keeps a dictionary of how words are read, including their accent.

## Supports

- Android 14 (API 34) or higher
- arm64-v8a or x86_64 devices

## VOICEVOX

This app uses VOICEVOX.\
Voice models are downloaded in the app and used under the [VOICEVOX terms](https://github.com/VOICEVOX/voicevox_vvm).

## Development

The toolchain (JDK, Gradle, Android SDK) comes from the [Nix](https://nixos.org/) flake; there is no Gradle wrapper.

```console
# Enter the dev shell (or use direnv)
nix develop

# Lint, static analysis and unit tests
gradle check

# Building
gradle assembleDebug

# Formatting
nix fmt
```

## License

[zlib](./LICENSE)
