// Proves that check.mjs catches each kind of mistake: every broken fixture must fail with the expected
// message and the valid one must pass. Run: npm --prefix tools/css-check run selftest
import { checkTemplate } from './check.mjs';

const t = (body, extra = {}) => ({
  name: 'css-fixture', context: 'rules', baseline: 'widely', features: ['min-max-clamp'], variables: [], body, ...extra,
});

const mustFail = {
  'baseline label lower than the code (@scope is newly)': [
    t('@scope (.card) { :scope { color: red; } }', { features: ['scope'] }), "declares 'baseline: widely'"],
  'baseline label higher than the code': [
    t('.a { font-size: clamp(1rem, 2vw, 2rem); }', { baseline: 'newly' }), "declares 'baseline: newly'"],
  'newly available feature not declared (light-dark)': [
    t('.a { color: light-dark(black, white); }', { baseline: 'newly' }), "add 'light-dark' to features"],
  'not Baseline at all (anchor positioning)': [
    t('.a { anchor-name: --tip; }'), 'not Baseline'],
  'declared feature that is not Baseline': [
    t('.a { color: red; }', { features: ['anchor-positioning'] }), "feature 'anchor-positioning' is not Baseline"],
  'unknown web-features id': [
    t('.a { color: red; }', { features: ['no-such-feature'] }), 'is not a web-features id'],
  'invalid value (missing comma in clamp)': [
    t('.a { font-size: clamp(1rem 2rem); }'), "invalid value for 'font-size'"],
  'unknown property': [
    t('.a { colr: red; }'), "unknown property 'colr'"],
  'misspelled pseudo-class': [
    t('.a:hovr { color: red; }'), 'warning'],
  'misspelled at-rule': [
    t('@contianer (width > 1px) { .a { color: red; } }'), 'warning'],
  'broken option value': [
    t('.a { container-type: $TYPE$; }', {
      features: ['container-queries'],
      variables: [{ name: 'TYPE', default: 'inline-size', options: ['inline-size', 'inline-sise'] }],
    }), "option 2: invalid value for 'container-type'"],
};

let failures = 0;

// Nesting detection: nested style rules count, rules directly inside an at-rule (e.g. @scope) do not.
const nestingCases = [
  ['.card { .title { color: red; } }', true],
  ['.card { &:hover { color: red; } }', true],
  ['@scope (.card) { :scope { color: red; } .title { color: blue; } }', false],
  ['@media (width >= 600px) { .card { color: red; } }', false],
  ['@layer base { .card { color: red; } }', false],
];
for (const [css, expected] of nestingCases) {
  const found = checkTemplate(t(css, { features: ['nesting', 'scope', 'cascade-layers'], baseline: 'newly' })).detected.has('css.selectors.nesting');
  const ok = found === expected;
  if (!ok) failures++;
  console.log(`${ok ? '✓' : '✗'} nesting ${expected ? 'detected' : 'not detected'} in: ${css}`);
}
for (const [name, [template, expected]] of Object.entries(mustFail)) {
  const { problems } = checkTemplate(template);
  const ok = problems.some((p) => p.includes(expected));
  if (!ok) failures++;
  console.log(`${ok ? '✓' : '✗'} catches: ${name}${ok ? '' : `\n    expected "${expected}", got: ${JSON.stringify(problems)}`}`);
}

const valid = t('.card { container-type: inline-size; }\n@container (width >= 400px) { .title { font-size: clamp(1rem, 2cqi, 2rem); } }',
  { features: ['container-queries'] });
const { problems } = checkTemplate(valid);
if (problems.length > 0) failures++;
console.log(`${problems.length === 0 ? '✓' : '✗'} accepts a valid template${problems.length ? `: ${JSON.stringify(problems)}` : ''}`);

if (failures > 0) {
  console.error(`\nSelf-test failed: ${failures} case(s).`);
  process.exit(1);
}
console.log(`\nSelf-test passed: ${Object.keys(mustFail).length} mistakes caught, valid template accepted.`);
