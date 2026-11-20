## 資料庫無法替一般檔案回滾

圖片包含檔案本體與資料庫 metadata。如果先寫檔再存資料庫，資料庫失敗會留下孤兒檔案；先存資料庫再寫檔，寫檔失敗又可能留下無法讀取的紀錄。單純加上 `@Transactional` 不會讓檔案系統一起加入交易。

可行的小型設計是先驗證、寫入唯一新檔，再儲存 metadata，並在交易完成回呼中處理回滾。檔名使用 UUID，不使用使用者提供的原始名稱來決定路徑。

```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) removeNewFile();
        }
    });
```

回呼清理也可能失敗，因此要留下可追蹤的日誌，並在下一次啟動比對 metadata 和受控檔案名稱。這讓中斷後的狀態有可修復路徑，而不是假設 finally 永遠會執行。

## 刪除需要保留復原的機會

刪除前先解析所有文章的 Markdown 圖片節點，草稿也要算進引用。單純搜尋字串可能把程式碼區塊內的範例誤判成引用，或忽略 reference-style 圖片。解析器比模糊比對更能對齊實際渲染行為。

確認沒有引用後，可以把檔案原子移到同目錄的暫存刪除名稱，再刪除 metadata。交易提交才真正刪除暫存檔；若回滾就移回原名。重新啟動時，若 metadata 還存在，還原暫存檔；若 metadata 已不存在，完成清理。

## 這仍然有明確的適用範圍

這個流程適合單一程式、單一可寫資料目錄與本機檔案系統。它不是跨機器分散式交易，也不能防止管理者手動移走 uploads。備份與搬移時應停機，把 DB 和圖片一起處理。

測試應刻意製造資料庫回滾，確認上傳的新檔被移除；也要在刪除 metadata 後模擬回滾，確認原檔恢復。只測成功路徑，無法證明補償設計真的有效。對無法完全自動修復的 IO 錯誤，應保留原始例外於伺服器日誌，並向使用者顯示不含路徑細節的訊息。
