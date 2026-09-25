# Modern CSS Toolkit

Live templates for modern CSS in JetBrains IDEs (2025.2 and later). Every template states its
[Baseline](https://web.dev/baseline) browser-support status, computed from the official web-features data.

| Group | Abbreviation | Inserts | Baseline |
|---|---|---|---|
| Layout | `css-container-named` | Named size container and a query that targets it by name | Widely available |
| Layout | `css-container-query` | Container query: make an element a size container and style its content by the container width | Widely available |
| Layout | `css-full-height` | Full viewport height that ignores mobile browser toolbars (dynamic viewport units) | Widely available |
| Layout | `css-grid-auto` | Responsive grid without media queries: as many columns as fit, each at least a minimum width | Widely available |
| Layout | `css-logical-spacing` | Padding with logical properties (block = top/bottom, inline = start/end; works in RTL) | Widely available |
| Layout | `css-media-range` | Media query with the range syntax (width >= ...) instead of min-width | Widely available |
| Layout | `css-scroll-snap` | Horizontal scroll-snap track (carousel without JavaScript) | Widely available |
| Layout | `css-subgrid` | Subgrid: children align to the parent grid (e.g. cards with aligned titles and buttons) | Widely available |
| Selectors | `css-focus-visible` | Keyboard-only focus ring with :focus-visible | Widely available |
| Selectors | `css-has` | Style a parent depending on its children with :has() | Widely available |
| Selectors | `css-nth-of` | Zebra rows counting only matching elements with :nth-child(... of selector) | Widely available |
| Selectors | `css-where-reset` | Zero-specificity defaults with :where(), easy to override anywhere | Widely available |
| Architecture | `css-component-bem` | Component with BEM naming in plain CSS: block, element and modifier as full selectors | Widely available |
| Architecture | `css-component-scope` | Component styles isolated with @scope (native alternative to BEM naming) | Newly available |
| Architecture | `css-component` | Component with native CSS nesting (child, state and modifier rules inside the block) | Widely available |
| Architecture | `css-container-style` | Container style query: style children when a container custom property has a value | Newly available |
| Architecture | `css-layers` | Cascade layers: declare the layer order (ITCSS-like) and open one layer | Widely available |
| Architecture | `css-property` | Typed custom property with @property (animatable, with a default value) | Newly available |
| Architecture | `css-reset` | Modern minimal reset inside a low-priority cascade layer | Widely available |
| Color | `css-color-tokens` | Color tokens in OKLCH with hover and subtle variants mixed by color-mix() | Widely available |
| Color | `css-dark-mode` | Dark mode overrides that follow the operating system setting | Widely available |
| Color | `css-light-dark` | Light and dark colors in one declaration with light-dark() and color-scheme | Newly available |
| Color | `css-relative-color` | Relative color: derive a lighter shade from another color with oklch(from ...) | Newly available |
| Typography | `css-fluid-type` | Fluid font size that grows with the viewport between a minimum and a maximum | Widely available |
| Typography | `css-text-balance` | Balanced line lengths for headings with text-wrap: balance | Newly available |
| Motion | `css-dialog-animation` | Animate a dialog in and out (display transition with allow-discrete and @starting-style) | Newly available |
| Motion | `css-entry-animation` | Entry animation with @starting-style: fade and slide in when the element appears | Newly available |
| Motion | `css-reduced-motion` | Respect reduced motion: near-instant animations and transitions for users who ask for it | Widely available |
| Motion | `css-smooth-scroll` | Smooth scrolling only for users who have not asked to reduce motion | Widely available |
| Motion | `css-view-transition` | Same-document view transition: name an element and tune its animation | Newly available |
| Accessibility | `css-visually-hidden` | Visually hidden but still read by screen readers | Widely available |
| Forms | `css-field-sizing` | Textarea and inputs that grow with their content (field-sizing) | Newly available |

Works in `.css`, `.scss`, `.less` and `<style>` blocks, but not in the indented Sass syntax (`.sass`), where braces
are invalid. Type the abbreviation and press <kbd>Tab</kbd>.

Built on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).

---

Made by [James Mosquera](https://www.jamesmosquera.com).
