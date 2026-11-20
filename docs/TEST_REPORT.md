# 測試與交付驗證報告

最新的格式化與品質整理已再次通過 28 項 Maven 測試、34 組逐像素／DOM／樣式比對及實際 JAR 重啟驗收，詳見 [FORMATTING_REPORT.md](FORMATTING_REPORT.md)。以下保留初版交付驗證紀錄。

驗證日期：2026-11-20（Asia/Taipei）。實際環境為 Windows、Oracle Java 21.0.11、Maven Wrapper 3.3.4／Maven 3.9.15、Spring Boot 3.5.14、Hibernate 6.6.49.Final、SQLite JDBC 3.49.1.0。

## 最終建置

```powershell
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=D:/personal-blog/.m2/repository' clean verify
```

**BUILD SUCCESS；28 tests，0 failures，0 errors，0 skipped。** 最終執行於臺北時間16:43:28完成，耗時26.401秒，產生 `target/personal-blog.jar`。本機快取路徑僅為驗證環境參數，一般使用者可直接執行 `mvnw.cmd clean verify`。

| 測試類別 | 數量 | 驗證內容 |
| --- | ---: | --- |
| AdminPolicyTest | 2 | prod 拒絕空白／展示密碼、既有帳號不覆寫、BCrypt位元組上限 |
| ApiFailureTest | 1 | 非預期上傳錯誤維持JSON、錯誤編號、隱藏DB診斷細節 |
| ContentTest | 5 | CRUD、狀態與時間、關聯與引用刪除、大小寫／空白、欄位長度邊界、CSRF與表單回填 |
| DatabaseTest | 2 | 真實SQLite CRUD、多對多、固定精度時間、重建連線FK、schema metadata |
| ErrorHandlingTest | 3 | 400／404／405、403／500友善頁不洩漏例外、錯誤追蹤編號 |
| FoundationTest | 1 | 真正啟動127.0.0.1:8080、HTTP首頁200、本地Bootstrap |
| ImageTest | 5 | PNG／JPEG／WebP解碼、格式／MIME／大小／尺寸、路徑防護、草稿與公開權限、引用解析、回滾與啟動修復、API權限 |
| MarkdownTest | 2 | 正常格式、raw HTML、危險協定／外部圖片、Markdown AST圖片引用 |
| PublicQueryTest | 1 | 僅發布、每頁六篇、同時間ID排序、組合條件、保留分頁、全文／關於／總覽 |
| SecurityTest | 4 | 登入正誤、後台權限、CSRF、BCrypt、不覆寫密碼、Session fixation與登出失效 |
| SeedLifecycleTest | 2 | 15／12／3／4／10／6初始化、實際重啟保留修改刪除、觸發器中斷整批回滾及重試 |

測試使用獨立暫存SQLite檔案，沒有以H2替代。刻意失敗案例會在測試日誌出現預期的SQLite trigger與例外堆疊；斷言確認回滾結果，這些並非未解決失敗。Mockito的動態agent警告不影響結果。

## 可執行 JAR 真實 HTTP 驗收

```powershell
python -X utf8 scripts/jar-smoke.py
```

**PASS。** 腳本在全新可寫工作目錄以 `java -jar` 啟動最終JAR，使用真正Cookie／CSRF／HTTP表單，而非MockMvc。

| 檢查 | 實際結果 |
| --- | --- |
| 首次初始化 | users=1、posts=15、published=12、drafts=3、categories=4、tags=10、images=6 |
| 前台與資源 | 第一／二頁各六篇；草稿404；六張PNG可讀；Bootstrap本地載入 |
| 權限與錯誤頁 | 未登入後台導向登入；缺CSRF返回真正403友善頁；無效網址404 |
| 寫入 | 登入後以HTTP新增分類與標籤、更新發布文章、刪除一篇草稿 |
| 停止再啟動 | posts=14、categories=5、tags=11、images=6，修改標題保留，刪除草稿未復原 |
| 身分與檔案 | admin雜湊未變、六張圖片SHA-256未變且仍可讀、foreign_key_check無違反 |

驗收資料保留於忽略的 `target/jar-smoke-<隨機值>/`，不進入原始碼交付。摘要見 [jar-smoke.json](evidence/jar-smoke.json)。

另以最終JAR執行prod、空初始密碼與新目錄，確認啟動失敗並明確要求 `BLOG_ADMIN_PASSWORD`；預期拒絕結果通過。

## 真正瀏覽器驗收

```powershell
# 先以獨立資料目錄啟動最終JAR
java -jar target/personal-blog.jar --app.data-dir=./target/browser-demo --debug=false
node scripts/browser-check.mjs
```

**PASS。** 使用本機Chrome與原生DevTools Protocol，無npm套件或新增前端框架。

- 桌面1440×1000與手機390×844：首頁、文章全文、後台管理頁無水平溢位。
- 真正登入表單、Session、POST登出可操作。
- 預覽的script與javascript連結不執行；正向Markdown顯示正常。
- 透過檔案輸入控制項上傳WebP，插入Markdown並顯示預覽，成功後刪除測試圖片。
- 無未處理JavaScript例外、無CSP違規、頁面沒有外部資源請求。
- 截圖位於 [screenshots](screenshots/)，README包含主要畫面。

## 已解決與限制

開發中發現的安全入口fallback、HTML option標記、SQLite例外型別與Bootstrap內嵌圖示CSP衝突均已修正後重驗。Windows舊暫存目錄ACL造成的clean失敗已清理並修正腳本建立方式，最終clean verify成功。

未發現未解決的功能缺陷。尚未在Unix主機實際執行，正式HTTPS部署及多人／多節點負載不屬本次驗收範圍。完整逐項操作見 [ACCEPTANCE.md](ACCEPTANCE.md)。
