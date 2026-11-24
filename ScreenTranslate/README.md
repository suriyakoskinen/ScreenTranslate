# Screen Translate

Open-source Android app that translates **Russian and Chinese text on your screen into English, in place**, in any app: Telegram, X/Twitter, WhatsApp, browsers, and more.

- **Floating button (文A)** that stays on screen over every app
- **Manual mode** (blue): tap the button to translate everything on screen; tap again to see the original
- **Auto mode** (green): long-press the button; new messages and scrolled content are translated automatically
- **In-place:** the English text is drawn exactly on top of the original text, in a light-yellow box
- **Private & free:** translation runs 100% on the phone (Google ML Kit, offline). No API key, no server, and your chats never leave the device

## How it works

1. An Android **Accessibility Service** reads every text element on screen along with its exact position.
2. Text containing Cyrillic or Chinese characters is translated on-device with **ML Kit Translation** (results are cached).
3. A click-through **overlay window** paints the English text at the same coordinates, so taps still reach the app underneath.

## Build & install

1. Install [Android Studio](https://developer.android.com/studio) and open this folder.
2. Let Gradle sync. If Android Studio asks for a Gradle wrapper, accept, or run `gradle wrapper`.
3. Connect your phone (USB debugging on) and press **Run**, or use *Build → Build APK(s)* and share the APK.
4. In the app: tap **Download language packs**, then **Enable Screen Translate in Accessibility**.
   - Android 13+: if the switch is greyed out, open *Settings → Apps → Screen Translate → ⋮ → Allow restricted settings* first.

## Known limitations (v0.1)

- **Text inside images** (screenshots, memes) is not translated yet; see the roadmap.
- Long English translations in small boxes are auto-shrunk and can get small.
- In manual mode, translations disappear when you scroll (positions change); tap again, or use auto mode.
- Some apps draw text in custom ways that Accessibility can't read; those need the OCR mode.
- Android only. iOS does not allow apps to read other apps' screens or draw over them.
- Google Play heavily restricts Accessibility-based apps, so distribute via GitHub Releases / F-Droid.

## Roadmap

- [ ] OCR mode for images: screen capture (MediaProjection) + ML Kit Text Recognition (Chinese) + Tesseract (Russian)
- [ ] More source languages selectable in the app (ML Kit supports ~50)
- [ ] Per-app on/off list (e.g. only Telegram and X)
- [ ] Better text fitting (expand boxes downwards instead of shrinking)
- [ ] Optional higher-quality cloud translation (DeepL / LLM) with your own key

## Contributing

PRs welcome. Keep the default fully on-device and private.

## License

MIT
