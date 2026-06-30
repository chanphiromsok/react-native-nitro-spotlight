# react-native-nitro-spotlight — project memory

Read the relevant fix file before touching any listed area.
Full architecture notes: `.claude/skills/react-native-nitro-spotlight-maintainer/SKILL.md`

## Known fixes (read before changing these areas)

| # | Area / symbol | Risk if ignored | Detail |
|---|---------------|-----------------|--------|
| F001 | `windowDpToLocalPx`, `refreshGeometryCache` (Android) | Cutout wrong Y in main activity or sheet | `.claude/fixes/F001-android-coordinate-origin.md` |
| F002 | `getTargetProps`, `useSpotlightTargets`, tour target views | Tour targets invisible, no error | `.claude/fixes/F002-jsx-ref-override.md` |
| F003 | `headerDimView`, `showHeaderDim`, `hideHeaderDim` | Crash "already has a parent" or dim behind dialog | `.claude/fixes/F003-header-dim-race.md` |
| F004 | `ringPaint` init, ref callback null, `animateToRect`, `callback()` in JSX | Compile error, map leak, crash, GC pressure | `.claude/fixes/F004-misc-android-kotlin.md` |
