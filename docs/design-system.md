# Muzz Chat — Design System

> Visual reference: `design-system.html`

---

## Colours

### Tier 1 — Primitives
| Token | Hex | Notes |
|---|---|---|
| Pink500 | #E8175D | Muzz brand pink |
| Pink700 | #C01048 | Pressed state of Pink500 |
| Neutral900 | #1A1A1A | Near-black |
| Neutral500 | #9E9E9E | Mid grey |
| Neutral300 | #BDBDBD | Disabled text / icons |
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
| colorBorder | Neutral50 | Dividers, input bar top border |
| textPrimary | Neutral900 | Message text on received bubbles, top bar |
| textSecondary | Neutral500 | Section headers, timestamps, placeholder |
| textDisabled | Neutral300 | Disabled input text, disabled button icon |
| textOnPrimary | White | Text/icons on pink (sent bubble, send button) |

---

## Typography
| Style | Size | Line height | Weight | Role mapping |
|---|---|---|---|---|
| bodyLarge | 16sp | 22sp | Regular | textPrimary — message body |
| bodyMedium | 14sp | 20sp | Regular | textPrimary — message body (compact) |
| bodySmall | 12sp | 16sp | Regular | textSecondary — timestamps |
| labelSmall | 11sp | 16sp | SemiBold, 0 tracking | textSecondary — section headers |

---

## Spacing
| Token | Value | Usage |
|---|---|---|
| xs | 4dp | Icon gaps, tight nudges |
| sm | 8dp | Inner padding, standard message gap |
| md | 16dp | Screen horizontal margin |
| lg | 24dp | Section spacing |
| xl | 32dp | Large layout gaps |
| bubblePadH | 12dp | Bubble horizontal padding |
| bubblePadV | 8dp | Bubble vertical padding |
| msgGrouped | 2dp | Gap: same user, <20s apart |
| msgSeparated | 8dp | Gap: different user or >20s apart |

---

## Shapes
| Component | Corner radius |
|---|---|
| MessageBubble | 18dp all corners; tail corner 4dp |
| TextInput | 24dp (pill) |
| Button (primary) | Circle |
| Button (secondary) | 20dp (pill) |

---

## Components

### Avatar
User profile image — initials fallback when no image available.

| State | Size | bg | text |
|---|---|---|---|
| md — default | 36dp | colorPrimary | textOnPrimary |
| lg | 48dp | colorPrimary | textOnPrimary + 2dp colorSurfaceVariant border |
| placeholder | 36dp | colorSurfaceVariant | textSecondary |

---

### TopBar
Composes back button + Avatar + name.

| State | Notes |
|---|---|
| Default | bg: colorSurface, border-bottom: colorBorder, back/name: textPrimary |

---

### Button

**variant = primary** (circle icon button)
| State | bg | icon |
|---|---|---|
| Default | colorPrimary | textOnPrimary |
| Pressed | colorPrimaryPressed | textOnPrimary, scale 0.95 |
| Disabled | colorPrimaryDisabled | textDisabled |

**variant = secondary** (outlined pill)
| State | bg | text | border |
|---|---|---|---|
| Default | colorSurface | textSecondary | colorBorder |
| Pressed | colorInputBackground | textSecondary | Neutral300, scale 0.97 |
| Disabled | colorSurface | textDisabled | Neutral50, opacity 0.6 |

---

### TextInput
Generic configurable text field.

| State | bg | text |
|---|---|---|
| Empty | colorInputBackground | textSecondary (placeholder) |
| Focused | colorInputBackground | textPrimary + colorPrimary ring @25% |
| Filled | colorInputBackground | textPrimary |
| Disabled | colorInputBackground | textDisabled, opacity 0.6 |

---

### MessageBubble
Bubble shell — receives text, background colour, and tail direction.

| State | bg | text | shape |
|---|---|---|---|
| Sent — tail | colorPrimary | textOnPrimary | 18dp, tail: bottom-right 4dp |
| Received — tail | colorSurfaceVariant | textPrimary | 18dp, tail: bottom-left 4dp |
| Sent — grouped | colorPrimary | textOnPrimary | all corners 18dp |
| Received — grouped | colorSurfaceVariant | textPrimary | all corners 18dp |
| Sent — sending | colorPrimary | textOnPrimary | opacity 0.5, pulsing dots below |

---

### MessageItem
Wraps MessageBubble — handles alignment, spacing, and delivery status.

| State | Alignment | Bottom padding (gap to the next bubble) | Notes |
|---|---|---|---|
| Sent — separated | end | msgSeparated (8dp) | status: SENT |
| Sent — grouped | end | msgGrouped (2dp) | status: SENT |
| Sent — sending | end | msgGrouped or msgSeparated, as above | status: SENDING, opacity 0.5 + pulsing dots |
| Sent — confirmed | end | — | status transitions SENDING → SENT on echo (~200ms) |
| Received — separated | start | msgSeparated (8dp) | — |
| Received — grouped | start | msgGrouped (2dp) | — |

Grouped = next message is same user AND sent <20s later. A grouped bubble has no tail, and the small gap below it joins it to the next bubble.

---

### SectionHeader
Centred timestamp divider. Single state.

- Style: `labelSmall`, colour: `textSecondary`, alignment: centre
- Shown above a message when the gap from the older message is >1h, or above the oldest message
- Format: today `11:59`, 1–6 days ago `Thursday 11:59`, otherwise `12 Mar 2026 11:59`

---

### ChatInput
Composes `TextInput` + `Button (primary)` + `Button (secondary)`.

| State | TextInput | Button primary | Button secondary |
|---|---|---|---|
| Empty | empty | disabled | disabled |
| Filled | filled | default | default |

- Both buttons enabled when `text.isNotBlank()`
- Button secondary ("Reply as Sarah") sits above the input row
- Bar bg: `colorSurface`, top border: `colorBorder`
