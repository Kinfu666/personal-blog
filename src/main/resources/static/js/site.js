document.querySelectorAll('form[data-confirm]').forEach((form) =>
    form.addEventListener('submit', (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    })
);

function csrfHeaders() {
    const token = document.querySelector('meta[name="csrf-token"]');
    const header = document.querySelector('meta[name="csrf-header"]');
    return token && header ? { [header.content]: token.content } : {};
}

async function upload(file) {
    if (!file) throw new Error('請先選擇圖片');
    if (file.size > 5 * 1024 * 1024) throw new Error('圖片最多 5 MiB');
    const data = new FormData();
    data.append('file', file);
    const response = await fetch('/admin/images', { method: 'POST', headers: csrfHeaders(), body: data });
    const result = await response.json();
    if (!response.ok || !result.url) throw new Error(result.message || '上傳失敗，請確認登入狀態後重試');
    return result;
}

const uploadForm = document.querySelector('#image-upload');
uploadForm?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const message = document.querySelector('#upload-message');
    const button = uploadForm.querySelector('button');
    button.disabled = true;
    message.textContent = '上傳中…';
    try {
        await upload(document.querySelector('#image-file').files[0]);
        window.location.reload();
    } catch (error) {
        message.textContent = error.message;
        button.disabled = false;
    }
});

document.querySelectorAll('[data-copy]').forEach((button) =>
    button.addEventListener('click', async () => {
        const message = document.querySelector('#copy-message');
        try {
            await navigator.clipboard.writeText(button.dataset.copy);
            message.textContent = '已複製 Markdown 連結';
        } catch {
            message.textContent = '無法存取剪貼簿，請選取上方連結並手動複製。';
        }
    })
);

const editor = document.querySelector('#content');
const preview = document.querySelector('#markdown-preview');
if (editor && preview) {
    let timer, request;
    const refresh = async () => {
        request?.abort();
        request = new AbortController();
        try {
            const response = await fetch('/admin/markdown/preview', {
                method: 'POST',
                signal: request.signal,
                headers: { ...csrfHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: editor.value })
            });
            const result = await response.json();
            if (!response.ok || typeof result.html !== 'string')
                throw new Error(result.message || '預覽失敗，請確認登入狀態');
            // HTML 僅來自與公開頁共用的伺服器 sanitizer。
            preview.innerHTML = result.html;
        } catch (error) {
            if (error.name !== 'AbortError') preview.textContent = error.message;
        }
    };
    editor.addEventListener('input', () => {
        clearTimeout(timer);
        timer = setTimeout(refresh, 300);
    });
    refresh();
    document.querySelector('#insert-image')?.addEventListener('click', async (event) => {
        const message = document.querySelector('#editor-message');
        event.target.disabled = true;
        message.textContent = '上傳中…';
        try {
            const result = await upload(document.querySelector('#editor-image').files[0]);
            editor.setRangeText('\n' + result.markdown + '\n', editor.selectionStart, editor.selectionEnd, 'end');
            editor.dispatchEvent(new Event('input'));
            editor.focus();
            message.textContent = '圖片已插入，請儲存文章。';
        } catch (error) {
            message.textContent = error.message;
        } finally {
            event.target.disabled = false;
        }
    });
}
