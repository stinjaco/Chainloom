$ErrorActionPreference = "Stop"
$Version = "8.13"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Boot = Join-Path $Root ".gradle-bootstrap"
$Zip = Join-Path $Boot "gradle-$Version-bin.zip"
$Dist = Join-Path $Boot "gradle-$Version"
$Tmp = Join-Path $Boot "wrapper-project"
New-Item -ItemType Directory -Force -Path $Boot | Out-Null
$GradleBat = Join-Path $Dist "bin\gradle.bat"
if (-not (Test-Path $GradleBat)) {
    Write-Host "Downloading official Gradle $Version..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$Version-bin.zip" -OutFile $Zip
    Expand-Archive -Path $Zip -DestinationPath $Boot -Force
}
if (Test-Path $Tmp) { Remove-Item -Recurse -Force $Tmp }
New-Item -ItemType Directory -Force -Path $Tmp | Out-Null
"rootProject.name='wrapper-bootstrap'" | Set-Content -Path (Join-Path $Tmp "settings.gradle") -Encoding ASCII
"tasks.wrapper { gradleVersion = '$Version'; distributionType = Wrapper.DistributionType.BIN }" | Set-Content -Path (Join-Path $Tmp "build.gradle") -Encoding ASCII
Push-Location $Tmp
try {
    & $GradleBat wrapper
    if ($LASTEXITCODE -ne 0) { throw "Gradle wrapper creation failed." }
} finally { Pop-Location }
Copy-Item (Join-Path $Tmp "gradlew") (Join-Path $Root "gradlew") -Force
Copy-Item (Join-Path $Tmp "gradlew.bat") (Join-Path $Root "gradlew.bat") -Force
New-Item -ItemType Directory -Force -Path (Join-Path $Root "gradle\wrapper") | Out-Null
Copy-Item (Join-Path $Tmp "gradle\wrapper\gradle-wrapper.jar") (Join-Path $Root "gradle\wrapper\gradle-wrapper.jar") -Force
Copy-Item (Join-Path $Tmp "gradle\wrapper\gradle-wrapper.properties") (Join-Path $Root "gradle\wrapper\gradle-wrapper.properties") -Force
Write-Host "Gradle wrapper created. Next run: .\gradlew.bat assembleDebug"
