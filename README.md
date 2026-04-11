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
- Asset import pipeline for `.bbmodel`, `.gltf`, `.animatruc.json`, and `.animatrucpack.json`
- Unified runtime pack format (`animatruc-pack`) with skeleton + model cubes/meshes + clips

## Blockbench Plugin
- Location: `blockbench-plugin/animatruc_exporter.js`
- Blockbench actions:
  - `Ouvrir .bbmodel pour AnimaTruc`
  - `Importer .gltf pour AnimaTruc`
  - `Valider pour AnimaTruc`
  - `Prévisualiser le pack AnimaTruc`
  - `Exporter AnimaTruc`
- Produces `.animatrucpack.json` (model + animations) directly consumable by AnimaTruc importer.

## Forge Loader + Example
- Loader class:
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/pack/AnimaTrucForgePackLoader.java`
- Example bootstrap:
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/example/AnimaTrucExampleBootstrap.java`
- Example pack resource:
  - `animatruc-forge/src/main/resources/assets/animatruc/animatrucpacks/example_humanoid.animatrucpack.json`

## E2E Test
- Plugin-exported pack fixture -> runtime import assertions:
  - `animatruc-proprietary-core/src/test/java/io/hoyatla/animatruc/core/e2e/PluginExportRuntimeE2ETest.java`

## Build
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :animatruc-forge:build
```

## Artifact
- Forge runtime JAR: `animatruc-forge/build/libs/animatruc-1.20.1-<version>.jar`
