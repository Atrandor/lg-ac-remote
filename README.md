# LG Klima – IR-Fernbedienung (Xiaomi IR-Blaster)

Minimale, werbefreie Android-App zur Steuerung einer LG-Klimaanlage über den
eingebauten IR-Blaster (Xiaomi u.a.), basierend auf dem 28-Bit-LG-AC-Protokoll
(Referenz: IRremoteESP8266).

## Bauen
1. Ordner in Android Studio öffnen (File → Open)
2. Gradle-Sync abwarten
3. Handy per USB anschließen (USB-Debugging aktiv) → Run ▶
   oder: Build → Generate Signed App Bundle/APK → APK

## Bedienung
- **EIN/Senden** überträgt den kompletten Zustand (Modus + Temp + Lüfter).
  LG-Klimaanlagen sind zustandsbasiert: jede Übertragung enthält alles.
- **AUS** sendet den festen Off-Code (0x88C0051).
- Temperatur ±, Modus- und Lüfter-Chips senden sofort.

## Wenn die Anlage nicht reagiert
1. **LG2-Timing-Schalter** umlegen (unten). Moderne Geräte nutzen LG2
   (Header 3200/9900 µs), ältere das klassische Timing (8500/4250 µs).
2. Nah ans Gerät (2–4 m), IR-Fenster der Klimaanlage direkt anvisieren.
3. Manche Xiaomi-Modelle drosseln IR im Energiesparmodus.

## Technik
- 28-Bit-Code: Sign(0x88) | Power(2) | Mode(3) | Temp(4) | Fan(4) | Checksum(4)
- Checksumme: Summe der 6 Nibbles über der Checksumme, & 0xF
- Träger: 38 kHz, Android ConsumerIrManager

## Bauen ohne lokale Toolchain (GitHub Actions)
1. Kostenloses GitHub-Konto anlegen, neues **privates** Repository erstellen
2. Alle Projektdateien per Web-Upload hochladen (Drag & Drop, inkl. `.github`-Ordner!)
3. Reiter **Actions** → Workflow "Build APK" → läuft automatisch bei Push
   oder manuell über "Run workflow"
4. Nach ~3–5 Min: Build anklicken → unten unter **Artifacts** die
   `LgAcRemote-debug-apk` herunterladen (ZIP mit app-debug.apk darin)
5. APK aufs Handy übertragen und installieren ("Unbekannte Quellen" erlauben)
