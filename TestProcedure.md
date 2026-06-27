# Redstone Link GUI — Test Procedure

## Prerequisites

1. Start a **Creative** world (or Survival with cheats enabled).
2. Have **Create** installed.
3. Have **Create Utilities** installed (for Void Link tests).
4. Give yourself the following items:
   - `redstone_link` blocks (Create)
   - `void_link` blocks (Create Utilities)
   - Frequency symbol items: `/give @p frequency:symbol_1`, `/give @p frequency:symbol_2`, etc.
   - A few miscellaneous items (dirt, stone, etc.) for filling inventory slots.
   - A tool to break/place blocks.

---

## A — Basic GUI Opening

| # | Action | Expected Result |
|---|--------|-----------------|
| A1 | Place a Redstone Link. Right-click it with an **empty hand**. | GUI opens showing two frequency slots (First/Last), preset panel on the left, Move/Back buttons, and player inventory. |
| A2 | Right-click a Redstone Link holding a **non-empty hand** (e.g., a dirt block). | GUI does **not** open — vanilla interaction occurs instead. |
| A3 | Right-click a **Void Link** with an empty hand. | GUI opens with the Void Link overlay texture (different from Redstone Link). |
| A4 | Right-click a **non-link block** (e.g., dirt, stone). | Nothing special — normal interaction. |

---

## B — Frequency Slots (0 = First, 1 = Last)

| # | Action | Expected Result |
|---|--------|-----------------|
| B1 | Open Redstone Link GUI. Left-click a frequency item from your cursor onto slot 0 (First). | Item appears in slot 0. Cursor is now **empty**. The link's frequency updates. |
| B2 | Left-click a different frequency item onto slot 1 (Last). | Item appears in slot 1. Cursor empty. Link updates. |
| B3 | Right-click on slot 0. | Slot 0 clears. Cursor empty. Link updates. |
| B4 | Press **Q** (throw) while hovering over slot 0. | Slot 0 clears. Cursor empty. Link updates. |
| B5 | **Shift-click** a frequency slot. | Nothing happens — QUICK_MOVE is safely ignored. |
| B6 | Press a **hotbar number key** while hovering over a frequency slot. | Nothing happens — SWAP is ignored. |
| B7 | **Double-click** a frequency slot. | Nothing happens — DOUBLE_CLICK is ignored. |
| B8 | **Creative middle-click** on a frequency slot. | Nothing happens — CLONE is ignored. |
| B9 | Left-click slot 0 with an item, then immediately left-click slot 1. | Only slot 0 gets the item. Slot 1 does **not** receive the same item. |
| B10 | Set frequencies, close GUI, verify the link transmits/receives on that frequency. | Link behaves correctly at the set frequency. |

---

## C — Preset Slots (2-9, 4 rows × 2 columns)

| # | Action | Expected Result |
|---|--------|-----------------|
| C1 | Left-click an item onto a preset slot (e.g., row 0 col 0). | Item appears in that preset slot. Persisted via attachment. |
| C2 | Right-click on a filled preset slot. | Slot clears. |
| C3 | Press **Q** on a filled preset slot. | Slot clears. |
| C4 | Left-click an item onto preset row 0 col 0, then left-click preset row 1 col 0. | Only row 0 col 0 gets the item. Row 1 col 0 does **not** receive the same item. |
| C5 | Close and reopen the GUI. | Previously set preset slots still show their items (persisted). |
| C6 | **Shift-click** a preset slot. | Nothing happens. |
| C7 | **Hotbar swap** on a preset slot. | Nothing happens. |
| C8 | **Double-click** on a preset slot. | Nothing happens. |

---

## D — Copy / Paste Buttons

| # | Action | Expected Result |
|---|--------|-----------------|
| D1 | Set both frequency slots. Click the **Copy** button next to a preset row. | The link's current frequencies are saved into that preset row. |
| D2 | Change the link frequencies to something else. Click the **Paste** button next to the row from D1. | The link frequencies revert to the saved preset. |
| D3 | Try to **Paste** from an empty preset row. | Paste button is **disabled** (not clickable). |
| D4 | Try to **Copy** when both frequency slots are empty. | Copy button is **disabled**. |
| D5 | Copy frequencies from a Redstone Link to a preset. Paste them into a **Void Link**. | Frequencies apply to the Void Link (cross-type paste). |

---

## E — Frequency Symbol Picker

| # | Action | Expected Result |
|---|--------|-----------------|
| E1 | Place a frequency symbol item in slot 0. **Middle-click** on slot 0. | Symbol picker screen opens. |
| E2 | Select a different symbol from the picker. | Slot 0 updates to the new symbol. |
| E3 | Middle-click on a **non-symbol** item in a slot. | Nothing happens — picker does not open. |
| E4 | Middle-click on a **preset slot** that contains a frequency symbol. | Symbol picker opens for that preset slot. |
| E5 | In the symbol picker for a preset slot, select a symbol. | Preset data updates and syncs to server. |
| E6 | Middle-click on an **empty** preset slot. | Click is consumed (slot interaction), but picker does not open. |

---

## F — Redstone Link Toggle (S/R Modes)

| # | Action | Expected Result |
|---|--------|-----------------|
| F1 | Open a Redstone Link GUI. | Toggle button is visible near the frequency slots. |
| F2 | Click the toggle button. | Link switches between Sender and Receiver mode. GUI updates. |
| F3 | Open a **Void Link** GUI. | No toggle button is shown (only a skull/claim button). |

---

## G — Void Link Ownership / Claim

| # | Action | Expected Result |
|---|--------|-----------------|
| G1 | Open a Void Link GUI. | Skull button is visible. Shows a skeleton skull if unowned. |
| G2 | Click the skull button to claim ownership. | Skull changes to your player head. Packet sent to server. |
| G3 | (Multiplayer) Another player opens the GUI of a link you own. | They see your player head. They can click to forfeit your ownership. |

---

## H — Relocate (Move Button)

| # | Action | Expected Result |
|---|--------|-----------------|
| H1 | Open any link GUI. Click the **Move** button. | GUI closes. A colored highlight appears on the source block's face. The "click to relocate" message appears. |
| H2 | Look at a valid target position. Right-click. | The link block moves to the new position. Highlight disappears. |
| H3 | Press **Sneak** while in move mode, then right-click. | Move is cancelled — link stays in place. |
| H4 | Move a link that has gauge panels connected. | Gauge constraints are enforced (same surface, within 24 blocks). |
| H5 | Try to move beyond the configured range (`MOVE_RANGE`). | Red highlight appears. Failure message shown. |

---

## I — Config Settings

| # | Action | Expected Result |
|---|--------|-----------------|
| I1 | Set `CLICK_MODE = BLOCK` in client config. | Right-clicking the block (**anywhere**) opens the GUI. |
| I2 | Set `CLICK_MODE = SLOT`. | Right-clicking only on the frequency **slot hitbox** opens the GUI. |
| I3 | Set `CLICK_MODE = SHIFT_BLOCK`. | Sneak + right-click anywhere on the block opens the GUI. |
| I4 | Set `MOVE_RANGE` to a new value in common config. | The relocate range respects the new limit (default: 24, max: 256). |

---

## J — JEI / EMI Compatibility

| # | Action | Expected Result |
|---|--------|-----------------|
| J1 | Open the GUI. Drag an item from JEI/EMI onto a frequency slot. | Item is placed in the slot (ghost-drag integration). |
| J2 | Drag an item from JEI/EMI onto a preset slot. | Item is placed in the preset slot. |

---

## K — Client / Server Separation

| # | Action | Expected Result |
|---|--------|-----------------|
| K1 | Open the GUI on a **dedicated server** (as a client). | GUI opens correctly. All frequency/preset changes sync via packets. |
| K2 | Open the GUI in **singleplayer**. | Works identically (packet logic runs locally). |

---

## L — Regression: Slot Isolation (Critical — Bug Fix Validation)

| # | Action | Expected Result |
|---|--------|-----------------|
| L1 | Place an item in frequency slot 0. Then place a **different** item in preset row 0 col 0. | Each slot shows only its own item. No cross-contamination. |
| L2 | Click frequency slot 0 with an item. Cursor should be empty immediately. Then click preset row 0 col 0. | Preset slot does **not** receive the same item. |
| L3 | Repeat L2 in reverse order (preset first, then frequency). | Same — no cross-contamination. |
| L4 | Set four different preset rows with different items. Verify each row shows its correct pair. | Each preset row is independent. |
| L5 | Set frequency slots, copy to preset row, change frequencies, paste back. | Frequencies correctly save and restore. |

---

## M — SableCompanion (Sublevel Compatibility)

| # | Action | Expected Result |
|---|--------|-----------------|
| M1 | Install SableCompanion. Relocate a link across sublevel boundaries. | Distance calculation accounts for sublevel offset. |
| M2 | Uninstall SableCompanion. Relocate a link normally. | Falls back to standard Euclidean distance. No crash. |

---

## N — Create Utilities Absence

| # | Action | Expected Result |
|---|--------|-----------------|
| N1 | Run the mod **without** Create Utilities installed. | Redstone Link features work normally. Void Link features are absent. No crash or error. |