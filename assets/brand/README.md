# vatori Brand Assets

This directory contains the source and exported brand assets for `vatori`.

## Concept

The current mark uses the first selected imagegen concept: a deep-green,
folded-ribbon `V` inside a warm rounded-square plate, with an orange rounded
terminal integrated into the upper-right stroke.

It intentionally does not mimic the Vaadin logo. The product name is `vatori`;
describe it as a Vaadin-based desktop framework when technical context needs
that relationship.

## Palette

- Deep green: `#0B3D38`
- Orange accent: `#FF7A59`
- Warm plate: `#FFFCF4` to `#EEE7D8`
- Plate stroke: `#D7CEBC`

The palette avoids the blue/yellow combination used by Tauri. The icon plate is
background-agnostic; wordmarks are exported as light-background and
dark-background variants instead of forcing one compromised universal color.

## Files

- `vatori-icon-source.png`: selected imagegen raster source.
- `vatori-icon.svg`: SVG wrapper for the exported icon PNG.
- `vatori-wordmark.svg`: editable horizontal wordmark source.
- `vatori-wordmark-on-dark.svg`: editable horizontal wordmark source for dark backgrounds.
- `vatori-icon-1024.png`: high-resolution transparent PNG icon export.
- `vatori-icon.icns`: macOS app icon export.
- `vatori-icon.ico`: multi-size icon export.
- `vatori-wordmark-1600x520.png`: transparent PNG wordmark export.
- `vatori-wordmark-on-dark-1600x520.png`: transparent PNG wordmark export for dark backgrounds.
- `vatori-background-check.png`: quick black/white background contrast check.
- `render_assets.py`: local renderer for PNG, ICO and ICNS exports from
  `vatori-icon-source.png`.

## Integration

- `README.md` and `README.zh-CN.md` use the light and dark wordmark exports.
- The macOS host builder uses `vatori-icon.icns` as the fallback app icon for
  Finder, the Dock and the application switcher. An application-owned
  `src/main/desktop/macos/AppIcon.icns` takes precedence.
- Applications keep ownership of their own in-app branding; the framework mark
  is not injected into Vaadin views or application frontend resources.
