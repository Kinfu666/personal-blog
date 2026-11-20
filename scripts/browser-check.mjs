// 直接使用 Chromium DevTools Protocol；不為專案引入額外前端測試框架。
import { spawn } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';

const base = process.env.BLOG_TEST_URL || 'http://127.0.0.1:8080';
const output = resolve('docs/screenshots');
await mkdir(output, { recursive: true });
const browser = spawn(
    process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    [
        '--headless=new',
        '--disable-gpu',
        '--no-first-run',
        '--no-default-browser-check',
        '--remote-debugging-port=9222',
        '--user-data-dir=' + resolve('target/browser-profile'),
        'about:blank'
    ],
    { windowsHide: true, stdio: 'ignore' }
);
let socket;
try {
    let targets;
    for (let n = 0; n < 100; n++) {
        try {
            targets = await (await fetch('http://127.0.0.1:9222/json')).json();
            if (targets.length) break;
        } catch {}
        await new Promise((r) => setTimeout(r, 100));
    }
    assert.ok(targets?.length, 'Chromium 啟動失敗');
    socket = new WebSocket(targets.find((t) => t.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
        socket.onopen = resolve;
        socket.onerror = reject;
    });
    let sequence = 0;
    const pending = new Map();
    const errors = [];
    const requests = [];
    const violations = [];
    socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        if (message.id) {
            const entry = pending.get(message.id);
            if (!entry) return;
            pending.delete(message.id);
            clearTimeout(entry.timer);
            message.error ? entry.reject(new Error(message.error.message)) : entry.resolve(message.result);
        } else if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
        else if (message.method === 'Network.requestWillBeSent') requests.push(message.params.request.url);
        else if (message.method === 'Log.entryAdded' && message.params.entry.source === 'security')
            violations.push(message.params.entry.text);
    };
    const cdp = (method, params = {}) =>
        new Promise((resolve, reject) => {
            const id = ++sequence;
            const timer = setTimeout(() => {
                pending.delete(id);
                reject(new Error('CDP timeout: ' + method));
            }, 15000);
            pending.set(id, { resolve, reject, timer });
            socket.send(JSON.stringify({ id, method, params }));
        });
    const evaluate = async (expression) => {
        const result = await cdp('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    const until = async (expression) => {
        for (let n = 0; n < 100; n++) {
            if (await evaluate(expression)) return;
            await new Promise((r) => setTimeout(r, 100));
        }
        throw new Error('Browser wait failed: ' + expression);
    };
    const navigate = async (path) => {
        await cdp('Page.navigate', { url: base + path });
        await until(
            `location.pathname === ${JSON.stringify(path.split('?')[0])} && document.readyState === 'complete'`
        );
        await evaluate('window.scrollTo(0, 0)');
        await evaluate('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))');
    };
    const viewport = async (width, height) =>
        cdp('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 500 });
    const capture = async (name) => {
        const screenshot = await cdp('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
        await writeFile(resolve(output, name + '.png'), Buffer.from(screenshot.data, 'base64'));
    };
    const fits = async () =>
        assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'), '頁面出現水平溢位');
    await cdp('Page.enable');
    await cdp('Runtime.enable');
    await cdp('Network.enable');
    await cdp('Log.enable');
    await viewport(1440, 1000);
    await navigate('/');
    await capture('home-desktop');
    await fits();
    await viewport(390, 844);
    await navigate('/');
    await capture('home-mobile');
    await fits();
    const articlePath = await evaluate(`document.querySelector('.post-card h2 a')?.getAttribute('href')`);
    if (articlePath) {
        await navigate(articlePath);
        await fits();
        await capture('article-mobile');
    }
    await navigate('/login');
    await evaluate(
        `document.querySelector('#username').value = 'admin'; document.querySelector('#password').value = 'DemoBlog!2026'; document.querySelector('form[action="/login"]').requestSubmit();`
    );
    await until(`location.pathname === '/admin' && document.readyState === 'complete'`);
    await viewport(1440, 1000);
    await capture('admin-desktop');
    await viewport(390, 844);
    for (const path of [
        '/admin',
        '/admin/posts',
        '/admin/categories',
        '/admin/tags',
        '/admin/images',
        '/admin/posts/new',
        '/about'
    ]) {
        await navigate(path);
        await fits();
        assert.ok(await evaluate('!!document.querySelector("main h1")'), '頁面缺少標題: ' + path);
    }
    await viewport(1440, 1000);
    await navigate('/admin/posts/new');
    const sample = '# 瀏覽器預覽\n\n<script>alert(1)</script>\n\n[危險](javascript:alert%281%29)';
    await evaluate(
        `document.querySelector('#content').value = ${JSON.stringify(sample)}; document.querySelector('#content').dispatchEvent(new Event('input'));`
    );
    await until(`document.querySelector('#markdown-preview h1')?.textContent === '瀏覽器預覽'`);
    assert.ok(
        await evaluate(`!document.querySelector('#markdown-preview script, #markdown-preview a[href^="javascript:"]')`)
    );
    await cdp('DOM.enable');
    const dom = await cdp('DOM.getDocument');
    const node = await cdp('DOM.querySelector', { nodeId: dom.root.nodeId, selector: '#editor-image' });
    await cdp('DOM.setFileInputFiles', {
        nodeId: node.nodeId,
        files: [resolve('src/test/resources/images/original.webp')]
    });
    await evaluate(`document.querySelector('#insert-image').click()`);
    await until(`document.querySelector('#editor-message').textContent.includes('圖片已插入')`);
    assert.ok(await evaluate(`document.querySelector('#content').value.includes('/uploads/')`));
    await until(`!!document.querySelector('#markdown-preview img')`);
    const uploadedUrl = await evaluate(`document.querySelector('#markdown-preview img').getAttribute('src')`);
    const presentation =
        '# 從資料邊界開始設計\n\n讓 Controller 接收意圖，Service 保護規則。\n\n- 使用 DTO 傳遞資料\n- 以交易保護更新\n- 寫測試確認結果\n\n```java\nrecord ArticleSummary(long id, String title) {}\n```\n\n![原創測試圖片](' +
        uploadedUrl +
        ')';
    await evaluate(
        `document.querySelector('#title').value = 'Markdown 預覽與圖片插入'; document.querySelector('#summary').value = '在同一個編輯畫面撰寫、預覽與整理圖片。'; document.querySelector('#content').value = ${JSON.stringify(presentation)}; document.querySelector('#content').dispatchEvent(new Event('input'));`
    );
    await until(`document.querySelector('#markdown-preview h1')?.textContent === '從資料邊界開始設計'`);
    await capture('editor-desktop');
    await navigate('/admin/images');
    await evaluate(`(async () => {
    const image = [...document.querySelectorAll('.image-thumb')].find(img => img.getAttribute('src') === ${JSON.stringify(uploadedUrl)});
    const action = image.closest('.card').querySelector('form').action;
    const header = document.querySelector('meta[name="csrf-header"]').content;
    const token = document.querySelector('meta[name="csrf-token"]').content;
    const response = await fetch(action, { method: 'POST', headers: { [header]: token } });
    if (!response.ok) throw new Error('測試圖片清理失敗');
  })()`);
    assert.deepEqual(errors, [], '瀏覽器出現未處理的 JavaScript 例外');
    assert.deepEqual(
        requests.filter((url) => /^https?:/.test(url) && new URL(url).origin !== new URL(base).origin),
        [],
        '頁面請求了外部資源'
    );
    assert.deepEqual(violations, [], '出現 CSP 違規');
    await navigate('/admin');
    await evaluate(`document.querySelector('form[action="/logout"]').requestSubmit()`);
    await until(`location.pathname === '/' && document.readyState === 'complete'`);
    console.log('PASS: 桌面與手機版、登入／登出、後台頁面、CSRF 預覽、WebP 上傳與插入、無水平溢位、無外部資源請求。');
    await cdp('Browser.close');
} finally {
    socket?.close();
    browser.kill();
}
