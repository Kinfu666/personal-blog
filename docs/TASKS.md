# TASKS.md — 開發與驗收清單

每階段先讀 SPEC/DESIGN；完成測試才勾選。Windows 使用 `mvnw.cmd`，Unix 使用 `./mvnw`。

## 01 專案基礎

- [x] 建立 Spring Boot Maven 專案、Wrapper、固定相容依賴、gitignore、基本設定。
- [x] 建立可啟動頁面、共用 Thymeleaf Layout、Bootstrap 本地資源。
- [x] `mvnw.cmd test` 成功，localhost:8080 可開啟。

## 02 資料庫

- [x] SQLite 路徑、schema 初始化、外鍵、索引及技術 metadata。
- [x] 六張業務表映射、Enum、Repository、交易與 DTO。
- [x] 真實 SQLite 暫存檔測試 CRUD、外鍵及多對多關聯。

## 03 管理員與權限

- [x] 初始 admin、BCrypt、UserDetailsService、SecurityFilterChain、登入/登出及 CSRF。
- [x] 測試登入正誤、未授權、有效/缺少 CSRF、重啟不覆寫密碼。

## 04 分類、標籤與文章

- [x] 分類/標籤 CRUD、重複名稱與引用刪除防護。
- [x] Post CRUD、DTO 驗證、草稿/發布/撤回、後台列表分頁。
- [x] 測試狀態轉換、驗證、關聯、刪除、404。

## 05 Markdown 與圖片

- [x] Markdown 安全 renderer、管理員即時預覽、插入圖片。
- [x] 圖片上傳、驗證、UUID 儲存、存取權限、引用檢查及刪除。
- [x] 測試 XSS、危險 URL、偽造副檔名、超大檔、路徑穿越、未授權、草稿圖片不可公開。

## 06 前台與後台 UI

- [x] 首頁、詳細頁、關於本站、分類/標籤/關鍵字篩選、分頁、後台儀表板。
- [x] 測試公開查詢僅已發布文章、分頁排序及組合條件；手機與桌面手動驗收。

## 07 展示內容

- [x] AI 撰寫 12 篇已發布技術文章、3 篇草稿（每篇具實質內容、Markdown、摘要、分類及標籤），建立 4 分類、10 標籤、6 張原創本地 PNG。
- [x] 種子資料與圖片打包在 resources，版本標記及失敗處理；禁止重啟重複載入或恢復刪除資料。
- [x] 新空資料目錄啟動確認數量、圖片、分頁；編輯/刪除文章後重啟確認變更保留。

## 08 品質與交付

- [x] 友善 403/404/500、日誌、移除死碼、Review 需求與安全性。
- [x] 單元與整合測試全過；執行 `mvnw.cmd clean verify`（Unix 對應 Wrapper）。
- [x] 在全新可寫資料目錄執行 `java -jar target/<實際產物>.jar`，確認無資料庫伺服器、無網路 CDN 時仍可展示；停止後再啟動確認持久化。
- [x] README 記錄必要 Java 版本、Windows/Unix 開發啟動、JAR 啟動、demo 帳密、資料位置、正式安全設定、測試、畫面截圖及限制，坦誠標示 AI 輔助開發。
- [x] 準備 GitHub 原始碼與獨立 JAR（可命名 personal-blog.jar）；不提交執行期 DB、私密資料或 target。由使用者決定上傳與寄送。

## 實際驗收紀錄（2026-11-20）

- 所有01–08階段依序完成；本次依使用者授權連續開發。
- 最終 `mvnw.cmd clean verify`：28項測試，0失敗、0錯誤、0跳過。
- 最終JAR空目錄首次啟動、HTTP後台寫入、停止與再次啟動驗證通過。
- Chrome桌面／手機、預覽、圖片上傳插入、無外部資源與CSP違規驗證通過。
- 詳見 `docs/TEST_REPORT.md`、`docs/ACCEPTANCE.md` 與 `docs/DEVELOPMENT_REPORT.md`。

## 格式化與品質整理驗收（2026-11-20）

- [x] 依規範整理 HTML／Thymeleaf、CSS、JavaScript、Java import、XML、SQL、JSON 與 Markdown，新增 `.editorconfig`。
- [x] Java／JavaScript AST、HTML 結構及屬性、CSS cascade 順序與宣告、資料與設定值等價檢查通過。
- [x] `mvnw.cmd verify`：28 項測試通過，0 失敗、0 錯誤、0 跳過。
- [x] 實際啟動前後版本：34 組桌面／手機畫面逐像素、DOM 與計算樣式完全一致。
- [x] 登入／登出、即時預覽、圖片上傳插入與刪除、JAR 首次初始化及重新啟動持久化驗收通過。
- [x] 修改前後以 `git diff --no-index` 審查；既有資料庫、第三方資源與交付封存檔雜湊保持不變。
- [x] 整理後 JAR 另存 `delivery/personal-blog-formatted.jar`，附 SHA-256；完整清單見 `docs/FORMATTING_REPORT.md`。

## Java import 整理驗收（2026-11-20）

- [x] 檢查全部 60 份 Java 原始碼：48 份正式程式、11 份測試與 1 份圖片產生腳本；實際修改 32 份正式程式與 11 份測試。
- [x] 將 103 處不必要的 FQN 改為明確 import 與簡短類別名稱，包含註解標記、泛型、例外、欄位與方法內參照；收整相關換行，原有程式邏輯不變。
- [x] 全專案統一依序分組為 static、java、javax、jakarta、第三方套件、專案套件；各組按完整 import 名稱排序，組間保留一個空行。
- [x] 以 Javac 解析實際符號，確認未使用、重複與萬用字元 import 均為 0；唯一保留的 FQN 為 `AdminService` 中 Spring Security 的 `User`，避免與專案 Entity `User` 衝突。
- [x] 使用相同 Java 21 編譯選項並排除除錯資訊，全部 64 個 class 位元組完全一致；未調整 MVC 分層、相依套件、業務規則或安全機制。
- [x] 執行 `mvnw.cmd -B -ntp -Dmaven.repo.local=D:/personal-blog/.m2/repository clean verify`：BUILD SUCCESS，28 項測試通過，0 失敗、0 錯誤、0 跳過。
- [x] 重新打包並另存 `delivery/personal-blog-imports.jar` 與 SHA-256；使用此交付 JAR 在獨立資料目錄執行既有 `scripts/jar-smoke.py`，首次初始化、登入、CSRF、HTTP 寫入及重啟持久化檢查全部通過。
- [x] `git diff --check` 通過；完整修改檔案清單、各檔用途、測試結果及 JAR 雜湊見 [import-check.json](evidence/import-check.json)。沒有已知未完成項目；後續由使用者驗收及決定 Git 提交。

VS Code 手動驗收：

1. 開啟 `SecurityConfig.java`、`PostRepository.java`、兩份 ExceptionHandler 與測試檔，確認類別名稱使用檔案上方的明確 import，分組與排序一致。
2. 在終端機執行 `.\mvnw.cmd -B -ntp '-Dmaven.repo.local=./.m2/repository' clean verify`，確認 28 項測試全部通過。
3. 執行 `java -jar delivery/personal-blog-imports.jar --server.port=18080 --app.data-dir=./target/import-manual-data`，開啟 `http://127.0.0.1:18080`，以展示帳號登入，檢查文章、預覽及圖片操作；停止後以同一命令重啟確認資料保留。
