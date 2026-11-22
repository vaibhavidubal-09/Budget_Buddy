package com.budgetbuddy.api.budgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Exercises Flyway, MySQL constraints and concurrent budget writes against an isolated database. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class BudgetMySqlIntegrationTest {

    private static final AtomicLong NEXT_USER_ID = new AtomicLong(9_000_000L);

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("budgetbuddy_test")
            .withUsername("budgetbuddy_test")
            .withPassword("budgetbuddy_test");

    @DynamicPropertySource
    static void configureMySql(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        properties.add("budgetbuddy.jwt.secret", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cleanMySqlDatabaseRunsEveryFlywayMigrationThroughV7() {
        MigrationInfo current = flyway.info().current();
        assertNotNull(current);
        assertEquals("7", current.getVersion().getVersion());

        long successfulMigrations = Arrays.stream(flyway.info().all())
                .filter(migration -> migration.getState() == MigrationState.SUCCESS)
                .count();
        assertEquals(7, successfulMigrations);
        assertTrue(Arrays.stream(flyway.info().all())
                .anyMatch(migration -> "3".equals(migration.getVersion().getVersion())
                        && migration.getState() == MigrationState.SUCCESS));

        Integer uniqueIndexCount = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database()
                  and table_name = 'category_budgets'
                  and index_name = 'uk_category_budgets_user_period_category'
                  and non_unique = 0
                """, Integer.class);
        assertEquals(3, uniqueIndexCount);
    }

    @Test
    void mysqlUniqueConstraintRejectsDuplicateUserCategoryAndPeriod() {
        long userId = createUser();
        insertBudget(userId, "Books", 500, "monthly");

        assertThrows(DataIntegrityViolationException.class,
                () -> insertBudget(userId, "Books", 700, "monthly"));

        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from category_budgets
                where user_id = ? and category = 'Books' and period = 'monthly'
                """, Integer.class, userId);
        assertEquals(1, count);
    }

    @Test
    void concurrentBudgetUpdatesWithSameVersionAllowOneWriterAndRejectTheStaleWriter() throws Exception {
        long userId = createUser();
        insertBudget(userId, "Transport", 1000, "monthly");
        Long budgetId = jdbcTemplate.queryForObject("""
                select id from category_budgets where user_id = ? and category = 'Transport'
                """, Long.class, userId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> updateBudgetConcurrently(
                    budgetId, userId, 1300, ready, start));
            Future<Integer> second = executor.submit(() -> updateBudgetConcurrently(
                    budgetId, userId, 1400, ready, start));

            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both requests should be ready before release");
            start.countDown();
            List<Integer> statuses = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertEquals(1, statuses.stream().filter(status -> status == 200).count());
            assertEquals(1, statuses.stream().filter(status -> status == 409).count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Integer version = jdbcTemplate.queryForObject(
                "select version from category_budgets where id = ?", Integer.class, budgetId);
        Integer limit = jdbcTemplate.queryForObject(
                "select budget_limit from category_budgets where id = ?", Integer.class, budgetId);
        assertEquals(1, version);
        assertTrue(List.of(1300, 1400).contains(limit));
    }

    private int updateBudgetConcurrently(Long budgetId, long userId, int limit,
                                         CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent budget updates");
        }
        return mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Transport","limit":%d,"period":"monthly","version":0}
                                """.formatted(limit)))
                .andReturn().getResponse().getStatus();
    }

    private long createUser() {
        long userId = NEXT_USER_ID.incrementAndGet();
        jdbcTemplate.update("""
                insert into app_users (id, full_name, email, password_hash)
                values (?, 'MySQL integration test', ?, 'not-used')
                """, userId, "mysql-test-" + userId + "@example.com");
        return userId;
    }

    private void insertBudget(long userId, String category, int limit, String period) {
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (?, ?, ?, ?)
                """, userId, category, limit, period);
    }
}
