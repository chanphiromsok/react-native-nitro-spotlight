---
url: https://github.com/floating-ui/floating-ui/discussions/3405
title: `refs.setFloating` triggering React Compiler lint error "Cannot access refs during render" · floating-ui/floating-ui · Discussion #3405
source: http
status: 200
chars: 1414
---

The [docs on `useFloating`](https://floating-ui.com/docs/usefloating#setfloating) recommend setting `ref` to `refs.setFloating` like this:

<div ref\={refs.setFloating}/>

This usage triggers a lint error "Cannot access refs during render" from the new rule [`react-hooks/refs`](https://react.dev/reference/eslint-plugin-react-hooks/lints/refs) that was added for React Compiler.

I wonder, is this a false-positive or is it a bug in this library?

It's a false positive in the lint rule  
`refs` is a plain object and `setFloating` is a state setter (used as a callback ref). No refs are being read in render.

Seems like there's a bug with how refs are inferred: [react/react#34775](https://github.com/react/react/issues/34775)

[View full answer](#discussioncomment-14657647)

It's a false positive in the lint rule  
`refs` is a plain object and `setFloating` is a state setter (used as a callback ref). No refs are being read in render.

Seems like there's a bug with how refs are inferred: [react/react#34775](https://github.com/react/react/issues/34775)

1 reply

[![@silverwind](https://avatars.githubusercontent.com/u/115237?s=60&v=4)](https://github.com/silverwind)

Thanks, I'll disable the rule at that line and wait for a potential fix in the lint rule.

Same lint error happens for

```
ref: elementRef => {
    listItemsRef.current[index] = elementRef
},
```

when using useListNavigation

0 replies