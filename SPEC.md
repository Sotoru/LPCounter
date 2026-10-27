# LPCounter — Specifica condivisa

Fonte di verità per **iOS** (`ios/`, SwiftUI) e **Android** (`android/`, Jetpack Compose).
Le due app devono avere gli stessi componenti di interfaccia e le stesse funzionalità; cambia solo
l'implementazione, che usa sempre componenti nativi e best practice della piattaforma
(Apple HIG / Material 3). Ogni modifica a funzionalità o UI parte da qui.

Stato: ✅ allineato · ⚠️ iOS da allineare (vedi [Divergenze](#divergenze-da-chiudere)).

## 1. Modello

- Due giocatori, `Player 1` e `Player 2`. Punti vita (LP) iniziali: **8000**.
- Per ogni giocatore: `lifePoints` (≥ 0) e `history`.
- `history` è una lista di **variazioni con segno** (`-500`, `+1000`), la più vecchia per prima.
  Non contiene totali assoluti. È **persistente** e **condivisa** tra app e widget.
- Massimo **200** voci per giocatore: oltre, si scartano le più vecchie.
- Stato condiviso app ↔ widget ↔ punteggio live: App Group (iOS), DataStore (Android).
  Ogni scrittura aggiorna widget e punteggio live.

## 2. Regole

| Operazione | Effetto |
|---|---|
| Modifica di `d` | `lifePoints = max(0, lifePoints + d)`; in history si registra la variazione **realmente applicata** (es. `-8000` se da 8000 un danno di 9000 viene limitato a 0) |
| Variazione nulla | Non viene registrata in history |
| Dimezza | `lifePoints = lifePoints / 2` (divisione intera); si registra `nuovo - vecchio` |
| Annulla | Sottrae l'ultima variazione dal totale **corrente** (`max(0, lifePoints - ultima)`) e la rimuove. Con history vuota non fa nulla |
| Azzera giocatore | `lifePoints = 8000` e history del giocatore svuotata |
| Azzera partita | Entrambi i totali a 8000; **la history non viene toccata** |

"Relativo al totale corrente" significa che modifiche fatte altrove (widget) non vengono perse da Annulla.

## 3. Calcolatrice

- Cifre 0–9, massimo **4 cifre**; uno `0` iniziale è ignorato.
- Valore applicato: **1–2 cifre = centinaia** (`5 → 500`, `25 → 2500`); **3–4 cifre = letterale** (`300 → 300`, `1234 → 1234`).
- Operazione `Sottrai` (default) o `Somma`, alternabile con il tasto `±`.
- Invio applica il valore come modifica (§2), azzera l'input e chiude il foglio **solo se il totale è cambiato**.
- Cancella azzera l'input. Il foglio mostra il totale corrente, l'operazione e il valore calcolato (`-` se vuoto).
- Tastierino: 3 colonne in verticale, 4 in orizzontale.

Esempi (da replicare nei test di entrambe le app), partendo da 8000:

| Input | Operazione | Risultato |
|---|---|---|
| `5` | Sottrai | 7500 |
| `5` | Somma | 8500 |
| `99` | Sottrai | 0 (clamp) |
| `300` | Sottrai | 7700 |
| `12345` | — | input `1234` (quinta cifra ignorata) |
| `0` | — | input `0` (ignorato) |

## 4. Schermata di gioco

- Due carte, una per giocatore. **Verticale:** Player 2 sopra, **ruotata di 180°** per leggere dal lato opposto; Player 1 sotto. **Orizzontale:** Player 2 a sinistra, Player 1 a destra, entrambi dritti.
- Pulsante **Azzera partita** al centro, sopra le carte; in orizzontale ruotato di 270°.
- Schermo sempre acceso durante la partita.
- Il foglio della calcolatrice per Player 2 è ruotato di 180° in verticale.
- Ogni carta ha **due pagine** scorrevoli in orizzontale con indicatore: *Contatore* e *Cronologia*.

### Pagina Contatore
- Totale grande al centro; un tocco apre la calcolatrice.
- Tre pulsanti: **Azzera giocatore** (disabilitato se il totale è 8000), **Annulla** (disabilitato se history vuota), **Dimezza**.

### Pagina Cronologia
- Elenco con la variazione più recente in cima: `+ 1000` in verde, `- 500` in rosso.
- Con history vuota: "No history" / "Nessuna cronologia".
- Voci uguali sono possibili: l'identità di una riga non è mai il suo valore.

## 5. Widget

- **Partita** (piccolo): LP2 sopra, LP1 sotto; in sola lettura; un tocco apre l'app.
- **Giocatore 1** (piccolo e grande): totale di Player 1 con pulsanti `−` e `+` da **1000**. Passano dalle regole di §2, quindi finiscono in history e sono annullabili dall'app.

## 6. Punteggio live

- iOS: Live Activity. Android: notifica persistente a bassa priorità.
- Mostra i due totali; tocco apre l'app; si aggiorna a ogni modifica.
- Android chiede il permesso notifiche **una sola volta**: un rifiuto non viene richiesto di nuovo.

## 7. Componenti nativi (differenze volute)

| Elemento | iOS (Apple) | Android (Google) |
|---|---|---|
| Pagine contatore/cronologia | `TabView` stile `.page` | `HorizontalPager` + indicatore a punti |
| Foglio calcolatrice | `.sheet` | `ModalBottomSheet` |
| Pulsanti | Liquid Glass (`glassEffect`) | Material 3 Expressive (`FilledIconButton`) |
| Lista cronologia | `List` | `LazyColumn` + `ListItem` |
| Widget | WidgetKit + App Intents | Glance |
| Punteggio live | Live Activity | Notifica ongoing |
| Persistenza | App Group `UserDefaults` | Jetpack DataStore |
| Lingue | en | en, it |

Aspetto, colori e animazioni seguono il sistema; non vanno imitati tra piattaforme.

## Divergenze da chiudere

Android è il riferimento (§2). iOS oggi differisce così:

1. ⚠️ **History in memoria e con totali assoluti** (`@State [Int]` in `GameCardView`). Va spostata nello store condiviso come variazioni con segno, max 200.
2. ⚠️ **Annulla** imposta il totale a `history.last ?? 8000`: non è relativo, e perde le modifiche del widget.
3. ⚠️ **Widget senza clamp:** `DataService.handleLifePoints` può portare il totale sotto zero, e non scrive in history.
4. ⚠️ **Cronologia:** il colore dipende da `number > 0` su totali assoluti, quindi è sbagliato; `ForEach(id: \.self)` rompe con valori duplicati, che con le variazioni sono comuni.
5. ⚠️ **Dimezza** registra il nuovo totale, e registra anche variazioni nulle.
6. ⚠️ **Test:** iOS non ha un test target; i casi di §3 vanno coperti.
7. ℹ️ Tooling: Android ha le stringhe in italiano; iOS solo `en.lproj`.
