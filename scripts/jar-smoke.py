"""以真正 JAR 與 HTTP 驗證首次初始化、後台寫入及重啟持久化；只使用 Python 標準函式庫。"""
import argparse
import hashlib
import http.cookiejar
import json
from pathlib import Path
import re
import socket
import sqlite3
import subprocess
import uuid
import time
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser


class FormParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.values = {}

    def handle_starttag(self, tag, attrs):
        values = dict(attrs)
        if tag == "input" and values.get("name"):
            self.values[values["name"]] = values.get("value", "")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default="target/personal-blog.jar")
    args = parser.parse_args()
    jar = Path(args.jar).resolve()
    assert jar.is_file(), f"找不到 JAR：{jar}"
    target = Path("target").resolve()
    target.mkdir(exist_ok=True)
    work = target / ("jar-smoke-" + uuid.uuid4().hex)
    work.mkdir()
    java_settings = subprocess.run(["java", "-XshowSettings:properties", "-version"], capture_output=True, text=True, check=True)
    java_home = re.search(r"java.home = (.+)", java_settings.stderr).group(1).strip()
    java = Path(java_home) / "bin" / ("java.exe" if __import__("os").name == "nt" else "java")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    base = f"http://127.0.0.1:{port}"
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def request(path, fields=None):
        data = None if fields is None else urllib.parse.urlencode(fields, doseq=True).encode()
        req = urllib.request.Request(base + path, data=data, headers={"Accept": "text/html"})
        try:
            with opener.open(req, timeout=15) as response:
                return response.status, response.read(), response.geturl(), response.headers
        except urllib.error.HTTPError as error:
            return error.code, error.read(), error.geturl(), error.headers

    def text(path):
        status, body, _, _ = request(path)
        assert status == 200, (path, status)
        return body.decode("utf-8")

    def token(path):
        form = FormParser()
        form.feed(text(path))
        return form.values["_csrf"]

    def login():
        csrf = token("/login")
        status, _, url, _ = request("/login", {"username": "admin", "password": "DemoBlog!2026", "_csrf": csrf})
        assert status == 200 and url.endswith("/admin"), "管理員登入失敗"

    def start(round_number):
        log = (work / f"startup-{round_number}.log").open("w", encoding="utf-8")
        process = subprocess.Popen([str(java), "-jar", str(jar), f"--server.port={port}",
                                    "--app.data-dir=./data", "--debug=false"], cwd=work, stdout=log, stderr=subprocess.STDOUT)
        for _ in range(150):
            if process.poll() is not None:
                log.close()
                raise AssertionError((work / f"startup-{round_number}.log").read_text(encoding="utf-8"))
            try:
                status, body, _, _ = request("/")
                if status == 200 and body.decode("utf-8").count('class="post-card"') == 6:
                    return process, log
            except (OSError, urllib.error.URLError):
                pass
            time.sleep(0.1)
        process.terminate()
        process.wait(timeout=15)
        log.close()
        raise AssertionError("JAR 啟動逾時")

    def stop(process, log):
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
        log.close()

    db = work / "data" / "blog.db"
    process, log = start(1)
    try:
        with sqlite3.connect(db) as connection:
            counts = {table: connection.execute(f"SELECT count(*) FROM {table}").fetchone()[0]
                      for table in ("posts", "categories", "tags", "images", "users")}
            assert counts == {"posts": 15, "categories": 4, "tags": 10, "images": 6, "users": 1}, counts
            assert connection.execute("SELECT count(*) FROM posts WHERE status='PUBLISHED'").fetchone()[0] == 12
            original_hash = connection.execute("SELECT password_hash FROM users WHERE username='admin'").fetchone()[0]
            image_names = [row[0] for row in connection.execute("SELECT stored_name FROM images")]
            draft = connection.execute("SELECT id FROM posts WHERE status='DRAFT' ORDER BY id LIMIT 1").fetchone()[0]
            published = connection.execute("SELECT id,content,category_id FROM posts WHERE status='PUBLISHED' ORDER BY id LIMIT 1").fetchone()
        home = text("/")
        assert home.count('class="post-card"') == 6
        assert text("/?page=1").count('class="post-card"') == 6
        assert request(f"/posts/{draft}")[0] == 404
        assert "這一頁暫時找不到" in request("/missing")[1].decode()
        assert request("/admin/posts", {})[0] == 403
        assert "無法執行這項操作" in request("/admin/posts", {})[1].decode()
        assert request("/admin")[2].endswith("/login")
        for name in image_names:
            assert (work / "data" / "uploads" / name).is_file()
            assert request("/uploads/" + name)[0] == 200
        css = request("/vendor/bootstrap.min.css")
        assert css[0] == 200 and len(css[1]) > 200000
        login()
        category_token = token("/admin/categories")
        assert request("/admin/categories", {"name": "重啟驗收分類", "_csrf": category_token})[0] == 200
        tag_token = token("/admin/tags")
        assert request("/admin/tags", {"name": "重啟驗收標籤", "_csrf": tag_token})[0] == 200
        post_id, content, category = published
        csrf = token(f"/admin/posts/{post_id}/edit")
        result = request(f"/admin/posts/{post_id}", {"title": "JAR 重啟後保留的文章", "summary": "HTTP 寫入與持久化驗收", "content": content,
                                                        "categoryId": category, "status": "PUBLISHED", "_csrf": csrf})
        assert result[0] == 200 and result[2].endswith("/admin/posts")
        assert request(f"/admin/posts/{draft}/delete", {"_csrf": token("/admin/posts")})[0] == 200
        checksums = {name: hashlib.sha256((work / "data" / "uploads" / name).read_bytes()).hexdigest() for name in image_names}
    finally:
        stop(process, log)

    # 第二次在同一個工作目錄啟動，驗證從磁碟還原，而不是沿用記憶體內容。
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    process, log = start(2)
    try:
        assert "JAR 重啟後保留的文章" in text(f"/posts/{post_id}")
        assert request(f"/posts/{draft}")[0] == 404
        login()
        with sqlite3.connect(db) as connection:
            assert connection.execute("SELECT count(*) FROM posts").fetchone()[0] == 14
            assert connection.execute("SELECT count(*) FROM posts WHERE id=?", (draft,)).fetchone()[0] == 0
            assert connection.execute("SELECT count(*) FROM categories").fetchone()[0] == 5
            assert connection.execute("SELECT count(*) FROM tags").fetchone()[0] == 11
            assert connection.execute("SELECT count(*) FROM images").fetchone()[0] == 6
            assert connection.execute("SELECT password_hash FROM users WHERE username='admin'").fetchone()[0] == original_hash
            assert connection.execute("SELECT value FROM app_metadata WHERE key='seed_version'").fetchone()[0] == "1"
            assert connection.execute("PRAGMA foreign_key_check").fetchall() == []
        for name, checksum in checksums.items():
            assert hashlib.sha256((work / "data" / "uploads" / name).read_bytes()).hexdigest() == checksum
            assert request("/uploads/" + name)[0] == 200
        report = {"result": "PASS", "jar": str(jar), "work_directory": str(work), "first_start": counts,
                  "restart_posts": 14, "restart_categories": 5, "restart_tags": 11, "persisted_images": 6,
                  "checks": ["JAR 首次初始化", "每頁六篇", "草稿與無效網址 404", "真實 CSRF 403 頁面", "登入與後台保護", "HTTP 寫入", "重啟不重置密碼", "修改與刪除持久化", "圖片雜湊不變", "本地 Bootstrap 資源"]}
        (target / "jar-smoke-result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(report, ensure_ascii=False, indent=2))
    finally:
        stop(process, log)


if __name__ == "__main__":
    main()
