---
url: https://react.dev/reference/eslint-plugin-react-hooks/lints/refs
title: refs – React
source: http
status: 200
chars: 3251
---

Validates correct usage of refs, not reading/writing during render. See the “pitfalls” section in [`useRef()` usage](https://react.dev/reference/react/useRef#usage).

## Rule Details[](#rule-details "Link for Rule Details ")

Refs hold values that aren’t used for rendering. Unlike state, changing a ref doesn’t trigger a re-render. Reading or writing `ref.current` during render breaks React’s expectations. Refs might not be initialized when you try to read them, and their values can be stale or inconsistent.

## How It Detects Refs[](#how-it-detects-refs "Link for How It Detects Refs ")

The lint only applies these rules to values it knows are refs. A value is inferred as a ref when the compiler sees any of the following patterns:

-   Returned from `useRef()` or `React.createRef()`.
    
    ```
    const scrollRef = useRef(null);
    ```
    
-   An identifier named `ref` or ending in `Ref` that reads from or writes to `.current`.
    
    ```
    buttonRef.current = node;
    ```
    
-   Passed through a JSX `ref` prop (for example `<div ref={someRef} />`).
    
    ```
    <input ref={inputRef} />
    ```
    

Once something is marked as a ref, that inference follows the value through assignments, destructuring, or helper calls. This lets the lint surface violations even when `ref.current` is accessed inside another function that received the ref as an argument.

## Common Violations[](#common-violations "Link for Common Violations ")

-   Reading `ref.current` during render
-   Updating `refs` during render
-   Using `refs` for values that should be state

### Invalid[](#invalid "Link for Invalid ")

Examples of incorrect code for this rule:

```
// ❌ Reading ref during renderfunction Component() {const ref = useRef(0);const value = ref.current; // Don't read during renderreturn <div>{value}</div>;}// ❌ Modifying ref during renderfunction Component({value}) {const ref = useRef(null);ref.current = value; // Don't modify during renderreturn <div />;}
```

### Valid[](#valid "Link for Valid ")

Examples of correct code for this rule:

```
// ✅ Read ref in effects/handlersfunction Component() {const ref = useRef(null);useEffect(() => {if (ref.current) {console.log(ref.current.offsetWidth); // OK in effect}});return <div ref={ref} />;}// ✅ Use state for UI valuesfunction Component() {const [count, setCount] = useState(0);return (<button onClick={() => setCount(count + 1)}>{count}</button>);}// ✅ Lazy initialization of ref valuefunction Component() {const ref = useRef(null);// Initialize only once on first useif (ref.current === null) {ref.current = expensiveComputation(); // OK - lazy initialization}const handleClick = () => {console.log(ref.current); // Use the initialized value};return <button onClick={handleClick}>Click</button>;}
```

## Troubleshooting[](#troubleshooting "Link for Troubleshooting ")

### The lint flagged my plain object with `.current`[](#plain-object-current "Link for this heading")

The name heuristic intentionally treats `ref.current` and `fooRef.current` as real refs. If you’re modeling a custom container object, pick a different name (for example, `box`) or move the mutable value into state. Renaming avoids the lint because the compiler stops inferring it as a ref.