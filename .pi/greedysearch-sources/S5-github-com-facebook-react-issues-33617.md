---
url: https://github.com/react/react/issues/33617
title: [Compiler Bug]: babel plugin and eslint plugin seemingly inconsistent
source: http
status: 200
chars: 2551
---

### What kind of issue is this?

-   React Compiler core (the JS output is incorrect, or your app works incorrectly after optimization)
-   babel-plugin-react-compiler (build issue installing or using the Babel plugin)
-   eslint-plugin-react-compiler (build issue installing or using the eslint plugin)
-   react-compiler-healthcheck (build issue installing or using the healthcheck script)

### Link to repro

[https://github.com/apollographql/apollo-client/blob/449c3ea07e638f71bbf0923bfca6de901bfc6ec6/src/react/hooks/internal/useDeepMemo.ts](https://github.com/apollographql/apollo-client/blob/449c3ea07e638f71bbf0923bfca6de901bfc6ec6/src/react/hooks/internal/useDeepMemo.ts)

### Repro steps

Take this code:

import { equal } from "@wry/equality";
import type { DependencyList } from "react";
import \* as React from "react";

export function useDeepMemo<TValue\>(
  memoFn: () \=> TValue,
  deps: DependencyList
) {
  const ref \= React.useRef<{ deps: DependencyList; value: TValue }\>(void 0);
  if (!ref.current || !equal(ref.current.deps, deps)) {
    ref.current \= { value: memoFn(), deps };
  }
  return ref.current.value;
}

In ESlint, this does not report any errors - but it should (and did in the past).

I would expect these errors, which I _do_ get during compilation:

```
Error in src/react/hooks/internal/useDeepMemo.ts:10:7 {
  reason: 'Ref values (the `current` property) may not be accessed during render. (https://react.dev/reference/react/useRef)',
  description: null,
  severity: 'InvalidReact',
  suggestions: null
}
Error in src/react/hooks/internal/useDeepMemo.ts:10:29 {
  reason: 'Ref values (the `current` property) may not be accessed during render. (https://react.dev/reference/react/useRef)',
  description: null,
  severity: 'InvalidReact',
  suggestions: null
}
Error in src/react/hooks/internal/useDeepMemo.ts:11:4 {
  reason: 'Ref values (the `current` property) may not be accessed during render. (https://react.dev/reference/react/useRef)',
  description: null,
  severity: 'InvalidReact',
  suggestions: null
}
Error in src/react/hooks/internal/useDeepMemo.ts:13:9 {
  reason: 'Ref values (the `current` property) may not be accessed during render. (https://react.dev/reference/react/useRef)',
  description: null,
  severity: 'InvalidReact',
  suggestions: null
}
```

### How often does this bug happen?

Every time

### What version of React are you using?

"eslint-plugin-react-compiler": "19.1.0-rc.2",

### What version of React Compiler are you using?

"babel-plugin-react-compiler": "19.1.0-rc.2",