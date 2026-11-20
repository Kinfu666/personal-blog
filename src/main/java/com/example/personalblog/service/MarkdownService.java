package com.example.personalblog.service;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {
    private static final Pattern LOCAL_IMAGE =
            Pattern.compile(
                    "/uploads/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|webp)");
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer =
            HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();
    private final Safelist allowed =
            new Safelist()
                    .addTags(
                            "p",
                            "br",
                            "hr",
                            "h1",
                            "h2",
                            "h3",
                            "h4",
                            "h5",
                            "h6",
                            "strong",
                            "em",
                            "blockquote",
                            "ul",
                            "ol",
                            "li",
                            "pre",
                            "code",
                            "a",
                            "img")
                    .addAttributes("a", "href", "title")
                    .addAttributes("img", "src", "alt", "title")
                    .addAttributes("ol", "start");

    public String render(String markdown) {
        String cleaned = Jsoup.clean(renderer.render(parser.parse(markdown)), allowed);
        var document = Jsoup.parseBodyFragment(cleaned);
        document.select("a[href]")
                .forEach(
                        link -> {
                            if (!safeLink(link.attr("href"))) link.removeAttr("href");
                            else link.attr("rel", "nofollow noopener noreferrer");
                        });
        document.select("img")
                .forEach(
                        img -> {
                            if (!isLocalImage(img.attr("src"))) img.remove();
                            else img.attr("loading", "lazy").attr("decoding", "async");
                        });
        return document.body().html();
    }

    public Set<String> imageReferences(String markdown) {
        Set<String> paths = new LinkedHashSet<>();
        parser.parse(markdown)
                .accept(
                        new AbstractVisitor() {
                            @Override
                            public void visit(Image image) {
                                if (isLocalImage(image.getDestination()))
                                    paths.add(image.getDestination());
                                visitChildren(image);
                            }
                        });
        return paths;
    }

    public boolean isLocalImage(String url) {
        return LOCAL_IMAGE.matcher(url).matches();
    }

    private boolean safeLink(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null)
                return !value.startsWith("//")
                        && !value.contains("\\")
                        && !value.chars().anyMatch(c -> c < 32);
            return Set.of("http", "https", "mailto")
                    .contains(uri.getScheme().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}
