Descrierea conceptului
=======================
Prima abordare
--------------
Un sistem de localizare bazat pe analiza fluxului video inspirat din modul de funcționare al creierului uman.

Un om este constient de pozitia aproximativa in care se afla facand apel la cunostiintele apriorii despre pozitia anumitor repere, de exemplu ajungand cu metroul in statia Politehnica se poate recunoaste statia deoarece are un aspect specific. Recunoasterea statie ofera o informatie foarte vaga despre pozitionare exacta, se stie doar ca este zona Politehnicii. La coborarea din tren (in urma cu cativa ani) aproape de scari exista un tonomat cu mancare. Recunoasterea tonomatului confera o pozitionare si mai exacta. Urcand pe scarile rulante la mijlocul acestora exista desentata cu marcarul o bucata de arta urbana (cineva s-a semnat pe perete). Recunoasterea acestui detaliu confera o localizare foarte precisa. Exemplul poate continua cu detalii din ce in ce mai fine.

De remarcat ca anumite repere au sens de sinte stataor. De exemplu statia Politehnica ofera localizarea vaga in zona politehnicii insa tonomatul de mancare nu ofera localizarea exacta decat daca este deja presupusa pozitionarea in zona Politehnicii. In caz contrar recunoasterea unui tonomat oarecare, in afara contextului, nu ofera nicio informatie. Acelasi rationament pentru arta urbana de pe scarile rulante. Astfel reperele pe care le urmarim se impart in doua categorii: repere de sine statatoare sau specifice (ex. Statia Politehnica, statia Unirii, cladirea rectoratului s.a.m.d.) sau repere contextuale sau generice (ex. Un tonomat de mancare, un cos de gunoi, o poarta rotativa). Reperele specifice sunt unice, au asociata o unica localizare. Reperele generice pot exista in multe locuri insa au sens numai in contextul in care deja a fost gasit un reper specific.

La prima vedere este o abordare atractiva. Avand la dispozitie memoria unui calculator putem tine intr-o baza de date coordonatele reprelor si genera pozitia cu o acuratete buna. Apar insa probleme.

Principala problema este ca aboradrea necesita o etapa de “invatare”. Pentru a fi o optiune viabila trebuie o invatarea automata asistata de GPS. Invatarea are urmatoarele etape: detectarea automata a posibilelor repre si inregistrarea pozitiei GPS apoi crearea unui clasificator (cascada Haar, cascada LBP, CNN s.a.m.d.) care sa poata recunoaste repreul. Un reper generic poate sa existe de mai multe ori in contextul unui reper specific: la statia Politehnica pot exista doua tonomate de mancare. Nu se poate diferentia intre cele doua decat prin introducerea unui reper intermediar care sa isi extinda contextul doar asupra unui dintre tonomate. Reperele generice isi pot schimba pozitia, pot sa dispara s.a.m.d.

In plus oricat de rafinata ar fi aceasta abordare nu ar putea obtine pozitia mai precis decat un senzor GPS si nu ar putea fi mai eficienta din punctul de vedere al utilizarii bateriei (utilizarea camerei plus procesarea necesara si probabil accesul la retea, deoarece nu se poate salva local toata baza de date cu repere, va consuma posibil mai multa baterie decat GPS-ul).

A doua abordare
----------------
O problema reala intalnita la dispozitivele GPS este pierderea semnalului in zone subterane cum ar fi tuneluri sau parcari. Putem folosi fluxul video pentru a deduce directia de miscare si a deduce pozitia GPS daca ar exista semnal. De asemenea puteam procesa fluxul video si in zone cu semnal GPS pentru a detecta miscarea si a reduce folosirea GPS-ului.

Implementarea propusa este urmatoare: se va identifica in fiecare cadru nu numar de 16 colturi. Se selecta o fereastra 6x6 centrata in fiecare colt. Presupunem ca de la un cadru la altul intensitate colturilor, si a celor 36 vecini, sunt cvasi constante. Se cauta intre ferestrele decpate la un cadru anterior ferastra cea mai apropia pentru fiecare dintre ferestrele de la cadrul curent.
  
Exista posibilitatea ca anumite colturi prezente in cadrul anterior sa nu se mai regaseasca in cadrul curent. Exista, de asemenea, posibilitatea ca unele colturi din cadrul curent sa nu fi existat in cadrul anterior. Avand in vedere aceste doua cazuri de exceptii se considera ca doua colturi din cadre diferite sunt aceleasi numai daca distanta dintre ferestrele care le incadreaza este mai mica decat un prag. Cunoscand corespondenta intre colturi din cadre consecutive se poate calcula deplasarea dx si dy, in pixeli, a fiecarui colt. Media deplasarilor va indica deplasarea efectiva care poate fi corelata cu deplasarea in metrii.

Arhitectura implementarii
===========================
Telefonul mobil preia fluxul video de la camera. Telefonul mobil functioneaza ca server video. Exista un script scris in Python care se conecteaza la telefonul mobil si primeste fluxul video pe care il analizeaza si conform abordarii a 2-a, descrisa mai sus, deduce deplasarea. Pentru a trimite inapoi telefonului datele calculate nu se poate folosi acelasi server ca cel video. Din acest motiv pe telefon mai exista un server pur TCP care asculta portul 9000. Scriptul va trimite pe portul 9000 al telefonului datele calculate, deplasarile dx si dy medii, in pixeli.
Telefonul are acces la GPS si la datele primite de la server. Telefonul primeste de la GPS pozitia in coordonate geografice si poate calcula distanta intre doua puncte succesive. In acelasi timp telefonul “stie” si deplasarea calculata de scriptul Python, in pixeli. Din moment ce telefonul are, pentru moment, acces la ambele surse de informatie poate corela si calibra deplasarea in pixeli, calculata de script, cu deplasarea efectiva, in coordonate. Atunci cand semnalul GPS va disparea telefonul isi va putea calcula pozitia pe baza deplasarii in pixeli si a ultimei pozitii GPS cunoscute.

Instructiuni de rulare a proiectului
=====================================
Proiectul este gazduit public pe GitHub la adresa:
https://github.com/alexnix/image_based_android_location.git
Codul poate fi descarcat prin butonul verde de “Clone or download” -> “Download ZIP”.

Rularea aplicatiei Android
----------------------------
Aplicatia android se afla in (eng.) repository-ul git de la adresa de mai sus.
1. Instalat android studio cu android api 7.1.1
2. A doua bara din programul Android Studio -> Sdk Manager -> Sdk Tools
instalat android sdk tools, google play services, android sdk build tools, android sdk platform tools, Intel x86 emulator accelerator, ConstraintLayout for Android,
Solver for ConstraintLayout, Android Support Repository, Google Repository
3. Deschis proiectul File -> Open... -> selectat proiectul de android unde este descarcat
4. In proiect app -> java -> com.arkconcepts.cameraserve -> TcpServiceHandler -> modificat la linia 29 la "host: " ip-ul cu ip-ul calculatorului pe care ruleaza serverul
5. A doua bara, Run 'app' si selecat dispozitivul mobil pe care va rula aplicatia

Rularea script-ului de procesare OpenCV
-------------------------------------------
Scriptul se afla la urmatoarea cale relativ la baza repository-ului git: CV_script -> “A doua abordare”.

Este necesar Python 3.6.3. Sunt necesare librarile opencv-python, numpy si matplotlib care pot fi instalate utilizand urmatoarea comanda:
```
Python -m pip install opencv-python numpy matplotlib
```
Scriptul de procesare poate fi pornit in doua moduri: in modul de test si in modul (eng.) live. In modul de test va fi analizat fisierul video taxi.mp4 si rezultatele vor fi afisate in terminal. Pentru a trece la cadrul urmator in modul test trebuie apasta o tasta in afara de litera ‘q’, la tastatura. Tasta ‘q’ va inchide programul. Pentru a porni scriptul in modul de test trebuie rulata comanda:
```
Python .\video_proc.py
```
In modul live scriptul are nevoie de un argument. Argumentul necesar este adresa IP a telefonului. Aceasta poate fi aflata din setarile telefonului, de exemplu daca este vorba de WiFi, in meniul de WiFi, efectuand atingere pe reteaua la care este conectat telefonul va aparea un dialog cu informatii precum adresa IP. Folosind aceasta adresa este necesar ca telefonul si calculatorul care ruleaza scriptul sa fie in aceiasi retea. In modul live nu este necesara apasarea vreunei taste pentru a trece la urmatorul cadru; rezultatele sunt afisate atat in termina dar sunt si transmise prin TCP catre portul 9000 al telefonului. Comanda pentru pornire in modul live este:
```
Python .\video_proc.py <IP>
```
