## 從一份文章摘要開始

文章清單需要標題、摘要和標籤，但不需要密碼雜湊或文章的可修改狀態。如果直接把 JPA Entity 交給畫面，畫面可能意外觸發延遲載入，也讓資料庫結構成為介面的隱性契約。DTO 的價值在於選擇資料，而不只是把欄位搬到另一個類別。

![Java 資料設計圖示]({{image:java}})

Java 的 record 適合表達一組已經確定的資料。它會產生建構子、存取方法、`equals`、`hashCode` 和 `toString`，讓型別的主要訊息回到資料本身。

```java
public record ArticleSummary(long id, String title, List<String> tags) {
    public ArticleSummary {
        Objects.requireNonNull(title);
        tags = List.copyOf(tags);
    }
}
```

這裡使用 compact constructor，在欄位指派前檢查標題並複製集合。呼叫端傳入的可變 List 即使之後增加元素，也不會影響已建立的摘要。`List.copyOf` 同時拒絕 null 集合與 null 元素，這個限制應該是資料契約的一部分。

## 不可變需要看整個物件圖

record 的欄位是 final，但 final 只保證參照不被重新指派。若欄位是一個可變的 Date、陣列或集合，內部內容依然可能改變。因此它提供的是淺層不可變；是否需要防禦複製，要依欄位型別決定。對 Instant、String 等不可變值則通常不必複製。

不要把 Entity 直接放進 record，然後就認為畫面取得了安全的快照。Entity 仍然連著 persistence context，關聯也可能尚未初始化。應在 Service 的唯讀交易中完成資料選擇與轉換，只讓畫面接觸真正需要的值。

## 值語意與使用限制

兩份 record 的相等性由所有 component 決定。這很適合測試 DTO，但也表示新增一個欄位會改變相等性的定義。record 是 final，不能當作需要代理或繼承的 JPA Entity；一般 Entity 仍保留明確生命週期與識別值。

練習時可建立兩份內容相同的摘要，驗證它們相等；再修改原始 tags 集合，確認摘要不受影響。最後試著呼叫摘要集合的新增方法，觀察不可修改集合如何明確拒絕操作。這些測試比單純驗證 getter 更能說明設計意圖。
