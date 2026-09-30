# 💬 Laboratorul 3: Rețeaua locală (Sistem de Chat & Transfer Fișiere pe Java)

> **Curs:** Paradigme de programare (*Paradigme de programare*)  
> **Tema:** Rețeaua locală — Transmiterea mesajelor, istoric, reply, transfer de fișiere și chat-room-uri  
> **Punctaj conform cerințelor:** 10 / 10 puncte  
> **Arhitectură:** Aplicație separată de **Server** cu panou de control GUI și aplicație separată de **Client** cu interfață modernă Dark/Slate.

---

## 📑 Cuprins

1. [Pornire Rapidă (Quick Start)](#1-pornire-rapidă-quick-start)
2. [Arhitectura Aplicației și Structura Proiectului](#2-arhitectura-aplicației-și-structura-proiectului)
3. [Cum sunt îndeplinite toate cerințele din temă (10 / 10)](#3-cum-sunt-îndeplinite-toate-cerințele-din-temă)
   - [a. Transmiterea și recepționarea mesajelor în rețea (nota 5)](#a-transmiterea-și-recepționarea-mesajelor-în-rețea-cerința-minimă-nota-5)
   - [b. Afișarea istoriei mesajelor primite (1 punct)](#b-afișarea-istoriei-mesajelor-primite-1-punct)
   - [c. Posibilitatea de a răspunde la mesajul recepționat (Reply) (1 punct)](#c-posibilitatea-de-a-răspunde-la-mesajul-recepționat-reply-1-punct)
   - [d. Transmiterea și recepționarea fișierelor (2 puncte)](#d-transmiterea-și-recepționarea-fișierelor-2-puncte)
   - [e. Crearea chat-room-urilor (1 punct)](#e-crearea-chat-room-urilor-1-punct)
4. [Panoul de Administrare al Serverului](#4-panoul-de-administrare-al-serverului-servergui)
5. [Rularea în Rețea între mai multe Calculatoare (LAN / Wi-Fi)](#5-rularea-în-rețea-între-mai-multe-calculatoare-lan--wi-fi)
6. [Testare Automată a Funcționalităților](#6-testare-automată-a-funcționalităților)
7. [Întrebări Frecvente și Răspunsuri la Susținere (Шпаргалка)](#7-întrebări-frecvente-și-răspunsuri-la-susținere)

---

## 1. 🚀 Pornire Rapidă (Quick Start)

Toate scripturile au detectare automată a versiunii OpenJDK pe macOS (Apple Silicon și Intel):

### Varianta A: Lansare Server
Deschideți un terminal și rulați:
```bash
cd lab3
./run_server.sh
```
*Se deschide panoul de control al Serverului. Apăsați butonul verde **„▶ Pornire Server”**.*

### Varianta B: Lansare Client
Deschideți un alt terminal și rulați:
```bash
cd lab3
./run_client.sh
```
*Se deschide fereastra de chat a utilizatorului. Apăsați **„Conectare”** (implicit pe `127.0.0.1:8888`). Puteți deschide oricâți clienți doriți în paralel!*

### Varianta C: Lansare Demo Complet (1 Server + 2 Clienți instant)
```bash
cd lab3
./run.sh demo
```

### Varianta D: Meniu Interactiv
```bash
cd lab3
./run.sh
```

---

## 2. 📂 Arhitectura Aplicației și Structura Proiectului

Proiectul este organizat curat conform principiilor OOP (Object-Oriented Programming), cu separare strictă între **protocolul comun de rețea**, **server** și **client**:

```
lab3/
├── README.md                 # Documentația completă a proiectului
├── run.sh                    # Meniu interactiv central (server / client / demo / test)
├── run_server.sh             # Script dedicat pentru compilare și pornire Server
├── run_client.sh             # Script dedicat pentru compilare și pornire Client
├── bin/                      # Fișierele de bytecode Java (.class)
├── downloads/                # Folder implicit pentru fișierele descărcate
├── history/                  # Stocare persistentă a istoricului camerelor pe disc (.log)
└── src/
    ├── ServerMain.java       # Punctul de intrare pentru Server
    ├── ClientMain.java       # Punctul de intrare pentru Client
    ├── TestNetworkChat.java  # Suită de teste automate pentru toate cerințele a-e
    │
    ├── common/               # Modele comune de date și protocol de comunicație
    │   ├── MessageType.java     # Enum cu tipurile de pachete (CHAT, FILE, REPLY, ROOM etc.)
    │   ├── NetworkMessage.java  # Clasa pachet serializabil cu text, reply, date binare etc.
    │   ├── ChatRoom.java        # Entitatea cameră de chat (nume, descriere, membri activi)
    │   ├── ClientInfo.java      # Metadate despre clientul conectat (IP, port, cameră)
    │   └── UITheme.java         # Paletă modernă Dark/Slate, antialiasing, butoane rotunjite
    │
    ├── server/               # Logica de rețea a Serverului
    │   ├── ChatServer.java      # ServerSocket, gestionare clienți concurenți, rute, broadcast
    │   ├── ClientHandler.java   # Fir separat (Thread) per client pentru recepție asincronă
    │   ├── ServerListener.java  # Interfață de decuplare între server și GUI
    │   └── ServerGUI.java       # Panoul vizual de administrare (start/stop, camere, log-uri)
    │
    └── client/               # Logica de rețea a Clientului
        ├── ChatClient.java         # Socket TCP, thread de recepție, apeluri API de rețea
        ├── ClientListener.java     # Callback-uri pentru actualizarea interfeței
        ├── MessageBubblePanel.java # Bule de chat moderne (avatar, citat reply, card fișier)
        ├── HistoryDialog.java      # Fereastră de căutare, filtrare și export a istoricului
        └── ClientGUI.java          # Interfața grafică completă de chat (sidebar, input, reply)
```

---

## 3. 🏆 Cum sunt îndeplinite toate cerințele din temă

### a. Transmiterea și recepționarea mesajelor în rețea (cerința minimă, nota 5)
* **Unde este implementat:** `server/ChatServer.java`, `server/ClientHandler.java`, `client/ChatClient.java`.
* **Cum funcționează:**
  1. Serverul ascultă pe un port TCP (implicit `8888`) folosind `ServerSocket`.
  2. La conectarea unui client, se instanțiază un fir de execuție dedicat `ClientHandler` care citește continuu obiecte `NetworkMessage` printr-un `ObjectInputStream`.
  3. Când un utilizator trimite un mesaj, clientul îl ambalează în `NetworkMessage` de tip `CHAT_MESSAGE`.
  4. Serverul primește mesajul, îl adaugă în istoricul camerei și îl difuzează (*broadcast*) tuturor clienților conectați în acea cameră.
  5. Clienții recepționează mesajul și afișează o bulă de chat stilizată pe firul grafic EDT (`SwingUtilities.invokeLater`).

---

### b. Afișarea istoriei mesajelor primite (1 punct)
* **Unde este implementat:** `client/HistoryDialog.java`, `server/ChatServer.java` (metodele `recordMessage`, `getRecentHistory`, `appendHistoryToDisk`).
* **Cum funcționează:**
  1. **La nivel de server:** Toate mesajele din fiecare cameră sunt păstrate într-un cache sincronizat în memorie (`CopyOnWriteArrayList`) și salvate automat pe disc în `lab3/history/history_<camera>.log`.
  2. **La conectare sau schimbarea camerei:** Clientul primește automat ultimele mesaje din istoricul camerei și le afișează instant în zona de chat.
  3. **Fereastră dedicată de căutare și export:** În antetul de chat există butonul **`📜 Istoric Mesaje`**:
     * Afișează într-un tabel complet toate mesajele primite (ora, expeditor, tip, conținut).
     * Oferă **filtru de căutare în timp real** după cuvinte cheie sau nume de expeditor.
     * Permite **exportul complet al istoricului într-un fișier text (`.txt`)** prin butonul „📥 Exportă Istoric”.
  4. **În Server:** Administratorul poate vedea în tab-ul „📜 Jurnal & Istoric Global” toate mesajele tranzitate și le poate exporta în fișier `.log`.

---

### c. Posibilitatea de a răspunde la mesajul recepționat (Reply) (1 punct)
* **Unde este implementat:** `client/MessageBubblePanel.java`, `client/ClientGUI.java`, `common/NetworkMessage.java`.
* **Cum funcționează:**
  1. Pe fiecare bulă de mesaj există acțiunea **`↩ Răspunde`**.
  2. La apăsare, deasupra câmpului de introducere a textului apare o **bară elegantă de citat (Reply Banner)**:
     ```
     ┌────────────────────────────────────────────────────────────────────────┐
     │ ▎ ↳ Răspuns către @Alex: "Salut, ai terminat laboratorul?"         [✕] │
     └────────────────────────────────────────────────────────────────────────┘
     [ 📎 Fișier ] [ Scrie mesajul tău aici...                     ] [ Trimite ✈ ]
     ```
  3. La trimitere, mesajul reține identificatorul mesajului părinte (`replyToId`), numele autorului citat (`replyToAuthor`) și un extras din text (`replyToSnippet`).
  4. Când mesajul este afișat în chat de către toți participanții, bula conține o **casetă de citare dedicată**, cu margine colorată în stânga, evidențiind exact la ce mesaj s-a răspuns!
  5. Utilizatorul poate oricând anula răspunsul apăsând butonul `✕`.

---

### d. Transmiterea și recepționarea fișierelor (2 puncte)
* **Unde este implementat:** `client/ClientGUI.java` (metoda `chooseAndSendFile`), `client/ChatClient.java` (metoda `sendFile`), `client/MessageBubblePanel.java` (metoda `createFileCard`).
* **Cum funcționează:**
  1. Clientul dispune de butonul **`📎 Fișier`**.
  2. Se deschide un `JFileChooser` care permite selectarea oricărui tip de fișier de pe disc (imagini, documente PDF, arhive ZIP, fișiere sursă etc.).
  3. Fișierul este citit binar (`Files.readAllBytes`) și împachetat într-un `NetworkMessage` de tip `FILE_TRANSFER`, având numele fișierului, dimensiunea în octeți și datele binare `byte[]`.
  4. Serverul recepționează fișierul, contorizează statistica și îl transmite participanților din cameră.
  5. În fereastra de chat a fiecărui participant apare un **Card de Fișier Interactiv**:
     * Iconiță sugestivă de fișier (`📁`).
     * Numele fișierului și dimensiunea formatată automat (`ex: 1.4 MB` sau `320.5 KB`).
     * Buton **`⬇ Descarcă`**: deschide dialogul de salvare pe disc.
     * După salvare, butonul se transformă în **`✅ Salvat`** și apare butonul **`📂 Deschide`** care deschide automat fișierul descărcat cu aplicația nativă din sistem (`Desktop.getDesktop().open(file)`).

---

### e. Crearea chat-room-urilor (1 punct)
* **Unde este implementat:** `server/ChatServer.java`, `client/ClientGUI.java`, `common/ChatRoom.java`.
* **Cum funcționează:**
  1. **Camere implicite:** La pornire, serverul inițializează automat 3 camere:
     * `#general` — camera principală de discuție pentru toți utilizatorii.
     * `#proiecte` — discuții tehnice și laborator.
     * `#random` — socializare liberă.
  2. **Creare cameră de către Utilizator (din Client):**
     * În bara laterală, lângă eticheta „CAMERE CHAT”, utilizatorul apasă butonul **`➕`**.
     * Introduce numele camerei (de exemplu `echipa-alpha`) și o descriere opțională.
     * Clientul transmite cererea către server (`CREATE_ROOM`).
     * Serverul creează camera, o adaugă în registrul central și transmite lista actualizată către toți clienții conectați.
     * Utilizatorul care a creat camera este comutat automat în ea!
  3. **Creare și Ștergere de către Administrator (din Server GUI):**
     * În panoul de control al serverului, tab-ul „📁 Camere de Chat” permite crearea oricărei camere noi sau ștergerea camerelor vechi.
     * Dacă o cameră este ștearsă de admin, utilizatorii aflați în ea sunt migrați automat și în siguranță în camera `#general`.
  4. **Izolare și Broadcast pe camere:** Mesajele și fișierele trimise într-o cameră ajung **doar la participanții din acea cameră**. Schimbarea camerei actualizează automat lista de membri online și încarcă istoricul corespunzător.

---

## 4. 🖥️ Panoul de Administrare al Serverului (`ServerGUI`)

Aplicația de server dispune de un GUI complet de monitorizare și administrare:

* **Controale de Pornire/Oprire:** Butoane dedicate `▶ Pornire Server` (verde) și `⏹ Oprire` (roșu), selecție port (implicit `8888`).
* **Detecție automată a IP-ului:** Afișează automat adresa IP locală a calculatorului (ex: `192.168.1.15`), utilă pentru conectarea colegilor din rețea.
* **5 Carduri de Statistici Live:**
  1. `Stare Server`: ACTIV / OPRIT.
  2. `Camere Active`: numărul de camere create.
  3. `Clienți Conectați`: numărul de utilizatori online în timp real.
  4. `Mesaje Tranzitate`: numărul total de mesaje trimise prin server.
  5. `Fișiere Trimise`: contor de transferuri de fișiere.
* **Tab 1: Camere de Chat:** Tabel detaliat cu numele camerei, descrierea, cine a creat-o și numărul de membri activi. Butoane de creare și ștergere.
* **Tab 2: Clienți Conectați:** Tabel cu numele de utilizator, adresa IP, portul de conexiune, camera în care se află și ora conectării. Permite funcția de **Kick (deconectare forțată cu motiv)**.
* **Tab 3: Jurnal & Istoric Global:** Monitorizează toate pachetele, mesajele de chat, transferurile de fișiere și evenimentele de sistem. Include buton de **Export în fișier `.log`**.
* **Bară de Anunțuri Globale (Broadcast):** Administratorul poate scrie un anunț care este distribuit instant în toate camerele de chat.

---

## 5. 🌐 Rularea în Rețea între mai multe Calculatoare (LAN / Wi-Fi)

Pentru a demonstra funcționarea în rețea locală la facultate sau acasă:

1. **Pe calculatorul care este Server:**
   * Lansați serverul: `./run_server.sh`.
   * Priviți eticheta din dreapta sus: de exemplu **`IP: 192.168.1.45`**.
   * Asigurați-vă că serverul este pornit pe portul `8888`.
2. **Pe calculatoarele Client (din aceeași rețea Wi-Fi sau LAN):**
   * Lansați clientul: `./run_client.sh`.
   * În bara de sus a clientului:
     * La **Host:** introduceți IP-ul serverului (ex: `192.168.1.45`).
     * La **Port:** introduceți `8888`.
     * La **Nume:** introduceți numele vostru (ex: `Mihai`).
     * Apăsați **Conectare**.
3. **Rezultat:** Calculatoarele comunică în timp real: transmit mesaje, fac reply, trimit fișiere mari și navighează prin diferite camere de chat!

---

## 6. 🧪 Testare Automată a Funcționalităților

În proiect este inclusă clasa `TestNetworkChat.java` care rulează un test complet, fără intervenție manuală, verificând automat fiecare cerință din barem:

```bash
cd lab3
java -cp bin TestNetworkChat
```

### Ieșirea testului:
```text
🧪 Pornire teste automate pentru Laboratorul 3 (Chat Rețea)...
[INFO] Serverul a pornit cu succes pe portul 9876 (IP local: 192.168.1.45)
✅ [1/6] Serverul a pornit cu succes pe portul 9876
✅ Client 2 (Bob) conectat!
✅ Client 1 (Alice) conectat!
✅ [2/6] Cerința a: Transmiterea și recepționarea mesajului a funcționat!
✅ [3/6] Cerința c: Răspunsul (Reply) la mesaj a funcționat cu succes! (Citat: @Alice)
✅ [4/6] Cerința e: Crearea camerei #proiect-nou a reușit și a fost notificată!
✅ [5/6] Cerința d: Fișierul "test_file_lab3.txt" (54 B) a fost recepționat intact!
✅ [6/6] Cerința b: Istoricul a fost interogat cu succes (2 mesaje înregistrate)!

🎉 TOATE CELE 5 CERINȚE (a, b, c, d, e) AU FOST TESTATE ȘI VALIDATE CU SUCCES!
```

---

## 7. 🎓 Întrebări Frecvente și Răspunsuri la Susținere (Шпаргалка)

### 1. Cum funcționează comunicarea prin socket-uri în Java?
* **Răspuns:** Serverul creează un `ServerSocket` care ascultă conexiuni TCP pe un port dat. Când un client apelează `new Socket(host, port)`, se stabilește o conexiune bi-direcțională bazată pe stream-uri (`InputStream` și `OutputStream`).

### 2. De ce fiecare client are propriul fir de execuție (Thread)?
* **Răspuns:** Citirea dintr-un socket prin `readObject()` este o operație blocantă (*blocking I/O*). Dacă serverul ar avea un singur thread, blocarea la citirea unui client ar îngheța comunicarea pentru toți ceilalți utilizatori. Prin alocarea unui `ClientHandler` per conexiune, toți clienții comunică concurent și independent.

### 3. De ce este important apelul `out.reset()` la `ObjectOutputStream`?
* **Răspuns:** `ObjectOutputStream` păstrează un tabel intern de referințe pentru obiectele deja serializate pentru optimizare. Dacă transmitem același obiect sau o listă modificată, stream-ul ar trimite doar referința din cache. Apelul `out.reset()` golește cache-ul de serializare, garantând transmiterea conținutului actualizat.

### 4. Cum este garantat faptul că interfața grafică Swing nu îngheață?
* **Răspuns:** Toate operațiile de rețea (conectare, citire din socket, transfer de fișiere) sunt executate pe fire de execuție de fundal (*worker threads*). Când sosesc date noi, actualizarea componentelor vizuale este trimisă pe firul principal grafic al lui Swing (Event Dispatch Thread - EDT) prin `SwingUtilities.invokeLater()`.

### 5. Cum este implementat transferul de fișiere mari?
* **Răspuns:** Fișierul este citit în octeți (`byte[]`), împachetat împreună cu metadatele sale (nume, dimensiune) într-un `NetworkMessage` de tip `FILE_TRANSFER`, și trimis prin rețea. La destinație, recipientul poate alege unde să salveze fișierul prin `JFileChooser`, fiind scris pe disc prin `FileOutputStream`.
