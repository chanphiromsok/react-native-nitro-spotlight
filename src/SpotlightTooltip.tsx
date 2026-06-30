import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Pressable,
  StyleSheet,
  useWindowDimensions,
  type ViewStyle,
} from 'react-native';
import type { Rect } from './Spotlight.nitro';
import type { SpotlightControls } from './useSpotlight';

export type SpotlightTooltipPlacement = 'above' | 'below' | 'auto';

export interface SpotlightTooltipProps {
  /** Controls from useSpotlight() or tour.spotlight. */
  controls: SpotlightControls;

  /** Tooltip content — fully unstyled, bring your own design. */
  children: ReactNode;

  /**
   * Where to place the tooltip relative to the cutout.
   * 'auto' picks whichever side has more space. Default: 'auto'.
   */
  placement?: SpotlightTooltipPlacement;

  /** Gap in pixels between the cutout edge and the tooltip. Default: 12. */
  gap?: number;

  /** Style applied to the tooltip container. Use for background, border radius, shadow, etc. */
  style?: ViewStyle;

  /**
   * How long to wait (ms) after a highlight starts before fading the tooltip in.
   * Lets the cutout animation travel most of the way before the tooltip appears.
   * Default: 180 (matches 60 % of the default 300 ms spotlight animation).
   */
  showDelay?: number;

  /**
   * Duration (ms) of the tooltip fade-in / fade-out animation. Default: 120.
   */
  fadeDuration?: number;
}

/**
 * SpotlightTooltip
 *
 * Renders tooltip content above the dim overlay. Must be placed as a child
 * of <Spotlight> — React UIView subviews composite above the native
 * CAShapeLayer dim layer automatically, so no hole-punching is needed.
 *
 * Backdrop press is handled by <Spotlight onBackdropPress={...}>.
 *
 * Visible only when controls.targetRect is non-null (i.e. a highlight is active).
 * Fades in after showDelay ms so it appears once the cutout animation has mostly
 * settled. On tour step transitions it fades out, waits for the cutout to travel,
 * then fades in at the new position.
 *
 * @example
 * ```tsx
 * const spotlight = useSpotlight()
 *
 * return (
 *   <>
 *     <YourContent />
 *     <Spotlight controls={spotlight} onBackdropPress={spotlight.clear}>
 *       <SpotlightTooltip controls={spotlight}>
 *         <Text>Here's a tip!</Text>
 *         <Button title="Got it" onPress={spotlight.clear} />
 *       </SpotlightTooltip>
 *     </Spotlight>
 *   </>
 * )
 * ```
 */
export function SpotlightTooltip({
  controls,
  children,
  placement = 'auto',
  gap = 12,
  style,
  showDelay = 180,
  fadeDuration = 120,
}: SpotlightTooltipProps) {
  const { width: screenWidth, height: screenHeight } = useWindowDimensions();
  const { targetRect } = controls;

  // displayRect is what the tooltip is currently positioned at.
  // It lags behind targetRect intentionally so we can animate between states.
  const [displayRect, setDisplayRect] = useState<Rect | null>(null);
  const displayRectRef = useRef<Rect | null>(null);
  const opacity = useRef(new Animated.Value(0)).current;
  const delayTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Use refs so the effect closure always reads current prop values without
  // needing them as deps (which would re-trigger transition logic on prop change).
  const showDelayRef = useRef(showDelay);
  const fadeDurationRef = useRef(fadeDuration);
  showDelayRef.current = showDelay;
  fadeDurationRef.current = fadeDuration;

  useEffect(() => {
    const clearDelay = () => {
      if (delayTimerRef.current) {
        clearTimeout(delayTimerRef.current);
        delayTimerRef.current = null;
      }
    };

    const updateDisplay = (rect: Rect | null) => {
      displayRectRef.current = rect;
      setDisplayRect(rect);
    };

    const fadeIn = (rect: Rect) => {
      updateDisplay(rect);
      opacity.setValue(0);
      delayTimerRef.current = setTimeout(() => {
        Animated.timing(opacity, {
          toValue: 1,
          duration: fadeDurationRef.current,
          useNativeDriver: true,
        }).start();
      }, showDelayRef.current);
    };

    if (!targetRect) {
      // Clear: fade out then unmount
      clearDelay();
      opacity.stopAnimation();
      Animated.timing(opacity, {
        toValue: 0,
        duration: fadeDurationRef.current,
        useNativeDriver: true,
      }).start(({ finished }: { finished: boolean }) => {
        if (finished) updateDisplay(null);
      });
    } else if (displayRectRef.current !== null) {
      // Step transition: quick fade out → reposition → fade in
      clearDelay();
      opacity.stopAnimation();
      Animated.timing(opacity, {
        toValue: 0,
        duration: fadeDurationRef.current * 0.4,
        useNativeDriver: true,
      }).start(({ finished }: { finished: boolean }) => {
        if (finished) fadeIn(targetRect);
      });
    } else {
      // First appearance
      clearDelay();
      fadeIn(targetRect);
    }

    return clearDelay;
  }, [targetRect, opacity]);

  const tooltipStyle = useMemo(() => {
    if (!displayRect) return null;
    const resolved = resolvePlacement(placement, displayRect, screenHeight, gap);
    return computeTooltipStyle(resolved, displayRect, screenWidth, screenHeight, gap);
  }, [displayRect, placement, gap, screenWidth, screenHeight]);

  const noop = useCallback(() => {}, []);

  if (!displayRect || !tooltipStyle) return null;

  return (
    <Animated.View
      style={[styles.tooltip, tooltipStyle, style, { opacity }]}
      pointerEvents="box-none"
    >
      {/* Pressable consumes the touch so it doesn't reach SpotlightView's backdrop handler. */}
      <Pressable onPress={noop}>{children}</Pressable>
    </Animated.View>
  );
}

function resolvePlacement(
  placement: SpotlightTooltipPlacement,
  rect: Rect,
  screenHeight: number,
  gap: number
): 'above' | 'below' {
  if (placement !== 'auto') return placement;
  const spaceBelow = screenHeight - (rect.y + rect.height) - gap;
  const spaceAbove = rect.y - gap;
  return spaceBelow >= spaceAbove ? 'below' : 'above';
}

const TOOLTIP_HORIZONTAL_MARGIN = 16;

function computeTooltipStyle(
  placement: 'above' | 'below',
  rect: Rect,
  screenWidth: number,
  screenHeight: number,
  gap: number
): ViewStyle {
  const maxWidth = screenWidth - TOOLTIP_HORIZONTAL_MARGIN * 2;
  const left = Math.max(
    TOOLTIP_HORIZONTAL_MARGIN,
    Math.min(
      rect.x + rect.width / 2 - maxWidth / 2,
      screenWidth - maxWidth - TOOLTIP_HORIZONTAL_MARGIN
    )
  );
  if (placement === 'below') {
    return { top: rect.y + rect.height + gap, left, maxWidth };
  }
  return { bottom: screenHeight - rect.y + gap, left, maxWidth };
}

const styles = StyleSheet.create({
  tooltip: {
    position: 'absolute',
    // backfaceVisibility avoids a GPU compositing layer flush on some Android drivers
    // when opacity animates; useNativeDriver handles the rest.
    backfaceVisibility: 'hidden',
  },
});
