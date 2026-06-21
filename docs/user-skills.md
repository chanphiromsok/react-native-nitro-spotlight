# User skills with Spotlight

Use `react-native-nitro-spotlight` to teach users how to do something in your app — not just point at random UI.

Think of each walkthrough as a **skill** your user learns:

- “Find the right item”
- “Save something for later”
- “Create your first project”
- “Invite a teammate”
- “Recover from an empty state”

A good spotlight tour should help the user leave with one clear ability.

## Skill recipe

Every user skill should have:

1. **Goal** — what the user can do after the tour
2. **Trigger** — when to teach it
3. **Steps** — 2–5 focused highlights
4. **Success action** — the user does the thing, not just taps “Next”
5. **Exit** — let them skip or close anytime

```ts
type UserSkill = {
  id: string;
  goal: string;
  trigger: 'first-run' | 'empty-state' | 'feature-discovery' | 'contextual-help';
  steps: Array<{
    id: string;
    title: string;
    description: string;
    durationMs?: number;
  }>;
};
```

## Example: teach filtering

```tsx
import { useMemo } from 'react';
import { Button, Text, View } from 'react-native';
import { Spotlight, useSpotlightTour } from 'react-native-nitro-spotlight';

export function SearchSkill() {
  const filteringSkill = useMemo(
    () => ({
      id: 'filter-results',
      goal: 'User can filter search results',
      trigger: 'empty-state' as const,
      steps: [
        {
          id: 'search-input',
          title: 'Start with a search',
          description: 'Type what you are looking for here.',
        },
        {
          id: 'filter-button',
          title: 'Narrow it down',
          description: 'Use filters to remove results you do not need.',
        },
        {
          id: 'first-result',
          title: 'Open the best match',
          description: 'Tap a result to view details.',
        },
      ],
    }),
    []
  );

  const tour = useSpotlightTour({ steps: filteringSkill.steps });

  return (
    <View style={{ flex: 1, padding: 24 }}>
      <View {...tour.getTargetProps('search-input')}>
        <Text>Search input</Text>
      </View>

      <View {...tour.getTargetProps('filter-button')}>
        <Text>Filter</Text>
      </View>

      <View {...tour.getTargetProps('first-result')}>
        <Text>Result</Text>
      </View>

      <Button title="Teach me filtering" onPress={() => tour.start()} />

      {tour.currentStep && (
        <View style={{ marginTop: 'auto', padding: 16 }}>
          <Text>{tour.currentStep.title}</Text>
          <Text>{tour.currentStep.description}</Text>
          <Button title="Next" onPress={tour.next} />
        </View>
      )}

      <Spotlight
        controls={tour.spotlight}
        dimOpacity={0.68}
        borderRadius={20}
        padding={8}
        onBackdropPress={tour.stop}
      />
    </View>
  );
}
```

## Skill patterns

### First-run skill

Use when the user opens the app for the first time.

Good for:

- navigation basics
- primary action
- account setup

Keep it short. Do not explain every feature.

### Empty-state skill

Use when the user has no content yet.

Good for:

- “Create your first item”
- “Import your first file”
- “Invite your first teammate”

This is usually better than a passive empty-state message.

### Feature-discovery skill

Use when you ship a new feature or detect that a user has not used one.

Good for:

- advanced filters
- saved views
- shortcuts
- collaboration tools

Avoid showing it too often. Teach once, then let the user rediscover it from help.

### Contextual-help skill

Use when the user seems stuck.

Good triggers:

- repeated failed submit
- no interaction for a while
- opening the same screen many times without completing the main action

Do not be annoying. Offer help, do not force it.

## Copywriting rules

Good spotlight copy is short and action-based.

Prefer:

- “Tap Filter to narrow results.”
- “Save this item for later.”
- “Invite a teammate by email.”

Avoid:

- “This is the filter button.”
- “Here you can interact with filtering functionality.”
- Long paragraphs inside tooltips.

## UX rules

- Teach one skill at a time.
- Use 2–5 steps per skill.
- Let users skip.
- Make the final step an action when possible.
- Do not block important buttons unless you mean to.
- Use `allowOverlayClick` when users should be able to tap the app underneath the dim overlay.
- Remember: `onBackdropPress` still fires even when `allowOverlayClick` is true.

## Suggested naming

Use names that describe what the user learns:

```ts
const skills = {
  createFirstProject: [...],
  filterSearchResults: [...],
  saveFavoriteItem: [...],
  inviteTeamMember: [...],
};
```

Avoid generic names like:

```ts
const tour1 = [...];
const onboardingSteps = [...];
```

## Measuring success

A user skill is working if users complete the real action more often.

Track events like:

- skill started
- step viewed
- skill skipped
- skill completed
- target action completed

Example:

```ts
tour.start();
analytics.track('skill_started', { skillId: 'filter-results' });
```

Then compare:

- users who saw the skill
- users who skipped it
- users who completed the target action
