import { getHostComponent, type HybridView } from 'react-native-nitro-modules';
import type { SpotlightMethods, SpotlightProps } from './Spotlight.nitro';
const SpotlightConfig = require('../nitrogen/generated/shared/json/SpotlightViewConfig.json');

export const SpotlightView = getHostComponent<SpotlightProps, SpotlightMethods>(
  'SpotlightView',
  () => SpotlightConfig
);

export type SpotlightRef = HybridView<SpotlightProps, SpotlightMethods>;
