# AnimaTruc GeckoLib Bridge

`animatruc-geckolib-compat` is a bridge addon for Forge 1.20.1.

It is not a GeckoLib fork, not a GeckoLib replacement, and does not ship GeckoLib classes.

## Purpose

- Keep **official GeckoLib** as an external dependency.
- Let **AnimaTruc** discover and convert Gecko resources at runtime:
  - `assets/<ns>/geo/*.geo.json`
  - `assets/<ns>/animations/*.animation.json`
- Resolve converted clips through the AnimaTruc clip resolver registry.

## Architecture

- `GeckoResourceLocator`
  - maps an animatable to `(model, animation)` resources.
  - supports explicit mapping and reflection fallback.
- `GeckoAssetImporter`
  - parses Gecko JSON and converts to `AnimationAssetPack`.
  - initial supported scope: named bones, named clips, rotation/position/scale keyframes, linear/step interpolation.
- `GeckoAssetCache`
  - caches imported packs by `(namespace, model path, animation path)`.
  - avoids reparsing every tick.
- `GeckoClipResolver`
  - resolves `clipName` from cached converted packs.
  - returns `null` on fallback path without crashing.

## Runtime Flow

1. Animatable asks for a clip.
2. Bridge locator resolves Gecko resources.
3. Cache fetches or imports resource pair.
4. Importer converts JSON to AnimaTruc pack.
5. Resolver returns matching `AnimationClip` (or `null` fallback).

## Dependencies

- `minecraft`
- `forge`
- `animatruc`
- `geckolib` (official external mod)

## Known Limits (Current Patch)

- Focuses on robust/simple Gecko animation cases first.
- Does not attempt full Gecko behavior parity for all advanced cases.
- Invalid or unsupported JSON nodes are ignored when possible; unresolved clips fallback to `null`.
