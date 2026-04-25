[CmdletBinding()]
param(
	[string]$Name = "Taskaholic",
	[string]$Version = "1.0",
	[string]$MainJar = "taskaholic-1.0-SNAPSHOT-all.jar",
	[string]$MainClass = "com.taskaholic.TaskaholicApp",
	[string]$IconPath = "src/main/resources/taskaholic-logo.ico"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$stageDir = Join-Path $root "target/jpackage-input"
$appImageDir = Join-Path $root "dist_image_fixed"
$installerDir = Join-Path $root "dist_fixed"

function Invoke-Checked {
	param([scriptblock]$Command, [string]$Step)
	& $Command
	if ($LASTEXITCODE -ne 0) {
		throw "$Step failed with exit code $LASTEXITCODE"
	}
}

Write-Host "==> Building project"
Invoke-Checked { mvn clean package dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=target/dependency" } "Maven build"

Write-Host "==> Preparing jpackage input"
if (Test-Path $stageDir) {
	Remove-Item $stageDir -Recurse -Force
}
New-Item -ItemType Directory -Path $stageDir | Out-Null

Copy-Item (Join-Path $root "target/$MainJar") $stageDir

# Keep runtime jars needed by JavaFX modules.
Copy-Item (Join-Path $root "target/dependency/javafx-*.jar") $stageDir

# Ensure packaged app includes data files next to the app jar.
$usersFile = Join-Path $root "users.txt"
$tasksFile = Join-Path $root "tasks.txt"
if (-not (Test-Path $usersFile)) {
	Set-Content -Path $usersFile -Value "[]" -NoNewline
}
if (-not (Test-Path $tasksFile)) {
	Set-Content -Path $tasksFile -Value "[]" -NoNewline
}
Copy-Item $usersFile $stageDir -Force
Copy-Item $tasksFile $stageDir -Force

if (Test-Path $appImageDir) {
	Remove-Item $appImageDir -Recurse -Force
}
if (Test-Path $installerDir) {
	Remove-Item $installerDir -Recurse -Force
}

$jpackageCommon = @(
	"--name", $Name,
	"--app-version", $Version,
	"--input", $stageDir,
	"--main-jar", $MainJar,
	"--main-class", $MainClass,
	"--java-options", "--module-path` `$APPDIR",
	"--java-options", "--add-modules javafx.controls,javafx.fxml"
)

if (Test-Path (Join-Path $root $IconPath)) {
	$jpackageCommon += @("--icon", (Join-Path $root $IconPath))
}

Write-Host "==> Creating app-image"
Invoke-Checked { jpackage --type app-image --dest $appImageDir @jpackageCommon } "jpackage app-image"

Write-Host "==> Creating installer exe"
Invoke-Checked { jpackage --type exe --dest $installerDir @jpackageCommon } "jpackage exe"

Write-Host ""
Write-Host "Done. Artifacts:"
Write-Host "  App image : $appImageDir/$Name/$Name.exe"
Write-Host "  Installer : $installerDir/$Name-$Version.exe"
