# vv-tts

An Android text-to-speech engine that speaks Japanese with [VOICEVOX](https://voicevox.hiroshiba.jp/) voices, on the device.

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

# Building
gradle assembleDebug
```

See [CONTRIBUTING.md](./CONTRIBUTING.md) for more information.

## License

[zlib](./LICENSE)
