CREATE TABLE IF NOT EXISTS app_metadata (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role = 'ADMIN'),
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 50),
    normalized_name TEXT UNIQUE NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 50),
    normalized_name TEXT UNIQUE NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS posts (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL CHECK(length(title) BETWEEN 1 AND 150),
    summary TEXT NOT NULL CHECK(length(summary) BETWEEN 1 AND 300),
    content TEXT NOT NULL CHECK(length(content) BETWEEN 1 AND 50000),
    status TEXT NOT NULL CHECK(status IN ('DRAFT', 'PUBLISHED')),
    author_id INTEGER NOT NULL REFERENCES users(id),
    category_id INTEGER NOT NULL REFERENCES categories(id),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    published_at TEXT,
    CHECK(
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR (status = 'DRAFT' AND published_at IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS post_tags (
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES tags(id),
    PRIMARY KEY(post_id, tag_id)
);

CREATE TABLE IF NOT EXISTS images (
    id INTEGER PRIMARY KEY,
    original_name TEXT NOT NULL,
    stored_name TEXT UNIQUE NOT NULL,
    media_type TEXT NOT NULL,
    size_bytes INTEGER NOT NULL CHECK(size_bytes > 0),
    uploaded_by INTEGER NOT NULL REFERENCES users(id),
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_posts_published ON posts(status, published_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_posts_category ON posts(category_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag ON post_tags(tag_id, post_id);

INSERT OR IGNORE INTO app_metadata(key, value) VALUES('schema_version', '1');
