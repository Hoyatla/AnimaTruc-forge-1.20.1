# AnimaTruc Gecko Bridge - Technical Notes

## Goal

Provide a distinct Forge addon that bridges Gecko JSON assets into AnimaTruc runtime clips, while keeping official GeckoLib as an external dependency.

## Identity and Packaging

- Bridge mod id: `animatruc_geckobridge`
- Addon name: `AnimaTruc GeckoLib Bridge`
- GeckoLib is required in `mods.toml` as dependency `modId="geckolib"`.
- Bridge jar must not contain `software/bernie/geckolib/**` classes.

## Runtime Components

- `GeckoResourceLocator`
  - maps animatable -> `(geo.json, animation.json)` resource pair.
  - explicit class mapping + reflection fallback.
- `GeckoAssetImporter`
  - parses Gecko JSON.
  - converts to `AnimationAssetPack` (skeleton + clips).
- `GeckoAssetCache`
  - cache by namespace/model/animation path triplet.
  - clear on resource reload.
- `GeckoClipResolver`
  - resolves clip names from converted packs.
  - returns `null` on unsupported/missing paths.

## Flow

1. `AnimationClipResolverRegistry` asks bridge resolver.
2. Locator resolves model/animation resources.
3. Cache hit -> return pack; miss -> loader + importer.
4. Resolver finds clip by exact/prefixed/suffix name.
5. Missing/invalid resources return `null` fallback, with warning (non-spam).

## Supported Conversion Scope (Current)

- bones by name (+ parent, pivot, bind rotation/scale)
- animation clips by name
- position / rotation / scale keyframes
- interpolation fallback linear or step

## Known Limits

- Not full Gecko behavior parity in this patch (focus on robust simple clips).
- Advanced features are intentionally deferred to keep bridge stable and maintainable.
