## 缺少資料也是查詢結果

訪客開啟一篇不存在的文章，系統應回傳 404，而不是讓 null 在數個方法之間流動，最後變成難以定位的 NullPointerException。Optional 可以讓「可能找不到」成為方法簽章的一部分，要求呼叫端在邊界做出決定。

```java
public ArticleDetail loadPublished(long id) {
    return repository.findById(id)
        .filter(post -> post.getStatus() == Status.PUBLISHED)
        .map(this::toDetail)
        .orElseThrow(ArticleNotFoundException::new);
}
```

上例把草稿與不存在文章統一處理，避免公開端點洩漏草稿是否存在。真正的狀態判斷放在 Service，Controller 只負責把結果交給畫面。若管理員需要編輯草稿，就提供另一個清楚命名的服務方法，而不是讓公開方法接收模糊的布林旗標。

## 預設值也有執行成本

`orElse(createDefault())` 在呼叫 orElse 之前就會執行 createDefault，即使 Optional 有值也一樣。若建立預設值需要讀檔或查資料庫，應使用 `orElseGet(this::createDefault)`，讓運算延後到確實缺值時才發生。

```java
String title = optionalTitle.orElseGet(() -> loadFallbackTitle());
```

延遲執行不代表可以在預設值中隱藏任意副作用。若缺少文章必須回傳 404，就不應在這裡順便建立一篇文章；查詢與寫入應保持明確，尤其要讓 HTTP GET 維持唯讀。

## 什麼地方不適合放 Optional

Optional 主要用於回傳值，不適合機械式替換所有 nullable 欄位。表單必填欄位應交給 Bean Validation，JPA 欄位則依資料模型設定 nullable 與約束。集合查詢找不到資料時，直接回傳空集合通常比 `Optional<List<T>>` 更自然。

也應避免先呼叫 `isPresent()` 再到處使用 `get()`。那會把流程重新寫成分散的 null 檢查，失去集中處理缺值的好處。當兩個分支都很長時，清楚的 if/else 仍可能比一長串 map、flatMap 更容易理解。

可以為上述查詢寫三個行為測試：發布文章得到詳細資料、草稿得到 404、不存在的 ID 也得到 404。測試應關心使用者看見的結果，不必重複驗證 Optional 本身的標準函式庫行為。
