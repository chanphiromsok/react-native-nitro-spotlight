# react-native-nitro-spotlight

Native spotlight overlay for React Native, powered by Nitro Modules. Highlight any measured React Native view with a dimmed overlay and animated cutout.

## Installation

```sh
npm install react-native-nitro-spotlight react-native-nitro-modules
```

`react-native-nitro-modules` is required because this library relies on [Nitro Modules](https://nitro.margelo.com/).

## Usage

Use the `useSpotlight` hook to create controls, attach refs to views you want to highlight, and render the `Spotlight` component as the last child of your screen so it appears above your content.

```tsx
import { useRef } from 'react';
import { Button, View, type ComponentRef } from 'react-native';
import { Spotlight, useSpotlight } from 'react-native-nitro-spotlight';

export function Example() {
  const spotlight = useSpotlight();
  const cardRef = useRef<ComponentRef<typeof View>>(null);

  return (
    <View style={{ flex: 1 }}>
      <View ref={cardRef}>{/* content to highlight */}</View>

      <Button
        title="Highlight card"
        onPress={() => spotlight.highlight(cardRef, { durationMs: 400 })}
      />
      <Button title="Clear" onPress={spotlight.clear} />

      <Spotlight
        spotlightRef={spotlight._ref}
        dimOpacity={0.68}
        cornerRadius={22}
        padding={8}
      />
    </View>
  );
}
```

## API

### `useSpotlight()`

Returns controls for driving the spotlight.

- `highlight(viewRef, options?)` — measures the view and animates the cutout to it.
- `clear()` — hides the spotlight.
- `_ref` — internal ref passed to `<Spotlight />`.

### `<Spotlight />`

Drop-in overlay component. Render it near the root of the screen, usually last.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `spotlightRef` | `RefObject` | required | Ref from `useSpotlight()._ref`. |
| `dimOpacity` | `number` | `0.6` | Overlay opacity. |
| `cornerRadius` | `number` | `16` | Cutout corner radius. |
| `padding` | `number` | `8` | Extra space around the highlighted view. |
| `style` | `ViewStyle` | fills parent | Additional overlay style. |

### Advanced: `SpotlightView`

For custom native ref wiring, you can use the lower-level `SpotlightView` export directly. Most apps should prefer `Spotlight` with `useSpotlight`.

## Example app

The example app in `example/src/App.tsx` demonstrates highlighting multiple views and clearing the overlay.

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
