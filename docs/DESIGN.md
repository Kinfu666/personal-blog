# Personal Blog System — 技術設計 v1.0

## 1. 技術與架構

Java 21、Spring Boot 3.5.x（選經確認可解析、相容的固定修補版）、Maven Wrapper、Spring MVC、Thymeleaf、Bootstrap、原生 JS、Spring Data JPA/Hibernate、SQLite JDBC、與 Hibernate 同版本的 `hibernate-community-dialects`（SQLiteDialect）、Spring Security/Session/BCrypt、Bean Validation、JUnit 5、MockMvc。Markdown 使用相容的 Java Markdown parser 與 HTML sanitizer。前端資源本地打包，無 CDN 依賴。不得自行更換架構。

瀏覽器 → Controller → Service → Repository → SQLite；Controller 將 ViewModel/DTO 放入 Model，由 Thymeleaf 渲染 HTML。圖片寫入獨立本機目錄，資料庫保留 metadata。預覽與上傳可使用有限 JSON 端點，不必做完整 REST API。

## 2. 專案配置

```text
personal-blog/
  AGENTS.md
  README.md
  docs/{SPEC,DESIGN,TASKS}.md
  pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
  src/main/java/com/example/personalblog/
    controller/ service/ repository/ entity/ dto/ config/ exception/ enums/
  src/main/resources/
    templates/{public,admin,error,fragments}/
    static/{css,js,vendor}/
    db/ seed/
    application.properties
  src/test/java/com/example/personalblog/
  data/ (runtime; gitignore)
  target/ (build; gitignore)
```

## 3. 資料模型

SQLite 使用 INTEGER PRIMARY KEY、TEXT、INTEGER；以明確 DDL 建表及索引，避免依賴 Hibernate 自動推導 SQLite DDL。JPA 欄位映射與 DDL 對齊；資料庫外鍵需每次連線啟用（測試驗證）。日期統一 UTC，顯示 Asia/Taipei；使用一致 ISO 格式與 JPA converter 或經驗證的映射。表名、欄名採 snake_case。

| 表 | 欄位與約束 |
|---|---|
| users | id PK, username UNIQUE NOT NULL, password_hash NOT NULL, role NOT NULL（ADMIN）, created_at NOT NULL |
| categories | id PK, name NOT NULL, normalized_name UNIQUE NOT NULL, created_at NOT NULL |
| tags | id PK, name NOT NULL, normalized_name UNIQUE NOT NULL, created_at NOT NULL |
| posts | id PK, title NOT NULL, summary NOT NULL, content NOT NULL, status NOT NULL CHECK(DRAFT/PUBLISHED), author_id FK users, category_id FK categories, created_at, updated_at NOT NULL, published_at nullable（已發布非空） |
| post_tags | post_id FK posts, tag_id FK tags, 複合 PK；文章刪除時清除此關聯，不得連帶刪除標籤 |
| images | id PK, original_name, stored_name UNIQUE, media_type, size_bytes, uploaded_by FK users, created_at；皆 NOT NULL。路徑由固定根目錄與 stored_name 推導 |
| app_metadata | key PK, value NOT NULL；初始化/版本標記，用途為技術性資料，不是業務資料表 |

關聯：Post → User/Category `ManyToOne`；Post ↔ Tag `ManyToMany`（join table）；Image → User `ManyToOne`。採單向映射為主、LAZY；明確查詢需要的資料，避免 N+1 與 LazyInitializationException；關閉 Open Session in View，於 Service 交易內轉 DTO。分頁查詢避免直接 fetch join 多值關聯造成錯誤計數，使用 ID 分頁及批次取標籤或明確 projection。索引 posts(status,published_at,id)、posts(category_id)、post_tags(tag_id,post_id)。

## 4. 路由設計

| 方法 | 路徑 | 功能 |
|---|---|---|
| GET | `/` | 文章清單，q/category/tag/page |
| GET | `/posts/{id}` | 已發布全文 |
| GET | `/about` | 關於本站 |
| GET/POST | `/login` | 登入頁/Security 驗證 |
| POST | `/logout` | 登出 |
| GET | `/admin` | 儀表板 |
| GET | `/admin/posts` | 管理文章列表 |
| GET | `/admin/posts/new`, `/admin/posts/{id}/edit` | 表單 |
| POST | `/admin/posts`, `/admin/posts/{id}` | 新增/更新（status 決定草稿或發布） |
| POST | `/admin/posts/{id}/delete` | 刪除 |
| GET | `/admin/categories`, `/admin/tags` | 管理分類/標籤 |
| POST | `/admin/categories`, `/admin/tags` | 新增 |
| POST | `/admin/categories/{id}`, `/admin/tags/{id}` | 更新 |
| POST | `/admin/categories/{id}/delete`, `/admin/tags/{id}/delete` | 刪除 |
| GET | `/admin/images` | 圖片管理 |
| POST | `/admin/images` | multipart 上傳，回傳 JSON URL |
| POST | `/admin/images/{id}/delete` | 刪除 |
| GET | `/uploads/{storedName}` | 讀取圖片（僅已發布文章引用或已登入管理員） |
| POST | `/admin/markdown/preview` | 安全 Markdown HTML 預覽 |

管理表單成功使用 Post/Redirect/Get；失敗顯示欄位錯誤。所有 POST 保留 CSRF；JS 請求包含 meta 提供的 token/header。上傳、預覽錯誤返回一致 JSON 訊息。登入後返回後台。

## 5. 實作規則

- Controller 薄層：接收 DTO、驗證、呼叫 Service、回傳 view。Service 負責交易、發布狀態、引用檢查。Repository 使用參數綁定。不得直接由表單綁定 Entity 或 role、日期、作者 ID。
- `PostForm`, `PostListItem`, `PostDetail`, `CategoryForm`, `TagForm`, `ImageResponse` 等 DTO 依需要建立。使用建構子注入，避免不必要 interface/implementation 分拆。
- SQLite `jdbc:sqlite:.../blog.db`，設定可寫 data 路徑，啟動前建目錄；連線池採保守小容量、busy_timeout，FK 開啟。採版本化 schema 初始化（如固定版本 SQL + initializer），禁止 create-drop 用於持久化環境；不要同時使用互相衝突的 SQL/JPA DDL 初始化。測試使用獨立 temp SQLite 檔案。
- `app.data-dir`、`app.seed.enabled`、`app.admin.initial-password` 等可外部設定。展示 profile 預設只綁 `127.0.0.1`；正式 profile 不使用示範密碼，需明確提供安全初始值。禁止提交真實秘密。
- 檔案儲存 `data/uploads`；限制 MIME+檔案簽章、解碼尺寸，防止壓縮炸彈；WebP 使用能驗證解碼的相容方案。以 UUID 及允許的副檔名儲存，normalize 後檢查根目錄，不使用原始檔名當路徑。禁止 SVG/HTML、外部任意檔案。讀取經 Controller 授權、正確 Content-Type 及 nosniff。文章引用檢查應解析 Markdown 圖片 URL，不靠模糊字串比對。刪除及上傳遇 DB/IO 失敗需補償或記錄可修復狀態。
- Markdown 渲染集中於一個 Service，停用 raw HTML，清理允許的標籤與 URL；僅允許安全連結及本站受控圖片路徑。Thymeleaf 預設 escaping，只有已清理 HTML 可使用非跳脫輸出。預覽與公開頁共用同一 renderer。
- 初始化資料以持久化標記控管，示範文章及圖片和 metadata 保持一致；重啟不可修改既有資料。內嵌種子內容於 resources，不呼叫外部 AI API。
