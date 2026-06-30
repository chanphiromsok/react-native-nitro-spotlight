# react-native-nitro-spotlight — project memory

Read this before making any change. Each invariant records what must be true,
where it is enforced, and what previously broke it (so you know the failure mode,
not just the rule). Full architecture notes live in
`.claude/skills/react-native-nitro-spotlight-maintainer/SKILL.md`.

---

## Invariants

### 1. JSX ref props must never override `getTargetProps` ref callbacks

**Rule**: Do not add an explicit `ref={someRef}` to a view that already has
`{...tour.getTargetProps('id')}` spread on it. In JSX, an explicit `ref` prop
wins over the same prop from a spread — the tour's ref callback is silently
dropped, the target map stays empty, and `highlightById` becomes a no-op.

**Enforced by**: `useSpotlightTargets.ts` — `getTargetProps` returns a
`ref: RefCallback` that must be the view's only ref.

**What broke it before**: `ShapeScreen.tsx` declared `const avatarRef =
useRef()` and then spread `{...getTargetProps('avatar')} ref={avatarRef}`.
The cutout never appeared because the map was empty.

**Fix pattern**: remove standalone refs; use only `getTargetProps`.

---

### 2. Android `windowDpToLocalPx` — two coordinate origins, detect via window token

**Rule**: `measureInWindow` DIP coordinates have different origins depending on
the window the overlay lives in:
- **Main activity window**: origin = `visibleFrame.top` (status-bar height).
  Use `refY = cachedVisibleFrame.top`.
- **Dialog/sheet window** (different `windowToken` from the activity): origin =
  the overlay's own screen y-position.
  Use `refY = cachedOverlayOrigin[1]`.

Detect the dialog case by comparing `windowToken` to the activity's
`decorView.windowToken`, **not** by comparing positions. The position heuristic
(`overlayOrigin > visibleFrame.top`) fires for any overlay below a navigation
header in the main activity and is always wrong there.

**Enforced by**: `SpotlightOverlayView.kt` — `refreshGeometryCache()` sets
`cachedIsDialogWindow`; `windowDpToLocalPx()` picks `refY` from it.

**What broke it before**:
- Original code always used `refY = visibleFrame.top` → BottomSheet cutout
  was placed ~1000 px off-screen above the sheet.
- Position-heuristic fix used `refY = overlayOrigin[1]` for any overlay
  below the status bar → main-activity cutout shifted downward by nav-header
  height (user saw "hole offset y to bottom").

---

### 3. `headerDimView` race — always re-check `parent` before `addView`

**Rule**: `hideHeaderDim()` sets `headerDimAdded = false` synchronously then
posts `removeView` asynchronously. If `showHeaderDim()` fires before the post
runs, `headerDimAdded == false` but the view is still attached to the decor.
Calling `addView` on an already-parented view throws
`"The specified child already has a parent"`.

**Enforced by**: `HybridSpotlightView.kt` — `showHeaderDim()` checks
`if (headerDimView.parent === dv) { headerDimAdded = true; return }` before
calling `addView`.

**What broke it before**: Rapid unmount/remount (e.g. fast navigation) caused
the overlay to accumulate multiple dim strips, then crash on the next highlight.

---

### 4. `headerDimView` must not be added to a different window

**Rule**: When the Spotlight overlay is inside a dialog window (sheet, modal),
the decor-view dim strip must NOT be added to the activity's decor view — it
would render behind the dialog and be invisible. Skip `showHeaderDim()` when
`spotlightView.windowToken != dv.windowToken`.

**Enforced by**: `HybridSpotlightView.kt` — first guard in `showHeaderDim()`.

**What broke it before**: Adding the dim strip to the activity decor while
the overlay was inside a BottomSheetDialogFragment placed it behind the sheet,
doubling the dim on the activity behind the dialog but not the sheet itself.

---

### 5. Kotlin property initializers run in declaration order — no forward refs

**Rule**: A property initializer cannot reference a property declared later in
the same class body. The compiler rejects it as "variable must be initialized".

**Enforced by**: Kotlin compiler.

**What broke it before**: `ringPaint` initializer used `cachedDensity` which
was declared ~50 lines later. Fix: use `resources.displayMetrics.density`
directly in the `ringPaint` init (same value; no forward dependency).

---

### 6. `useSpotlightTargets` map must delete on unmount, not store `null`

**Rule**: The ref callback receives `null` when a view unmounts. Store `null`
with `set(id, null)` and the map entry leaks forever; future `get(id)` returns
`null` and `highlightById` silently no-ops for that id until full remount.
Use `delete(id)` on null.

**Enforced by**: `useSpotlightTargets.ts` — `ref` callback calls
`targetsRef.current.delete(id)` for `null`.

**What broke it before**: Views that unmounted and remounted would stop
receiving highlights because the stale `null` entry shadowed the new ref.

---

### 7. `animateToRect` must re-read `_ref.current` inside the closure

**Rule**: `highlight()` captures the native ref at call time for
`measureInWindow`, then passes the result to `animateToRect`. If `<Spotlight>`
unmounts between the `measureInWindow` callback and `highlightAnimated`, the
stale ref throws or silently fails. Re-read `_ref.current` inside
`animateToRect` and bail if it is null.

**Enforced by**: `useSpotlight.ts` — `animateToRect` reads `_ref.current`
fresh before calling `highlightAnimated`.

**What broke it before**: Fast screen transitions caused a "cannot read
property of null" error on the stale native ref.

---

### 8. `callback()` from nitro-modules must not be called in the render path

**Rule**: Each call to `callback(fn)` allocates a new Nitro HostObject.
Calling it inside JSX (e.g. `hybridRef={callback(hybridRef)}`) allocates 3
new wrappers on every `targetRect` update (every highlight tick), causing
measurable GC pressure and potential stale-closure bugs.

**Enforced by**: `Spotlight.tsx` — `hybridRefCb`, `onBackdropPressCb`, and
`onTargetLayoutCb` are wrapped in `useMemo` so the `callback()` call happens
only when the underlying function identity changes.

**What broke it before**: Render-path `callback()` caused visible frame drops
on devices with limited memory when a tour stepped quickly through targets.

---

## Cross-cutting change checklist

When you change **Android coordinate math** (`windowDpToLocalPx` or
`refreshGeometryCache`), manually verify all three scenarios:
1. Main activity, overlay in React tree below a nav header (most common).
2. Main activity, overlay via Teleport `PortalHost` at root (edge-to-edge,
   Android 15+).
3. Overlay inside a `BottomSheetDialogFragment` / `formSheet`.

When you change **`getTargetProps`** or **`useSpotlightTargets`**, verify
that `useSpotlightTour` still maps step IDs to views correctly.

When you change **`highlightById` or `highlight`**, verify the 0×0 guard
(a zero-size measurement means the target is not yet laid out — do not
forward it to the native layer, which would draw an invisible 0×0 hole).

When you change **`headerDimView` lifecycle** in `HybridSpotlightView`, run
rapid highlight → clear → highlight sequences and confirm no
`"already has a parent"` crash.

When you add a new **Kotlin property** that uses another property's value
in its initializer, ensure the referenced property is declared earlier in
the file. Kotlin initializes class-level properties in textual order.
