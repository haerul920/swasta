param(
    [string]$AvdName = "Medium_Phone_API_36.1",
    [switch]$SkipBuild,
    [switch]$BuildOnly
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
Push-Location $ProjectRoot

try {
    # 1. Configure JDK 21 (Android Studio bundled JBR)
    $JbrPath = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path "$JbrPath\bin\java.exe") {
        $env:JAVA_HOME = $JbrPath
    } elseif ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        throw "JDK tidak ditemukan. Pastikan Android Studio terpasang di '$JbrPath' atau set JAVA_HOME ke JDK 17/21."
    }
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Write-Host "[1/5] Menggunakan JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan

    # 2. Configure Android SDK & verify tools
    if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

    $AdbExe = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    $EmulatorExe = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"

    if (-not (Test-Path $AdbExe)) {
        throw "adb.exe tidak ditemukan di '$AdbExe'. Pastikan Android SDK Platform-Tools sudah terpasang."
    }
    if (-not (Test-Path $EmulatorExe)) {
        throw "emulator.exe tidak ditemukan di '$EmulatorExe'. Pastikan Android Emulator sudah terpasang."
    }

    # 3. Ensure local.properties, .env, and debug.keystore exist
    $LocalPropsPath = Join-Path $ProjectRoot "local.properties"
    if (-not (Test-Path $LocalPropsPath)) {
        $EscapedSdk = $env:ANDROID_HOME -replace "\\", "\\" -replace ":", "\:"
        Set-Content -Path $LocalPropsPath -Value "sdk.dir=$EscapedSdk" -Encoding ASCII
        Write-Host "Dibuat: local.properties" -ForegroundColor Yellow
    }

    $EnvFilePath = Join-Path $ProjectRoot ".env"
    if (-not (Test-Path $EnvFilePath)) {
        $EnvExamplePath = Join-Path $ProjectRoot ".env.example"
        if (Test-Path $EnvExamplePath) {
            Copy-Item $EnvExamplePath $EnvFilePath
        } else {
            Set-Content -Path $EnvFilePath -Value "# Swasana uses Firebase AI Logic (App Check)" -Encoding UTF8
        }
        Write-Host "Dibuat: .env" -ForegroundColor Yellow
    }

    $KeystorePath = Join-Path $ProjectRoot "debug.keystore"
    if (-not (Test-Path $KeystorePath)) {
        $KeytoolExe = Join-Path $env:JAVA_HOME "bin\keytool.exe"
        if (-not (Test-Path $KeytoolExe)) {
            $KeytoolExe = "keytool"
        }
        Write-Host "Membuat debug.keystore..." -ForegroundColor Yellow
        & $KeytoolExe -genkeypair -v -keystore $KeystorePath -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
        if ($LASTEXITCODE -ne 0) {
            throw "Gagal membuat debug.keystore."
        }
    }

    $GradlewBat = Join-Path $ProjectRoot "gradlew.bat"
    if (-not (Test-Path $GradlewBat)) {
        throw "gradlew.bat tidak ditemukan di '$ProjectRoot'."
    }

    # 4. Build Debug APK
    $ApkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not $SkipBuild) {
        Write-Host "[2/5] Membangun APK Debug (assembleDebug)..." -ForegroundColor Cyan
        & $GradlewBat assembleDebug
        if ($LASTEXITCODE -ne 0) {
            throw "Build Gradle gagal dengan exit code $LASTEXITCODE."
        }
    } else {
        Write-Host "[2/5] Melewati build (-SkipBuild)." -ForegroundColor Yellow
    }

    if (-not (Test-Path $ApkPath)) {
        throw "File APK debug tidak ditemukan di '$ApkPath'."
    }

    if ($BuildOnly) {
        Write-Host "Build selesai! APK tersedia di: $ApkPath" -ForegroundColor Green
        exit 0
    }

    # 5. Check attached Android devices / emulator
    Write-Host "[3/5] Mengecek perangkat Android / emulator..." -ForegroundColor Cyan
    & $AdbExe start-server | Out-Null
    $OnlineDevices = @(& $AdbExe devices | Where-Object { $_ -match "^\S+\s+device$" })

    if ($OnlineDevices.Count -eq 0) {
        $AvailableAvds = @(& $EmulatorExe -list-avds)
        if ($AvailableAvds -notcontains $AvdName) {
            throw "AVD '$AvdName' tidak ditemukan. AVD yang tersedia: $($AvailableAvds -join ', ')"
        }

        Write-Host "Menjalankan emulator '$AvdName'..." -ForegroundColor Yellow
        Start-Process -FilePath $EmulatorExe -ArgumentList "-avd", $AvdName | Out-Null

        Write-Host "Menunggu emulator terdeteksi oleh ADB..." -ForegroundColor Cyan
        & $AdbExe wait-for-device

        $MaxWaitSec = 180
        $Waited = 0
        while ($Waited -lt $MaxWaitSec) {
            $BootProp = (& $AdbExe shell getprop sys.boot_completed 2>$null)
            if ($null -ne $BootProp -and $BootProp.Trim() -eq "1") {
                break
            }
            Start-Sleep -Seconds 3
            $Waited += 3
        }
        if ($Waited -ge $MaxWaitSec) {
            throw "Emulator tidak selesai booting dalam $MaxWaitSec detik."
        }
        Write-Host "Emulator siap digunakan." -ForegroundColor Green
    } else {
        Write-Host "Perangkat terdeteksi: $($OnlineDevices[0])" -ForegroundColor Green
    }

    # 6. Install and launch app
    Write-Host "[4/5] Menginstall APK debug ke perangkat..." -ForegroundColor Cyan
    & $AdbExe install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        throw "Gagal menginstall APK ke perangkat."
    }

    Write-Host "[5/5] Menjalankan aplikasi Swasana..." -ForegroundColor Cyan
    & $AdbExe shell am start -W -n id.swasana.app.debug/id.swasana.MainActivity
    if ($LASTEXITCODE -ne 0) {
        throw "Gagal menjalankan MainActivity."
    }

    Write-Host ""
    Write-Host "=== Swasana berhasil dijalankan! ===" -ForegroundColor Green
    Write-Host "Mode Lokal (tanpa Firebase):"
    Write-Host "  - Masuk dengan email apa saja & password min. 8 karakter"
    Write-Host "  - Untuk akses Admin, masuk dengan email: admin@swasana.id (password min. 8 karakter)"
}
finally {
    Pop-Location
}
