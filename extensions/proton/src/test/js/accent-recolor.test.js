const fs = require('fs');
const assert = require('assert');
const tones = {'109,74,255':'255,0,0','138,110,255':'255,60,40','146,146,249':'255,80,80'};
const js = fs.readFileSync(__dirname + '/../../main/js/accent-recolor.js', 'utf8')
  .replace('__TONES__', JSON.stringify(tones).replace(/"/g,"'"))
  .replace('__SHIFT__','-244.6').replace('__SATURATION__','0.62');

const decl = pairs => {
  const props = Object.keys(pairs);
  const style = { length: props.length, getPropertyValue: p => pairs[p] };
  props.forEach((p,i)=> style[i]=p);
  return style;
};
const sheet = rules => ({ cssRules: rules, ownerNode: null });
const appSheet = sheet([
  { selectorText:':root', style: decl({'--primary':'#6d4aff','--bg':'#1c1b24','--rgb':'109 74 255'}) },
]);
const sheets = [appSheet];

let observerCallback = null;
const head = {
  children: [],
  appendChild(node) {
    const at = head.children.indexOf(node);
    if (at >= 0) head.children.splice(at, 1);
    head.children.push(node);
    node.parentNode = head;
    Object.defineProperty(node, 'nextSibling', {
      get: () => head.children[head.children.indexOf(node)+1] || null, configurable: true });
    if (node.id === 'hx-accent' && !sheets.some(s => s.ownerNode === node)) sheets.push({cssRules:[], ownerNode:node});
    if (observerCallback) observerCallback([{addedNodes:[node]}]);
  },
};
global.window = {
  requestAnimationFrame: fn => fn(),
  MutationObserver: function (cb) { observerCallback = cb; this.observe = () => {}; },
};
global.requestAnimationFrame = global.window.requestAnimationFrame;
global.MutationObserver = global.window.MutationObserver;
global.setTimeout = fn => fn();
global.setInterval = () => 0;
global.clearInterval = () => {};
global.document = {
  styleSheets: sheets,
  head,
  documentElement: {},
  getElementById: id => head.children.find(n => n.id === id) || null,
  createElement: () => ({ tagName: 'STYLE', parentNode: null, nextSibling: null }),
};

eval(js);

const styleNode = document.getElementById('hx-accent');
assert(styleNode, 'override stylesheet was created');
assert(styleNode.textContent.includes('--primary:#ff0000'), 'mapped tone recoloured');
assert(!styleNode.textContent.includes('#1c1b24'), 'background left alone');
assert(styleNode.textContent.includes('--rgb:255 0 0'), 'bare triplet recoloured');
console.log('initial override:', styleNode.textContent);

const lateNode = { tagName: 'LINK', id: 'late', parentNode: null, nextSibling: null,
                   addEventListener: (_e, fn) => fn() };
sheets.push({ ownerNode: lateNode, cssRules: [
  { selectorText: '.late', style: decl({'color':'#8a6eff'}) } ]});
head.appendChild(lateNode);

assert(styleNode.textContent.includes('.late{color:#ff3c28 !important;}'), 'late stylesheet recoloured');
assert(head.children[head.children.length-1] === styleNode, 'override sheet moved back to last in head');
console.log('after late chunk:', styleNode.textContent);
console.log('PASS');
