$jdkVersion = "21.0.4"
$mavenVersion = "3.8.9"
$jdkDir = "$PSScriptRoot\jdk"
$mavenDir = "$PSScriptRoot\maven"

# Java 21.0.4 install (This download is specifically 21.0.4+7)
Write-Host "Downloading Java $jdkVersion..."
Invoke-WebRequest -Uri "https://aka.ms/download-jdk/microsoft-jdk-$jdkVersion-windows-x64.zip" -OutFile "$PSScriptRoot\jdk.zip"
Expand-Archive -Path "$PSScriptRoot\jdk.zip" -DestinationPath $jdkDir
Remove-Item "$PSScriptRoot\jdk.zip"

# Maven 3.9.9 install
Write-Host "Downloading Maven $mavenVersion..."
Invoke-WebRequest -Uri "https://dlcdn.apache.org/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip" -OutFile "$PSScriptRoot\maven.zip"
Expand-Archive -Path "$PSScriptRoot\maven.zip" -DestinationPath $mavenDir
Remove-Item "$PSScriptRoot\maven.zip"

# Environment variables (temporary in powershell)
$env:JAVA_HOME = "$jdkDir\jdk-$jdkVersion+7"
$env:MAVEN_HOME = "$mavenDir\apache-maven-$mavenVersion"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

# Build using JavaPackager
Write-Host "Building project..."
mvn package "-DjdkPath=$env:JAVA_HOME" "-Djavafx.platform=win"

# Find zipped Windows build
$winBuild = Get-ChildItem -Path target\*.zip | Select-Object -ExpandProperty FullName
$winBuildName = Split-Path -Leaf $winBuild
$existingBuild = Join-Path -Path $PSScriptRoot -ChildPath $winBuildName

# Remove existing Windows build zip if it exists
if (Test-Path $existingBuild) {
    Write-Host "Removing existing build: $existingBuild"
    Remove-Item -Force $existingBuild
}

# Move new Windows build to repo root
Write-Host "Moving final build..."
Move-Item -Path $winBuild -Destination $PSScriptRoot

# Remove Java, Maven, and unwanted build output
Write-Host "`nCleaning up..."
Remove-Item -Recurse -Force $jdkDir
Remove-Item -Recurse -Force $mavenDir
Remove-Item -Recurse -Force target

Write-Host "`nFinished!"
