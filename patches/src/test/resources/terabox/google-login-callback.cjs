/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync(process.argv[2], 'utf8');
const block = source.match(/private static final String LOGIN_PAGE_SCRIPT = """\n([\s\S]*?)\n\s*""";/)[1];
const script = block.split('\n').map(line => line.trim().replace(/\\$/, '')).join('').replace(/\\\\/g, '\\');
const scenario = process.argv[3];
let googleCallback;
let interval;
let callbacks = 0;
let requests = 0;
let nativeDeliveries = 0;
let rejections = 0;
let nextResponse = 'success';

class Element {
    setAttribute() {}
    remove() {}
}

class ScriptElement extends Element {
    get src() {return this.url;}
    set src(value) {this.url = value;}
}

class FrameElement extends Element {
    set src(value) {
        if (value.startsWith('dubox://')) nativeDeliveries++;
    }
}

const context = {
    HTMLScriptElement: ScriptElement,
    Element,
    document: {
        createElement: name => name === 'script' ? new ScriptElement() : Object.assign(new FrameElement(), {style: {}}),
        documentElement: {appendChild() {}},
        querySelector: () => null
    },
    XMLHttpRequest: class {open() {}},
    setInterval: callback => {interval = callback; return 1;},
    clearInterval() {},
    setTimeout() {},
    fetch: async () => {
        requests++;
        if (nextResponse === 'transport-failure') throw new Error('fixture transport failure');
        const body = nextResponse === 'rejection' ? {code: 460040} : {code: 0, data: {ndus: 'fixture-session', userid: 'fixture-user'}};
        return {text: async () => JSON.stringify(body)};
    },
    google: {accounts: {id: {initialize: config => {googleCallback = config.callback;}}}},
    fixture: response => {
        assert.equal(response.code, 460040);
        rejections++;
    }
};
context.window = context;
vm.runInNewContext(script, context);
interval();
const configuration = {callback: () => {
    callbacks++;
    const element = new ScriptElement();
    element.src = '/passport/ssologin?client=android&clienttype=1&callback=fixture';
}};
const originalCallback = configuration.callback;
context.google.accounts.id.initialize(configuration);
const credential = 'fixture-google-credential';
const flush = () => new Promise(resolve => setImmediate(resolve));

async function main() {
    switch (scenario) {
        case 'opener-first':
            googleCallback({credential});
            context.__tbDeliver(credential);
            await flush();
            assert.equal(callbacks, 1);
            assert.equal(requests, 1);
            assert.equal(nativeDeliveries, 1);
            break;
        case 'popup-first':
            context.__tbDeliver(credential);
            googleCallback({credential});
            await flush();
            assert.equal(callbacks, 1);
            assert.equal(requests, 1);
            assert.equal(nativeDeliveries, 1);
            break;
        case 'initialize-twice':
            assert.equal(configuration.callback, originalCallback);
            context.google.accounts.id.initialize(configuration);
            googleCallback({credential});
            context.__tbDeliver(credential);
            await flush();
            assert.equal(configuration.callback, originalCallback);
            assert.equal(nativeDeliveries, 1);
            break;
        case 'server-rejection':
            nextResponse = 'rejection';
            googleCallback({credential});
            await flush();
            assert.equal(rejections, 1);
            assert.equal(nativeDeliveries, 0);
            nextResponse = 'success';
            context.__tbDeliver(credential);
            await flush();
            assert.equal(requests, 2);
            assert.equal(nativeDeliveries, 1);
            break;
        case 'transport-failure':
            nextResponse = 'transport-failure';
            googleCallback({credential});
            await flush();
            assert.equal(nativeDeliveries, 0);
            nextResponse = 'success';
            context.__tbDeliver(credential);
            await flush();
            assert.equal(requests, 2);
            assert.equal(nativeDeliveries, 1);
            break;
        case 'different-credential':
            googleCallback({credential});
            await flush();
            context.__tbDeliver('another-fixture-credential');
            await flush();
            assert.equal(requests, 2);
            assert.equal(nativeDeliveries, 2);
            break;
        default:
            throw new Error('Unknown scenario: ' + scenario);
    }
}
main().catch(error => {console.error(error); process.exitCode = 1;});
