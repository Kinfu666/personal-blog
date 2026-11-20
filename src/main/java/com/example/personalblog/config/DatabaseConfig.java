package com.example.personalblog.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean
    DataSource dataSource(@Value("${app.data-dir}") String directory) throws Exception {
        Path root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("uploads"));
        SQLiteConfig sqlite = new SQLiteConfig();
        sqlite.enforceForeignKeys(true);
        sqlite.setBusyTimeout(5000);
        SQLiteDataSource source = new SQLiteDataSource(sqlite);
        source.setUrl("jdbc:sqlite:" + root.resolve("blog.db"));
        HikariConfig pool = new HikariConfig();
        pool.setDataSource(source);
        // SQLite 只有一個 writer；單一連線也讓同一交易的檔案補償順序可預期。
        pool.setMaximumPoolSize(1);
        pool.setMinimumIdle(1);
        pool.setPoolName("blog-sqlite");
        HikariDataSource dataSource = new HikariDataSource(pool);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/schema-v1.sql"));
                try (var statement = connection.createStatement();
                        var version =
                                statement.executeQuery(
                                        "SELECT value FROM app_metadata WHERE"
                                                + " key='schema_version'")) {
                    if (!version.next() || !"1".equals(version.getString(1)))
                        throw new IllegalStateException("不支援此資料庫 schema 版本，請先確認程式與資料版本。");
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        } catch (Exception error) {
            dataSource.close();
            throw error;
        }
        return dataSource;
    }
}
