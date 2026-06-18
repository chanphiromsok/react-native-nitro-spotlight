import { useRef, useCallback } from 'react';
import type { RefObject, ComponentRef } from 'react';
import { View } from 'react-native';
import type { SpotlightView } from './Spotlight.nitro';
import type { SpotlightRef } from './SpotlightView';

// Internal shared ref — <Spotlight> writes here, useSpotlight reads here.
export type SpotlightInstance = RefObject<SpotlightRef | null>;

export interface HighlightOptions {
  /** Animation duration in ms. Default 300. */
  durationMs?: number;
}

export interface SpotlightControls {
  /** @internal — consumed by <Spotlight controls={...}>, not for direct use */
  _ref: SpotlightInstance;

  /**
   * Highlight a view by passing its ref.
   *
   * @example
   * spotlight.highlight(cardRef)
   * spotlight.highlight(cardRef, { durationMs: 500 })
   */
  highlight(
    viewRef: RefObject<ComponentRef<typeof View> | null>,
    options?: HighlightOptions
  ): void;

  /** Clear the spotlight. */
  clear(): void;
}

/**
 * useSpotlight
 *
 * @example
 * ```tsx
 * const spotlight = useSpotlight()
 *
 * return (
 *   <>
 *     <View ref={cardRef}>...</View>
 *     <Button onPress={() => spotlight.highlight(cardRef)} />
 *     <Spotlight controls={spotlight} />
 *   </>
 * )
 * ```
 */
export function useSpotlight(): SpotlightControls {
  const _ref = useRef<SpotlightView | null>(null);

  const highlight = useCallback(
    (
      viewRef: RefObject<ComponentRef<typeof View> | null>,
      { durationMs = 300 }: HighlightOptions = {}
    ) => {
      const instance = _ref.current;
      if (!instance || !viewRef.current) return;

      viewRef.current.measureInWindow((x, y, width, height) => {
        if (width === 0 && height === 0) {
          requestAnimationFrame(() => {
            viewRef.current?.measureInWindow((x2, y2, w2, h2) => {
              instance.highlightAnimated(x2, y2, w2, h2, durationMs);
            });
          });
          return;
        }
        instance.highlightAnimated(x, y, width, height, durationMs);
      });
    },
    []
  );

  const clear = useCallback(() => {
    _ref.current?.clear();
  }, []);

  return { _ref, highlight, clear };
}
