# 各階段開發紀錄

已依 TASKS 順序連續執行；本次使用者明確授權覆蓋逐階段等待確認規定。原始根目錄四份文件保留，`docs/` 為指定文件結構，根目錄 TASKS 同步進度。沒有新增既定範圍外產品功能或框架。

| 階段 | 完成項目與影響檔案 | 測試命令／實際結果 | VS Code 手動驗收 |
| --- | --- | --- | --- |
| 01 基礎 | pom、Wrapper、properties、layout、Bootstrap、首頁、安全入口 | Wrapper test：1項通過；實際127.0.0.1:8080 HTTP200 | 啟動後看首頁與本地CSS |
| 02 資料庫 | schema-v1、DatabaseConfig、Entity、Enum、Repository、DTO | Wrapper test：累計3項通過，真實SQLite CRUD／FK／多對多 | 檢查data/blog.db及表結構 |
| 03 權限 | AdminService、初始化、安全鏈、登入／登出頁 | Wrapper test：累計6項通過，帳密、CSRF、不覆寫hash | 登入正誤、未登入後台、POST登出 |
| 04 內容管理 | TaxonomyService、PostService、表單DTO、管理Controller與模板 | Wrapper test：累計10項通過，驗證、關聯、狀態、刪除、404 | 新增分類與文章，發布／撤回、測試表單錯誤 |
| 05 Markdown／圖片 | 集中renderer、AST引用、圖片Service、JSON端點、原生JS | Wrapper test：累計15項通過，XSS、檔案安全與公開權限 | 預覽、上傳、複製連結與刪除防護 |
| 06 UI | 公開清單、全文、篩選、分頁、關於本站、總覽 | Wrapper test：累計17項通過；實際Chrome桌面／手機與編輯器通過 | 依ACCEPTANCE前台與UI項目操作 |
| 07 示範內容 | 15篇Markdown、6張Java原創PNG、SeedService、版本交易 | Wrapper test：累計19項通過；首次／重啟與刻意失敗回滾通過 | 全新data啟動並比對數量，修改後重啟 |
| 08 品質交付 | 錯誤頁／例外紀錄、Session／prod政策、補償測試、文件、JAR、ZIP | clean verify：28項全過；JAR雙啟動HTTP驗收及最終Chrome檢查通過 | 使用獨立JAR與完整清單重驗 |

所有階段的開發檢查均使用 `mvnw.cmd`，本機 Maven 快取參數為 `-Dmaven.repo.local=D:/personal-blog/.m2/repository`，只屬於開發環境。相容版本已由 Maven 實際解析與建置確認。第一階段首頁曾於安全配置編譯前先跑到401，第四階段測到一處HTML標記錯誤，均先修正後重跑才繼續。

最終安全檢查找到 Bootstrap 內嵌下拉圖示與 CSP 的衝突，改成本地 SVG；完整安全政策保留。Windows Python 暫存目錄 ACL 問題已改為正常繼承權限的專案測試目錄，並清理舊目錄後重新建置。

程式運作重點：Controller只處理HTTP與表單；Service決定商業規則與交易；Repository執行參數化查詢；Entity對齊明確DDL；DTO隔離畫面與持久層。Markdown預覽與全文共用同一安全清理流程。DB交易無法回滾檔案，因此以交易完成回呼與啟動reconcile處理一致性。

尚存限制為單機SQLite／單管理員範圍，以及未在Unix主機實跑。未發現未解決的功能缺陷。後續建議先依使用者驗收清單重驗，再由使用者決定Git提交或交付；不自動新增下一階段功能。
