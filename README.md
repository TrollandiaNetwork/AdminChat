# AdminChat - TrollNetwork
**AdminChat** è un plugin Velocity leggero ed efficiente per la gestione della Staff Chat tra i membri dell'amministrazione.

## Comandi disponibili
- **`/adminchat <enable|disable|toggle>`** (alias base)
  Gestisce la modalità di ascolto e scrittura per la tua sessione:
  - `enable`: Abilita permanentemente la ricezione ed invio dei messaggi staff, mettendoti in staff chat.
  - `disable`: Disabilita del tutto la chat e inibisce la ricezione dei messaggi in arrivo. (Se qualcuno scrive tra gli amministratori, tu non lo vedrai).
  - `toggle`: Inverte banalmente l'ultimo stato (se era attiva la spegne, se spenta la attiva).

- **`/a <messaggio>`** (alias quick-message)
  Se hai la necessità di inviare un singolo messaggio rapido, senza abilitare e ri-disabilitare manualmente la chat, scrivi `/a Ciao amministratori!`.
  *Nota bene:* se hai inibito te stesso con `/adminchat disable`, questo comando ti ricorderà di riattivarla prima!

- **`/a`** (senza argomenti)
  Funziona esattamente come `/adminchat toggle`, utilissimo per accendere e spegnere la comunicazione prolungata con lo staff a mano.

- **`/stafflist`**
  Visualizza in tempo reale la lista dei membri dello staff (che hanno la StaffChat potenzialmente abilitata e sono connessi al network in quel momento).

*(La Console ha la possibilità di usare sia `/adminchat disable` e `/adminchat enable` come filtro dei logs, sia di mandare messaggi con `/a messaggio`.)*

---

## Logica "Cache dello Staff"
Per evitare un inutile e pesante ciclo tra *tutti* i giocatori online ogni volta che dev'essere inviato un messaggio della chat admin, il plugin si affida ad un sistema di **Cache Locale**.

1. **All'accesso di un Giocatore (`PostLoginEvent`)**
   Quando un giocatore conclude la fase di connessione backend per entrare al proxy, il plugin chiama il proprio `PermissionChecker` per confermare che l'utente in entrata possieda il permesso nativo `adminchat.staffchat`. 
   In caso positivo verificherà lo status per aggiungerlo alla `staffCache` (`ConcurrentHashMap.newKeySet()`), che funge da rubrica attiva dei giocatori admin online.

2. **Aggiornamento "Live" dei Permessi (`PermissionsSetupEvent`)** (WIP, momentanemante disabilitato. Da capire l'effettivo utilizzo)
   Velocity offre una Hook per LuckPerms, che ordina ciclicamente al proxy di ri-validare internamente il nodo permessi (es. quando l'owner server digita `/lp user Karma177 parent add admin`).
   Il listener di questo evento "sonda" se per tale giocatore, nella sua nuova funzione appena re-iniettata da LuckPerms, è presente tale permesso con priorità `Tristate.TRUE`.
   Se sì viene istantaneamente agganciato alla `staffCache` (se non c'era, permettendogli quindi di leggere retroattivamente da quel momento e inviare messaggi); se gli è stato rubato il rango, al prossimo tick il `PermissionsSetupEvent` ne leggerà un `Tristate.FALSE/UNDEFINED` estirpandolo preventivamente e al volo dalla memoria prima di mandare messaggi o fargli leggere informazioni confidenziali.

3. **Lazy Update**
   Quando effettivamente un messaggio di StaffChat deve essere sparato ai client... il plugin semplicemente *itera esplicitamente sulla singola `staffCache`*.
   Se malauguratamente, nel tempo trascorso, uno o più player si sono improvvisamente disconnessi fisicamente da Velocity senza innescare gli Eventi corretti, il sistema verificherà dentro al medesimo broadcast `if(staffData.isActive())`.
   Se l'istanza è viva il pacchetto fluirà sereno al client; nel caso in cui una socket sia morta o inattiva senza possibilità di recupero, il "Lazy Update" se ne disferà `staffCache.remove(staffData)` a runtime, tenendo pulizia e velocità assolute ad immenso risparmio energetico dei Thread Velocity.

---

## Le 3 Cache di Sistema
Il cuore moderno del plugin si basa su queste 3 precise collection in `ConcurrentHashMap.newKeySet()`, create appositamente per garantire massima velocità ThreadSafe su ambienti Proxy Multi-Core:

- **`staffCache`**
  È l'anagrafica principale. Custodisce tutti i giocatori online in real-time che possiedono il permesso `adminchat.staffchat`. Serve al plugin come iteratore ottimizzato: invece di spammare `for` su migliaia di utenti ignoti per cercare un amministratore, sa sempre esattamente a quanti e quali profili mirare il check. Quando ti disconnetti (o ti tolgono il rank da console), vieni rimosso da questa cache principale.

- **`chatEnabledPlayers`**
  Filtra la "RICEZIONE" dei messaggi. Di base, entrando nel server, se hai il permesso, vieni aggiunto di forza qui dentro. Tuttavia, se esegui `/adminchat disable`, vieni estirpato da questo singolare mini-set pur rimanendo dentro a `staffCache`. Ciò impedisce a Velocity di inviarti fisicamente in chat le stringhe dello staff, bloccando gli input esterni verso il tuo client in modo pulito senza mai toccare i permessi originali del server.

- **`toggledPlayers`**
  Filtra "L'INVIO" asincrono in chat. Quando un membro dello staff esegue `/a` o `/adminchat toggle`, entra in questo set. Ogniqualvolta che il listener di chat di Velocity avvista un utente provare a parlare nella chat pubblica neutrale tramite la barra standard, interroga questa determinata cache: se fai parte di `toggledPlayers`, la chat pubblica viene bloccata e la tua frase sparata esclusivamente ai colleghi in AdminChat.

---

## TODO
- [ ] Refactor Codice
- [ ] Riformattazione estetica dei messaggi in ingresso/uscita per farla matchare al 100% con lo stile e i colori della chat del network principale.
- [x] Creazione del sistema di alert (Join e Quit) per notificare alla `staffCache` l'accesso al proxy di un membro dello staff in tempo reale.
  - [ ] Filtrare gli utenti per-modalita'

