Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip" -OutFile "gradle.zip"
Expand-Archive -Path "gradle.zip" -DestinationPath ".\gradle_bin" -Force
.\gradle_bin\gradle-8.10.2\bin\gradle.bat run --no-daemon
