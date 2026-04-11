# AnimaTruc Proprietary Core (Clean-Room)

This module is the clean-room runtime baseline for AnimaTruc.

Scope in this stage:
- animation channels and keyframe sampling
- deterministic clip mixer with weighted and additive layers
- adaptive update cadence policy for high-entity-count scenarios
- per-instance runtime with cached pose behavior

Constraints:
- package namespace is `io.hoyatla.animatruc.*`
- no dependency on external animation runtime code
- Java 17 toolchain

This module is intended to be used as the technical base for a fully proprietary runtime line.
