# Proprietary Transition Status

Repository transition to proprietary clean-room line is completed in this branch:
- inherited legacy source modules removed from workspace
- runtime namespace is now exclusively `io.hoyatla.animatruc.*`
- proprietary modules:
  - `animatruc-proprietary-core`
  - `animatruc-forge`

Verification targets:
1. final Forge JAR contains no `software/bernie/*` classes
2. final Forge JAR metadata references only `modId=animatruc`
3. repository license is strict All Rights Reserved
