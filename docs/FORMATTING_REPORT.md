# 程式碼格式化與品質整理報告

本次完整閱讀 AGENTS、SPEC、DESIGN 與 TASKS 後，依既有 MVC 與技術選型整理。沒有新增產品功能、應用程式框架或相依套件，也沒有調整商業規則、路由或權限。Git 提交與發布仍由使用者決定。

## 完成項目與運作方式

| 類型 | 整理內容與保留條件 |
| --- | --- |
| HTML／Thymeleaf | 16 份模板以 4 空格表示父子層級，長屬性換行、功能區塊空行，保留全部表達式、屬性與文字。品牌文字與句內行內元素維持相鄰，避免多一格空白改變呈現。補齊 option 的可省略結束標籤，實際 DOM 保持一致。 |
| CSS | 每個宣告獨立一行，選擇器間空行，加入全域、版心、導覽、按鈕、文章、表單、預覽與響應式區塊註解；選擇器、規則與宣告順序、值及 important 均保持等價。沒有因靜態掃描而刪除動態使用的樣式。 |
| JavaScript | 統一 4 空格、括號、分號與函式／事件排版，拆開同一行的多個敘述。保留 CSRF、fetch、預覽清理結果、AbortController 與錯誤提示；AST 比對一致。測試腳本的 PASS 輸出是驗收結果，並非除錯訊息。 |
| Java | 維持現有類別、方法及變數命名與分層；依 Javac 已解析的符號展開 wildcard import，再移除無用 import、統一排版。60 份 Java 檔案宣告 AST 完全一致。 |
| 設定與文件 | pom.xml 展開節點；SQL 一欄一行；properties 加區塊說明；JSON 以 4 空格排版；Markdown 補齊標題空行與檔尾換行。Maven 模型、SQL token、JSON 與設定值未變。 |
| EditorConfig | 新增 UTF-8、4 空格、LF、檔尾換行與空白規範；cmd 保留 CRLF、Markdown 保留語意換行，vendor 排除格式化。 |

格式化工具僅存在於忽略的 `.tools/format-qa/`，不加入 pom 或網站執行依賴。Bootstrap、示範圖片、既有資料庫及原有交付 JAR／ZIP／校驗檔皆以 SHA-256 驗證未變。`target` 僅由正常 Maven 建置生成，沒有直接格式化或手動修改建置產物；本次使用 `verify`，避免 `clean` 刪除既有 target 下的驗收資料。

## 測試命令與實際結果

```powershell
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=D:/personal-blog/.m2/repository' verify
node --check src/main/resources/static/js/site.js
node --check scripts/browser-check.mjs
```

Maven：**BUILD SUCCESS，28 項測試，0 失敗、0 錯誤、0 跳過**，耗時 22.019 秒。真實 SQLite、CSRF、Session、XSS、發布／撤回、引用刪除防護與初始化回滾等既有測試全部通過。兩個 JavaScript 檔案的語法檢查通過。

修改前後均實際啟動 JAR，使用相同的隔離資料庫、Chrome 版本與 viewport 比對。17 種頁面／狀態各測試桌面 1440×1000 與手機 390×844，比對時以瀏覽器的 DevTools Protocol 隱藏原生捲動指示器，排除其暫態動畫，沒有修改網站 CSS；共 **34 組畫面像素完全一致**，34 組 DOM、元素尺寸與計算樣式也完全一致。涵蓋首頁兩頁、組合搜尋、無結果、兩篇全文、關於、登入、404、總覽、文章列表與草稿、分類、標籤、圖片、新增與編輯表單。

另實際操作登入／登出、含 XSS 輸入的即時預覽、WebP 上傳與插入、測試圖片刪除，全部通過，無 JavaScript 或 CSP 錯誤與外部資源請求。

```powershell
# 本次於 .tools/format-qa/functional 工作目錄，避免覆寫既有截圖
$env:BLOG_TEST_URL='http://127.0.0.1:18080'
node D:/personal-blog/scripts/browser-check.mjs

# 本次於 .tools/format-qa/jar-http 工作目錄，隔離驗收 DB
python -X utf8 D:/personal-blog/scripts/jar-smoke.py --jar D:/personal-blog/target/personal-blog.jar
```

JAR 實際 HTTP 驗收通過：首次資料為 15 篇文章、4 分類、10 標籤、6 圖片；新增分類／標籤、修改文章、刪除草稿並停止重啟後，文章 14 篇、分類 5、標籤 11、圖片 6，變更、密碼雜湊與圖片內容持久化正常。以上所有寫入都使用新建的隔離測試資料，既有 DB 未修改。

## Git 差異審查

目前目錄沒有 `.git`，因此沒有 HEAD 或 index 可供一般 git diff 比對；未擅自初始化 Git。先保存所有原始檔，再將完成後檔案鏡像，以真正的 Git no-index 比對與空白檢查：

```powershell
git -c core.autocrlf=false diff --no-index -- .tools/format-qa/before .tools/format-qa/after
git -c core.autocrlf=false diff --no-index --check -- .tools/format-qa/before .tools/format-qa/after
```

審查重點為 Thymeleaf 表達式完整、程式 AST 等價、CSS cascade 不變、無依賴或安全設定變更。差異檔在 `.tools/format-qa/changes.diff`，統計在同目錄 `diff-stat.txt`；`--check` 沒有空白錯誤輸出。檢查時僅以單次 `-c core.autocrlf=false` 排除本機 Git 換行轉換，不修改使用者 Git 設定。Git no-index 比較有差異時退出碼 1 屬預期結果，不代表測試失敗。

機器可讀摘要見 [formatting-check.json](evidence/formatting-check.json)。34 組前後截圖、DOM／樣式及比較細節保留在 `.tools/format-qa/visual-before/`、`visual-after/`、`visual-comparison.json`，既有 README 截圖沒有被覆寫。

## 使用者在 VS Code 的手動驗收

1. 開啟 `.editorconfig`、模板、site.css 與 site.js，確認 4 空格層級、每個 CSS 宣告獨立一行、長屬性與函式換行。
2. 停止正在使用 8080 的程式後執行 `mvnw.cmd verify`，確認 28 項通過。
3. 執行 `java -jar delivery/personal-blog-formatted.jar`，開啟 `http://127.0.0.1:8080`。
4. 以 `admin / DemoBlog!2026` 登入，操作文章／分類／標籤／圖片；測試預覽、發布／撤回、缺欄位回填與引用刪除提示。
5. 在瀏覽器切換桌面與 390px 手機視窗，確認導覽、表單、文章與圖片維持原來外觀。
6. 需要重啟驗收時使用獨立 `--app.data-dir`，停止後再用同一個資料位置啟動。

## 新 JAR 與尚存限制

交付：`delivery/personal-blog-formatted.jar`；SHA-256：`delivery/personal-blog-formatted.jar.sha256`。原有交付封存檔保持不變，沒有重新壓縮原始碼 ZIP。

沒有已知未解決的功能或外觀問題。Unix 尚未實機驗證，沿用既有交付限制。下一步僅需使用者依上述步驟驗收與自行決定 Git 提交；不新增功能階段。

## 完整修改檔案清單

原始碼與文件：修改 74 個既有檔案，新增 3 個檔案。另新增交付 JAR 與其 SHA-256 檔案。

### 新增

- `.editorconfig`
- `docs/FORMATTING_REPORT.md`
- `docs/evidence/formatting-check.json`

### 修改

- `DESIGN.md`
- `README.md`
- `SPEC.md`
- `TASKS.md`
- `docs/DESIGN.md`
- `docs/PROJECT_TREE.md`
- `docs/SPEC.md`
- `docs/TASKS.md`
- `docs/TEST_REPORT.md`
- `docs/evidence/jar-smoke.json`
- `docs/evidence/junit-summary.json`
- `pom.xml`
- `scripts/GenerateSeedImages.java`
- `scripts/browser-check.mjs`
- `src/main/java/com/example/personalblog/config/ImageReconciler.java`
- `src/main/java/com/example/personalblog/config/SeedInitializer.java`
- `src/main/java/com/example/personalblog/controller/AdminPostController.java`
- `src/main/java/com/example/personalblog/controller/ApiExceptionHandler.java`
- `src/main/java/com/example/personalblog/controller/ImageApiController.java`
- `src/main/java/com/example/personalblog/controller/ImageController.java`
- `src/main/java/com/example/personalblog/controller/LoginController.java`
- `src/main/java/com/example/personalblog/controller/PublicController.java`
- `src/main/java/com/example/personalblog/controller/TaxonomyController.java`
- `src/main/java/com/example/personalblog/controller/WebExceptionHandler.java`
- `src/main/java/com/example/personalblog/dto/NameForm.java`
- `src/main/java/com/example/personalblog/dto/PostForm.java`
- `src/main/java/com/example/personalblog/dto/PreviewForm.java`
- `src/main/java/com/example/personalblog/entity/BaseEntity.java`
- `src/main/java/com/example/personalblog/entity/Category.java`
- `src/main/java/com/example/personalblog/entity/Image.java`
- `src/main/java/com/example/personalblog/entity/NamedEntity.java`
- `src/main/java/com/example/personalblog/entity/Post.java`
- `src/main/java/com/example/personalblog/entity/Tag.java`
- `src/main/java/com/example/personalblog/entity/User.java`
- `src/main/java/com/example/personalblog/entity/UtcInstantConverter.java`
- `src/main/java/com/example/personalblog/service/AdminService.java`
- `src/main/java/com/example/personalblog/service/DashboardService.java`
- `src/main/java/com/example/personalblog/service/ImageService.java`
- `src/main/java/com/example/personalblog/service/MarkdownService.java`
- `src/main/java/com/example/personalblog/service/PostService.java`
- `src/main/java/com/example/personalblog/service/SeedService.java`
- `src/main/java/com/example/personalblog/service/TaxonomyService.java`
- `src/main/resources/application.properties`
- `src/main/resources/db/schema-v1.sql`
- `src/main/resources/seed/articles.json`
- `src/main/resources/static/css/site.css`
- `src/main/resources/static/js/site.js`
- `src/main/resources/templates/admin/dashboard.html`
- `src/main/resources/templates/admin/images.html`
- `src/main/resources/templates/admin/post-form.html`
- `src/main/resources/templates/admin/posts.html`
- `src/main/resources/templates/admin/taxonomy.html`
- `src/main/resources/templates/error.html`
- `src/main/resources/templates/error/400.html`
- `src/main/resources/templates/error/403.html`
- `src/main/resources/templates/error/404.html`
- `src/main/resources/templates/error/500.html`
- `src/main/resources/templates/fragments/admin.html`
- `src/main/resources/templates/fragments/layout.html`
- `src/main/resources/templates/public/about.html`
- `src/main/resources/templates/public/home.html`
- `src/main/resources/templates/public/login.html`
- `src/main/resources/templates/public/post.html`
- `src/test/java/com/example/personalblog/AdminPolicyTest.java`
- `src/test/java/com/example/personalblog/ApiFailureTest.java`
- `src/test/java/com/example/personalblog/ContentTest.java`
- `src/test/java/com/example/personalblog/DatabaseTest.java`
- `src/test/java/com/example/personalblog/ErrorHandlingTest.java`
- `src/test/java/com/example/personalblog/FoundationTest.java`
- `src/test/java/com/example/personalblog/ImageTest.java`
- `src/test/java/com/example/personalblog/MarkdownTest.java`
- `src/test/java/com/example/personalblog/PublicQueryTest.java`
- `src/test/java/com/example/personalblog/SecurityTest.java`
- `src/test/java/com/example/personalblog/SeedLifecycleTest.java`
