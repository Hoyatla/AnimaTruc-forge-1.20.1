# AnimaTruc

AnimaTruc is a proprietary Minecraft animation runtime targeting Forge 1.20.1 with Java 17.

## Modules
- `animatruc-proprietary-core`: clean-room animation runtime (`io.hoyatla.animatruc.*`)
- `animatruc-forge`: Forge integration layer and mod bootstrap

## Runtime Features
- Deterministic blending (weighted + additive)
- Layer system with per-bone masks
- Animation graph runtime with state transitions and fades
- Procedural modifiers (look-at, breathing, two-bone IK)
- Built-in runtime profiling
- Asset import pipeline for `.bbmodel`, `.gltf`, and `.animatruc.json`

## Blockbench Plugin
- Location: `blockbench-plugin/animatruc.blockbench.js`
- Installs two actions in Blockbench:
  - `Export AnimaTruc Pack`
  - `Validate For AnimaTruc`
- Produces `.animatruc.json` packs directly consumable by AnimaTruc importer.

## Build
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :animatruc-forge:build
```

## Artifact
- Forge runtime JAR: `animatruc-forge/build/libs/animatruc-1.20.1-<version>.jar`
