import { StatusBar } from 'expo-status-bar';
import { useRef, type ElementRef } from 'react';
import { Pressable, StyleSheet, Text, ToastAndroid, View } from 'react-native';
import { Spotlight, useSpotlight } from 'react-native-nitro-spotlight';

// ─── App ──────────────────────────────────────────────────────────────────────

export default function App() {
  const spotlight = useSpotlight();
  const startRef = useRef<ElementRef<typeof View>>(null);
  const featureRef = useRef<ElementRef<typeof View>>(null);
  const finishRef = useRef<ElementRef<typeof View>>(null);

  return (
    <View style={styles.screen}>
      <StatusBar style="light" />

      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.eyebrow}>react-native-nitro-spotlight</Text>
          <Text style={styles.title}>SpotlightView example</Text>
          <Text style={styles.subtitle}>
            Tap a step to move the native spotlight overlay between measured
            React Native views.
          </Text>
        </View>

        <View style={styles.card} ref={startRef}>
          <Text style={styles.cardLabel}>Step 1</Text>
          <Text style={styles.cardTitle}>Point at any view</Text>
          <Text style={styles.cardCopy}>
            The example measures a regular React Native view and sends its
            window rect to SpotlightView.
          </Text>
        </View>

        <View style={styles.row}>
          <View style={styles.feature} ref={featureRef}>
            <Text style={styles.featureIcon}>✨</Text>
            <Text style={styles.featureTitle}>Animated cutout</Text>
            <Text style={styles.featureCopy}>
              Moves with highlightAnimated.
            </Text>
          </View>

          <View style={styles.feature} ref={finishRef}>
            <Text style={styles.featureIcon}>🎯</Text>
            <Text style={styles.featureTitle}>Clear anytime</Text>
            <Text style={styles.featureCopy}>
              Calls the native clear method.
            </Text>
          </View>
        </View>

        <View style={styles.actions}>
          <SpotlightButton
            label="Highlight intro"
            onPress={() => spotlight.highlight(startRef, { durationMs: 400 })}
          />
          <SpotlightButton
            label="Highlight feature"
            onPress={() => spotlight.highlight(featureRef, { durationMs: 400 })}
          />
          <SpotlightButton
            label="Highlight clear"
            onPress={() => spotlight.highlight(finishRef, { durationMs: 400 })}
          />
          <SpotlightButton
            label="Clear"
            variant="secondary"
            onPress={spotlight.clear}
          />
        </View>
      </View>

      <Spotlight
        spotlightRef={spotlight._ref}
        dimOpacity={0.68}
        cornerRadius={22}
        padding={8}
        allowOverlayClick
      />
    </View>
  );
}

// ─── SpotlightButton ──────────────────────────────────────────────────────────

function SpotlightButton({
  label,
  onPress,
  variant = 'primary',
}: {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary';
}) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={() => {
        onPress();
        ToastAndroid.show(`${label},`, 1000);
      }}
      style={({ pressed }) => [
        styles.button,
        variant === 'secondary' && styles.secondaryButton,
        pressed && styles.pressedButton,
      ]}
    >
      <Text
        style={[
          styles.buttonText,
          variant === 'secondary' && styles.secondaryButtonText,
        ]}
      >
        {label}
      </Text>
    </Pressable>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#101624',
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 72,
    paddingBottom: 32,
  },
  header: {
    gap: 10,
    marginBottom: 28,
  },
  eyebrow: {
    color: '#8FB7FF',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 34,
    fontWeight: '800',
    letterSpacing: -0.8,
  },
  subtitle: {
    color: '#B8C2D8',
    fontSize: 16,
    lineHeight: 23,
  },
  card: {
    gap: 8,
    padding: 22,
    borderRadius: 24,
    backgroundColor: '#F5F8FF',
  },
  cardLabel: {
    color: '#4263EB',
    fontSize: 13,
    fontWeight: '800',
    textTransform: 'uppercase',
  },
  cardTitle: {
    color: '#111827',
    fontSize: 23,
    fontWeight: '800',
  },
  cardCopy: {
    color: '#475569',
    fontSize: 15,
    lineHeight: 22,
  },
  row: {
    flexDirection: 'row',
    gap: 14,
    marginTop: 16,
  },
  feature: {
    flex: 1,
    minHeight: 156,
    gap: 8,
    padding: 18,
    borderRadius: 22,
    backgroundColor: '#1B2440',
    borderWidth: 1,
    borderColor: '#2B3658',
  },
  featureIcon: { fontSize: 28 },
  featureTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '800',
  },
  featureCopy: {
    color: '#B8C2D8',
    fontSize: 14,
    lineHeight: 20,
  },
  actions: {
    gap: 12,
    marginTop: 'auto',
  },
  button: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 50,
    borderRadius: 16,
    backgroundColor: '#8FB7FF',
  },
  secondaryButton: {
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: '#3B4768',
  },
  pressedButton: {
    opacity: 0.72,
    transform: [{ scale: 0.98 }],
  },
  buttonText: {
    color: '#0B1020',
    fontSize: 16,
    fontWeight: '800',
  },
  secondaryButtonText: { color: '#F5F8FF' },
});
