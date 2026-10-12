# TestLab Shop – Cvičenie 03
## Stabilita a udržiavateľnosť GUI testov

**Kompletný Maven projekt v Jave, pripravený na otvorenie v IntelliJ.**
Nie je potrebné spúšťať generátor ani prepisovať zdrojové súbory z odpovede.
Projekt nadväzuje na cvičenie 02 a prezentáciu
`03_Stabilita_a_udrziavatelnost_GUI_testov.pptx`.

Používa Java 21, Selenium 4.49.0, JUnit Jupiter 5.12.2, JUnit BOM a explicitný
`junit-platform-launcher`. Neobsahuje Groovy. Firefox vetva používa konkrétny
`GeckoDriverService.Builder`. Verzie nadväzujú na poslednú konfiguráciu cvičenia 02;
nie je to tvrdenie o najnovších verziách nástrojov.

## 1. Otvorenie v IntelliJ

1. Rozbaľ celý ZIP do nového priečinka. Nemiešaj súbory s cvičením 02.
2. V IntelliJ vyber **Open** a otvor priečinok s týmto `pom.xml` alebo samotný POM.
   Importuj ho ako Maven projekt. Nevytváraj nový Groovy projekt.
3. Nastav **Project SDK = JDK 21**. Pre Maven Runner použi tiež JDK 21.
4. V okne Maven vykonaj **Reload All Maven Projects**. Pri prvom importe sa
   stiahnu závislosti, preto musí fungovať prístup k Maven repozitáru.
5. Pracovný priečinok JUnit konfigurácie musí byť tento koreň projektu,
   v ktorom sú `pom.xml` a `config/`.

Konfigurácia je samostatná a nemení pôvodnú webovú aplikáciu ani cvičenie 02.
ZIP obsahuje zdrojový projekt, nie Maven, JDK, prehliadač ani offline JAR knižnice.

## 2. Backend a tri izolované účty

Pôvodná PHP/Vue aplikácia TestLab Shop 1.0.0 a MariaDB musia byť už dostupné.
Tento balík backend neobsahuje a do databázy priamo nepristupuje.

V **pôvodnom PHP projekte** spusti tri samostatné vytvorenia fixtures:

```powershell
php bin/fixtures.php create
php bin/fixtures.php create
php bin/fixtures.php create
```

Výstupy `workspace_code`, `email` a `password` zapíš do
**`config/test.properties`** v tomto Java projekte:

```properties
base.url=http://127.0.0.1:8080/
browser=chrome
headless=false
timeout.seconds=30
cleanup.order=true

run.1.workspace=SEM_WORKSPACE_1
run.1.email=SEM_EMAIL_1
run.1.password=SEM_PASSWORD_1

run.2.workspace=SEM_WORKSPACE_2
run.2.email=SEM_EMAIL_2
run.2.password=SEM_PASSWORD_2

run.3.workspace=SEM_WORKSPACE_3
run.3.email=SEM_EMAIL_3
run.3.password=SEM_PASSWORD_3
```

**Všetky hodnoty `SEM_...` nahraď skutočnými údajmi z fixtures.** Každé opakovanie
musí mať iný účet a iný `run-*` priestor. Nový prehliadač sám neobnovuje databázu.
Konfigurácia odmietne duplicitné emaily a názvy priestorov; vzťah účtu k deklarovanému
priestoru sama neoveruje cez API. Údaje preto preber z príslušného výstupu fixtures.
Na pracovných priestoroch nespúšťaj súčasne iné testy ani ručné objednávky.

`base.url` je adresa **webu**, nie `/api/index.php`. Podporovaný je aj podpriečinok,
napríklad `https://example.test/testlab/`. Desktopové Selenium nepoužíva androidovú
emulátorovú adresu `10.0.2.2` pre backend bežiaci na tom istom PC.

Pri lokálnom backende môžeš v osobitnom termináli pôvodného PHP projektu spustiť:

```powershell
php -S 127.0.0.1:8080 -t public
```

Heslá nedávaj do verejného repozitára. `config/test.properties` je v `.gitignore`;
`config/test.example.properties` je priložená vzorová kópia.
V cestách v `.properties` používaj `/` alebo escapované `\\`.

## 3. Spustenie testu

V Maven integrácii IntelliJ najskôr spusti cieľ:

```text
test-compile
```

Potom otvor triedu:

```text
src/test/java/sk/testlab/cvicenie03/StableOrderTest.java
```

Zelenou šípkou spusti triedu alebo metódu `referenceOrderRemainsStable()`.
Je to **JUnit test**, nie aplikácia s `main()`.

Metóda používa `@RepeatedTest(3)`. Každé opakovanie má vlastný účet, nový prehliadač,
vlastné artefakty a upratanie. Opakovania sú sekvenčné. Neskorší úspech neskrýva
predchádzajúce zlyhanie. Očakávanie na správnom nasadení je **3 vykonania, 0 failures,
0 errors**; nejde o tvrdenie o tu vykonanom integračnom teste.



## 4. Experiment s oneskorením API

V **súkromnej konfigurácii PHP backendu**, nie v Java properties, nastav:

```php
'app_env' => 'testing',
'fault_mode' => 'none',
'api_delay_ms' => 1200,
```

Nemeň režim počas prebiehajúcej série. Pre novú kontrolovanú sériu priprav nové
pracovné priestory. Test ani tento balík nenastavujú oneskorenie na serveri.
Cieľ je zopakovať scenár 3× aj s oneskorením a vysvetliť jednu odstránenú príčinu
nestability. Tri úspešné vykonania nie sú dôkazom nulovej nestability.

## 5. Scenár a štruktúra

Scenár: prihlásenie → KB-001 × 2 + MS-001 × 1 → STUDENT10 → kuriér → objednávka.
Test očakáva **8563 centov**, viditeľnú sumu **85,63 €**, stav **created**,
platné UUID zodpovedajúce URL a správne množstvá oboch produktov.

```text
src/test/java/sk/testlab/cvicenie03/
  StableOrderTest.java       scenár, assertions, lifecycle a upratanie
  pages/
    BasePage.java            explicitné čakanie a základné UI operácie
    LoginPage.java           prihlásenie
    CatalogPage.java         katalóg, zásoby, pridanie produktov
    CartPage.java            košík, kupón, nový cenový súhrn
    CheckoutPage.java        kontaktné údaje a jediné odoslanie
    OrderPage.java           detail, čítanie hodnôt, bezpečné zrušenie
  support/
    TestConfig.java         načítanie a validácia konfigurácie
    BrowserFactory.java     Chrome / Edge / Firefox
    Artifacts.java          screenshot, DOM, metadata a chyby
```

Lokátory patria do Page Objectov, obchodné assertions do testu. Stránky neuchovávajú
`WebElement` ako pole. `CartPage` si len dočasne odloží starý súhrn, počká na jeho
odpojenie od DOM a potom vyhľadá nový prvok. To zodpovedá Vue GUI TestLab Shop 1.0.0;
pokiaľ sa spôsob vykresľovania aplikácie zmení, treba prispôsobiť aj podmienku.
Čakanie nevyžaduje cenu 8563: nesprávne číslo má odhaliť assertion, nie timeout.

Kód cvičenia 02 už používal explicitné čakanie. Tu sa oddelí od scenára a centralizuje.
`examples/anti-pattern.java.txt` je osobitný zámerne chybný výkladový úryvok;
Maven ho nekompiluje ani nespúšťa.

## 6. Dôkazy a upratanie

Každé opakovanie vytvorí `target/artifacts/run-N-UUID/` s:
- `run.properties`: posledný krok, deklarovaný workspace, zásoby pred testom,
  očakávané a skutočné hodnoty, verzia prehliadača a výsledok upratania;
- `order-id.txt`: UUID získané z vlastného GUI scenára;
- `passed-before-cleanup.*` alebo `failure.png`, `failure.html`, `failure.txt`;
- prípadné `setup-failure.*`, `cleanup-failure.*` a zachytená URL.

Screenshot/DOM sa zachytávajú **pred uprataním a zatvorením prehliadača**.
Pri nedostupnej relácii nemožno screenshot získať; chyba ukladania dôkazu sa zaznamená
bez nahradenia pôvodného zlyhania. DOM maskuje password/hidden hodnoty a uložené texty
redigujú heslo z konfigurácie. Nejde o úplnú anonymizáciu: používaj simulované údaje,
pretože screenshoty a HTML môžu obsahovať ďalšie osobné údaje.

Predvolené `cleanup.order=true` zruší iba objednávku vytvorenú týmto opakovaním,
až po assertions nad stavom `created`. Históriu ani workspace nemaže. Neznáme UUID
sa nehľadá cez „poslednú objednávku“; test žiada ručnú kontrolu vlastného priestoru.
Neurčité odoslanie sa naslepo neopakuje. GUI test nedokazuje vnútornú správnosť
skladovej databázovej transakcie.

Na ponechanie objednávok:

```powershell
mvn test "-Dcleanup.order=false"
```

Potom ich uprac ručne vo vlastnom priestore. Pri Maven behu sú JUnit reporty aj v
`target/surefire-reports/`. Príkaz `mvn clean` vymaže celý `target`, teda aj dôkazy.
