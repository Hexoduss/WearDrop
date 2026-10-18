# WearDrop

[![Build APK](https://github.com/Hexoduss/WearDrop/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Hexoduss/WearDrop/actions/workflows/build-apk.yml)

WearDrop is a free Android app that turns your wardrobe into a digital closet and suggests outfits built from the clothes you actually own. Everything runs on the device: no account, no API keys, no subscriptions.

## Features

- **Digital wardrobe** – add items from the camera or the gallery (up to 20 photos at once). Each photo is analyzed on the device: the main colors come from the image, and the item type comes from ML Kit's bundled image-labeling model. Every detail can be edited, including name, category, colors, pattern, style, season and formality.
- **Stylist** – three outfit ideas for the chosen occasion, each with a title, the reasons it works and a styling tip. A rule engine scores combinations on:
  - color harmony (neutrals, single accent, analogous and complementary hues, tonal looks);
  - pattern balance;
  - formality and style for the occasion;
  - season versus the current weather;
  - pieces worn in the last few days.

  Outfits can also be built around a chosen item.
- **Favorites and history** – save outfits and keep track of what you wore.
- **Weather-aware** – forecast from [Open-Meteo](https://open-meteo.com), free and keyless.
- **English and Italian** – follows the system language and can be switched in Settings (per-app language).
- Light and dark theme.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room for local storage, Coil for images
- ML Kit Image Labeling (bundled, offline) and a custom palette-based color extractor
- Rule-based outfit engine (`stylist/OutfitEngine.kt`), no network required
- OkHttp + kotlinx.serialization for the weather API
- AppCompat per-app locales for the language switch
- minSdk 26, targetSdk 34

## Build

### Automatic APK

Every push to `main` runs [`build-apk.yml`](.github/workflows/build-apk.yml), which builds an APK and publishes it under [Releases](https://github.com/Hexoduss/WearDrop/releases). All builds are signed with the same debug key committed to the repository, so new versions install over old ones.

### Local

Requirements: JDK 17 and the Android SDK (API 34), or Android Studio.

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```
app/src/main/java/com/weardrop/
├── analysis/    on-device photo analysis (colors + item type)
├── data/        Room entities and DAO, color palette, settings, photo import, repository
├── stylist/     outfit rule engine and its models (occasions, titles, reasons, tips)
├── weather/     Open-Meteo client
└── ui/
    ├── wardrobe/   item grid and photo import
    ├── item/       item details and editing
    ├── stylist/    outfit generation
    ├── outfits/    favorites and history
    ├── settings/   language, city, about
    └── theme/      palette and typography
```

Strings live in `res/values/strings.xml` (English) and `res/values-it/strings.xml` (Italian).

## Privacy

Photos and data stay on the device, and photos are analyzed locally. The only network request is the weather forecast, which sends the city name to Open-Meteo.

## Roadmap

- Background removal for item photos
- Planning outfits on a calendar
- Wardrobe statistics (never-worn items, most-used colors)
