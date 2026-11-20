package com.example.personalblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "images")
public class Image extends BaseEntity {
    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    protected Image() {}

    public Image(
            String originalName,
            String storedName,
            String mediaType,
            long sizeBytes,
            User uploadedBy) {
        this.originalName = originalName;
        this.storedName = storedName;
        this.mediaType = mediaType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
