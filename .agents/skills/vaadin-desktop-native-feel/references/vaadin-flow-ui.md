# Vaadin Flow UI Guidance

Use this when changing Vaadin views, themes, components or desktop runtime APIs.

## Vaadin Desktop Is Not A Website

Do not design the first screen like a marketing page. A desktop Vaadin app should open directly into the working surface:

- command palette,
- settings window,
- grid/list/detail workspace,
- inspector/devtool,
- file/project/session dashboard.

Avoid hero sections, decorative cards, page-scale headings and web landing-page composition unless the window is truly an onboarding window.

## Desktop Layout

- Prefer dense but readable layouts.
- Keep toolbar buttons stable in size.
- Use icons for common tools; use text for commands where ambiguity would slow users down.
- Use tabs, split panes, sidebars, status bars and command palettes as desktop primitives.
- Avoid nested cards and web dashboard card grids for operational UI.
- Keep typography modest inside panels; reserve large display text for rare onboarding or empty states.

## Vaadin Components

- `Grid` is a desktop-strength component, but tune density, keyboard navigation and lazy loading.
- `Button` click paths should give immediate feedback if server work takes over 200 ms.
- `Dialog` is acceptable for lightweight Flow UI, but native file dialogs, save dialogs, alerts and permission prompts should go through `Desktop.current()` and the host.
- `Notification` for system-level events should use native notifications, not only Vaadin overlay notifications.
- `TextField`, `ComboBox` and editable `Grid` cells must be IME-tested.

## Theme Rules

- Use system font for chrome.
- Follow system dark/light mode and accent color where practical.
- Do not hardcode one brand hue across all UI.
- Do not draw fake window chrome, shadows or titlebar controls in Flow unless the host intentionally creates a frameless window and owns the hit testing.
- Avoid `cursor: pointer` in desktop chrome; it reads as web.
- Disable text selection in static chrome.

## Flow Over IPC Awareness

Every Flow UI decision should consider transport cost:

- High-frequency events must be batched or throttled.
- Do not send host IPC on every keystroke unless the product surface requires it and it is measured.
- Lazy data providers should be observed for request frequency and payload size.
- Push/resync behavior must be explicit in trace logs.
- Compatibility HTTP is allowed only as an adapter; do not bake it into public desktop APIs.

## Desktop API Rule

If the capability is native, call the desktop runtime API:

```java
Desktop.current().notifications().show("Title", "Message");
Desktop.current().clipboard().writeText("text");
Desktop.current().fileDialog().openFile();
Desktop.current().setWindowTitle("Title");
```

If the API only logs in compatibility mode, still route through it. That preserves the contract for the real host.

