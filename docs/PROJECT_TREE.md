# 專案完整目錄與各層職責

以下列出所有交付原始碼與獨立交付物。根目錄原有規格文件保留；`docs/` 是專案指定的文件入口，兩份 TASKS 進度同步。生成的 Maven 快取、建置與驗收資料不展開，也不包含於原始碼ZIP。

```text
personal-blog/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── delivery/
│   ├── SHA256SUMS.txt
│   ├── personal-blog-formatted.jar
│   ├── personal-blog-formatted.jar.sha256
│   ├── personal-blog-source.zip
│   └── personal-blog.jar
├── docs/
│   ├── evidence/
│   │   ├── browser-check.txt
│   │   ├── clean-verify.txt
│   │   ├── formatting-check.json
│   │   ├── jar-smoke.json
│   │   └── junit-summary.json
│   ├── screenshots/
│   │   ├── admin-desktop.png
│   │   ├── article-mobile.png
│   │   ├── editor-desktop.png
│   │   ├── home-desktop.png
│   │   └── home-mobile.png
│   ├── ACCEPTANCE.md
│   ├── DESIGN.md
│   ├── DEVELOPMENT_REPORT.md
│   ├── FORMATTING_REPORT.md
│   ├── PROJECT_TREE.md
│   ├── SPEC.md
│   ├── TASKS.md
│   ├── TEST_REPORT.md
│   └── THIRD_PARTY.md
├── scripts/
│   ├── GenerateSeedImages.java
│   ├── browser-check.mjs
│   └── jar-smoke.py
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── personalblog/
│   │   │               ├── config/
│   │   │               │   ├── AdminInitializer.java
│   │   │               │   ├── DatabaseConfig.java
│   │   │               │   ├── ImageReconciler.java
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   └── SeedInitializer.java
│   │   │               ├── controller/
│   │   │               │   ├── AdminPostController.java
│   │   │               │   ├── ApiExceptionHandler.java
│   │   │               │   ├── DashboardController.java
│   │   │               │   ├── ImageApiController.java
│   │   │               │   ├── ImageController.java
│   │   │               │   ├── LoginController.java
│   │   │               │   ├── PublicController.java
│   │   │               │   ├── TaxonomyController.java
│   │   │               │   └── WebExceptionHandler.java
│   │   │               ├── dto/
│   │   │               │   ├── DashboardStats.java
│   │   │               │   ├── ImageContent.java
│   │   │               │   ├── ImageResponse.java
│   │   │               │   ├── NameForm.java
│   │   │               │   ├── NamedItem.java
│   │   │               │   ├── PostDetail.java
│   │   │               │   ├── PostForm.java
│   │   │               │   ├── PostListItem.java
│   │   │               │   └── PreviewForm.java
│   │   │               ├── entity/
│   │   │               │   ├── BaseEntity.java
│   │   │               │   ├── Category.java
│   │   │               │   ├── Image.java
│   │   │               │   ├── NamedEntity.java
│   │   │               │   ├── Post.java
│   │   │               │   ├── Tag.java
│   │   │               │   ├── User.java
│   │   │               │   └── UtcInstantConverter.java
│   │   │               ├── enums/
│   │   │               │   └── PostStatus.java
│   │   │               ├── exception/
│   │   │               │   ├── NotFoundException.java
│   │   │               │   └── RuleViolation.java
│   │   │               ├── repository/
│   │   │               │   ├── CategoryRepository.java
│   │   │               │   ├── ImageRepository.java
│   │   │               │   ├── PostRepository.java
│   │   │               │   ├── PostSpecifications.java
│   │   │               │   ├── TagRepository.java
│   │   │               │   └── UserRepository.java
│   │   │               ├── service/
│   │   │               │   ├── AdminService.java
│   │   │               │   ├── DashboardService.java
│   │   │               │   ├── ImageService.java
│   │   │               │   ├── MarkdownService.java
│   │   │               │   ├── PostService.java
│   │   │               │   ├── SeedService.java
│   │   │               │   └── TaxonomyService.java
│   │   │               └── PersonalBlogApplication.java
│   │   └── resources/
│   │       ├── db/
│   │       │   └── schema-v1.sql
│   │       ├── seed/
│   │       │   ├── articles/
│   │       │   │   ├── 01-record.md
│   │       │   │   ├── 02-optional.md
│   │       │   │   ├── 03-time.md
│   │       │   │   ├── 04-mvc.md
│   │       │   │   ├── 05-validation.md
│   │       │   │   ├── 06-security.md
│   │       │   │   ├── 07-sqlite.md
│   │       │   │   ├── 08-jpa.md
│   │       │   │   ├── 09-transactions.md
│   │       │   │   ├── 10-maven.md
│   │       │   │   ├── 11-git.md
│   │       │   │   ├── 12-markdown.md
│   │       │   │   ├── 13-testing.md
│   │       │   │   ├── 14-index.md
│   │       │   │   └── 15-stream.md
│   │       │   ├── images/
│   │       │   │   ├── database.png
│   │       │   │   ├── java.png
│   │       │   │   ├── markdown.png
│   │       │   │   ├── mvc.png
│   │       │   │   ├── security.png
│   │       │   │   └── tools.png
│   │       │   └── articles.json
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   ├── chevron.svg
│   │       │   │   └── site.css
│   │       │   ├── js/
│   │       │   │   └── site.js
│   │       │   └── vendor/
│   │       │       ├── bootstrap.LICENSE.txt
│   │       │       └── bootstrap.min.css
│   │       ├── templates/
│   │       │   ├── admin/
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── images.html
│   │       │   │   ├── post-form.html
│   │       │   │   ├── posts.html
│   │       │   │   └── taxonomy.html
│   │       │   ├── error/
│   │       │   │   ├── 400.html
│   │       │   │   ├── 403.html
│   │       │   │   ├── 404.html
│   │       │   │   └── 500.html
│   │       │   ├── fragments/
│   │       │   │   ├── admin.html
│   │       │   │   └── layout.html
│   │       │   ├── public/
│   │       │   │   ├── about.html
│   │       │   │   ├── home.html
│   │       │   │   ├── login.html
│   │       │   │   └── post.html
│   │       │   └── error.html
│   │       ├── application-prod.properties
│   │       └── application.properties
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── personalblog/
│       │               ├── AdminPolicyTest.java
│       │               ├── ApiFailureTest.java
│       │               ├── ContentTest.java
│       │               ├── DatabaseTest.java
│       │               ├── ErrorHandlingTest.java
│       │               ├── FoundationTest.java
│       │               ├── ImageTest.java
│       │               ├── MarkdownTest.java
│       │               ├── PublicQueryTest.java
│       │               ├── SecurityTest.java
│       │               └── SeedLifecycleTest.java
│       └── resources/
│           └── images/
│               └── original.webp
├── .editorconfig
├── .gitattributes
├── .gitignore
├── AGENTS.md
├── DESIGN.md
├── README.md
├── SPEC.md
├── TASKS.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## 各層與主要類別

| 位置 | 職責 |
| --- | --- |
| PersonalBlogApplication | Spring Boot啟動入口，Java21可直接執行封裝JAR。 |
| controller/PublicController | 首頁查詢、已發布全文與關於本站。 |
| controller/AdminPostController、TaxonomyController | DTO綁定、Bean Validation、欄位錯誤、PRG及管理操作。 |
| controller/ImageController、ImageApiController | 圖片頁面／受控讀取、有限JSON上傳與Markdown預覽。 |
| controller/LoginController、DashboardController | 登入展示資訊與統計畫面。 |
| controller/ApiExceptionHandler、WebExceptionHandler | JSON／頁面錯誤一致性與伺服器追蹤編號，不向訪客洩漏堆疊。 |
| service/PostService | 文章交易、關聯驗證、發布時間、公開資料隔離、分頁後載入DTO。 |
| service/TaxonomyService、DashboardService | 分類標籤正規化與引用防護、唯讀統計。 |
| service/AdminService | UserDetailsService、初始密碼政策、BCrypt及不覆寫帳號。 |
| service/MarkdownService | CommonMark AST、raw HTML跳脫、jsoup清理、安全URL及圖片引用解析。 |
| service/ImageService | 大小／簽章／MIME／尺寸／解碼、UUID、授權、檔案補償與啟動修復。 |
| service/SeedService | 讀取內嵌原創內容、整批初始化與版本標記；失敗交易與檔案回滾。 |
| repository | 五個業務Repository與PostSpecifications；post_tags由Post的關聯映射維護。 |
| entity | 六張業務表映射，BaseEntity與NamedEntity共用資料欄位；UtcInstantConverter固定UTC精度。 |
| dto | 不暴露Entity的表單、清單、全文、圖片、預覽與統計資料；作者與日期不由表單指定。 |
| enums、exception | 草稿／發布狀態及可辨識的找不到資料／商業驗證錯誤。 |
| config/DatabaseConfig | 外部資料路徑、建目錄、單連線池、每連線FK、版本化DDL與schema版本檢查。 |
| config/SecurityConfig | 管理路由角色、登入／登出、保留CSRF與Session fixation、CSP與nosniff等header。 |
| config/*Initializer、ImageReconciler | 依序建立admin、修復檔案、初始化示範內容。 |
| resources/db | 明確SQL欄位、FK、CHECK、UNIQUE、索引與技術metadata。 |
| resources/templates | public／admin畫面、共用fragments與友善error頁面。 |
| resources/static | 本地Bootstrap與MIT授權、CSS／原創SVG及原生JS。 |
| resources/seed | 15篇原創Markdown、分類標籤metadata、6張已打包PNG。 |
| src/test | JUnit5、MockMvc、真實暫存SQLite與重啟／故障回滾測試；WebP正向測試圖片。 |
| scripts | 開發時Java產圖、Python標準庫JAR驗收、Node原生DevTools瀏覽器驗收；皆非執行期依賴。 |
| docs | 規格、各階段紀錄、驗收與測試證據、畫面截圖。 |
| delivery | 獨立JAR、乾淨原始碼ZIP、SHA-256；整個目錄被gitignore。 |

## 執行期與建置目錄

- `data/blog.db`：預設SQLite資料庫；`data/uploads/`：UUID圖片與可修復的刪除暫存。位置可外部設定。
- `target/classes/`、`target/test-classes/`：編譯結果；`target/surefire-reports/`：完整測試XML與文字。
- `target/personal-blog.jar`：建置產物；`target/jar-smoke-*/`、`target/browser-demo/`：實際驗收資料。
- `.m2/`、`.tools/`：本次開發環境使用的依賴快取、格式化工具與日誌，不交付、不作執行期依賴。

資料流為瀏覽器 → Controller → Service → Repository → SQLite；Thymeleaf接收DTO，Markdown非跳脫HTML僅來自共用清理器。OSIV關閉，關聯查詢與DTO轉換都在Service交易內完成。
