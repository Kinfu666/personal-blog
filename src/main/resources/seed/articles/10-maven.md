## 讓另一台機器可以從同一步開始

「我的電腦可以執行」通常藏著版本與環境差異。Maven Wrapper 把 Maven 的下載位置與版本放進專案，使用者只需要相容的 Java，就能以相同命令建置。Wrapper 的首次使用仍需要網路下載 Maven 與依賴，這與網站執行時是否需要網路是兩件事。

![命令列工具圖示]({{image:tools}})

```text
# Windows PowerShell
.\mvnw.cmd clean verify

# Linux / macOS
./mvnw clean verify
```

固定 Spring Boot parent 的修補版本，可讓它管理的一組相依套件保持相容。若另外使用 hibernate-community-dialects，版本應和 Hibernate core 完全一致，避免執行時出現方法不相容。

## 幾個生命週期階段的差別

test 會編譯並執行單元與設定好的測試；package 在其後建立封裝；verify 則繼續執行封裝後配置的驗證。clean 會清掉舊產物，讓遺留 class 不會掩蓋已刪除原始碼的問題。測試成功不能取代 JAR 的實際啟動驗證。

Spring Boot Maven Plugin 會把應用程式與相依 JAR 放進可執行封裝。交付後可以在一個新的可寫資料目錄執行，確認初始化不依賴開發者的 IDE 設定。

```text
java -jar target/personal-blog.jar --app.data-dir=./demo-data
```

## 可重現包含設定與交付邊界

資料庫位置、初始密碼與埠號應可由外部設定提供。不要在程式碼裡寫入個人電腦的絕對路徑，也不要把真實憑證放進 application.properties。展示密碼只能用於明確的展示模式，正式模式首次初始化應要求外部提供。

原始碼套件通常不包含 target、執行期 data、IDE 私人設定或本機 Maven 快取。可執行 JAR 應獨立交付，並附上 SHA-256 供接收者比對。

最後從另一個空目錄啟動 JAR，登入後修改文章、停止並重新啟動。若修改仍存在、圖片仍能讀取且種子資料未重複新增，才表示交付物真正跨過「能編譯」到「能使用」的界線。
