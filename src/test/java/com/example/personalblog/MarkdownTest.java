package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.personalblog.service.MarkdownService;

class MarkdownTest {
    private final MarkdownService markdown = new MarkdownService();

    @Test
    void formatsAndSanitizes() {
        String result =
                markdown.render(
                        "# 標題\n\n"
                                + "- 清單\n\n"
                                + "```java\n"
                                + "int x=1;\n"
                                + "```\n\n"
                                + "[安全](https://example.com)\n"
                                + "<script>alert(1)</script>\n\n"
                                + "[壞](javascript:alert%281%29)\n\n"
                                + "![追蹤](https://example.com/a.png)");
        assertThat(result)
                .contains("<h1>", "<ul>", "<pre>", "<code>", "https://example.com")
                .doesNotContain("<script", "href=\"javascript:", "src=\"https://", "onerror=");
    }

    @Test
    void referencesUseMarkdownAst() {
        String path = "/uploads/12345678-1234-1234-1234-123456789abc.png";
        assertThat(markdown.imageReferences("![圖][ref]\n\n[ref]: " + path)).containsExactly(path);
        assertThat(
                        markdown.imageReferences(
                                "`![圖](" + path + ")`\n\n" + path + "\n\n[連結](" + path + ")"))
                .isEmpty();
        assertThat(markdown.render("![圖](" + path + ")")).contains("src=\"" + path + "\"");
        assertThat(markdown.render("[壞](//evil.com) ![圖](/uploads/../secret.png)"))
                .doesNotContain("href=", "<img");
    }
}
