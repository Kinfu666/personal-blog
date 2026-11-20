## 登入建立的是伺服器端身分

管理員送出帳號密碼後，Spring Security 驗證 BCrypt 雜湊，成功才在 Session 保存驗證狀態。瀏覽器持有的是 Session cookie，不需要把密碼或自製權限欄位放進每個請求。這種單一後台情境，Session 已能滿足需求，不必額外引入 token 架構。

![安全驗證圖示]({{image:security}})

登入成功時應保留 Session fixation 防護，更新 Session ID，避免攻擊者先知道一個固定 ID 再誘使管理員登入。Cookie 的 HttpOnly 可以降低腳本直接讀取 cookie 的風險；正式 HTTPS 環境還應設定 Secure。

## CSRF 關心請求從哪裡來

瀏覽器會自動附上符合範圍的 cookie。若另一個網站能誘導已登入的管理員送出刪除請求，單靠「有登入」不足以阻止操作。CSRF token 用來確認寫入請求確實帶著本站提供的資訊。

```html
<form action="/admin/posts/42/delete" method="post">
  <input type="hidden" name="_csrf" value="由伺服器產生的 token">
  <button>刪除文章</button>
</form>
```

這是概念示例，實際專案應由 Thymeleaf 與 Security 整合產生 token。原生 JavaScript 上傳與預覽則從 meta 取得 token 與 header 名稱，附在 fetch 請求中。不要為了讓非同步功能通過測試，就忽略整個後台的 CSRF。

## 前端確認與後端授權各有用途

刪除確認視窗可以避免誤按，但不是安全措施。使用者可以跳過按鈕直接送出 HTTP，因此每個管理路由都要在後端要求 ADMIN，所有寫入使用 POST。登出同樣使用 POST，避免第三方圖片或連結觸發登出。

登入失敗只顯示「帳號或密碼不正確」，不透露帳號是否存在。密碼只以雜湊儲存，初始化不能在每次重啟重新設定，否則環境設定錯誤可能悄悄覆寫既有帳號。

驗收時至少測試未登入讀後台、已登入但缺 CSRF 的寫入、帶有效 token 的寫入，以及登入前後 Session ID 的變化。這些測試保護的是實際存取邊界，比只確認登入頁能打開更有價值。
