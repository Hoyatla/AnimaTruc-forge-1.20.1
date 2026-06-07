# AnimaTruc Blender Addon

Fichier addon: `animatruc_blender_io.py`

## Scope
- export direct `Blender -> .animatruc.json`
- import `animatruc(.json|pack) -> Blender`
- preserve les faces polygonales et les UVs du mesh Blender
- exporte les `Action` Blender en clips AnimaTruc
- importe les clips en `Action` Blender
- exporte et reimporte les metadonnees de texture/materiau
- supporte les meshes rigides et les meshes skinnes multi-os

## Contraintes actuelles
- les `Armature Modifier` sont exportes comme skin runtime AnimaTruc a partir du mesh de repos
- les PNG restent des fichiers separes, seuls les chemins/metadonnees sont serialises
- les cubes du pack sont importes comme meshes Blender simples
- une seule matiere runtime par mesh exporte est recommandee

## Recommandation de rig
- utiliser une armature
- creer un bone `root`
- attacher chaque piece rigide a un os ou utiliser des groupes de poids
- limiter a 4 influences par vertex
- eviter les scales negatifs et appliquer les transforms avant export

## Installation
1. Blender -> `Edit > Preferences > Add-ons > Install...`
2. Choisir `animatruc_blender_io.py`
3. Activer `AnimaTruc Blender I/O`

## Entrees UI
- `File > Import > AnimaTruc Pack (.animatruc.json)`
- `File > Export > AnimaTruc Pack (.animatruc.json)`
- `View3D > Sidebar > AnimaTruc`

## Options export
- `Pack Unit Scale`: multiplie les coordonnees Blender avant ecriture
- `Ticks Per Second`: cadence des clips exportes
- `Selection Only`: exporte uniquement la selection
- `Apply Modifiers`: applique les modificateurs non-armature
- `Triangulate Meshes`: triangule les polygones avant ecriture
- `Export Actions`: exporte les actions Blender en clips

## Options import
- `Pack Unit Scale`: divise les coordonnees du pack a l'import
- `Ticks Per Second`: cadence utilisee pour reconstruire les actions
- `Create Actions`: recree les clips comme actions Blender
- `Create Collection`: range l'import dans une collection dediee
