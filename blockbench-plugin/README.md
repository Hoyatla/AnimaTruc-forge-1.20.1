# Plugin Blockbench : AnimaTruc Exportateur

## Fichier
- `animatruc_exporter.js`

## Installation
1. Ouvre Blockbench.
2. Va dans `Fichier -> Plugins -> Charger un plugin depuis un fichier...`.
3. Selectionne `blockbench-plugin/animatruc_exporter.js`.

## Actions ajoutees
- `Fichier -> Ouvrir .bbmodel pour AnimaTruc`
- `Fichier -> Importer -> Importer .gltf pour AnimaTruc`
- `Fichier -> Importer -> Importer AnimaTruc`
- `Animation -> Valider pour AnimaTruc`
- `Animation -> Previsualiser le pack AnimaTruc`
- `Fichier -> Exporter -> Exporter AnimaTruc (legacy)`
- `Fichier -> Exporter -> Exporter AnimaTruc JSON`

## Formats supportes
- Import :
  - `.animatrucpack`
  - `.animatrucpack.json`
  - `.animatruc`
  - `.animatruc.json`
- Export :
  - `.animatrucpack` : export legacy
  - `.animatruc.json` : export JSON prefere pour edition et compatibilite outillage

## Contenu du pack
- os du squelette (nom, parent, pivot, bind pose)
- cubes du modele (liaison os + bornes)
- meshes du modele (vertices + faces + UV)
- animations
- pistes par os : `translation` / `rotation` / `scale`
- metadonnees de texture et de materiau exportees en best-effort a partir du projet Blockbench

## Notes d'import
- les rotations runtime en quaternion sont reconverties en Euler pour Blockbench
- les meshes sont recrees avec vertices/faces/UV
- les textures PNG ne sont pas embarquees dans le pack et restent a fournir separement
- les cubes retrouvent leur bone, mais pas une rotation/cube origin specifique si elle n'etait pas serialisee dans le pack
- Blockbench reste adapte aux workflows cube-centric et aux meshes simples, pas au skinning multi-os dense

## Import runtime (Java)
```java
AnimationAssetPack pack = AnimationAssetImporters.importFromPath(Path.of("my_model.animatruc.json"));
```

## Notes
- Runtime Java recommande pour le build : Java 17.
- Le plugin exporte uniquement les canaux supportes par AnimaTruc (`translation`, `rotation`, `scale`).
- Pour des meshes complexes Forge, preferer Blender -> `.animatruc.json`.
