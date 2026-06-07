# AnimaTruc Blender Rig Guide

## Target
- Minecraft Forge 1.20.1
- Java 17
- CPU-skinned runtime geometry through AnimaTruc

## Recommended Authoring Rules
1. Use a single armature per exported actor.
2. Keep a dedicated `root` bone at world origin.
3. Use deform bones for runtime motion and keep helper bones non-deforming when possible.
4. Apply object transforms before export.
5. Keep rest pose clean and stable. Do not export from a posed scene.
6. Use a maximum of 4 weights per vertex.
7. Normalize all vertex weights.
8. Avoid negative scale on armatures and meshes.
9. Prefer one material per exported mesh object.
10. Keep texture atlases power-of-two (`256`, `512`, `1024`).

## Forge-Compatible Layout
- Rigid parts:
  - parent the mesh object to the target bone, or
  - use a single deform group on the mesh
- Smooth skinning:
  - use an `Armature` modifier
  - keep vertex groups named exactly like deform bones
  - keep no more than 4 effective influences per vertex

## Export Expectations
- Preferred output: `.animatruc.json`
- Legacy accepted: `.animatrucpack` or `.animatrucpack.json`
- The pack contains:
  - skeleton
  - meshes/cubes
  - clips
  - texture/material metadata
- The pack does not embed PNG bytes. Keep textures as separate files in mod assets.

## Minecraft/Forge Constraints
- CPU skinning is more expensive than vanilla cube transforms. Keep triangle counts reasonable.
- Split opaque and translucent meshes when possible.
- Prefer atlas-friendly UVs and stable texture paths.
- Do not assume Blender material graphs will survive to runtime. Only base texture metadata is exported.
- If a mesh uses multiple visual materials, split it into multiple mesh objects before export.

## Recommended Bone Naming
- `root`
- `pelvis`
- `spine`, `chest`, `neck`, `head`
- `left_upper_arm`, `left_lower_arm`, `left_hand`
- `right_upper_arm`, `right_lower_arm`, `right_hand`
- `left_upper_leg`, `left_lower_leg`, `left_foot`
- `right_upper_leg`, `right_lower_leg`, `right_foot`

## Validation Checklist
1. Mesh deforms correctly in Blender rest pose and animated pose.
2. No vertex is left without a valid deform group.
3. UVs exist on every exported mesh that should receive a texture.
4. Texture image paths resolve correctly from the exported pack.
5. Exported actor can be re-imported into Blender without losing bone hierarchy or clip tracks.

## Practical Recommendation
- Use Blockbench for simple cube-centric authoring.
- Use Blender for dense meshes, UV-heavy assets, and multi-bone skinning.
- For Forge delivery, prefer:
  - Blender -> `.animatruc.json`
  - PNG texture alongside the pack
  - runtime loading through `AnimationAssetImporters`
