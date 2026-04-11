# AnimaTruc

AnimaTruc is a proprietary Minecraft animation runtime targeting Forge 1.20.1 with Java 17.

## Modules
- `animatruc-proprietary-core`: clean-room animation runtime (`io.hoyatla.animatruc.*`)
- `animatruc-forge`: Forge integration layer and mod bootstrap

## Build
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :animatruc-forge:build
```

## Artifact
- Forge runtime JAR: `animatruc-forge/build/libs/animatruc-1.20.1-<version>.jar`
