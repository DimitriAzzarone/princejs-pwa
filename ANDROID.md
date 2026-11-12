# Prince of Persia per Android

L'app contiene l'HTML originale e tutte le sue risorse incorporate. Il file index.html della PWA non viene modificato nel repository. La copia inclusa nell'APK rimuove soltanto la registrazione del service worker, superflua per i file già installati, e aggiunge il supporto alla voce narrante Android.

Avvio locale da origine HTTPS WebViewAssetLoader, senza permessi di rete o accesso ai file. Gioco a schermo intero in orizzontale, comandi touch e tastiera fisica conservati. Preferenze e salvataggi già previsti dal gioco rimangono nella memoria locale dell'app. Nessuna migrazione automatica dei dati della PWA dal browser.

La voce narrante italiana usa il motore TTS installato sul dispositivo, preferendo una voce italiana offline. Se manca una voce italiana, il gioco conserva i sottotitoli. Dopo una pausa, la voce Android riprende dall'inizio della frase corrente. Disponibilità della voce, resa audio, touch, prestazioni e comportamento su telefono/tablet restano da provare sul dispositivo.

## Compilazione

Il ramo android-apk aggiunge il contenitore Android e un workflow GitHub Actions. Il ramo main e la pubblicazione PWA restano intatti.

Eseguire dalla radice:

    python3 tools/bundle_game.py
    python3 tools/check_bundle.py
    node tools/check_speech.cjs

Poi, nella cartella android, eseguire Gradle 8.9 con Java 17 e Android SDK 35:

    gradle assembleRelease

Il workflow produce un APK release non firmato, che viene firmato per la consegna con la chiave dedicata dell'app conservata fuori dal repository. Non pubblicare chiave o password nei sorgenti. Gli aggiornamenti devono riutilizzare la stessa firma e aumentare versionCode.

Identificativo Android: com.dimitriazzarone.princeofpersia. Versione iniziale: 1.0, codice 1. Sorgente HTML e risorse originali mantengono le rispettive attribuzioni e licenze; il contenitore non modifica la titolarità dei materiali del gioco.
