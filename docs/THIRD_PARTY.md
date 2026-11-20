# 第三方元件

本專案使用 Maven Central 的固定版本；Spring Boot 管理的依賴採 3.5.14 parent/BOM，不另外混用不同版本。實際元件封裝在可執行 JAR 的 `BOOT-INF/lib`，各套件自身授權與 notice 保留在其 JAR 中。

| 元件 | 版本 | 用途／授權 |
| --- | --- | --- |
| Spring Boot | 3.5.14 | MVC、Thymeleaf、Security、Validation、測試整合；Apache 2.0 |
| Hibernate ORM / community dialects | 6.6.49.Final（同版本） | JPA 與 SQLite dialect；LGPL 2.1 |
| SQLite JDBC | 3.49.1.0 | 內嵌 SQLite；Apache 2.0，SQLite 本身為 public domain |
| CommonMark Java | 0.24.0 | Markdown AST 與 HTML renderer；BSD 2-Clause |
| jsoup | 1.21.2 | HTML 白名單清理；MIT |
| TwelveMonkeys ImageIO WebP | 3.12.0 | 純 Java WebP 解碼；BSD 3-Clause |
| Bootstrap CSS | 5.3.8 | 本地 RWD 樣式；MIT，完整授權隨 `static/vendor/bootstrap.LICENSE.txt` 打包 |
| Maven Wrapper | 3.3.4，Maven 3.9.15 | 建置工具；Apache 2.0 |

示範文章與六張幾何 PNG 為本次開發產生，未下載外部文章或圖片；產圖程式為 `scripts/GenerateSeedImages.java`。測試 WebP 為開發時繪製的原創色塊。沒有執行期網路內容依賴、付費 API 或 AI API。
