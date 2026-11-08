# Muzz Chat — Design System

> Visual reference: `design-system.html`

Components never write a colour, size, shape, opacity, scale or duration inline. Each value is declared once, as close as possible to what uses it:
- A value used by one component is a private constant in that component's file.
- A value that several components use, or are likely to, is a token in `ui/theme`: `Color.kt`, `Typography`, `Spacing`, `Dimens`, `ComponentShapes`, `Alpha` and `Scale`.
- Colours and text styles are always tokens.

Every value is listed here, in a token table or in its component's section.

---

## Colours

### Tier 1 — Primitives
| Token | Hex | Notes |
|---|---|---|
| Pink500 | #E8175D | Muzz brand pink |
| Pink700 | #C01048 | Pressed state of Pink500 |
| Neutral900 | #1A1A1A | Near-black |
| Neutral500 | #9E9E9E | Mid grey |
| Neutral300 | #BDBDBD | Disabled text / icons, pressed border |
| Neutral200 | #F2F2F2 | Light grey |
| Neutral100 | #F5F5F5 | Near-white surface |
| Neutral50 | #E0E0E0 | Border / divider |
| White | #FFFFFF | Pure white |

### Tier 2 — Semantic roles
| Role | → Primitive | Usage |
|---|---|---|
| colorPrimary | Pink500 | Brand CTAs, send button, sent bubble bg |
| colorPrimaryPressed | Pink700 | Send button pressed state |
| colorPrimaryDisabled | Neutral200 | Send button disabled bg |
| colorBackground | White | Screen / scaffold background |
| colorSurface | White | Top bar, input bar surface |
| colorSurfaceVariant | Neutral200 | Received bubble bg |
| colorInputBackground | Neutral100 | TextInput field background |
| colorBorder | Neutral50 | Dividers, input bar top border, secondary and floating button border |
| colorBorderPressed | Neutral300 | Secondary and floating button border when pressed |
| textPrimary | Neutral900 | Message text on received bubbles, top bar |
| textSecondary | Neutral500 | Section headers, timestamps, placeholder |
| textDisabled | Neutral300 | Disabled input text, disabled button icon |
| textOnPrimary | White | Text/icons on pink (sent bubble, send button) |

---

## Typography
| Style | Size | Line height | Weight | Role mapping |
|---|---|---|---|---|
| titleMedium | 16sp | 20sp | SemiBold | textPrimary — TopBar title |
| bodyLarge | 16sp | 22sp | Regular | textPrimary — message body |
| bodyMedium | 14sp | 20sp | Regular | textPrimary — message body (compact), TextInput text |
| bodySmall | 12sp | 16sp | Regular | textSecondary — timestamps |
| labelLarge | 14sp | 20sp | SemiBold | textSecondary — button labels; textOnPrimary — Avatar initial |
| labelSmall | 11sp | 16sp | SemiBold, 0 tracking | textSecondary — section headers |

---

## Spacing
| Token | Value | Usage |
|---|---|---|
| xs | 4dp | Icon gaps, tight nudges |
| sm | 8dp | Inner padding, small gaps |
| md | 16dp | Screen horizontal margin |
| lg | 24dp | Section spacing |
| xl | 32dp | Large layout gaps |

---

## Dimens
| Token | Value | Usage |
|---|---|---|
| buttonPrimarySize | 44dp | Primary and floating button diameter |
| iconSize | 18dp | Icons inside buttons, TopBar back icon |
| borderWidth | 1.5dp | Secondary and floating button border |
| focusRingWidth | 2dp | TextInput focus ring |
| dividerWidth | 1dp | ChatInput top border, TopBar bottom border |

---

## Alpha
| Token | Value | Usage |
|---|---|---|
| disabled | 0.6 | Whole component, when disabled (secondary and floating button, TextInput) |
| focusRing | 0.25 | colorPrimary, as the TextInput focus ring |

---

## Scale
| Token | Value | Usage |
|---|---|---|
| buttonPrimaryPressed | 0.95 | Primary button, while pressed |
| buttonSecondaryPressed | 0.97 | Secondary button, while pressed |
| buttonFloatingPressed | 0.95 | Floating button, while pressed |

---

## Shapes
| Component | Corner radius | Token |
|---|---|---|
| MessageBubble | 18dp all corners; tail corner 4dp | Private to `MessageBubble` |
| TextInput | 24dp (pill) | ComponentShapes.textInput |
| Button (primary) | Circle | ComponentShapes.buttonPrimary |
| Button (secondary) | 20dp (pill) | ComponentShapes.buttonSecondary |
| Button (floating) | Circle | ComponentShapes.buttonFloating |

---

## Icons
Icons are vector drawables in `res/drawable`, loaded with `ImageVector.vectorResource`. They are drawn in the colour the component tints them with.

| Drawable | Usage |
|---|---|
| ic_send | Send button |
| ic_reply | "Reply as Sarah" button |
| ic_arrow_down | "Jump to latest" button |
| ic_back | TopBar back button |

---

## Components

### Avatar
A circle with the initial of a name. Takes `name: String`. Its size is private to `Avatar.kt`.
- 36dp circle in colorPrimary
- The name's first character, uppercased, centred, in `labelLarge`
- Single size and state. Every Avatar has a name, so there is no placeholder

| State | Size | bg | text |
|---|---|---|---|
| Default | 36dp | colorPrimary | textOnPrimary |

---

### TopBar
Composes a back button, `Avatar` and a title. Takes `title`, `onBack` and `backInteractionSource`. The screen remembers the source, and the back button reads its pressed state from it. Its values are private to `TopBar.kt`.
- Bar: colorSurface, padding `Spacing.sm` horizontal and `Spacing.xs` vertical, then a `dividerWidth` colorBorder divider along its bottom. The row is 56dp tall
- Back button: `ic_back` at `iconSize` in textPrimary, centred in a 36dp circle, in a 48dp touch target. There is no ripple: while pressed, the circle is colorInputBackground. Content description: `R.string.back` ("Back")
- `Avatar(title)` sits `Spacing.xs` after the back button's touch target, and the title `Spacing.sm` after the Avatar
- Title: `titleMedium` in textPrimary, filling the rest of the row, on one line, ellipsized when too long
- The screen applies the status bar inset

| State | bg | back button |
|---|---|---|
| Default | colorSurface | colorSurface circle, ic_back textPrimary |
| Back pressed | colorSurface | colorInputBackground circle, ic_back textPrimary |

---

### Button
Generic action button. The caller passes a `MutableInteractionSource`, and the button reads its pressed state from it, so it holds no state of its own. The scale animates with `animateFloatAsState`. There is no ripple.

**variant = primary** (circle icon button)
- Size `buttonPrimarySize`, shape `ComponentShapes.buttonPrimary`, icon `iconSize`
- Shows `icon` only. `label` is the icon's content description

| State | bg | icon |
|---|---|---|
| Default | colorPrimary | textOnPrimary |
| Pressed | colorPrimaryPressed | textOnPrimary, scale 0.95 |
| Disabled | colorPrimaryDisabled | textDisabled |

**variant = secondary** (outlined pill)
- Shape `ComponentShapes.buttonSecondary`, border `borderWidth`, padding `Spacing.md` horizontal and `Spacing.sm` vertical
- Shows `icon` (`iconSize`) and `label` (`labelLarge`) in a row, `Spacing.xs` apart

| State | bg | text | border |
|---|---|---|---|
| Default | colorSurface | textSecondary | colorBorder |
| Pressed | colorInputBackground | textSecondary | colorBorderPressed, scale 0.97 |
| Disabled | colorSurface | textDisabled | colorBorder, opacity 0.6 |

**variant = floating** (outlined circle icon button, floating over content)
- Size `buttonPrimarySize`, shape `ComponentShapes.buttonFloating`, border `borderWidth`, icon `iconSize`
- Shows `icon` only. `label` is the icon's content description

| State | bg | icon | border |
|---|---|---|---|
| Default | colorSurface | textPrimary | colorBorder |
| Pressed | colorInputBackground | textPrimary | colorBorderPressed, scale 0.95 |
| Disabled | colorSurface | textDisabled | colorBorder, opacity 0.6 |

---

### TextInput
Generic configurable text field. The caller passes a `MutableInteractionSource`, and the field reads its focused state from it.
- `BasicTextField`, shape `ComponentShapes.textInput`, padding `Spacing.md` horizontal and `Spacing.sm` vertical, text `bodyMedium`
- Grows with its text up to `maxLines`, then scrolls. Unlimited unless the caller passes it
- Focus ring: a `focusRingWidth` border in colorPrimary at `Alpha.focusRing`

| State | bg | text |
|---|---|---|
| Empty | colorInputBackground | textSecondary (placeholder) |
| Focused | colorInputBackground | textPrimary + colorPrimary ring @25% |
| Filled | colorInputBackground | textPrimary |
| Disabled | colorInputBackground | textDisabled, opacity 0.6 |

---

### MessageBubble
Bubble shell: the message text on its background. Its values are private to `MessageBubble.kt`.
- Text `bodyMedium`, padding 12dp horizontal and 8dp vertical
- Corners 18dp. The last bubble of a run has a 4dp tail corner on its own side: bottom-end when sent, bottom-start when received. Start and end follow the layout direction
- SENDING: the whole bubble is at opacity 0.5. It animates to 1 with `animateFloatAsState` when the message is sent

| State | bg | text | shape |
|---|---|---|---|
| Sent — tail | colorPrimary | textOnPrimary | 18dp, tail: bottom-end 4dp |
| Received — tail | colorSurfaceVariant | textPrimary | 18dp, tail: bottom-start 4dp |
| Sent — grouped | colorPrimary | textOnPrimary | all corners 18dp |
| Received — grouped | colorSurfaceVariant | textPrimary | all corners 18dp |
| Sent — sending | colorPrimary | textOnPrimary | as above, opacity 0.5 |

---

### MessageItem
Wraps MessageBubble and handles alignment, spacing, width and the SendingIndicator. Its values are private to `MessageItem.kt`.
- The bubble sits in a column 80% of the row's width, aligned to the bubble's side. The bubble wraps its text inside the column
- Bottom padding is the gap to the next bubble: 2dp when grouped, 8dp otherwise
- While SENDING, a SendingIndicator sits `Spacing.xs` below the bubble, on its side. It shows and hides with `AnimatedVisibility`, so the row closes smoothly when the message is sent
- No side padding: the message list adds the screen margin

| State | Alignment | Bottom padding (gap to the next bubble) | Notes |
|---|---|---|---|
| Sent — separated | end | 8dp | status: SENT |
| Sent — grouped | end | 2dp | status: SENT |
| Sent — sending | end | 2dp or 8dp, as above | status: SENDING, bubble opacity 0.5, SendingIndicator below |
| Sent — confirmed | end | — | SENDING → SENT on echo (~200ms): the bubble fades to opacity 1 and the indicator hides |
| Received — separated | start | 8dp | — |
| Received — grouped | start | 2dp | — |

Grouped = next message is same user AND sent <20s later. A grouped bubble has no tail, and the small gap below it joins it to the next bubble.

---

### SendingIndicator
Three pulsing dots below a message that is SENDING. Single state. Its values are private to `SendingIndicator.kt`.
- Dots: 6dp circles in `textSecondary`, `Spacing.xs` apart
- Each dot's opacity pulses between 0.3 and 1, 600ms each way, and each dot starts 200ms after the one before
- Content description: `R.string.sending` ("Sending"), on the whole indicator

---

### SectionHeader
Centred timestamp divider. Single state.

- Style: `labelSmall`, colour: `textSecondary`, alignment: centre
- Padding: `Spacing.sm` top and bottom
- Shown above a message when the gap from the older message is >1h, or above the oldest message
- Format: today `11:59`, 1–6 days ago `Thursday 11:59`, otherwise `12 Mar 2026 11:59`

---

### ChatInput
The message input: composes `TextInput` + `Button (primary)`. The screen passes in an interaction source for each. Its values are private to `ChatInput.kt`.
- Bar: `colorSurface`, with a `dividerWidth` `colorBorder` divider along its top, then padding `Spacing.md` horizontal and `Spacing.sm` vertical
- Row: `TextInput` filling the space, then Button primary (`ic_send`, content description "Send"), `Spacing.sm` apart and aligned to the bottom, so Send stays beside the last line
- TextInput grows to at most 8 lines, then scrolls
- The screen applies the keyboard and navigation bar insets

| State | TextInput | Button primary |
|---|---|---|
| Empty | empty | disabled |
| Filled | filled | default |

- Send is enabled when `text.isNotBlank()`

---

### MockChatInput
Mock data: stands in for Sarah's device, which the exercise does not have. Composes a "Reply as Sarah" `Button (secondary)` above an unchanged `ChatInput`. The real app shows `ChatInput` alone.
- Strip: `colorSurface`, the button end-aligned, padding `Spacing.md` horizontal and `Spacing.sm` top. `ChatInput`, with its divider, sits below it
- Button: `ic_reply`, label `R.string.reply_as` with Sarah's name, enabled when `text.isNotBlank()`. It sends the input as Sarah, through the same flow as Send

| State | Button secondary | ChatInput |
|---|---|---|
| Empty | disabled | Empty |
| Filled | default | Filled |

---

### ChatScreen
Composes `TopBar`, the message list and `MockChatInput` in a `Scaffold` on colorBackground. It remembers every interaction source and the list's scroll state, so every component below it is stateless.
- TopBar: the title is Sarah's name
- List: the newest message at the bottom, and scrolling up loads older pages. Messages render as `MessageItem` and timestamp headers as `SectionHeader`
- List spacing: a `Spacing.md` side margin and no vertical padding. The gaps below the bubbles and SectionHeader's padding space the rows
- When the user sends a message, with Send or Reply as Sarah, the list scrolls to it once it appears. Other new messages leave the list where it is
- Jump to latest: a `Button (floating)` with `ic_arrow_down`, content description `R.string.jump_to_latest` ("Jump to latest"), over the list's bottom end, `Spacing.md` from the edge and `Spacing.sm` above the input. It fades in while the newest message is scrolled out of view, and a tap snaps the list to the newest message with no animation
- Insets: TopBar applies the status bar inset, and MockChatInput the navigation bar and keyboard insets. The list sits between them and applies none, so the keyboard never covers the input
