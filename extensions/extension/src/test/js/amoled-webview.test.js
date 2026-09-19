const fs = require('fs');
const assert = require('assert');
const js = fs.readFileSync(__dirname + '/../../main/js/amoled-webview.js', 'utf8');

const head = { children: [], appendChild(node) { head.children.push(node); } };
global.document = {
  head,
  documentElement: {},
  getElementById: id => head.children.find(node => node.id === id) || null,
  createElement: () => ({ tagName: 'STYLE' }),
};

eval(js);
eval(js);

assert.strictEqual(head.children.length, 1, 'stylesheet injected once');
const styleNode = document.getElementById('hx-amoled');
assert(styleNode.textContent.includes('html,body,'), 'page canvas blackened before the settings render');
assert(styleNode.textContent.includes('.mobile-settings.mobile-settings'), 'settings canvas targeted');
assert(styleNode.textContent.includes('background-color:#000000'), 'page background changed to black');
assert(!styleNode.textContent.includes('--background-'), 'raised surface tokens left unchanged');
console.log('PASS');
