package com.example.personalblog.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.example.personalblog.dto.ImageContent;
import com.example.personalblog.dto.ImageResponse;
import com.example.personalblog.entity.Image;
import com.example.personalblog.enums.PostStatus;
import com.example.personalblog.exception.NotFoundException;
import com.example.personalblog.exception.RuleViolation;
import com.example.personalblog.repository.ImageRepository;
import com.example.personalblog.repository.PostRepository;
import com.example.personalblog.repository.UserRepository;

@Service
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private final ImageRepository images;
    private final UserRepository users;
    private final PostRepository posts;
    private final MarkdownService markdown;
    private final Path root;

    public ImageService(
            ImageRepository images,
            UserRepository users,
            PostRepository posts,
            MarkdownService markdown,
            @Value("${app.data-dir}") String directory) {
        this.images = images;
        this.users = users;
        this.posts = posts;
        this.markdown = markdown;
        this.root = Path.of(directory).toAbsolutePath().normalize().resolve("uploads");
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> list() {
        return images.findAllByOrderByIdDesc().stream().map(this::response).toList();
    }

    @Transactional
    public ImageResponse upload(MultipartFile file, String username) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw invalid("圖片不得為空，且每張最多 5 MiB");
        try {
            byte[] bytes;
            try (var input = file.getInputStream()) {
                bytes = input.readNBytes((int) MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) throw invalid("圖片最多 5 MiB");
            return store(bytes, file.getOriginalFilename(), file.getContentType(), username);
        } catch (IOException error) {
            throw new UncheckedIOException("圖片讀取失敗", error);
        }
    }

    @Transactional
    public ImageResponse store(
            byte[] bytes, String originalName, String mediaType, String username) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid("圖片不得為空，且每張最多 5 MiB");
        try {
            String extension = validate(bytes, mediaType);
            String stored = UUID.randomUUID() + "." + extension;
            Path destination = resolve(stored);
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            // 寫檔先於資料庫提交；交易回滾時移除新檔，避免 metadata 與檔案分離。
            afterCompletion(
                    committed -> {
                        if (!committed) deleteQuietly(destination);
                    });
            String original = Optional.ofNullable(originalName).orElse("圖片");
            original = original.replaceAll("[\\p{Cntrl}]", "_");
            if (original.length() > 255) original = original.substring(0, 255);
            Image image =
                    new Image(
                            original,
                            stored,
                            "image/" + (extension.equals("jpg") ? "jpeg" : extension),
                            bytes.length,
                            users.findByUsername(username).orElseThrow(NotFoundException::new));
            images.saveAndFlush(image);
            return response(image);
        } catch (IOException error) {
            throw new UncheckedIOException("圖片儲存失敗", error);
        }
    }

    @Transactional
    public void delete(Long id) {
        Image image = images.findById(id).orElseThrow(NotFoundException::new);
        if (referenced(image.getStoredName(), false)) throw invalid("仍有文章引用此圖片，請先移除文章內的圖片連結");
        Path file = resolve(image.getStoredName());
        Path trash = root.resolve(image.getStoredName() + ".deleted");
        try {
            if (Files.exists(file)) Files.move(file, trash, StandardCopyOption.ATOMIC_MOVE);
            afterCompletion(
                    committed -> {
                        try {
                            if (committed) Files.deleteIfExists(trash);
                            else if (Files.exists(trash))
                                Files.move(trash, file, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException error) {
                            log.error("圖片補償失敗；下次啟動將依 metadata 修復檔案 {}", trash.getFileName(), error);
                        }
                    });
            images.delete(image);
            images.flush();
        } catch (IOException error) {
            throw new UncheckedIOException("圖片刪除失敗", error);
        }
    }

    @Transactional(readOnly = true)
    public ImageContent read(String stored, boolean admin) {
        Path file = resolve(stored);
        Image image = images.findByStoredName(stored).orElseThrow(NotFoundException::new);
        if (!admin && !referenced(stored, true)) throw new NotFoundException();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new NotFoundException();
        try {
            return new ImageContent(Files.readAllBytes(file), image.getMediaType());
        } catch (NoSuchFileException error) {
            throw new NotFoundException();
        } catch (IOException error) {
            throw new UncheckedIOException("圖片讀取失敗", error);
        }
    }

    @Transactional(readOnly = true)
    public void reconcile() throws IOException {
        try (var files = Files.list(root)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".deleted")) {
                    String stored = name.substring(0, name.length() - 8);
                    if (images.findByStoredName(stored).isPresent())
                        Files.move(file, resolve(stored), StandardCopyOption.REPLACE_EXISTING);
                    else Files.delete(file);
                } else if (markdown.isLocalImage("/uploads/" + name)
                        && images.findByStoredName(name).isEmpty()) {
                    Files.delete(file);
                }
            }
        }
    }

    private boolean referenced(String stored, boolean publicOnly) {
        String url = "/uploads/" + stored;
        return posts.findContents(publicOnly ? PostStatus.PUBLISHED : null).stream()
                .anyMatch(text -> markdown.imageReferences(text).contains(url));
    }

    private String validate(byte[] bytes, String declared) throws IOException {
        String format;
        if (bytes.length >= 8
                && bytes[0] == (byte) 137
                && bytes[1] == 80
                && bytes[2] == 78
                && bytes[3] == 71
                && bytes[4] == 13
                && bytes[5] == 10
                && bytes[6] == 26
                && bytes[7] == 10) format = "png";
        else if (bytes.length >= 3
                && bytes[0] == (byte) 255
                && bytes[1] == (byte) 216
                && bytes[2] == (byte) 255) format = "jpeg";
        else if (bytes.length >= 12
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) format = "webp";
        else throw invalid("僅接受實際格式為 JPEG、PNG 或 WebP 的圖片");
        if (!("image/" + format).equalsIgnoreCase(declared)) throw invalid("宣告的 MIME 與實際圖片格式不一致");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("無法解碼此圖片");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1
                        || height < 1
                        || width > 10000
                        || height > 10000
                        || (long) width * height > 25_000_000)
                    throw invalid("圖片尺寸上限為 10000 像素，且總像素不得超過 2500 萬");
                if (reader.read(0) == null) throw invalid("圖片解碼失敗");
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException | IIOException error) {
            throw invalid("圖片損壞或無法解碼");
        }
        return format.equals("jpeg") ? "jpg" : format;
    }

    private Path resolve(String stored) {
        if (!markdown.isLocalImage("/uploads/" + stored)) throw new NotFoundException();
        Path file = root.resolve(stored).normalize();
        if (!file.startsWith(root)) throw new NotFoundException();
        return file;
    }

    private ImageResponse response(Image image) {
        String url = "/uploads/" + image.getStoredName();
        return new ImageResponse(
                image.getId(),
                image.getOriginalName(),
                url,
                "![圖片](" + url + ")",
                image.getSizeBytes());
    }

    private RuleViolation invalid(String message) {
        return new RuleViolation("file", message);
    }

    private void afterCompletion(Consumer<Boolean> action) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        action.accept(status == STATUS_COMMITTED);
                    }
                });
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException error) {
            log.error("回滾後圖片清理失敗，下次啟動會清理 {}", file.getFileName(), error);
        }
    }
}
