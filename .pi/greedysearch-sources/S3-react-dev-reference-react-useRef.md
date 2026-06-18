---
url: https://react.dev/reference/react/useRef
title: useRef – React
source: http
status: 200
chars: 7919
---

`useRef` is a React Hook that lets you reference a value that’s not needed for rendering.

```
const ref = useRef(initialValue)
```

-   [Reference](#reference)
    -   [`useRef(initialValue)`](#useref)
-   [Usage](#usage)
    -   [Referencing a value with a ref](#referencing-a-value-with-a-ref)
    -   [Manipulating the DOM with a ref](#manipulating-the-dom-with-a-ref)
    -   [Avoiding recreating the ref contents](#avoiding-recreating-the-ref-contents)
-   [Troubleshooting](#troubleshooting)
    -   [I can’t get a ref to a custom component](#i-cant-get-a-ref-to-a-custom-component)

* * *

## Reference[](#reference "Link for Reference ")

### `useRef(initialValue)`[](#useref "Link for this heading")

Call `useRef` at the top level of your component to declare a [ref.](https://react.dev/learn/referencing-values-with-refs)

```
import { useRef } from 'react';function MyComponent() {const intervalRef = useRef(0);const inputRef = useRef(null);// ...
```

[See more examples below.](#usage)

#### Parameters[](#parameters "Link for Parameters ")

-   `initialValue`: The value you want the ref object’s `current` property to be initially. It can be a value of any type. This argument is ignored after the initial render.

#### Returns[](#returns "Link for Returns ")

`useRef` returns an object with a single property:

-   `current`: Initially, it’s set to the `initialValue` you have passed. You can later set it to something else. If you pass the ref object to React as a `ref` attribute to a JSX node, React will set its `current` property.

On the next renders, `useRef` will return the same object.

#### Caveats[](#caveats "Link for Caveats ")

-   You can mutate the `ref.current` property. Unlike state, it is mutable. However, if it holds an object that is used for rendering (for example, a piece of your state), then you shouldn’t mutate that object.
-   When you change the `ref.current` property, React does not re-render your component. React is not aware of when you change it because a ref is a plain JavaScript object.
-   Do not write _or read_ `ref.current` during rendering, except for [initialization.](#avoiding-recreating-the-ref-contents) This makes your component’s behavior unpredictable.
-   In Strict Mode, React will **call your component function twice** in order to [help you find accidental impurities.](https://react.dev/reference/react/useState#my-initializer-or-updater-function-runs-twice) This is development-only behavior and does not affect production. Each ref object will be created twice, but one of the versions will be discarded. If your component function is pure (as it should be), this should not affect the behavior.

* * *

## Usage[](#usage "Link for Usage ")

### Referencing a value with a ref[](#referencing-a-value-with-a-ref "Link for Referencing a value with a ref ")

Call `useRef` at the top level of your component to declare one or more [refs.](https://react.dev/learn/referencing-values-with-refs)

```
import { useRef } from 'react';function Stopwatch() {const intervalRef = useRef(0);// ...
```

`useRef` returns a ref object with a single `current` property initially set to the initial value you provided.

On the next renders, `useRef` will return the same object. You can change its `current` property to store information and read it later. This might remind you of [state](https://react.dev/reference/react/useState), but there is an important difference.

**Changing a ref does not trigger a re-render.** This means refs are perfect for storing information that doesn’t affect the visual output of your component. For example, if you need to store an [interval ID](https://developer.mozilla.org/en-US/docs/Web/API/setInterval) and retrieve it later, you can put it in a ref. To update the value inside the ref, you need to manually change its `current` property:

```
function handleStartClick() {const intervalId = setInterval(() => {// ...}, 1000);intervalRef.current = intervalId;}
```

Later, you can read that interval ID from the ref so that you can call [clear that interval](https://developer.mozilla.org/en-US/docs/Web/API/clearInterval):

```
function handleStopClick() {const intervalId = intervalRef.current;clearInterval(intervalId);}
```

By using a ref, you ensure that:

-   You can **store information** between re-renders (unlike regular variables, which reset on every render).
-   Changing it **does not trigger a re-render** (unlike state variables, which trigger a re-render).
-   The **information is local** to each copy of your component (unlike the variables outside, which are shared).

Changing a ref does not trigger a re-render, so refs are not appropriate for storing information you want to display on the screen. Use state for that instead. Read more about [choosing between `useRef` and `useState`.](https://react.dev/learn/referencing-values-with-refs#differences-between-refs-and-state)

#### 

Example

1

of

2:

Click counter[](#click-counter "Link for this heading")

This component uses a ref to keep track of how many times the button was clicked. Note that it’s okay to use a ref instead of state here because the click count is only read and written in an event handler.

import { useRef } from 'react';

export default function Counter() {
  let ref = useRef(0);

  function handleClick() {
    ref.current = ref.current + 1;
    alert('You clicked ' + ref.current + ' times!');
  }

  return (
    <button onClick\={handleClick}\>
      Click me!
    </button\>
  );
}

If you show `{ref.current}` in the JSX, the number won’t update on click. This is because setting `ref.current` does not trigger a re-render. Information that’s used for rendering should be state instead.

### Pitfall

**Do not write _or read_ `ref.current` during rendering.**

React expects that the body of your component [behaves like a pure function](https://react.dev/learn/keeping-components-pure):

[...content trimmed...]

##### Deep Dive

#### How to avoid null checks when initializing useRef later[](#how-to-avoid-null-checks-when-initializing-use-ref-later "Link for How to avoid null checks when initializing useRef later ")

If you use a type checker and don’t want to always check for `null`, you can try a pattern like this instead:

```
function Video() {const playerRef = useRef(null);function getPlayer() {if (playerRef.current !== null) {return playerRef.current;}const player = new VideoPlayer();playerRef.current = player;return player;}// ...
```

Here, the `playerRef` itself is nullable. However, you should be able to convince your type checker that there is no case in which `getPlayer()` returns `null`. Then use `getPlayer()` in your event handlers.

* * *

## Troubleshooting[](#troubleshooting "Link for Troubleshooting ")

### I can’t get a ref to a custom component[](#i-cant-get-a-ref-to-a-custom-component "Link for I can’t get a ref to a custom component ")

If you try to pass a `ref` to your own component like this:

```
const inputRef = useRef(null);return <MyInput ref={inputRef} />;
```

You might get an error in the console:

Console

TypeError: Cannot read properties of null

By default, your own components don’t expose refs to the DOM nodes inside them.

To fix this, find the component that you want to get a ref to:

```
export default function MyInput({ value, onChange }) {return (<inputvalue={value}onChange={onChange}/>);}
```

And then add `ref` to the list of props your component accepts and pass `ref` as a prop to the relevant child [built-in component](https://react.dev/reference/react-dom/components/common) like this:

```
function MyInput({ value, onChange, ref }) {return (<inputvalue={value}onChange={onChange}ref={ref}/>);};export default MyInput;
```

Then the parent component can get a ref to it.

Read more about [accessing another component’s DOM nodes.](https://react.dev/learn/manipulating-the-dom-with-refs#accessing-another-components-dom-nodes)