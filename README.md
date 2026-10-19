# Tooltips Improved

Makes tooltips readable, however long they get. Client-side only, with no dependencies, so Fabric API isn't needed.

## Features

**Folded armor trims.** Vanilla spends three lines on an armor trim:

```
Upgrade:
 Sentry Armor Trim
 Iron Material
```

This shows `Sentry Armor Trim (Iron)` instead: the pattern in gray, the material in its own colour. Hold **Shift** to see the full vanilla lines. Materials from other mods keep their full name unless a lang key `tooltipsimproved.<their trim_material key>` gives a short one (see `assets/tooltipsimproved/lang/en_us.json`).

**Text wrapping.** Long lines are broken at word boundaries, keeping their colours, so the tooltip fits in the space beside the cursor instead of stretching across the screen. Other modes wrap only what wouldn't fit on the screen at all, to half the screen, or to a fixed width. Vanilla's narrow 170 pixel wrap on button tooltips can be turned off so those follow the same setting.

**Smarter placement.** Vanilla puts a tooltip beside the cursor. When it fits on neither side, vanilla draws it over the cursor and hides what you're pointing at. Tooltips Improved instead centers it above or below the cursor, or moves it to the nearest screen corner that leaves the cursor clear. A tooltip taller than the screen shows its top rather than its bottom. Button tooltips can sit centered above or below their button.

**Scrolling.** Tooltips that still don't fit can be moved, including past the screen edges:

| Input | Moves the tooltip |
|-------|-------------------|
| Mouse wheel | Up / down |
| Left Alt + mouse wheel, or a side wheel | Left / right |
| Page Up / Page Down | Up / down, while held |
| Left Alt + Page Up / Page Down | Left / right, while held |
| Movement keys, WASD by default (off by default) | Any direction |

The wheel only moves a tooltip along a direction it doesn't fit in, and otherwise keeps scrolling the screen as usual. Keys are rebindable under Controls → Inventory, to keys or mouse buttons, and there's an unbound "Reset Tooltip Position" key. The tooltip glides into place, starts back in place when you hover something else, and resets when its screen closes. Sideways movement uses Left Alt rather than Tooltip Scroll's Shift, because Shift expands tooltips here (and Ctrl is Sprint).

**Looks.** The background can be made see-through so it covers less of the inventory, and the extra gap under the first line can be removed.

## Config

Every option has an in-game settings screen. On Fabric, open it from [Mod Menu](https://modrinth.com/mod/modmenu) (optional) by selecting Tooltips Improved and clicking the settings button. On NeoForge, use the Config button in the Mods list. Changes apply immediately and are saved to `config/tooltipsimproved.json`, which can also be edited by hand.

| Option | Default | |
|--------|---------|-|
| `foldTrims` | `true` | Show armor trims on one line |
| `wrap` | `CURSOR` | `OFF`, `CURSOR` (fit beside the cursor), `SCREEN` (only what wouldn't fit on the screen at all), `HALF_SCREEN`, or `FIXED` |
| `wrapWidth` | `200` | Width in pixels for `FIXED` |
| `unwrapButtonTooltips` | `false` | Skip vanilla's 170 pixel wrap on button tooltips |
| `placement` | `CENTER_WHEN_CRAMPED` | For tooltips that don't fit beside the cursor: `VANILLA`, `CENTER_WHEN_CRAMPED`, `CORNER_WHEN_CRAMPED`, or `CORNER` (always a corner) |
| `placeAllTooltips` | `false` | Apply `placement` to every tooltip, not just ones following the cursor |
| `topFirst` | `true` | Show the top of a tooltip taller than the screen |
| `buttonTooltipsBesideButton` | `false` | Center button tooltips above or below the button |
| `backgroundOpacity` | `1.0` | 0 to 1; text stays opaque |
| `removeFirstLineGap` | `false` | Drop the gap under the first line |
| `scrollWheel` | `true` | Move tooltips with the mouse wheel |
| `movementKeys` | `false` | Also move tooltips with your movement keys; ignored while typing |
| `resetWhenHidden` | `true` | Put a tooltip back in place once it's hidden |
| `invertHorizontal` / `invertVertical` | `false` | Flip the wheel direction |
| `wheelStep` | `10` | Pixels per wheel notch |
| `keySpeed` | `300` | Pixels per second while a key is held |
| `smoothness` | `0.25` | Share of the distance covered per frame at 60 FPS; 0.05 glides, 1 is instant |

## Credits

The feature set is inspired by [Better Trim Tooltips](https://modrinth.com/mod/better-trim-tooltips), [Tooltip Scroll](https://modrinth.com/mod/tooltip-scroll) and [Adaptive Tooltips](https://modrinth.com/mod/adaptive-tooltips). The implementation is written from scratch for one codebase across loaders and versions.

## Targets

| Minecraft | Fabric | NeoForge |
|-----------|--------|----------|
| 1.21.1    | ✔      | ✔        |
| 1.21.11   | ✔      | ✔        |
| 26.2      | ✔      | ✔        |
| 26.3      | ✔      | ✔        |

Forge support is a work in progress.

Built with [Stonecutter](https://stonecutter.kikugie.dev/) from one source tree. Edit `settings.gradle.kts` to add or drop versions, and `stonecutter.properties.toml` for per-version dependency numbers.

## Working with it

- Switch the active version/loader with the Stonecutter Gradle tasks (`Set active project to …`) or by changing `stonecutter active` in `stonecutter.gradle.kts`. This rewrites the source tree for that target, so switch back to the VCS version (`26.3-fabric`) before committing.
- Version-specific code uses Stonecutter comments, e.g. `//? if fabric {` or `//? if >=1.21.5 {`.
- Build one target: `./gradlew :26.3-fabric:build`. Run `buildAndCollect` on a target to copy its jars into `build/libs/<mod version>/`.
- `runClient -Pmodmenu` (Fabric) adds Mod Menu and Fabric API to the dev game, to try the settings screen from Mod Menu.
- `runClient -Pselftest` starts the game, runs the in-game checks in `SelfTest` on the title screen, logs `PASS`/`FAIL` per check, and quits.

## Code map

Every tooltip goes through one vanilla method, hooked in `mixin/TooltipRenderMixin`. In order:

1. `TooltipWrap` breaks text lines that are too wide.
2. `TooltipPlacement` wraps vanilla's positioner to apply the placement rules, then keeps the top of tall tooltips in view.
3. `TooltipScroll` moves the result by the scroll offset.

Other pieces:

- Trim folding: `TooltipsImproved.trimLine` and `mixin/ArmorTrimMixin`.
- Background opacity: `TooltipBackground` and `mixin/TooltipBackgroundMixin`.
- Button tooltips: `mixin/WidgetTooltipHolderMixin` and `mixin/TooltipMixin`.
- Keybinds: added to the vanilla list by `mixin/OptionsMixin`, so no loader keybind API is needed.
- Settings screen: `TooltipsImprovedScreen`, built from vanilla's options widgets. It's hooked into Mod Menu by `TooltipsImprovedModMenu` and into NeoForge's mod list in `TooltipsImprovedNeoForge`.

Hooks use plain Mixin features, except the background hook before 1.21.6, which uses MixinExtras' `@WrapMethod` on Fabric and NeoForge (both ship MixinExtras).

Stonecutter guide: https://stonecutter.kikugie.dev/wiki/start/
