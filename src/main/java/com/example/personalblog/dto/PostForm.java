package com.example.personalblog.dto;

import com.example.personalblog.enums.PostStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

public class PostForm {
    @NotBlank(message = "請輸入標題")
    @Size(max = 150, message = "標題最多 150 字")
    private String title;

    @NotBlank(message = "請輸入摘要")
    @Size(max = 300, message = "摘要最多 300 字")
    private String summary;

    @NotBlank(message = "請輸入內容")
    @Size(max = 50000, message = "內容最多 50000 字")
    private String content;

    @NotNull(message = "請選擇分類")
    private Long categoryId;

    private Set<Long> tagIds = new LinkedHashSet<>();

    @NotNull(message = "請選擇文章狀態")
    private PostStatus status = PostStatus.DRAFT;

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v == null ? null : v.strip();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String v) {
        summary = v == null ? null : v.strip();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        content = v;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long v) {
        categoryId = v;
    }

    public Set<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(Set<Long> v) {
        tagIds = v == null ? new LinkedHashSet<>() : v;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus v) {
        status = v;
    }
}
