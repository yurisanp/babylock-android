const { chromium } = require('playwright');
(async () => {
const jobs = JSON.parse(process.argv[2]);
const b = await chromium.launch();
for (const [url, out, w, h, scale] of jobs) {
const p = await b.newPage({ viewport: { width: +w, height: +h }, deviceScaleFactor: +(scale||1) });
await p.goto('file://' + url); await p.evaluate(() => document.fonts.ready); await p.waitForTimeout(250);
await p.screenshot({ path: out, omitBackground: true }); await p.close(); }
await b.close(); })();
