# Running this project on Windows 11

This is a Java/Spring Boot project. Nothing in the code is Mac/Linux-specific —
Java, Maven, H2, and Docker all work the same way on Windows. The only things
that actually needed changing for you were:

1. **Added the Maven wrapper** (`mvnw.cmd`, `.mvn/wrapper/`) — without this,
   you'd have had to install Maven yourself. Now you only need a JDK.
2. **Nothing else.** No source file, config file, or dependency needs editing
   to run this on Windows. The one file you *might* want to touch is listed
   in "If something doesn't work" below — it's optional, not required.

## 1. Prerequisite: install a JDK (Java 17)

You need Java 17 (not 8, not 21 — the project is built and tested against 17).

- Easiest: install **Eclipse Temurin 17** from https://adoptium.net/temurin/releases/?version=17
  (pick the `.msi` installer for Windows x64) — it adds Java to your PATH automatically.
- Verify it worked — open **Command Prompt** and run:
  ```
  java -version
  ```
  It should print something like `openjdk version "17...."`.

You do **not** need to install Maven separately — step 3 uses the wrapper
(`mvnw.cmd`) bundled in this zip, which downloads the correct Maven version
automatically the first time you run it.

## 2. Extract the zip

Right-click `toll-plaza-finder.zip` → **Extract All...** → pick a folder
(e.g. `C:\Users\<you>\Desktop\toll-plaza-finder`).

Open **Command Prompt** (or PowerShell) and `cd` into that folder:
```
cd C:\Users\<you>\Desktop\toll-plaza-finder
```

## 3. Run it

```
mvnw.cmd spring-boot:run
```

First run will take a minute or two (it downloads Maven itself, then all the
project's dependencies). Wait for a line like:
```
Started TollPlazaApplication in X seconds
```
Leave that window open — that's the running server.

## 4. Try it in a browser

Open:
```
http://localhost:8080/swagger-ui.html
```
Expand `POST /api/v1/toll-plazas` → **Try it out** → paste:
```json
{ "sourcePincode": "560064", "destinationPincode": "411045" }
```
→ **Execute**. You should get back a route + list of toll plazas.

## 5. Run the automated tests (optional)

```
mvnw.cmd test
```
Expect to see `BUILD SUCCESS` and `Tests run: 37, Failures: 0, Errors: 0` near
the bottom.

## Requirements to actually get results back

- **Internet access is required** — the app looks up each pincode's
  coordinates using a free online service (OpenStreetMap Nominatim) the first
  time it sees that pincode. If your machine has no internet access, pincode
  lookups will fail with `"Invalid source or destination pincode"` even for
  real pincodes.
- Windows Firewall may show a one-time popup asking whether `java.exe` can
  access the network the first time you run it — click **Allow**, otherwise
  the geocoding calls above will be blocked.

## If something doesn't work

**"Port 8080 is already in use"** — some other program on your machine is
already using port 8080. Fix: open `src\main\resources\application.yml` in
Notepad, find this line near the top:
```yaml
server:
  port: 8080
```
change `8080` to something else, e.g. `8081`, save, and re-run. (Then use that
port number instead of 8080 in all the URLs above.)

**`'mvnw.cmd' is not recognized...`** — make sure you `cd`'d into the extracted
project folder first (the one containing `mvnw.cmd`, `pom.xml`, `src`), not
its parent folder.

**`'java' is not recognized...`** — the JDK install from step 1 didn't add
Java to your PATH. Close and reopen Command Prompt after installing (PATH
changes only apply to new windows), or reinstall and make sure "Add to PATH"
is checked.
