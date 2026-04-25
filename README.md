# Taskaholic

JavaFX + Maven desktop app for task posting/booking, with a PowerShell script to build a Windows `.exe` installer.

## 1. Requirements

Install the following before running:

- JDK 25 (LTS) (must include `jpackage`)
- Maven 3.9+
- Windows PowerShell 5.1+ (or PowerShell 7+)

Quick checks:

```powershell
java -version
mvn -version
jpackage --version
```

Expected:

- Java version should be `25.x`
- Maven should be `3.9.x` or newer
- `jpackage` should be available on `PATH`

## 2. Project Setup

Clone and enter project directory:

```powershell
git clone <YOUR_REPO_URL>
cd taskaholic
```

This app uses local JSON text files in project root:

- `users.txt`
- `tasks.txt`

These are already included and are used at runtime.

## 3. Build and Run (Dev)

Compile the project:

```powershell
mvn clean compile
```

Run tests:

```powershell
mvn clean test
```

Run the JavaFX app from Maven:

```powershell
mvn javafx:run
```

## 4. Build Windows .exe (Installer + App Image)

Use the preconfigured script:

```powershell
.\build-installer.ps1
```

What the script does:

1. Runs Maven package + dependency copy
2. Prepares `target/jpackage-input`
3. Builds app image to `dist_image_fixed/`
4. Builds Windows installer `.exe` to `dist_fixed/`

Output files:

- App image executable: `dist_image_fixed/Taskaholic/Taskaholic.exe`
- Installer executable: `dist_fixed/Taskaholic-1.0.exe`

If PowerShell blocks script execution, run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Then run the script again.

## 5. Troubleshooting

- `mvn` not found:
  - Install Maven and reopen terminal.
- `jpackage` not found:
  - Ensure JDK 25 is installed and `%JAVA_HOME%\bin` is in `PATH`.
- JavaFX runtime errors:
  - Use JDK 25 and do a clean build: `mvn clean package`.

## 6. Share With Teammates

After pulling this repository, teammates only need:

1. JDK 25
2. Maven 3.9+
3. PowerShell (for installer build)

Then they can run:

```powershell
mvn clean test
.\build-installer.ps1
```
