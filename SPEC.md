# LPCounter — Specifica condivisa

Fonte di verità per **iOS** (`ios/`, SwiftUI) e **Android** (`android/`, Jetpack Compose).
Le due app devono avere gli stessi componenti di interfaccia e le stesse funzionalità; cambia solo
l'implementazione, che usa sempre componenti nativi e best practice della piattaforma
(Apple HIG / Material 3). Ogni modifica a funzionalità o UI parte da qui.

Stato: le due app sono allineate tra loro; ciò che è specificato ma non ancora implementato è in [Divergenze](#divergenze-da-chiudere).

- Regole ufficiali di conteggio, con le fonti: [`docs/rules/yugioh.md`](docs/rules/yugioh.md).
- Design e prototipo interattivo: [`docs/design/LPCounter.figcraft`](docs/design/LPCounter.figcraft) (FigCraft): pagine *Componenti*, *Prototipo* (flussi "LPCounter iOS" e "LPCounter Android") e *Responsive*, ognuna con iOS sopra e Android sotto.

## 1. Modello

- Due giocatori, `Player 1` e `Player 2`. Punti vita (LP) iniziali: **LP iniziali** delle impostazioni (§5), 8000 o 4000; default **8000**.
- Per ogni giocatore: `lifePoints` (≥ 0) e `history`.
- `history` è una lista di **variazioni con segno** (`-500`, `+1000`), la più vecchia per prima.
  Non contiene totali assoluti. È **persistente** e **condivisa** tra app e widget.
- Massimo **200** voci per giocatore: oltre, si scartano le più vecchie.
- **Duelli recenti:** rimossi. Si gioca un duello alla volta: la Home mostra solo quello in corso.
- Stato condiviso app ↔ widget ↔ punteggio live: App Group (iOS), DataStore (Android).
  Ogni scrittura aggiorna widget e punteggio live.

## 2. Regole

| Operazione | Effetto |
|---|---|
| Modifica di `d` | `lifePoints = max(0, lifePoints + d)`; in history si registra la variazione **realmente applicata** (es. `-8000` se da 8000 un danno di 9000 viene limitato a 0) |
| Variazione nulla | Non viene registrata in history |
| Dimezza | `lifePoints = (lifePoints + 1) / 2` (metà **arrotondata per eccesso**, come da regolamento: 2425 → 1213); si registra `nuovo - vecchio` |
| Annulla | Sottrae l'ultima variazione dal totale **corrente** (`max(0, lifePoints - ultima)`) e la rimuove. Con history vuota non fa nulla |
| Azzera giocatore | `lifePoints` = LP iniziali e history del giocatore svuotata |
| Azzera partita | Entrambi i totali agli LP iniziali e **la history di entrambi svuotata** (nuovo duello) |
| Elimina tutti i dati | Totali agli LP iniziali, history svuotata, timer fermato. Le impostazioni restano |

"Relativo al totale corrente" significa che modifiche fatte altrove (widget) non vengono perse da Annulla.

## 3. Calcolatrice

- Cifre 0–9, massimo **4 cifre**; uno `0` iniziale è ignorato.
- Valore applicato: **1–2 cifre = centinaia** (`5 → 500`, `25 → 2500`); **3–4 cifre = letterale** (`300 → 300`, `1234 → 1234`).
- Operazione `Sottrai` (default) o `Somma`, scelta con **due pulsanti grandi affiancati** (− Sottrai in rosso, + Somma in verde): quello attivo è pieno del suo colore.
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

Dimezza, partendo da 2425: risultato **1213**; da 1: **1**.

## 4. Schermata di gioco

- Due carte, una per giocatore. **Verticale:** Player 2 sopra, **ruotata di 180°** per leggere dal lato opposto; Player 1 sotto. **Orizzontale:** Player 1 a sinistra, Player 2 a destra, entrambi dritti.
- Schermata a tutto schermo, aperta da **Gioca** in Home (§9), senza barra di navigazione né tab. **Nessun gesto indietro** (né dal bordo né a tutto schermo): si esce solo con `⌂`. Se i totali non sono più quelli iniziali, `⌂` chiede "Uscire dal duello?" ("Tienilo per riprenderlo dalla Home, oppure chiudilo ora."): *Riprendi più tardi* (esce, il duello resta), *Termina il duello* (distruttivo: = Azzera partita, poi esce), *Annulla* (resta).
- **Orientamento:** sul telefono la partita è **sempre orizzontale**; la Home segue il dispositivo. Se serve, lo schermo ruota prima di entrare nella partita e, premendo Home, torna come è tenuto il dispositivo prima di uscirne: la transizione non coincide mai con una rotazione e la Home non appare mai girata. Su tablet e desktop la partita si impagina secondo la forma della finestra.
- In alto: **Home** (`⌂`) da solo a sinistra; al centro un gruppo con il **timer** solo se attivo, **Azzera partita** (`↺`), **Dado** e **Moneta**. Lontano dalla Dynamic Island su entrambi i lati. Timer (`mm:ss` a 7 segmenti; a 0 resta `0:00` in rosso; un tocco lo mette in **pausa** o lo riprende; il tempo in pausa non scorre). Le carte stanno sotto; ognuna ha in cima il nome del giocatore.
- **Azzera partita** chiede conferma ("Azzerare la partita? I punti vita tornano a quelli iniziali e la cronologia si svuota." — *Azzera partita* / *Annulla*) se l'impostazione *Conferma prima di azzerare* è attiva; altrimenti azzera subito.
- **Intro all'ingresso** (circa 1 s): una banda di punti accesi scorre dall'alto in basso sulla matrice, come un pannello che si accende; poi i totali si accendono con il *test delle lampade* (dal basso in alto); infine nomi dei giocatori, pulsanti e barra appaiono con una dissolvenza. Con *Riduci movimento* il tavolo appare subito.
- **Dado e moneta** (per gli effetti delle carte): aprono un pannello sopra lo schermo: la moneta **si gira** (Testa/Croce si alternano) e il dado **rotola** (un dado vero, con i pallini a punti della matrice, che gira e cambia faccia) per circa 2,5 s rallentando, poi si fermano sul risultato casuale (dado 1–6, moneta Testa/Croce). *Ancora* rilancia, un tocco fuori o *Chiudi* chiude. Con *Riduci movimento* il risultato appare subito; VoiceOver lo annuncia. Gli stessi due pulsanti sono anche in alto a sinistra in Home.
- **Fine duello:** se *Fine duello automatica* è attiva e un totale arriva a 0, la carta di chi ha perso mostra "Sconfitto", l'altra "Vince"; i pulsanti delle carte si nascondono e nella barra `↺` diventa **Nuova partita** (= Azzera partita, senza conferma). Per correggere un errore si usa *Nuova partita*.
- Schermo sempre acceso durante la partita.
- Il foglio della calcolatrice per Player 2 è ruotato di 180° in verticale.
- Ogni carta mostra il *Contatore* o la *Cronologia*: il pulsante `🕘` accanto ad *Annulla* e *Dimezza* mette la cronologia al posto del totale (stessa area, altezza fissa) e diventa `#` per tornare al totale: i pulsanti non si spostano. Le due carte occupano sempre metà schermo ciascuna. Nessun indicatore a pagine e nessuno scorrimento tra pagine (lo scorrimento sul totale modifica i LP).

### Pagina Contatore
- Totale grande al centro; un tocco apre la calcolatrice.
- **Effetti dedicati:** *Azzera partita* fa un **test delle lampade** (tutti i punti si accendono dal basso, poi appare il nuovo totale); *Dimezza* fa **dissolvere** il vecchio totale punto per punto nella sua metà. Le altre modifiche contano fino al nuovo valore. Con *Riduci movimento* il totale cambia subito.
- **Scorrimento sul totale** (asse prevalente, dal punto di vista del giocatore): verso destra **+100**, verso sinistra **−100**, verso l'alto **+1000**, verso il basso **−1000**. Passa dalle regole di §2 (clamp, history, annullabile). Disattivato a duello finito. Con VoiceOver/TalkBack: regolabile ±100 e azioni "Aggiungi 1000" / "Togli 1000".
- Tre pulsanti: **Cronologia** (`🕘`), **Annulla** (disabilitato se history vuota), **Dimezza**. *Azzera giocatore* non è più un pulsante della carta (la regola di §2 resta).

### Pagina Cronologia
- Elenco con la variazione più recente in cima: `+ 1000` in verde, `- 500` in rosso.
- Con history vuota: "No history" / "Nessuna cronologia".
- Voci uguali sono possibili: l'identità di una riga non è mai il suo valore.

## 5. Impostazioni

Aperte dall'ingranaggio in alto a destra della Home (§9). Valori persistenti.

| Gruppo | Voce | Controllo | Default | Effetto |
|---|---|---|---|---|
| Duello | LP iniziali | Menu 8000 / 4000 · Speed Duel | 8000 | Se entrambi i totali sono ancora quelli iniziali si applica subito, altrimenti al prossimo azzeramento |
| | Timer del round | Menu Spento / 45 / 50 min (+ la durata personalizzata scelta in Home) | Spento | §9, Timer: parte con *Gioca* |
| | Fine duello automatica | Interruttore | Sì | §4, Fine duello |
| Schermata di gioco | Conferma prima di azzerare | Interruttore | Sì | §4, Azzera partita |
| | Vibrazione | Interruttore | Sì | Feedback aptico a ogni modifica dei LP |
| | Oscura lo schermo | Menu Mai / 30 s / 1 / 2 / 5 min | 1 min | In partita, dopo questo tempo senza tocchi né modifiche, uno strato quasi nero copre il tavolo (i totali restano appena visibili); il primo tocco lo riaccende senza agire. Disattivato con VoiceOver |
| Aspetto | (anteprima) | Totale a punti grandi come in partita | — | Mostra subito colore e forma |
| | Colore | Elenco Blu notte / Ghiaccio / Brina / Ciano / Ambra | Blu notte | Colore acceso, alone e punti spenti (§10), anche in widget e punteggio live |
| | Stile pixel | Segmentato Tondi / Rombi / Quadrati | Tondi | Forma dei punti (§10), accesi e spenti, molto diverse tra loro |
| Dati | Elimina tutti i dati | Pulsante distruttivo con conferma ("Eliminare tutti i dati?" — *Elimina* / *Annulla*) | — | §2, Elimina tutti i dati |
| Informazioni | Versione, Font | Sola lettura | — | Versione dell'app; crediti del font DSEG7 (OFL) |

Ogni voce ha un'icona e una riga di spiegazione; sotto ogni gruppo una nota (es. i gesti sul totale sotto *Schermata di gioco*).

## 6. Widget

- **Partita** (piccolo): LP2 sopra, LP1 sotto; in sola lettura; un tocco apre l'app.
- **Giocatore 1** (piccolo e grande): totale di Player 1 con pulsanti `−` e `+` da **1000**. Passano dalle regole di §2, quindi finiscono in history e sono annullabili dall'app.

- Stile LED (§10) anche qui: matrice con totali e barra che ne accendono i punti; seguono *Stile pixel* (§5). Widget per la schermata di blocco: numeri a 7 segmenti.

## 6b. App Apple Watch (Player 1)

- Compagna dell'app iPhone, **solo iOS** (Android non ha un equivalente: vedi Divergenze). Una schermata per **Player 1**, nello stile LED (§10), con colore e forma dei punti presi dall'iPhone.
- Mostra il totale a punti; sotto l'**importo** a 7 segmenti: **1000** di base, oppure quello scelto con la **Digital Crown** (passi da 100, fino a 4000).
- **−** (rosso) e **+** (verde) applicano l'importo, poi l'importo torna a 1000; una **pressione lunga** applica 100. **Annulla** toglie l'ultima variazione.
- Le modifiche passano all'iPhone, che le applica con le regole di §2 (come i widget §6): finiscono nella history, aggiornano widget e punteggio live e sono annullabili dall'app. Se l'iPhone non è raggiungibile il Watch mostra subito la modifica, l'icona `iPhone barrato`, e la consegna quando torna in contatto.

## 7. Punteggio live

- iOS: Live Activity. Android: notifica persistente a bassa priorità.
- Mostra i due totali; tocco apre l'app; si aggiorna a ogni modifica. In stile LED (§10): `LP·COUNTER`, totali a punti, numeri a 7 segmenti nella Dynamic Island.
- Android chiede il permesso notifiche **una sola volta**: un rifiuto non viene richiesto di nuovo.

## 8. Componenti nativi (differenze volute)

| Elemento | iOS (Apple) | Android (Google) |
|---|---|---|
| Contatore/cronologia | Pulsante nell'intestazione della carta | `HorizontalPager` + indicatore a punti (da allineare) |
| Foglio calcolatrice | `.sheet` | `ModalBottomSheet` |
| Conferma azzeramento | `confirmationDialog` | `AlertDialog` |
| Impostazioni dalla Home | `.sheet` + `NavigationStack` | — (da allineare) |
| Pila Home → Partita | `NavigationStack` (barre nascoste in partita) | `AnimatedContent` + `BackHandler` |
| Home, Impostazioni | `Form` (`Toggle`, `Picker` `.segmented`) | `LazyColumn`/lista con `Switch` e `SingleChoiceSegmentedButtonRow` |
| Timer | `Text(timerInterval:)` | `Text` aggiornato ogni secondo |
| Pulsanti | Liquid Glass (`glassEffect`) | Material 3 Expressive (`FilledIconButton`) |
| Lista cronologia | `List` | `LazyColumn` + `ListItem` |
| Widget | WidgetKit + App Intents | Glance |
| Punteggio live | Live Activity | Notifica ongoing |
| Persistenza | App Group `UserDefaults` | Jetpack DataStore |
| Lingue | en, it | en, it |

I controlli restano nativi e seguono il sistema; tutto il resto segue lo stile LED comune di §10.

## 9. Home e navigazione

- Radice: la **Home**, senza tab né titolo. In alto a destra solo l'ingranaggio delle **Impostazioni** (`⚙︎`, §5, in un foglio). In basso una barra: **Dado** e **Moneta** a sinistra, **timer** (con la ✕ se ce n'è uno) a destra.
- Home è una pila: **DUEL** apre la partita (§4) con *push*; si torna con `⌂` o indietro di sistema. La partita in corso si riprende, non si azzera (*Riprendi*).
- Al centro dello schermo il titolo **in verticale**, una parola per riga: `IT'S` / `TIME` / `TO` (punti 2×2); la frase finisce nel **pulsante principale `DUEL`** (grande, largo), che avvia la partita (frase della serie, in inglese in ogni lingua).
- Tra titolo e pulsante, se c'è un duello in corso, i due totali (Player 1 · Player 2, a 7 segmenti); poi il pulsante, che diventa **Riprendi** quando c'è un duello in corso. Tutto centrato, su una sola schermata senza scorrimento.
- **Timer:** nella barra in basso, a destra, un pulsante con l'icona del timer e la durata ("50 min", il conto alla rovescia, o solo l'icona attenuata se spento) che apre un menu: *Spento*, preset **45** e **50 min**, *Personalizza…* (1–180 min, passi da 5). In partita il timer si vede **solo se è attivo**, come badge in alto (§4): un tocco lo mette in pausa o lo riprende. Si **rimuove** solo dalla Home, con la ✕ che compare accanto al pulsante del timer quando ce n'è uno (in corso o in pausa). *Gioca* riprende un timer in pausa. Premendo *Gioca* con il timer attivo, se non c'è un timer in corso (o è scaduto) ne parte uno nuovo dalla durata scelta;
  se è in corso continua. Con *Spento* il timer in corso si ferma. Persistente; *Azzera partita* non lo tocca (il round copre il match).

## 10. Stile LED

- Display **VFD a matrice di punti**: sempre **nero AMOLED** (`#000000`) + il **colore** scelto (§5); predefinito **Blu notte** (`#A9C8FF` acceso, alone `#3E6BFF`, `#0C111F` spento), nessun tema chiaro.
- Una **sola matrice** di LED, ancorata allo schermo, dietro ogni schermata e foglio: le scritte grandi e i totali **accendono i punti della matrice stessa** (font 5×7, ogni punto del font è un punto della griglia). **Stessa griglia ovunque** (passo 5 pt), Home, partita e Impostazioni: in partita i totali accendono blocchi di punti (fino a 3×3) della stessa griglia; in Home `IT'S TIME TO` e `DUEL` (punti 3×3). Nei widget e nella Live Activity passo 3 pt.
- Un totale più lungo (5 cifre) usa blocchi più piccoli, così resta nella carta; la griglia non cambia.
- **Totali LP** disegnati punto per punto su celle 5×7, coi punti spenti visibili, senza alone; cambiando contano fino al nuovo valore. In partita le carte non hanno sfondo né bordo: i totali stanno direttamente sulla matrice.
- Le scritte piccole (etichette, liste, impostazioni) sono troppo piccole per la griglia: **monospaziato di sistema**, maiuscolo, con alone, segue il testo dinamico. Timer, calcolatrice e totali in lista: **DSEG7** a 7 segmenti (OFL), coi segmenti spenti visibili.
- Verde `+`, rosso `−` / sconfitto. La forma dei punti (accesi e spenti) segue *Stile pixel* (§5).
- Pulsanti, interruttori, segmentati, menu, fogli e dialog restano **nativi**, con accento blu.
- **Calcolatrice:** sulla stessa matrice: il giocatore, il totale a punti e sotto `− 500 = 7500` (valore nel colore dell'operazione); il selettore Sottrai / Somma; il tastierino nativo (cifre a 7 segmenti, ✕ cancella, ✓ applica, pieno nel colore dell'operazione).
- **Nessuno splash**: l'app apre subito la Home (schermata di avvio di sistema nera). La partita entra senza animazioni d'ingresso.

## Divergenze da chiudere

Android è il riferimento (§2). iOS è allineato a §1–§7, escluse le voci qui sotto (stesse regole in `PlayerScore` e `CalculatorState`, stessi
esempi di §3 nei test di entrambe le app, stesse lingue `en` e `it`). Resta:

0. ℹ️ **LP iniziali e superfici esterne:** su Android widget e notifica leggono gli LP iniziali dallo stesso DataStore; su iOS widget e Live Activity usano ancora 8000 come valore di riserva se non c'è un totale salvato (caso raro: l'app salva sempre i totali).

3. ⚠️ **Scorrimento sul totale (§4), pagine solo dall'indicatore e niente pulsante Azzera giocatore:** solo iOS; Android ha ancora il pager scorrevole.

2. ⚠️ **Stile LED (§10), Home senza tab con frase che si scrive (§9), Stile pixel al posto di Tema (§5):** solo iOS. Android ha ancora il tema e l'aspetto precedente, anche per widget e notifica.

4. ⚠️ **Azzera partita svuota la cronologia (§2):** solo iOS; Android la mantiene ancora.

5. ℹ️ **App Apple Watch (§6b):** solo iOS; Android potrebbe avere un'app Wear OS, non prevista.

1. ℹ️ **Dati preesistenti:** su iOS, una history salvata prima dell'allineamento non esiste (era solo in memoria), quindi
   non serve migrazione. I totali restano nelle stesse chiavi.
