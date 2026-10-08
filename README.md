# Not App

**Not App is not a launcher** — it's a smart shortcut container: one home-screen icon that opens your personal list of apps, URLs, and custom intents. Configure it once, tap it endlessly.

<a href="https://play.google.com/store/apps/details?id=gift.dhamma.noapp&hl=en"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80"/></a>

<a href="https://play.google.com/store/apps/details?id=gift.dhamma.noapp&hl=en"><img alt="Not App on Google Play" src="icons/notApp-android.jpg" height="350"/></a>

## Two copies side by side

GitHub releases carry **Not App Git** (`gift.dhamma.noapp.git`): the same app under its own package and name. It installs next to the
Google Play app (`gift.dhamma.noapp`), not over it, and keeps its own settings and shortcut list - handy for two setups on one phone.
Use Settings → export / import to move a list from one to the other.

## What is Not App?

Instead of cluttering your home screen with dozens of app shortcuts, **Not App** gives you one icon that does many things. It lives on your home screen alongside any launcher — no need to replace the one you love.

## How It Works

Choose your interaction style:

### LIST Mode (Default)
Tapping the app icon brings up your full shortcut list in a sleek bottom sheet. Long tap for direct OS shortcuts to individual items.

### DIRECT Mode
Tapping the app icon launches your primary shortcut instantly, no UI, no delay. Long tap for shortcuts to your backups and alternatives, managed by the OS.

### MIX Mode
Tapping the app icon launches your primary shortcut instantly, same as DIRECT — but your full shortcut list also opens on top of it, so the rest of your items are always one tap away. Long tap still works too, managed by the OS.

In LIST and MIX mode, swiping the list away collapses it into a small floating button instead of closing outright — drag it wherever's convenient and it stays on screen (even over other apps, with the "draw over other apps" permission) until you tap it to bring the list back. Drag it onto the trash target that appears mid-drag to dismiss it for good — that also flips off the "show floating button" setting, so it won't keep coming back on you. Can be turned off entirely in Settings if you'd rather swiping just close the list, like before.

## Features

- **Unlimited shortcuts**: App launches, URLs, custom intents — configure as many as you need
- **Three interaction modes**: LIST for exploration, DIRECT for speed, MIX for both at once
- **Drag-to-reorder**: Arrange your shortcuts exactly how you want them
- **Customizable icons**: Emoji badges, colored labels, or the original app icons
- **Floating peek button**: an optional draggable bubble that keeps your list one tap away after you swipe it aside, in LIST and MIX mode — drag-to-remove built in
- **Recent apps row**: an optional row of your recently-used apps at the top of the list, for one-tap access without adding them as shortcuts
- **8 launcher icon styles**: pick how the app icon itself looks from Settings, independent of your shortcut icons
- **Pin to home screen**: pin any single shortcut as its own standalone home-screen icon
- **Smart sharing**: Share text directly to your shortcuts — use `{{word}}` placeholders in URLs to dynamically insert shared content
- **Import/export**: Backup and restore your entire config as JSON
- **OS integration**: Syncs with Android App Shortcuts for long-press menu support, reliably across all three modes

## Build

```
./gradlew assembleDebug
```

Requires Android SDK platform 36 + build-tools 36.1.0 (`local.properties` with `sdk.dir=...`, gitignored).

## License

MIT (see `LICENSE`).
