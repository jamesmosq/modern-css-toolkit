# Research — Modern CSS Toolkit (2026-09-25)

Everything here was verified against the sources listed; re-check dates before relying on statuses.

## 1. Market (JetBrains Marketplace search API, 2026-09-25)
No plugin offers modern-CSS live templates. Closest results: "Snippet Toolkit for Tailwind CSS" (paid,
Tailwind only), "BulmaCSS, Bootstrap 4, Font awesome" and "UXG Live Templates" (both abandoned in 2020),
"CSS Variables Assistant" (16k downloads, custom-property completion, not templates).
Emmet and the IDE's CSS completion already cover basics (flex centering, margins, shadows) → no filler.

## 2. Which source decides "can I use it" — Baseline, not the W3C snapshot
- W3C "CSS Snapshot 2026" (Group Note, 2026-06-22, https://www.w3.org/TR/css-2026/) classifies specs by
  stability and says: "The primary audience is CSS implementers, not CSS authors, as this definition includes
  modules by specification stability, not Web browser adoption rate." Example: Nesting is listed under
  "rough interoperability" yet is Baseline widely available.
- For authors the criterion is **Baseline** (https://web.dev/baseline), data from the W3C WebDX CG:
  npm `web-features` 3.40.0 (Apache-2.0, published 2026-09-24), `data.json` → `features[id].status`
  with `baseline` = `"high"` (widely) | `"low"` (newly) | `false`, plus `baseline_low_date`/`baseline_high_date`
  and `compat_features` (BCD keys such as `css.at-rules.scope`).
- Tooling to compute status from code: `compute-baseline` 0.5.0 (Apache-2.0), `@mdn/browser-compat-data`
  8.1.3 (CC0), parser `css-tree` 3.2.1 (MIT). NOT yet verified: that css-tree parses `@scope` and nesting.

## 3. Candidate catalog with status (web-features 3.40.0)
Decision (owner): include widely (high) and newly (low) available, labelled; exclude non-Baseline.

Widely available (high) — id: low date / high date
- Layout: `grid` 2017/2020, `subgrid` 2023-09/2026-03, `flexbox-gap` 2021/2023, `container-queries` 2023-02/2025-08,
  `aspect-ratio` 2021/2024, `logical-properties` 2021/2024, `viewport-unit-variants` (dvh/svh/lvh) 2022/2025,
  `media-query-range-syntax` 2023/2025-09
- Selectors: `has` 2023-12/2026-06, `is`/`where` 2021/2023, `nth-child-of` 2023/2025-11, `focus-visible` 2022/2024
- Architecture: `nesting` 2023-12/2026-06, `cascade-layers` 2022/2024-09
- Color: `oklab` (oklch) 2023/2025-11, `color-mix` 2023/2025-11, `color-scheme` 2022/2024
- Values: `min-max-clamp` 2020/2023
- Motion/a11y: `prefers-reduced-motion`, `prefers-color-scheme` 2020/2022, `individual-transforms` 2022/2025,
  `scroll-snap` 2020/2022; HTML-related: `dialog` 2022/2024

Newly available (low) — id: low date
- `scope` (@scope) 2026-03-24, `container-style-queries` 2026-05-19, `light-dark` 2024-05-13,
  `relative-color` 2024-09-16, `text-wrap-balance` 2024-05-13, `starting-style` + `transition-behavior`
  2024-08-06, `view-transitions` (same-document) 2025-10-14, `registered-custom-properties` (@property)
  2024-07-09, `popover` 2025-01-27, `field-sizing` 2026-06-16, `content-visibility` 2025-09-15,
  `font-size-adjust` 2024-07-25, `details-name` 2024-09-03, `sibling-count` 2026-08-18 (very new)

Not Baseline (false) — excluded
- `anchor-positioning`, `scroll-driven-animations`, `text-wrap-pretty`, `interpolate-size`, `if`,
  `cross-document-view-transitions`, `custom-media-queries`, `css-modules` (import attributes)
- Surprises per the data: `accent-color` and `overscroll-behavior` are also NOT Baseline.
- `line-clamp` is NOT Baseline and the prefixed `-webkit-line-clamp` / `-webkit-box-orient` have no web-features
  data at all → the "truncate after N lines" template was dropped (2026-09-25).

## 6. Validators (evaluated 2026-09-25)
- lightningcss 1.33.0: parses all modern syntax (nesting, @scope, @container, @layer, :has, oklch); warns on
  misspelled selectors/at-rules; catches wrong math functions. BUT it only types the properties it models: others
  (scroll-snap-type, scroll-behavior, outline-offset, text-wrap, field-sizing...) come back as `custom`, and newer
  values (subgrid, allow-discrete) or CSS-wide keywords (inherit) as `unparsed`.
- css-tree 3.2.1 lexer (MDN syntax data): validates all of those correctly and catches unknown properties and
  wrong values, but misses `clamp(1rem 2rem)` and cannot parse native nesting.
- Used together (tools/css-check): lightningcss first; declarations it cannot type go to css-tree; a value with a
  math function in a property lightningcss knows but cannot type is invalid.

## 4. Where CSS live templates apply (verified in IDEA 2025.2 bytecode, css-impl.jar)
- Context ids: `CSS` (generic), `CSS_RULESET_LIST` (top level), `CSS_DECLARATION_BLOCK` (inside `{}`),
  `CSS_PROPERTY_VALUE` (after `prop:`). No SCSS/Less-specific ids exist.
- `CssLiveTemplateContextType.isInContext` = `PsiUtilCore.getLanguageAtOffset(file, offset).isKindOf(CSSLanguage)`.
- `SCSSLanguage`, `SASSLanguage`, `LESSLanguage` all call `Language(CSSLanguage.INSTANCE, ...)` → templates apply
  in .scss/.sass/.less. **Trap:** `.sass` is the indented syntax (no braces) → brace templates are invalid there.
  Decision pending (owner): document it (current) or write a custom context in Kotlin that excludes SASS.
- Vue/HTML `<style>`: language at offset is CSS/SCSS → expected to work; UNVERIFIED in a running IDE.

## 5. Methodologies (BEM and others)
- BEM is a naming convention (Yandex, https://en.bem.info/methodology/), not a W3C/MDN standard.
- MDN, "Using CSS nesting" → "Concatenation (is not possible)": native nesting cannot build `&__element`
  (the suffix is parsed as a type selector). BEM + nesting works only in Sass/SCSS; plain CSS BEM needs full
  selectors (`.card__title`).
- Native features now cover what BEM solved by naming: `@scope` (isolation, newly 2026-03), `@layer`
  (ordering, widely), `:where()` (zero specificity). ITCSS maps directly to `@layer` order.
- Decision (owner): **option A** — methodology-specific variants with their own abbreviation where naming
  matters (component scaffolds): `css-component` (native nesting), `css-component-bem` (full selectors),
  `css-component-scope` (@scope), BEM with `&__` for .scss. The rest of the catalog is methodology-agnostic.
  Later option C: a Settings choice + custom Kotlin macro (UI strings en/es).
- Out of scope: OOCSS/SMACSS (older, overlap BEM), utility-first (Tailwind's domain). CUBE CSS: maybe later.
