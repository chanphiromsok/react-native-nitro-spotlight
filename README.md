# react-native-nitro-spotlight

Native spotlight overlay for React Native, powered by [Nitro Modules](https://nitro.margelo.com/). Highlight any measured React Native view with a dimmed native overlay, rounded cutout, and animated transitions.

## Features

- Native Android and iOS overlay
- Animated cutout movement
- Configurable dim opacity, border radius, and padding
- Configurable border width and border color around the spotlight cutout
- Touch pass-through support with `allowOverlayClick`
- Simple hook-based API
- Lower-level `SpotlightView` escape hatch for advanced native wiring

## Installation

```sh
npm install react-native-nitro-spotlight react-native-nitro-modules
```

or:

```sh
yarn add react-native-nitro-spotlight react-native-nitro-modules
```

`react-native-nitro-modules` is required because this library is implemented as a Nitro View.

## Basic usage

Use `useSpotlight()` to create controls, attach a ref to the view you want to highlight, and render `<Spotlight />` once near the root of the screen.

```tsx
import { useRef, type ComponentRef } from 'react';
import { Button, Text, View } from 'react-native';
import { Spotlight, useSpotlight } from 'react-native-nitro-spotlight';

export function Example() {
  const spotlight = useSpotlight();
  const cardRef = useRef<ComponentRef<typeof View>>(null);

  return (
    <View style={{ flex: 1, padding: 24 }}>
      <View ref={cardRef} style={{ padding: 20, borderRadius: 16 }}>
        <Text>This view can be highlighted</Text>
      </View>

      <Button
        title="Highlight card"
        onPress={() => spotlight.highlight(cardRef, { durationMs: 400 })}
      />

      <Button title="Clear" onPress={spotlight.clear} />

      <Spotlight
        spotlightRef={spotlight._ref}
        dimOpacity={0.68}
        borderRadius={22}
        padding={8}
        borderColor="#FFFFFF"
      />
    </View>
  );
}
```

## Touch behavior

By default, the spotlight renders a white border/ring around the cutout and blocks touches on the dimmed backdrop outside the cutout. Touches inside the cutout pass through to the highlighted content.

The `style` prop only applies to the zero-size native anchor view, not the native overlay drawing. Use `borderWidth`, `borderColor`, and `borderRadius` to style the cutout.

Set `borderWidth={0}` to remove the cutout ring:

```tsx
<Spotlight
  spotlightRef={spotlight._ref}
  borderWidth={0}
/>
```

Set `allowOverlayClick` when you want Pressables/buttons underneath the dim overlay to remain clickable:

```tsx
<Spotlight
  spotlightRef={spotlight._ref}
  allowOverlayClick
/>
```

You can also listen for backdrop presses when the overlay is blocking the backdrop:

```tsx
<Spotlight
  spotlightRef={spotlight._ref}
  onBackdropPress={() => spotlight.clear()}
/>
```

> Note: `onBackdropPress` is for blocked backdrop taps. When `allowOverlayClick` is `true`, backdrop touches pass through to the underlying React Native views instead.

## API

### `useSpotlight()`

Returns controls for driving the spotlight.

| Field | Type | Description |
| --- | --- | --- |
| `highlight` | `(viewRef, options?) => void` | Measures a React Native `View` ref and animates the spotlight cutout to it. |
| `clear` | `() => void` | Hides the spotlight overlay. |
| `_ref` | `RefObject` | Internal ref passed to `<Spotlight spotlightRef={spotlight._ref} />`. |

### `highlight(viewRef, options?)`

```ts
spotlight.highlight(viewRef, { durationMs: 300 });
```

Options:

| Option | Type | Default | Description |
| --- | --- | --- | --- |
| `durationMs` | `number` | `300` | Animation duration in milliseconds. |

### `<Spotlight />`

Render one `Spotlight` component for the screen or flow you want to control.

| Prop | Type | Description |
| --- | --- | --- |
| `spotlightRef` | `RefObject<SpotlightRef \| null>` | Ref from `useSpotlight()._ref`. |
| `dimOpacity` | `number` | Opacity of the dim overlay. Omitted values are not sent to native. |
| `borderRadius` | `number` | Border radius of the cutout hole. Omitted values are not sent to native. |
| `padding` | `number` | Extra space around the highlighted view. Omitted values are not sent to native. |
| `borderWidth` | `number` | Width of the border around the cutout. Set to `0` to remove it. Omitted values are not sent to native. |
| `borderColor` | `string` | Color of the border around the cutout. Supports hex colors such as `'#FFFFFF'`. Omitted values are not sent to native. |
| `allowOverlayClick` | `boolean` | Allows touches on the dim overlay to pass through to underlying Pressables/buttons. Omitted values are not sent to native. |
| `onBackdropPress` | `() => void` | Called when the blocked dim backdrop is tapped. |
| `style` | `ViewStyle` | Additional style for the zero-size native anchor. This does not style the native overlay drawing. |

## Advanced: `SpotlightView`

Most apps should use `Spotlight` with `useSpotlight()`. For custom native ref wiring or direct method calls, the lower-level `SpotlightView` is exported as an escape hatch.

```tsx
import { SpotlightView } from 'react-native-nitro-spotlight';
```

## Example app

The example app in [`example/src/App.tsx`](example/src/App.tsx) demonstrates highlighting multiple views, animated transitions, clearing the overlay, and enabling `allowOverlayClick`.

Run the example from the repository root:

```sh
yarn example start
```

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
