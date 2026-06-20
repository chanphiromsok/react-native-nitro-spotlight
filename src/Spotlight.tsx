import { useCallback, useEffect, useState, type RefObject } from 'react';
import { StyleSheet, type ViewStyle } from 'react-native';
import { callback } from 'react-native-nitro-modules';
import { SpotlightView, type SpotlightRef } from './SpotlightView';

interface SpotlightComponentProps {
  /**
   * The ref object returned by useSpotlight().
   * This is the only required prop.
   */
  spotlightRef: RefObject<SpotlightRef | null>;

  /** Opacity of the dim overlay. Default 0.6 */
  dimOpacity?: number;

  /** Border radius of the cutout hole. Default 16 */
  cornerRadius?: number;

  /** Padding around the target rect. Default 8 */
  padding?: number;

  /** Additional style. Usually not needed since the component fills the screen by default. */
  style?: ViewStyle;
}

/**
 * Spotlight
 *
 * Drop-in overlay that highlights a measured view with a native cutout.
 * Pair with useSpotlight() to drive it.
 *
 * Renders absolutely positioned, filling its parent by default —
 * place it as the last child of your root view so it draws on top.
 *
 * @example
 * ```tsx
 * const spotlight = useSpotlight()
 *
 * return (
 *   <View style={{ flex: 1 }}>
 *     <YourContent />
 *     <Spotlight spotlightRef={spotlight.spotlightRef} />
 *   </View>
 * )
 * ```
 */
export function Spotlight({
  spotlightRef,
  dimOpacity = 0.6,
  cornerRadius = 16,
  padding = 8,
  style,
}: SpotlightComponentProps) {
  const [spotlightInstance, setSpotlightInstance] =
    useState<SpotlightRef | null>(null);

  const hybridRef = useCallback((ref: SpotlightRef | null) => {
    setSpotlightInstance(ref);
  }, []);

  useEffect(() => {
    if (spotlightInstance) {
      spotlightRef.current = spotlightInstance;
    }
    return () => {
      spotlightRef.current = null;
    };
  }, [spotlightInstance, spotlightRef]);

  return (
    <SpotlightView
      pointerEvents="none"
      hybridRef={callback(hybridRef)}
      dimOpacity={dimOpacity}
      cornerRadius={cornerRadius}
      padding={padding}
      style={[styles.fill, style]}
    />
  );
}

const styles = StyleSheet.create({
  fill: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
  },
});
