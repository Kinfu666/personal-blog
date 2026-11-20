## 一次新增文章會經過哪些責任

瀏覽器送出的表單只是使用者意圖，不能直接當成可儲存的 Entity。Controller 接收 PostForm、執行基本驗證，再把通過驗證的資料與登入帳號交給 Service。Service 決定作者、確認分類存在、處理發布時間，最後透過 Repository 寫入資料庫。

![MVC 分層圖示]({{image:mvc}})

```java
@PostMapping("/admin/posts")
public String create(@Valid PostForm form, BindingResult errors,
                     Principal principal) {
    if (errors.hasErrors()) return showForm(form);
    postService.create(form, principal.getName());
    return "redirect:/admin/posts";
}
```

BindingResult 應緊接在被驗證的表單參數後面。驗證失敗時直接重繪原表單，保留使用者輸入與欄位錯誤；成功才重新導向列表。這個 Post/Redirect/Get 流程可避免重新整理頁面時意外重送新增請求。

## Service 是商業規則的邊界

Controller 不應決定「撤回時清空發布時間」，因為同一個寫入行為將來也可能從批次初始化或測試呼叫。規則若只放在 Web 層，就容易出現繞過驗證的入口。Service 也需要驗證重要輸入與關聯，並以交易保證更新的整體性。

Repository 則描述如何查詢資料，不適合組合介面提示或處理登入跳轉。Entity 保存狀態與必要的不變條件，DTO 決定跨層傳遞的內容。每一層都有作用，但不需要為每個簡單 Service 再拆一對 interface 與 implementation。

## 關閉 Open Session in View 之後

當 OSIV 關閉，Thymeleaf 不應在渲染時才讀取尚未初始化的分類或標籤。應在 Service 的唯讀交易內完成查詢與 DTO 轉換。這會提早暴露資料需求，也讓 SQL 的數量比較容易控制。

若列表每篇文章都各查一次標籤，就會形成 N+1。解法不是把所有關聯改成 EAGER，而是針對那個使用情境明確載入一頁需要的資料。畫面不需要的作者密碼、完整 Markdown 與其他關聯，都不應成為查詢的附帶成本。

檢查分層時，可以問：如果移除瀏覽器，這條發布規則還會成立嗎？如果更換畫面，Repository 是否仍然可用？這兩個問題常比類別數量更能指出責任是否清楚。
