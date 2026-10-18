# WearDrop

[![Build APK](https://github.com/Hexoduss/WearDrop/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Hexoduss/WearDrop/actions/workflows/build-apk.yml)

App Android per digitalizzare il proprio guardaroba e ricevere proposte di outfit generate dall'AI, a partire dai capi che si possiedono davvero.

## Funzionalità

- **Catalogo dei capi** – aggiunta da fotocamera o galleria (fino a 20 foto per volta). Ogni foto viene analizzata da un modello di visione, che estrae nome, categoria, colori, fantasia, stile, stagione e livello di formalità. Tutti i campi sono modificabili a mano.
- **Stylist** – tre proposte di outfit per occasione, con motivazione e consiglio di styling. Tiene conto del meteo locale, delle preferenze di stile dell'utente e dei capi indossati di recente. Può costruire gli outfit attorno a un capo scelto.
- **Preferiti e storico** – salvataggio degli outfit e registro di cosa è stato indossato.
- **Meteo automatico** tramite [Open-Meteo](https://open-meteo.com), senza chiave.
- Tema chiaro e scuro, interfaccia in italiano.

## Stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room per la persistenza locale, Coil per le immagini, OkHttp e kotlinx.serialization per la rete
- [Claude API](https://docs.anthropic.com) (Messages API con tool use per ottenere output JSON strutturato)
- minSdk 26, targetSdk 34

## Configurazione

L'analisi dei capi e le proposte di outfit usano la Claude API di Anthropic, che è **a consumo**. Serve una chiave API personale, creata da [console.anthropic.com](https://console.anthropic.com).

La chiave si inserisce nell'app (**Impostazioni → Chiave API**) e resta salvata solo sul dispositivo. Non è inclusa nel codice né nell'APK.

Dalle impostazioni si sceglie anche il modello. Haiku, il predefinito, è il più economico. Sonnet e Opus offrono qualità maggiore a un costo più alto.

## Build

### APK automatico

A ogni push su `main`, la workflow [`build-apk.yml`](.github/workflows/build-apk.yml) compila un APK e lo pubblica tra le [Releases](https://github.com/Hexoduss/WearDrop/releases). Tutte le build sono firmate con la stessa chiave di debug inclusa nel repository, quindi ogni aggiornamento si installa sopra il precedente.

### In locale

Requisiti: JDK 17 e Android SDK (34), oppure Android Studio.

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

## Struttura

```
app/src/main/java/com/armadioai/
├── ai/          client Claude e prompt (analisi foto, proposte di outfit)
├── data/        entità e DAO Room, impostazioni, import delle foto, repository
├── weather/     client Open-Meteo
└── ui/
    ├── wardrobe/   griglia dei capi e aggiunta foto
    ├── item/       scheda e modifica del capo
    ├── stylist/    generazione degli outfit
    ├── outfits/    preferiti e storico
    ├── settings/   chiave API, modello, città, preferenze
    └── theme/      palette e tipografia
```

## Privacy

Foto e dati restano sul dispositivo. A Claude viene inviata una foto solo durante l'analisi del capo corrispondente. Per le proposte di outfit vengono inviate soltanto le descrizioni testuali dei capi.

## Roadmap

- Rimozione automatica dello sfondo dalle foto
- Pianificazione degli outfit sul calendario
- Statistiche del guardaroba (capi mai indossati, colori più frequenti)
