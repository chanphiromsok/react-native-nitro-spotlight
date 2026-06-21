---
name: react-native-nitro-spotlight
description: Use when working on the react-native-nitro-spotlight library or consuming it in an app. Covers Spotlight, useSpotlight, useSpotlightTour, native iOS/Android overlay behavior, touch pass-through semantics, animation hitch avoidance, and docs/examples for this package.
license: MIT
---

# react-native-nitro-spotlight

Native spotlight overlay for React Native New Architecture apps, powered by Nitro Modules.

Use this skill when the user asks to:

- implement or debug `react-native-nitro-spotlight`
- use `Spotlight`, `useSpotlight`, or `useSpotlightTour`
- change iOS/Android overlay behavior
- debug animation hitches, touch pass-through, backdrop presses, or cutout geometry
- update README/docs/examples for this library

## Project facts

- Package name: `react-native-nitro-spotlight`
- Requires React Native New Architecture
- Requires `react-native-nitro-modules`
- Public API is exported from `src/index.tsx`
- Main component: `Spotlight`
- Main hook: `useSpotlight`
- Tour hook: `useSpotlightTour`
- Low-level escape hatch: `SpotlightView`

## Preferred public usage

Prefer the high-level API:

```tsx
const spotlight = useSpotlight();

<Spotlight controls={spotlight} />

spotlight.highlight(targetRef, { durationMs: 400 });
spotlight.clear();
```

Do not recommend direct `SpotlightView` usage unless the user needs custom Nitro ref wiring.

## Tour usage

Use `useSpotlightTour({ steps })` for onboarding flows.

Important rules:

- Keep `steps` stable with `useMemo`.
- Spread `tour.getTargetProps(id)` on each target view.
- Start only after targets are mounted.
- Render `<Spotlight controls={tour.spotlight} />` once near the root.

```tsx
const steps = useMemo(() => [
  { id: 'filter', title: 'Filter', description: 'Filter results.' },
], []);

const tour = useSpotlightTour({ steps });

<View {...tour.getTargetProps('filter')} />
<Spotlight controls={tour.spotlight} />
```

## Touch semantics

Be precise. This is easy to misunderstand.

- Touches inside the cutout pass through to the app.
- By default, backdrop touches are blocked.
- `allowOverlayClick` means backdrop touches pass through to views/buttons underneath.
- `onBackdropPress` still fires when the backdrop is tapped, regardless of `allowOverlayClick`.

Good wording:

> `allowOverlayClick` lets the user click buttons under the dim overlay. It does not disable `onBackdropPress`.

Avoid wording that says `onBackdropPress` only fires when `allowOverlayClick` is false.

## Animation behavior

Repeatedly highlighting the same target can restart path animations and create a visible hitch even when CPU is fine.

Expected behavior:

- Same target while currently animating: ignore duplicate highlight.
- Different target: interrupt and animate to the new target.
- Clear while clear is already animating: ignore duplicate clear.

When profiling this issue:

- Time Profiler may show no CPU hotspot.
- Core Animation is better for visible animation hitches/frame drops.
- Simulator traces include dev-client, Swift metadata, Hermes, and dyld noise.

## Native implementation notes

### iOS

Relevant files:

- `ios/SpotlightOverlayView.swift`
- `ios/HybridSpotlightView.swift`

Current drawing approach:

- `UIBezierPath(roundedRect:cornerRadius:)`
- `CAShapeLayer`
- even-odd fill for dim overlay hole

Avoid variable names that shadow UIKit methods. For example, do not name a local `point` and then call `point(inside:with:)`; it becomes a `CGPoint` shadowing bug.

### Android

Relevant files:

- `android/src/main/java/com/margelo/nitro/spotlight/SpotlightOverlayView.kt`
- `android/src/main/java/com/margelo/nitro/spotlight/HybridSpotlightView.kt`

Current drawing approach:

- `Path.addRoundRect(...)`
- `Paint(Paint.ANTI_ALIAS_FLAG)`
- even-odd overlay path

React Native `measureInWindow` returns DIP coordinates on Android. Convert to local physical pixels before drawing/hit testing.

## Validation checklist

After changes, run at least:

```sh
npx tsc --noEmit
```

For iOS/native changes, also run from the example app:

```sh
npm run build:ios
```

For Android/native changes, run:

```sh
npm run build:android
```

## Docs tone

Docs should be friendly and modern, but not ambiguous. Keep examples copy-pasteable and explain touch behavior clearly.
