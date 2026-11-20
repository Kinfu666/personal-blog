# AGENTS.md — AI 開發指引

## 任務與優先順序
你是 Personal Blog System 的開發助手。先完整閱讀 `docs/SPEC.md`、`docs/DESIGN.md`、`docs/TASKS.md`。SPEC 定義需求，DESIGN 定義技術，TASKS 定義順序。若規格衝突或需新增框架，先提出問題，不能自行擴張功能或替換技術。

## 工作方式
1. 先檢查現有檔案、Java/Maven 環境，提出本階段計畫及影響檔案。
2. 一次執行 TASKS 中一個階段。優先小而完整的改動；不覆寫使用者未授權的檔案。
3. 寫實際可運行的程式碼，不留 TODO、假功能、未實作方法或無效測試。
4. 執行測試與必要的實際啟動檢查，列出命令、結果、未完成項目；未通過不得宣稱完成。
5. 更新 TASKS 勾選已驗收項目，等待使用者確認下一階段；Git 提交由使用者決定，不自動發布或推送。

## 程式碼品質
- Java 21、Spring Boot 3.5 系列、Maven；固定相容依賴版本。優先標準 Spring 設計與建構子注入。
- Controller、Service、Repository、Entity、DTO 各自明確責任；命名清楚，避免過長方法、重複邏輯、無意義封裝、過度抽象。
- Bean Validation 搭配 Service 商業規則；所有錯誤可追蹤、所有資料寫入可驗證。
- 註解說明 WHY，不逐行翻譯程式；用繁體中文撰寫文件及介面文字，Java identifiers 使用英文。
- 保留 Spring Security CSRF、Session fixation 保護及 BCrypt；不得為了測試關閉全部安全性。資料庫與檔案系統路徑採外部設定，不寫死本機絕對路徑。
- 不加入 Vue、React、JWT、Redis、Docker、微服務架構。未取得授權不得新增付費 API 或網路依賴。示範內容由開發時生成，不設計執行期 AI 功能。
- 不提交 data/、target/、真實憑證、IDE 私人設定。JAR 另外打包交付；不將密碼或令牌寫入日誌。

## 每階段回報格式
- 完成項目
- 新增/修改檔案與其用途（重點解釋 AI 程式碼如何運作）
- 測試命令、實際通過/失敗結果
- 使用者在 VS Code 的手動驗收步驟
- 尚存問題與下一階段建議
