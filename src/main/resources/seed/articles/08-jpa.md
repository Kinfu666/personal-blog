## 一篇文章可以對應多列 SQL 結果

清單每頁六篇文章，每篇可能有三個標籤。當 SQL 把文章與標籤 join 在一起，一篇文章就可能展開成三列。如果直接對展開後的資料套用 LIMIT，六列不一定等於六篇文章。ORM 為避免錯誤，有時會改成先載入大量資料再在記憶體分頁。

```java
Page<Post> page = repository.findAll(specification, pageable);
List<Long> ids = page.stream().map(Post::getId).toList();
List<Post> hydrated = repository.findWithRelationsByIds(ids);
```

第一個查詢只對文章集合分頁，第二個查詢限定這一頁的 ID，再一次載入分類與標籤。轉換 DTO 時以 ID 建立 Map，依第一個查詢的順序輸出，因為 SQL 的 IN 條件不保證回傳順序。

## 計數、排序與篩選都要一起考慮

Page 需要總筆數，計數查詢不應因為標籤 join 而重複計算文章。若只是篩選「具有某個標籤」，可以用 EXISTS 或子查詢表達，不必把標籤欄位加入分頁結果。查詢條件應使用參數綁定，避免把使用者字串拼入 SQL。

穩定排序至少包含發布時間與 ID。只有發布時間時，同秒建立的資料可能在不同頁之間漂移；有第二個唯一排序欄位，才能讓翻頁行為可預期。

## 不要用 EAGER 掩蓋資料需求

把所有關聯設成 EAGER，會讓本來不需要標籤的查詢也付出成本，而且不保證只產生一條 SQL。較好的方向是保留 LAZY，針對 Service 的使用情境明確查詢，並在交易中產生不含代理的 DTO。

如果文章有大量 Markdown，清單可以進一步使用 projection，避免讀取全文。這應根據實際資料量與效能量測決定，而不是在小型示範專案一開始就引入複雜查詢架構。

驗證時建立超過一頁文章，讓部分文章有零、一、多個標籤，再確認每頁筆數、總數與順序。同時檢查日誌是否出現 collection fetch pagination 警告，以及 SQL 數量是否隨文章數線性增加。結果正確和查詢成本可控，兩者都值得確認。
