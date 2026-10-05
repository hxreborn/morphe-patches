const fs = require("node:fs");

const [vendorPath, chunkPath] = process.argv.slice(2);
const vendor = fs.readFileSync(vendorPath, "utf8");
const chunk = fs.readFileSync(chunkPath, "utf8");

const kind = {};
for (const m of vendor.matchAll(/[,{]([\w$]+):\(\)=>ɵɵ([a-zA-Z]+\d?)(?=[,}])/g)) kind[m[1]] = m[2];
const bindingSlots = { property: 1, domProperty: 1, attribute: 1, conditional: 1, twoWayProperty: 1, classProp: 2, styleProp: 2, classMap: 2, styleMap: 2 };
const constsArgument = { element: 2, elementStart: 2, domElement: 2, domElementStart: 2, elementContainer: 1, elementContainerStart: 1, template: 5, conditionalCreate: 5, repeaterCreate: 5 };
const any = new Proxy(function () {}, { get: (_, k) => (k === Symbol.toPrimitive ? () => "" : any), apply: () => any });

function blockEnd(text, open, opener, closer) {
  let depth = 0;
  for (let i = open; i < text.length; i++) {
    const c = text[i];
    if (c === '"' || c === "'" || c === "`") { for (i++; text[i] !== c; i++) if (text[i] === "\\") i++; }
    else if (c === opener) depth++;
    else if (c === closer && --depth === 0) return i + 1;
  }
  throw new Error(`unterminated ${opener} at ${open}`);
}

function run(source, instructions, flags) {
  const calls = [];
  const recorder = new Proxy({}, {
    get: (_, alias) => {
      const call = new Proxy(function () {}, {
        get: (_, k) => (k === Symbol.toPrimitive ? () => "" : any),
        apply: (_, __, args) => { calls.push([alias, args]); return call; },
      });
      return call;
    },
  });
  const scope = new Proxy({}, { has: () => true, get: (_, k) => (k === Symbol.unscopables ? undefined : k === instructions ? recorder : any) });
  new Function("scope", `with(scope){return (${source})}`)(scope)(flags, any);
  return calls;
}

function check(name, decls, vars) {
  const start = chunk.indexOf(`function ${name}(e,t){`);
  const source = chunk.slice(start, blockEnd(chunk, chunk.indexOf("{", start), "{", "}"));
  const opening = source.match(/^function [\w$]+\(e,t\)\{if\(1&e\)\{(?:const [\w$]+=[\w$]+\.[\w$]+\(\);)?([\w$]+)\./);
  if (!opening) return null;
  const instructions = opening[1];
  const component = name.slice(0, name.indexOf("_"));
  const owner = chunk.indexOf(`type:${component},`);
  if (owner < 0) return [`no component definition for ${component}`];
  const constsAt = chunk.indexOf("consts:", owner) + "consts:".length;
  const consts = new Function(`return ${chunk.slice(constsAt, blockEnd(chunk, constsAt, "[", "]"))}`)();
  const errors = [];

  const creation = run(source, instructions, 1);
  for (const [alias, args] of creation) {
    const at = constsArgument[kind[alias]];
    if (at !== undefined && typeof args[at] === "number" && !(args[at] < consts.length)) errors.push(`consts[${args[at]}] beyond ${consts.length}`);
  }
  const created = new Set(creation.filter(([, a]) => typeof a[0] === "number").map(([, a]) => a[0]));
  if (Math.max(...created) >= decls) errors.push(`slot ${Math.max(...created)} beyond decls ${decls}`);

  let cursor = 0;
  let bindings = 0;
  for (const [alias, args] of run(source, instructions, 2)) {
    const k = kind[alias] || "";
    if (k === "advance") cursor += args.length ? args[0] : 1;
    else if (k in bindingSlots) {
      bindings += bindingSlots[k];
      if (!created.has(cursor)) errors.push(`${k} ${args[0]} on uncreated slot ${cursor}`);
    } else if (/^(text)?[iI]nterpolate\d?$/.test(k)) bindings += Number(k.match(/\d?$/)[0] || 1);
  }

  const reserved = new Map();
  for (const m of source.slice(source.indexOf("if(2&e){")).matchAll(/\.([\w$]+)\((\d+),(\d+)?/g)) {
    const [, family, arity] = (kind[m[1]] || "").match(/^(pureFunction|pipeBind)(\d)$/) || [];
    if (family === "pureFunction") reserved.set(Number(m[2]), Number(arity) + 1);
    if (family === "pipeBind") {
      reserved.set(Number(m[3]), Number(arity) + 1);
      if (!created.has(Number(m[2]))) errors.push(`pipe ${m[2]} not created`);
    }
  }
  let next = bindings;
  for (const [offset, size] of [...reserved].sort((a, b) => a[0] - b[0])) {
    if (offset !== next) errors.push(`pure slot ${offset}, expected ${next}`);
    next = offset + size;
  }
  if (next !== vars) errors.push(`vars ${vars}, expected ${next}`);
  return errors;
}

const declarations = [...chunk.matchAll(/\b([\w$]+_Template),(\d+),(\d+)[,)]/g)];
const checked = declarations.map(([, name, decls, vars]) => [name, check(name, Number(decls), Number(vars))]).filter(([, e]) => e);
const failures = checked.filter(([, e]) => e.length);
for (const [name, errors] of failures) console.log(`${name}: ${errors.join("; ")}`);
console.log(`${checked.length} templates checked, ${failures.length} inconsistent`);
process.exit(failures.length || !checked.length ? 1 : 0);
