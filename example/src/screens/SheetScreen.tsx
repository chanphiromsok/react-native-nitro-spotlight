import { useRef, type ElementRef } from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';
import { Spotlight, useSpotlight } from 'react-native-nitro-spotlight';
import { TooltipCard } from '../components/TooltipCard';
import { SpotlightButton } from '../components/SpotlightButton';
import { spotlightProps, styles } from '../theme/styles';

/**
 * Demonstrates Spotlight inside a form sheet / bottom sheet.
 *
 * Presented with presentation: 'formSheet', which maps to:
 *   iOS  — UISheetPresentationController (floats as a card over the app)
 *   Android — BottomSheetDialogFragment (slides up in its own Dialog window)
 *
 * Both scenarios run the Spotlight overlay inside a separate window from the
 * main activity. The cutout must still land exactly on the highlighted view.
 * On Android, headerDimView is skipped when the overlay's windowToken differs
 * from the activity decor view — the dialog's own scrim covers the backdrop.
 */
export function SheetScreen() {
  const spotlight = useSpotlight();
  const titleRef = useRef<ElementRef<typeof View>>(null);
  const featRef = useRef<ElementRef<typeof View>>(null);
  const actRef = useRef<ElementRef<typeof View>>(null);

  return (
    <View style={sheetStyles.root}>
      <View style={sheetStyles.content}>
        {/* Platform banner */}
        <View style={sheetStyles.banner}>
          <Text style={sheetStyles.bannerText}>
            {Platform.OS === 'ios'
              ? 'iOS · UISheetPresentationController'
              : 'Android · BottomSheetDialogFragment'}
          </Text>
        </View>

        {/* Target A */}
        <View ref={titleRef} style={styles.card}>
          <Text style={styles.cardLabel}>Target A</Text>
          <Text style={styles.cardTitle}>Header row</Text>
          <Text style={styles.cardCopy}>
            Tap "Highlight A" — the cutout should align exactly with this card
            even though the overlay is inside a dialog window.
          </Text>
        </View>

        {/* Target B */}
        <View style={styles.row}>
          <View ref={featRef} style={styles.feature}>
            <Text style={styles.featureIcon}>📐</Text>
            <Text style={styles.featureTitle}>Target B</Text>
            <Text style={styles.featureCopy}>Left feature card.</Text>
          </View>
          <View ref={actRef} style={styles.feature}>
            <Text style={styles.featureIcon}>🎯</Text>
            <Text style={styles.featureTitle}>Target C</Text>
            <Text style={styles.featureCopy}>Right feature card.</Text>
          </View>
        </View>
      </View>

      {/* Actions — pinned to bottom of sheet */}
      <View style={sheetStyles.actions}>
        <View style={styles.actionGrid}>
          <SpotlightButton
            label="Highlight A"
            onPress={() => spotlight.highlight(titleRef, { durationMs: 350 })}
          />
          <SpotlightButton
            label="Highlight B"
            onPress={() => spotlight.highlight(featRef, { durationMs: 350 })}
          />
          <SpotlightButton
            label="Highlight C"
            onPress={() => spotlight.highlight(actRef, { durationMs: 350 })}
          />
        </View>
        <SpotlightButton
          label="Clear"
          variant="secondary"
          onPress={spotlight.clear}
        />
      </View>

      <Spotlight
        controls={spotlight}
        {...spotlightProps}
        onBackdropPress={spotlight.clear}
      >
        {spotlight.targetRect ? (
          <TooltipCard targetRect={spotlight.targetRect}>
            <View style={styles.tooltip}>
              <Text style={styles.tooltipTitle}>Correctly positioned</Text>
              <Text style={styles.tooltipCopy}>
                Cutout coordinates are resolved inside the sheet's own window —
                no offset from the screen origin or status bar.
              </Text>
              <View style={styles.tooltipActions}>
                <SpotlightButton label="Got it" onPress={spotlight.clear} />
              </View>
            </View>
          </TooltipCard>
        ) : null}
      </Spotlight>
    </View>
  );
}

const sheetStyles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#101624',
  },
  content: {
    flex: 1,
    padding: 20,
    paddingTop: 12,
    gap: 14,
  },
  banner: {
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    backgroundColor: '#1B2440',
    borderWidth: 1,
    borderColor: '#2B3658',
    marginBottom: 4,
  },
  bannerText: {
    color: '#8FB7FF',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.4,
  },
  actions: {
    gap: 10,
    paddingHorizontal: 20,
    paddingBottom: 36,
  },
});
