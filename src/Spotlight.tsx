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

  /** Whether taps on the dimmed overlay should pass through to Pressables underneath. */
  allowOverlayClick?: boolean;

  /** Called when the dimmed backdrop outside the cutout is tapped and allowOverlayClick is false. */
  onBackdropPress?: () => void;

  /** Additional style for the zero-size native anchor. Usually not needed. */
  style?: ViewStyle;
}

/**
 * Spotlight
 *
 * Drop-in overlay that highlights a measured view with a native cutout.
 * Pair with useSpotlight() to drive it.
 *
 * Renders a zero-size native anchor in the React tree.
 * The real overlay is mounted natively only while a highlight is active.
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
  allowOverlayClick = false,
  onBackdropPress,
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
      hybridRef={callback(hybridRef)}
      dimOpacity={dimOpacity}
      cornerRadius={cornerRadius}
      padding={padding}
      allowOverlayClick={allowOverlayClick}
      onBackdropPress={callback(onBackdropPress)}
      pointerEvents="none"
      style={[styles.anchor, style]}
    />
  );
}

const styles = StyleSheet.create({
  anchor: {
    position: 'absolute',
    width: 0,
    height: 0,
    opacity: 0,
  },
});
