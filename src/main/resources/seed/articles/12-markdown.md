## Markdown 產生 HTML，安全邊界仍在 HTML

Markdown 易於撰寫，但不保證輸出安全。原始 HTML、危險連結協定、圖片載入與屬性都可能帶來風險。安全流程應先解析 Markdown，再限制 HTML 標籤與屬性，最後對連結和本站圖片路徑進行明確檢查。

![Markdown 文件圖示]({{image:markdown}})

```java
String rendered = renderer.render(parser.parse(markdown));
String cleaned = Jsoup.clean(rendered, allowedTags);
return validateLinksAndImages(cleaned);
```

這是流程示例，實際 renderer 還應停用或跳脫 raw HTML。只用正則刪掉 script 不足以防 XSS，因為瀏覽器可以透過多種標籤、屬性或編碼解讀內容。成熟的 sanitizer 能減少自行處理 HTML 語法的風險。

## 預覽和公開頁必須共用規則

如果即時預覽使用另一套瀏覽器 parser，文章可能在預覽時看起來安全，發布後卻產生不同 HTML。集中一個 Service 提供清理後的 HTML，讓預覽 API 和全文頁共用，才能用同一組測試保護兩個入口。

Thymeleaf 預設跳脫文字，標題、摘要與檔名都應維持這個預設。只有 renderer 清理後的文章 HTML，才可以使用非跳脫輸出。不要把使用者原始 Markdown 直接放到 innerHTML。

## 圖片路徑與權限需要一起設計

允許任意外部圖片，可能讓閱讀者自動連線到第三方追蹤位置。本站只接受受控的 `/uploads/UUID.png` 等路徑，實際讀取再查詢 metadata 與文章發布狀態。草稿圖片即使知道網址，訪客也不應讀到。

撤回文章時，圖片公開權限也會改變，因此不適合使用長時間公開快取。已經下載到使用者裝置的資料無法收回，但後續的新請求應立即依最新權限判定。

驗證至少包含 script、事件屬性、javascript 協定、協定相對連結、外部圖片與路徑穿越。同時保留標題、清單、程式碼、正常連結與本地圖片的正向測試，避免安全修正把必要功能一併移除。
