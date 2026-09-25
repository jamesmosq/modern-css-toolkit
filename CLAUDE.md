# CLAUDE.md — Modern CSS Toolkit

JetBrains IDE plugin (Kotlin, live templates only for now) with **modern CSS** templates, each labelled with
its **Baseline** browser-support status. Owner: James Mosquera (jamesmosq).
Sister projects with the same build setup: `../bootstrap-toolkit` (Bootstrap 5.3 templates, submitted to the
Marketplace 2026-09-25) and `../photo-placeholders/photo-placeholders` (picsum.photos, Marketplace id 34516).
This project was created from bootstrap-toolkit on 2026-09-25; its generator/test design is the model.

## Read first
- `docs/RESEARCH.md` — market check, Baseline data, candidate catalog with dates, verified CSS contexts,
  BEM/methodology analysis and the owner's decisions. Do not redo that research; update it.
- Status: prototype. 4 templates (`css-container-query`, `css-fluid-type`, `css-layers`, `css-component-scope`),
  generator + 9 unit tests green. Never run in an IDE. No git remote yet (owner creates
  `github.com/jamesmosq/modern-css-toolkit`). No signing keys yet (`~/.modern-css-toolkit-signing/` does not exist;
  owner may approve copying the photo-placeholders ones, as done for bootstrap-toolkit).

## Owner decisions (2026-09-25)
- Name "Modern CSS Toolkit", id `com.jamesmosquera.moderncsstoolkit` (**permanent once published — never change**).
- Prefix `css-` for every template. This plugin is generic modern CSS; Bootstrap-specific CSS (`--bs-*`
  component vars, breakpoints, data-bs-theme) belongs to bootstrap-toolkit 0.2.0 with the `bs5-` prefix.
- Include Baseline **widely** and **newly** available features, labelled; exclude non-Baseline ones.
- Methodology: **option A** — variants with their own abbreviation where naming matters (component scaffolds:
  native nesting / BEM full selectors / @scope / BEM `&__` for SCSS); everything else methodology-agnostic.
  Option C (Settings choice + custom Kotlin macro) possible later.
- `.sass` (indented syntax) receives CSS templates too (SASS language extends CSS) and braces are invalid there:
  currently **documented only**; a Kotlin custom context excluding SASS is an open decision.
- Value rule: no generic filler that Emmet / IDE completion already gives.

## Working rules
- Replies to the owner in **Spanish**; code, comments, commits, CLAUDE.md and docs in English.
- **Never add a `Co-Authored-By: Claude` trailer** (or any Claude attribution) to commits or PRs.
- Read official docs before touching platform/Gradle code (https://plugins.jetbrains.com/docs/intellij/). Do not guess APIs.
- Never push to `main`, publish, create releases or tags without the owner's explicit OK. Work on `develop`
  (a push to `main` makes CI create a draft GitHub release). The owner sometimes switches branches from the IDE:
  check `git branch --show-current` before committing.
- Rewriting pushed history is blocked by the auto-mode classifier; give the owner the command instead.
- Plugin name rules (Marketplace): ≤30 chars, no "Plugin"/"IntelliJ"/"JetBrains", no third-party trademarks.
- Every user-visible change gets a line under `## [Unreleased]` in CHANGELOG.md. Conventional commits.
- UI strings (once a UI exists): resource bundle English + Spanish, kept in sync.
- Never name a source directory or package `build` (`.gitignore` ignores every `build` path — it bit bootstrap-toolkit).
- Windows + `core.autocrlf=true`: byte-exact vendored files go under `src/test/resources/vendor/` (`-text` in
  `.gitattributes`), otherwise hashes/data checks break after a checkout.
- JDK: Gradle needs `JAVA_HOME=C:\Users\jmosquerarei\.jdks\openjdk-22.0.1` (not set globally in this shell).

## Commands
```bash
./gradlew build                 # compile + plugin tests (generates the XML first)
./gradlew -p buildSrc test      # generator unit tests (NOT run by `check`; CI runs both)
./gradlew verifyPlugin          # Plugin Verifier (must pass before any release; ~10 min, run in background)
./gradlew buildPlugin           # build/distributions/*.zip
./gradlew signPlugin            # then, in a separate invocation: ./gradlew verifyPluginSignature
```
`runIde` opens an unlicensed IDEA where CSS/JS/Vue plugins do not load (they need `com.intellij.modules.ultimate`):
test by installing `build/distributions/*.zip` in the owner's WebStorm 2026.2 (Settings > Plugins > Install from Disk).

## Layout
- `src/templates/<category>/css-*.css` — the single source. Header comment, then the body:
  `description:`, `context: rules|declarations|value` (→ `CSS_RULESET_LIST` / `CSS_DECLARATION_BLOCK` /
  `CSS_PROPERTY_VALUE`), `baseline: widely|newly`, `features: <web-features ids>`, `var NAME: default`,
  optional `options NAME: a, b` (enum list, must contain the default) and `expr NAME: date("yyyy")`.
- `buildSrc/.../generator/` — `TemplateSource` (parse + validate), `LiveTemplateXml`, `GenerateLiveTemplates`
  task → `build/generated/liveTemplates/liveTemplates/ModernCssToolkit.xml` (resource root, not in git).
  The description gets " (Baseline: widely|newly available)" appended.
- `src/test/kotlin/.../LiveTemplatesTest.kt` — prefix, single CSS context, Baseline label, braces, description lists all.
- `src/main/resources/META-INF/plugin.xml` — description must list every template (test enforces).

## Next steps (in order)
1. **Verification pipeline** (`tools/css-check`, Node, like bootstrap-toolkit's `tools/tsx-check`):
   a) parse every template (defaults filled in, every `options` value) with `css-tree`; first prove css-tree
      accepts `@scope`, `@container`, `@layer` and nesting — if not, evaluate `lightningcss` instead;
   b) verify every `features:` id exists in `web-features` and that the declared `baseline` equals the
      *worst* status among them;
   c) stronger: map the parsed CSS (properties, at-rules, selectors, functions) to BCD keys and compute the status
      with `compute-baseline`, so an undeclared newer feature fails the build.
   Pin package versions, commit package-lock, add a CI step and re-add the npm entry to `.github/dependabot.yml`
   (removed because the folder does not exist yet).
2. Owner test in WebStorm: expansion in .css, .scss, .less, Vue `<style>` and HTML `<style>`; indentation of
   multi-line templates; the `.sass` trap.
3. Catalog (see RESEARCH.md §3), grouped: layout (grid auto-fit, subgrid, container queries, aspect-ratio,
   logical props, dvh), selectors (:has, :is/:where, :nth-child of, :focus-visible), architecture (layers, nesting,
   @scope, component scaffolds per option A), color (oklch, color-mix, light-dark, relative color, color-scheme),
   typography (clamp, text-wrap balance), motion/a11y (prefers-reduced-motion, prefers-color-scheme, @starting-style,
   view transitions, scroll-snap), components (dialog/popover entry animations).
4. Marketplace prep as in bootstrap-toolkit: description, screenshots, verifyPlugin, signing, THIRD_PARTY_NOTICES
   if any third-party text/data is shipped (web-features data is only used at build/test time).
