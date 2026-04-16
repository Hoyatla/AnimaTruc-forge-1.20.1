# AnimaTruc

AnimaTruc is a proprietary Minecraft animation runtime targeting Forge 1.20.1 with Java 17.

## Licensing
- `animatruc-proprietary-core`, `animatruc-forge`, and `animatruc-geckolib-compat`: `All Rights Reserved`
- `animatruc-geckolib-compat/THIRD_PARTY_NOTICES.txt` keeps upstream attribution notices for GeckoLib references.

## Modules
- `animatruc-proprietary-core`: clean-room animation runtime (`io.hoyatla.animatruc.*`)
- `animatruc-forge`: Forge integration layer and mod bootstrap
- `animatruc-geckolib-compat`: bridge addon between official GeckoLib JSON assets and AnimaTruc runtime

## Runtime Features
- Deterministic blending (weighted + additive)
- Layer system with per-bone masks
- Animation graph runtime with state transitions and fades
- Procedural modifiers (look-at, breathing, two-bone IK, multi-limb IK)
- Built-in runtime profiling
- Asset import pipeline for `.bbmodel`, `.gltf`, `.animatruc.json`, and `.animatrucpack.json`
- Unified runtime pack format (`animatruc-pack`) with skeleton + model cubes/meshes + clips
- Forge ground-contact utilities for per-limb block-surface stepping

## Blockbench Plugin
- Location: `blockbench-plugin/animatruc_exporter.js`
- Blockbench actions:
  - `Ouvrir .bbmodel pour AnimaTruc`
  - `Importer .gltf pour AnimaTruc`
  - `Valider pour AnimaTruc`
  - `Previsualiser le pack AnimaTruc`
  - `Exporter AnimaTruc`
- Produces `.animatrucpack.json` (model + animations) directly consumable by AnimaTruc importer.

## Ground Contact IK (Forge)
- Core IK config:
  - `animatruc-proprietary-core/src/main/java/io/hoyatla/animatruc/core/modifier/IkChainDefinition.java`
  - `animatruc-proprietary-core/src/main/java/io/hoyatla/animatruc/core/modifier/MultiLimbIKModifier.java`
- Forge ground solver:
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/ground/GroundContactController.java`
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/ground/GroundContactLimbDefinition.java`
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/ground/GroundContactContextAdapter.java`

```java
GroundContactController ground = GroundContactController.forHumanoid(
        GroundContactLimbDefinition.builder("left_leg", "ik_left_foot", new Vec3f(-2f, 0f, 1f))
                .phaseOffset(0f)
                .stepDistance(0.45f)
                .build(),
        GroundContactLimbDefinition.builder("right_leg", "ik_right_foot", new Vec3f(2f, 0f, 1f))
                .phaseOffset(0.5f)
                .stepDistance(0.45f)
                .build()
);

animator.addModifier(new MultiLimbIKModifier(List.of(
        IkChainDefinition.builder("left_leg", "left_upper_leg", "left_lower_leg", "left_foot", "ik_left_foot")
                .plane(IkPlane.YZ).lengths(5f, 5f).build(),
        IkChainDefinition.builder("right_leg", "right_upper_leg", "right_lower_leg", "right_foot", "ik_right_foot")
                .plane(IkPlane.YZ).lengths(5f, 5f).build()
), false));

Map<String, Vec3f> targets = ground.update(entity, entity.getYRot(), partialTicks);
AnimatorContext context = GroundContactContextAdapter.attachTargets(
        AnimatorContext.visibleNear(),
        targets,
        GroundContactContextAdapter.horizontalSpeed(entity)
);
AnimatorResult frame = animator.update(context, partialTicks);
```

## Auto Presets (2 / 6 / 8 / Myriapod)
- Auto detection API:
  - `animatruc-proprietary-core/src/main/java/io/hoyatla/animatruc/core/preset/LimbAutoDetector.java`
  - `animatruc-proprietary-core/src/main/java/io/hoyatla/animatruc/core/preset/LimbDetectionOptions.java`
  - `animatruc-proprietary-core/src/main/java/io/hoyatla/animatruc/core/preset/LimbDetectionReport.java`
- Forge preset factory:
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/preset/AutoGroundContactPresetFactory.java`
  - `animatruc-forge/src/main/java/io/hoyatla/animatruc/forge/preset/AutoGroundContactPreset.java`
- Detection anti-false-positive strategy:
  - name token scoring (`foot/paw/hoof` + `lower_leg/knee` + `upper_leg/thigh/hip`)
  - hierarchy validation (end -> mid -> root)
  - exclusion filter (`arm`, `hand`, `wing`, `tail`, `head`, `antenna`)
  - geometric sanity checks (bone lengths + lower-body direction)
  - confidence threshold and chain caps

```java
Optional<AutoGroundContactPreset> preset = AutoGroundContactPresetFactory.detect(pack.skeleton());
if (preset.isPresent()) {
    AutoGroundContactPreset resolved = preset.get();
    animator.addModifier(resolved.createIkModifier());
    GroundContactController controller = resolved.createGroundController();
}
```

## Config Menu
- Registered in Forge mod list as `AnimaTruc Presets` (client-side screen).
- Backing config file: `config/animatruc-client.toml`
- Main toggles:
  - auto detection master switch
  - biped / hexapod / octopod / myriapod enable flags
  - ground raycast enable flag
  - performance mode (`FULL`, `BALANCED`, `LIGHT`)
  - detection confidence preset cycle

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
- GeckoLib compat JAR: `animatruc-geckolib-compat/build/libs/compat-animatruc-geckolib-1.20.1-<version>.jar`

## GeckoLib Compat Mode
- Bridge mod id is `animatruc_geckobridge` (distinct addon identity).
- Official GeckoLib stays required as an external dependency.
- The bridge does not embed or repackage `software.bernie.geckolib` classes.
- Runtime flow:
  - animatable -> Gecko resource discovery (`geo/*.geo.json`, `animations/*.animation.json`)
  - import/convert to AnimaTruc pack + cache
  - clip resolve to AnimaTruc `AnimationClip`
  - clean fallback (`null`) + warning if resource/clip conversion fails

## Developer Manuals
- Text manual: `docs/AnimaTruc_Developer_Manual.txt`
- PDF manual: `docs/AnimaTruc_Developer_Manual.pdf`
