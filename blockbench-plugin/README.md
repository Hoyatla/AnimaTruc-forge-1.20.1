# Blockbench Plugin: AnimaTruc Exporter

## File
- `animatruc_exporter.js`

## Install
1. Open Blockbench.
2. Go to `File -> Plugins -> Load Plugin from File...`.
3. Select `blockbench-plugin/animatruc_exporter.js`.

## Actions Added
- `File -> Open .bbmodel For AnimaTruc`
- `File -> Import -> Import .gltf For AnimaTruc`
- `Animation -> Validate For AnimaTruc`
- `Animation -> Preview AnimaTruc Pack`
- `File -> Export -> Export AnimaTruc Runtime Pack`

## Export Format
- Extension: `.animatrucpack.json`
- Contains:
  - skeleton bones (name, parent, pivot, bind pose)
  - model cubes (bone binding + bounds)
  - animation clips
  - per-bone tracks: translation / rotation / scale
- Rotation is exported as quaternions for direct runtime use.

## Runtime Import (Java)
```java
AnimationAssetPack pack = AnimationAssetImporters.importFromPath(Path.of("my_model.animatrucpack.json"));
```

## Notes
- Recommended Java runtime for build: Java 17.
- The plugin exports only channels supported by AnimaTruc (`translation`, `rotation`, `scale`).
