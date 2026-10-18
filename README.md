# Armadio AI 2.0

App Android (Kotlin + Jetpack Compose): fotografi i tuoi capi, Claude li riconosce e li cataloga, poi ti propone outfit adatti a occasione e meteo, usando solo quello che hai nell'armadio.

## Cosa fa

- **Armadio**: aggiungi capi dalla fotocamera o dalla galleria (fino a 20 foto alla volta). L'AI riconosce nome, categoria, colori, fantasia, stile, stagione e formalità. Puoi filtrare per categoria e correggere tutto a mano.
- **Stylist**: scegli l'occasione e l'app recupera da sola il meteo della tua città. Puoi partire da un capo preciso ("crea un outfit con questo capo"). Ricevi 3 outfit con foto, motivazione e un consiglio di styling.
- **I miei outfit**: preferiti salvati e storico di cosa hai indossato. Lo Stylist evita di riproporti i capi messi negli ultimi 3 giorni.
- **Impostazioni**: chiave API (con pulsante di verifica), scelta del modello (Haiku, Sonnet o Opus), città per il meteo, preferenze di stile.

## Prima di tutto: la chiave API

L'AI funziona con una chiave API di Anthropic, che si **inserisce dentro l'app** (Impostazioni). Non va messa nel codice.
Creala su https://console.anthropic.com → *API Keys*. Serve un po' di credito: ogni analisi di un capo o richiesta di outfit costa pochi centesimi o meno.

## Installazione — opzione A: senza Android Studio (consigliata)

Il progetto contiene una build automatica GitHub che crea l'APK per te.

1. Crea un account gratuito su https://github.com, poi un nuovo repository (anche privato).
2. Carica tutto il contenuto della cartella `ArmadioAI` (pulsante *Add file → Upload files*). Assicurati che venga caricata anche la cartella nascosta `.github`. Se il caricamento dal browser la salta, crea a mano il file `.github/workflows/build-apk.yml` (*Add file → Create new file*) e incolla il contenuto.
3. Apri la scheda **Actions**: la build "Build APK" parte da sola (ci mette circa 5 minuti).
4. Quando finisce, vai in **Releases** (colonna di destra del repository) e scarica `ArmadioAI.apk` **direttamente dal telefono**.
5. Apri il file e consenti l'installazione da "origini sconosciute" quando Android lo chiede.

Ogni volta che modifichi il codice su GitHub viene creato un nuovo APK. Tutte le build hanno la stessa firma, quindi puoi installarle sopra la precedente senza perdere l'armadio.

## Installazione — opzione B: con Android Studio

1. Installa [Android Studio](https://developer.android.com/studio) e apri la cartella `ArmadioAI`.
2. Aspetta la fine del Gradle Sync.
3. Collega il telefono con il debug USB attivo e premi **Run ▶**, oppure usa *Build → Build APK(s)*.

## Struttura del codice

| Percorso | Ruolo |
|---|---|
| `ai/ClaudeClient.kt`, `ai/Prompts.kt` | Chiamate a Claude con output strutturato (tool use): analisi foto e proposte di outfit |
| `weather/WeatherClient.kt` | Meteo da Open-Meteo (gratis, senza chiave) |
| `data/` | Database Room (capi, outfit salvati, storico), impostazioni, gestione foto |
| `ui/wardrobe`, `ui/item`, `ui/stylist`, `ui/outfits`, `ui/settings` | Le schermate, ognuna con il suo ViewModel |
| `ui/theme/Theme.kt` | Palette (crema, terracotta, oliva) con tema chiaro e scuro |

## Privacy

Foto e dati restano sul telefono. Una foto viene inviata a Claude solo quando un capo va analizzato. Per gli outfit vengono inviate solo le descrizioni testuali dei capi, non le foto.

## Idee per i prossimi passi

- Rimozione automatica dello sfondo dalle foto
- Calendario: outfit pianificati per i prossimi giorni
- Statistiche dell'armadio (capi mai indossati, colori più usati)
- Suggerimenti di acquisto per completare l'armadio
