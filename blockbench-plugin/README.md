# Blockbench Plugin: AnimaTruc Exporter

## File
- `animatruc.blockbench.js`

## Install
1. Open Blockbench.
2. Go to `File -> Plugins -> Load Plugin from File...`.
3. Select `blockbench-plugin/animatruc.blockbench.js`.

## Actions Added
- `File -> Export -> Export AnimaTruc Pack`
- `Animation -> Validate For AnimaTruc`

## Export Format
- Extension: `.animatruc.json`
- Contains:
  - skeleton bones (name, parent, pivot, bind pose)
  - animation clips
  - per-bone tracks: translation / rotation / scale
- Rotation is exported as quaternions for direct runtime use.

## Runtime Import (Java)
```java
AnimationAssetPack pack = AnimationAssetImporters.importFromPath(Path.of("my_model.animatruc.json"));
```

## Notes
- Recommended Java runtime for build: Java 17.
- The plugin exports only channels supported by AnimaTruc (`translation`, `rotation`, `scale`).
