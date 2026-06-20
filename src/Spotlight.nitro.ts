import type {
  HybridView,
  HybridViewProps,
  HybridViewMethods,
} from 'react-native-nitro-modules';

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface SpotlightProps extends HybridViewProps {
  dimOpacity?: number;
  /** Border radius of the cutout hole. */
  borderRadius?: number;
  padding?: number;
  /** Width of the border around the cutout. Set to 0 to remove it. */
  borderWidth?: number;
  /** Color of the border around the cutout. */
  borderColor?: string;
  /** Whether taps on the dimmed overlay should pass through to Pressables underneath. */
  allowOverlayClick?: boolean;
  // Called after native measures the target — JS uses this to position tooltip
  onTargetLayout?: (rect: Rect) => void;
  /** Called when the dimmed backdrop outside the cutout is tapped and allowOverlayClick is false. */
  onBackdropPress?: () => void;
}

export interface SpotlightMethods extends HybridViewMethods {
  highlight(x: number, y: number, width: number, height: number): void;

  highlightAnimated(
    x: number,
    y: number,
    width: number,
    height: number,
    durationMs: number
  ): void;

  clear(): void;
}

export type SpotlightView = HybridView<SpotlightProps, SpotlightMethods>;
