// Verifies every CSS live template: it must parse as valid modern CSS and its declared Baseline status must
// match the status computed from the code with the official web-features data.
//
// Usage (after ./gradlew build, which writes build/generated/templateManifest.json):
//   npm ci --prefix tools/css-check && npm --prefix tools/css-check run check
// Self-test (templates that must fail): npm --prefix tools/css-check run selftest
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { transform } from 'lightningcss';
import { features } from 'web-features';

const here = dirname(fileURLToPath(import.meta.url));

const RANK = { high: 2, low: 1, false: 0 };
const worse = (a, b) => (RANK[b] < RANK[a] ? b : a);

// BCD key -> { feature, baseline }. The status is the worse of the key's own status and its feature's overall
// status: e.g. `anchor-name` alone is "low" but anchor positioning as a whole is not Baseline, and a template
// should not be labelled Baseline while the feature it belongs to is not (that is what web.dev shows).
const keyIndex = new Map();
for (const [id, feature] of Object.entries(features)) {
  for (const [key, status] of Object.entries(feature.status?.by_compat_key ?? {})) {
    keyIndex.set(key, { feature: id, baseline: worse(status.baseline, feature.status.baseline) });
  }
}
const LABEL = { high: 'widely', low: 'newly' };

/** Every expansion a user can produce: defaults, plus one per option of each `options` variable. */
function expansions(t) {
  const fill = (overrides) => t.variables.reduce(
    (css, v) => css.replaceAll(`$${v.name}$`, overrides[v.name] ?? v.default),
    t.body.replaceAll('$END$', ''),
  );
  return [fill({}), ...t.variables.flatMap((v) => v.options.map((o) => fill({ [v.name]: o })))];
}

/** Puts the snippet where the IDE would put it: at stylesheet level or inside a rule. */
function wrap(context, css) {
  if (context === 'rules') return css;
  if (context === 'declarations') return `.probe {\n${css}\n}`;
  throw new Error(`context '${context}' is not supported by css-check yet`);
}

/** Parse errors, misspelled selectors/at-rules, unknown properties and invalid values. */
function syntaxProblems(css) {
  const problems = [];
  let result;
  try {
    result = transform({
      filename: 'template.css', code: Buffer.from(css), errorRecovery: true,
      visitor: {
        Declaration(d) {
          if (d.property === 'custom' && !d.value.name.startsWith('--')) problems.push(`unknown property '${d.value.name}'`);
          if (d.property === 'unparsed' && !JSON.stringify(d.value.value).includes('"type":"var"')) {
            problems.push(`invalid value for '${d.value.propertyId.property}'`);
          }
        },
      },
    });
  } catch (e) {
    return [`parse error: ${e.message}`];
  }
  for (const w of result.warnings) problems.push(`warning: ${w.message}`);
  return problems;
}

/** BCD keys used by the snippet (only keys that web-features knows are kept). */
function detectKeys(css) {
  const keys = new Set();
  const add = (...candidates) => candidates.forEach((k) => keyIndex.has(k) && keys.add(k));

  // At-rules and the features inside @media / @container preludes.
  for (const m of css.matchAll(/@([a-z-]+)([^{;]*)/g)) {
    const [, rule, prelude] = m;
    add(`css.at-rules.${rule}`);
    for (const f of prelude.matchAll(/\(\s*([a-z-]+)\s*(?=[:<>=)])/g)) add(`css.at-rules.${rule}.${f[1]}`);
    if (rule === 'media' && /[<>]=?/.test(prelude)) add('css.at-rules.media.range_syntax');
  }
  // Selectors: pseudo-classes/elements and nesting, taken from the text before each '{'.
  for (const m of css.matchAll(/([^{};]*)\{/g)) {
    const selector = m[1].trim();
    if (selector.startsWith('@')) continue;
    for (const p of selector.matchAll(/::?([a-z-]+)/g)) add(`css.selectors.${p[1]}`);
    if (selector.includes('&') || isNested(css, m.index + m[0].length - 1)) add('css.selectors.nesting');
  }
  // Declarations: property, keyword values, functions and units.
  for (const m of css.matchAll(/(?:^|[;{\s])(-?[a-z][a-z-]*)\s*:\s*([^;{}]+)(?=[;}]|$)/gm)) {
    const [, property, value] = m;
    if (property.startsWith('--')) continue;
    add(`css.properties.${property}`);
    for (const word of value.matchAll(/(?<![\w-])([a-z][a-z-]*)(?![\w(-])/g)) add(`css.properties.${property}.${word[1]}`);
    for (const fn of value.matchAll(/([a-z-]+)\(/g)) add(`css.types.${fn[1]}`, `css.types.color.${fn[1]}`, `css.types.image.gradient.${fn[1]}`);
    for (const u of value.matchAll(/\d(?:\.\d+)?([a-zA-Z]+)\b/g)) {
      const unit = u[1];
      const family = { dv: 'dynamic', sv: 'small', lv: 'large' }[unit.slice(0, 2)];
      if (family && /^(dv|sv|lv)(h|w|i|b|min|max)$/.test(unit)) add(`css.types.length.viewport_percentage_units_${family}`);
      else if (/^cq(w|h|i|b|min|max)$/.test(unit)) add('css.types.length.container_query_length_units');
      else add(`css.types.length.${unit}`);
    }
  }
  return keys;
}

/**
 * A rule is nested (CSS nesting) when its closest enclosing block is a style rule. Rules directly inside an
 * at-rule (@media, @container, @layer, @scope...) are not nesting.
 */
function isNested(css, index) {
  const stack = []; // true = style rule block, false = at-rule block
  let start = 0;
  for (let i = 0; i < index; i++) {
    if (css[i] === '{') {
      stack.push(!css.slice(start, i).split(/[;}]/).pop().trim().startsWith('@'));
      start = i + 1;
    } else if (css[i] === '}') {
      stack.pop();
      start = i + 1;
    } else if (css[i] === ';') {
      start = i + 1;
    }
  }
  return stack.length > 0 && stack[stack.length - 1];
}

/** All problems of one template; an empty list means it passes. */
export function checkTemplate(t) {
  const problems = [];
  const declared = new Map();
  for (const id of t.features) {
    const f = features[id];
    if (!f) problems.push(`'${id}' is not a web-features id`);
    else declared.set(id, f.status.baseline);
  }

  const detected = new Map(); // key -> { feature, baseline }
  expansions(t).forEach((css, i) => {
    const where = i === 0 ? 'defaults' : `option ${i}`;
    let wrapped;
    try { wrapped = wrap(t.context, css); } catch (e) { problems.push(e.message); return; }
    for (const p of syntaxProblems(wrapped)) problems.push(`${where}: ${p}`);
    for (const key of detectKeys(wrapped)) detected.set(key, keyIndex.get(key));
  });

  for (const [key, { feature, baseline }] of detected) {
    if (baseline === false) problems.push(`uses ${key} (${feature}), which is not Baseline`);
    else if (baseline === 'low' && !declared.has(feature)) problems.push(`uses ${key} (${feature}, newly available): add '${feature}' to features`);
  }
  for (const [id, baseline] of declared) {
    if (baseline === false) problems.push(`feature '${id}' is not Baseline`);
  }

  const statuses = [...declared.values(), ...[...detected.values()].map((d) => d.baseline)];
  const worst = statuses.reduce(worse, 'high');
  if (worst !== false && LABEL[worst] !== t.baseline) {
    problems.push(`declares 'baseline: ${t.baseline}' but the code is Baseline ${LABEL[worst]} available`);
  }
  return { problems, detected, worst };
}

function main() {
  const manifestPath = join(here, '../../build/generated/templateManifest.json');
  const templates = JSON.parse(readFileSync(manifestPath, 'utf8'));
  if (templates.length === 0) throw new Error(`no templates in ${manifestPath}`);
  let failed = 0;
  for (const t of templates) {
    const { problems, detected, worst } = checkTemplate(t);
    const summary = [...new Set([...detected.values()].map((d) => `${d.feature}:${d.baseline}`))].sort().join(', ');
    if (problems.length > 0) {
      failed++;
      console.error(`✗ ${t.name}\n    ${problems.join('\n    ')}`);
    } else {
      console.log(`✓ ${t.name} — Baseline ${LABEL[worst]} (${summary})`);
    }
  }
  if (failed > 0) {
    console.error(`\n${failed} of ${templates.length} templates failed.`);
    process.exit(1);
  }
  console.log(`\nCSS check passed: ${templates.length} templates, web-features ${JSON.parse(readFileSync(join(here, 'node_modules/web-features/package.json'), 'utf8')).version}.`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) main();
