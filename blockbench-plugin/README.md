# Plugin Blockbench : AnimaTruc Exportateur

## Fichier
- `animatruc_exporter.js`

## Installation
1. Ouvre Blockbench.
2. Va dans `Fichier -> Plugins -> Charger un plugin depuis un fichier...`.
3. Sélectionne `blockbench-plugin/animatruc_exporter.js`.

## Actions ajoutées
- `Fichier -> Ouvrir .bbmodel pour AnimaTruc`
- `Fichier -> Importer -> Importer .gltf pour AnimaTruc`
- `Animation -> Valider pour AnimaTruc`
- `Animation -> Prévisualiser le pack AnimaTruc`
- `Fichier -> Exporter -> Exporter AnimaTruc`

## Format d'export
- Extension : `.animatrucpack`
- Contenu :
  - os du squelette (nom, parent, pivot, bind pose)
  - cubes du modèle (liaison os + bornes)
  - meshes du modèle (vertices + faces + UV)
  - animations
  - pistes par os : translation / rotation / scale
- La rotation est exportée en quaternions pour usage runtime direct.

## Import runtime (Java)
```java
AnimationAssetPack pack = AnimationAssetImporters.importFromPath(Path.of("my_model.animatrucpack"));
```

## Notes
- Runtime Java recommandé pour le build : Java 17.
- Le plugin exporte uniquement les canaux supportés par AnimaTruc (`translation`, `rotation`, `scale`).
