## 同一個瞬間，不同的顯示方式

伺服器在不同時區執行時，文章發布順序不應改變。Instant 表達時間軸上的一個瞬間，適合儲存 createdAt、updatedAt 與 publishedAt。畫面再依指定的 Asia/Taipei 時區格式化，避免把伺服器預設時區偷偷當成產品規則。

```java
Instant publishedAt = Instant.parse("2026-01-02T03:04:05Z");
DateTimeFormatter display = DateTimeFormatter
    .ofPattern("yyyy/MM/dd HH:mm")
    .withZone(ZoneId.of("Asia/Taipei"));
System.out.println(display.format(publishedAt)); // 2026/01/02 11:04
```

LocalDateTime 沒有時區或偏移量，單獨看到 `2026-01-02T11:04` 無法知道它代表哪一個瞬間。若需求是生日或營業日，LocalDate 很適合；若需求是「何時發布」，Instant 通常更清楚。

## 文字儲存要保持一致精度

SQLite 可以用 TEXT 儲存時間，但字串排序必須建立在一致格式上。混用有無小數的 ISO 字串，可能讓同一秒內的排序不符合預期。可用固定九位小數及 UTC 標記，讓字典序對應時間順序。

```java
DateTimeFormatter storage = new DateTimeFormatterBuilder()
    .appendInstant(9)
    .toFormatter();
String value = storage.format(publishedAt);
// 2026-01-02T03:04:05.000000000Z
```

欄位映射應集中在 JPA converter，讀寫都經過相同轉換。不要在每個 Repository 或 Controller 各自決定格式，否則資料修復與排序問題會非常難追查。

## 發布時間是一條商業規則

首次發布設定發布時間；編輯已發布文章保留原時間；撤回成草稿清空；再次發布建立新時間。這些行為應在同一個交易中完成，並由資料庫 CHECK 約束保證「已發布必有時間、草稿沒有發布時間」。更新時間與發布時間不同，不能只用一個欄位兼任。

同一瞬間建立多篇文章時，排序還需要穩定的第二條件，例如 ID 降冪。否則使用者翻頁時，資料庫可能以不同順序回傳同時間的資料。

較複雜的時間邏輯可注入 Clock，測試以固定時鐘驗證日期邊界，避免依賴睡眠等待。時間測試的目的在於確認規則，而不是測量機器執行得有多快。
