package com.nexbuy.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner seedAdmin(JdbcTemplate jdbcTemplate, PasswordEncoder encoder) {
        return args -> {
            ensureCommerceSupportSchema(jdbcTemplate);

            Integer existing = jdbcTemplate.queryForObject(
                    "select count(*) from users where lower(email) = lower(?)",
                    Integer.class,
                    "admin@nexbuy.com"
            );
            if (existing != null && existing > 0) {
                jdbcTemplate.update(
                        "update users set role = ?, status = ? where lower(email) = lower(?)",
                        "ADMIN",
                        "ACTIVE",
                        "admin@nexbuy.com"
                );
                return;
            }

            jdbcTemplate.update(
                    "insert into users (email, password_hash, status, role) values (?, ?, ?, ?)",
                    "admin@nexbuy.com",
                    encoder.encode("Admin@123"),
                    "ACTIVE",
                    "ADMIN"
            );
        };
    }

    private void ensureCommerceSupportSchema(JdbcTemplate jdbcTemplate) {
        ensureEnumValue(
                jdbcTemplate,
                "orders",
                "payment_status",
                "refund_pending",
                "ALTER TABLE orders MODIFY COLUMN payment_status ENUM('initiated','success','failed','refund_pending','refunded','cancelled') NOT NULL DEFAULT 'initiated'"
        );
        ensureEnumValue(
                jdbcTemplate,
                "payments",
                "status",
                "refund_pending",
                "ALTER TABLE payments MODIFY COLUMN status ENUM('created','authorized','captured','failed','refund_pending','refunded','cancelled') NOT NULL DEFAULT 'created'"
        );

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS refunds (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  order_id BIGINT NOT NULL,
                  payment_id BIGINT NULL,
                  amount_cents INT NOT NULL,
                  currency CHAR(3) NOT NULL DEFAULT 'INR',
                  status ENUM('pending','processed','failed') NOT NULL DEFAULT 'pending',
                  provider_refund_id VARCHAR(191) NULL,
                  note VARCHAR(255) NULL,
                  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  processed_at DATETIME NULL,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  CONSTRAINT uk_refund_order UNIQUE (order_id),
                  CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
                ) ENGINE=InnoDB
                """);


        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_requests (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NULL,
                  type ENUM('chat','recommend','image_search','voice') NOT NULL,
                  request_payload JSON NULL,
                  response_ref TEXT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_ai_request_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                ) ENGINE=InnoDB
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS searches (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NULL,
                  query_text VARCHAR(255) NOT NULL,
                  type ENUM('text','voice','image') NOT NULL,
                  result_count INT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_search_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                ) ENGINE=InnoDB
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS return_requests (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  order_id BIGINT NOT NULL,
                  status ENUM('requested','approved','rejected','received','refunded') NOT NULL DEFAULT 'requested',
                  refund_status ENUM('not_started','pending','processed','failed') NOT NULL DEFAULT 'not_started',
                  reason VARCHAR(255) NULL,
                  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  reviewed_at DATETIME NULL,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  CONSTRAINT uk_return_order UNIQUE (order_id),
                  CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
                ) ENGINE=InnoDB
                """);
    }

    private void ensureEnumValue(JdbcTemplate jdbcTemplate,
                                 String tableName,
                                 String columnName,
                                 String requiredValue,
                                 String alterSql) {
        String columnType = jdbcTemplate.query(
                """
                        select column_type
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = ?
                          and column_name = ?
                        limit 1
                        """,
                rs -> rs.next() ? rs.getString("column_type") : null,
                tableName,
                columnName
        );
        if (columnType == null || columnType.contains("'" + requiredValue + "'")) {
            return;
        }
        jdbcTemplate.execute(alterSql);
    }
}