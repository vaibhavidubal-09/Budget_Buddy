package com.budgetbuddy.api.expenses;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.budgetbuddy.api.budgets.Budget;
import com.budgetbuddy.api.budgets.BudgetPlanApplyRequest;
import com.budgetbuddy.api.budgets.BudgetPlanItemRequest;
import com.budgetbuddy.api.budgets.BudgetRepository;
import com.budgetbuddy.api.budgets.BudgetService;
import com.budgetbuddy.api.budgets.ExpectedBudgetVersion;
import com.budgetbuddy.api.assistant.AssistantFinancialContextService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:budgetbuddy-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "budgetbuddy.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class ExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private AssistantFinancialContextService assistantFinancialContextService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void createsBudgetAndTrimsCategoryWhitespace() throws Exception {
        ensureTestUser(371L);

        mockMvc.perform(post("/api/budgets")
                        .with(jwt().jwt(token -> token.claim("userId", 371L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"  Food & Dining  ","limit":2500,"period":"monthly"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Food & Dining"))
                .andExpect(jsonPath("$.period").value("monthly"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 371 and category = 'Food & Dining' and period = 'monthly'",
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void sequentialDuplicateBudgetCreationReturnsConflict() throws Exception {
        ensureTestUser(372L);
        String first = """
                {"category":"Transport","limit":1800,"period":"monthly"}
                """;
        String duplicateWithOuterWhitespace = """
                {"category":"  Transport  ","limit":2000,"period":"monthly"}
                """;

        mockMvc.perform(post("/api/budgets")
                        .with(jwt().jwt(token -> token.claim("userId", 372L)))
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budgets")
                        .with(jwt().jwt(token -> token.claim("userId", 372L)))
                        .contentType(MediaType.APPLICATION_JSON).content(duplicateWithOuterWhitespace))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A budget already exists for this category and period."));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 372 and category = 'Transport' and period = 'monthly'",
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void simultaneousDuplicateBudgetCreationKeepsOneAndReturnsConflictForOther() throws Exception {
        ensureTestUser(373L);
        String request = """
                {"category":"Books","limit":900,"period":"monthly"}
                """;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> createBudgetConcurrently(373L, request, ready, start));
            Future<Integer> second = executor.submit(() -> createBudgetConcurrently(373L, request, ready, start));

            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<Integer> statuses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertEquals(1, statuses.stream().filter(code -> code == 201).count());
            org.junit.jupiter.api.Assertions.assertEquals(1, statuses.stream().filter(code -> code == 409).count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 373 and category = 'Books' and period = 'monthly'",
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    private int createBudgetConcurrently(long userId, String request, CountDownLatch ready,
                                         CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent budget request");
        }
        return mockMvc.perform(post("/api/budgets")
                        .with(jwt().jwt(token -> token.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void appliesMultiBudgetPlanAndPreservesOtherPeriods() throws Exception {
        ensureTestUser(374L);
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (374, 'Food & Dining', 100, 'monthly')");
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (374, 'Other', 200, 'monthly')");
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (374, 'Education', 300, 'weekly')");
        Long foodId = budgetId(374L, "monthly", "Food & Dining");
        Long otherId = budgetId(374L, "monthly", "Other");

        mockMvc.perform(post("/api/budgets/apply-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 374L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period":"monthly",
                                 "expectedBudgets":[{"id":%d,"version":0},{"id":%d,"version":0}],
                                 "budgets":[{"category":"Food & Dining","limit":150},{"category":"Shopping","limit":250}]}
                                """.formatted(foodId, otherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        org.junit.jupiter.api.Assertions.assertEquals(150, budgetLimit(foodId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 374 and category = 'Shopping' and period = 'monthly'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where id = ?", Integer.class, otherId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 374 and category = 'Education' and period = 'weekly' and budget_limit = 300",
                Integer.class));
    }

    @Test
    void rejectsDuplicateCategoriesInAppliedPlanBeforeWriting() throws Exception {
        ensureTestUser(375L);
        mockMvc.perform(post("/api/budgets/apply-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 375L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period":"monthly","expectedBudgets":[],
                                 "budgets":[{"category":"Food & Dining","limit":100},
                                            {"category":" Food & Dining ","limit":200}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A category can appear only once in a budget plan."));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 375", Integer.class));

        mockMvc.perform(post("/api/budgets/apply-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 375L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period":"monthly","expectedBudgets":[],
                                 "budgets":[{"category":"Books","limit":100}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Choose a category from the supported budget categories."));
    }

    @Test
    void staleBudgetPlanVersionReturnsConflictWithoutChangingRows() throws Exception {
        ensureTestUser(376L);
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (376, 'Education', 100, 'monthly')");
        Long id = budgetId(376L, "monthly", "Education");

        mockMvc.perform(post("/api/budgets/apply-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 376L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period":"monthly","expectedBudgets":[{"id":%d,"version":3}],
                                 "budgets":[{"category":"Education","limit":500}]}
                                """.formatted(id)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Budgets for this period changed since you opened the plan. Reload and try again."));

        org.junit.jupiter.api.Assertions.assertEquals(100, budgetLimit(id));
    }

    @Test
    void planEndpointRequiresAuthenticationAndCannotApplyAnotherUsersSnapshot() throws Exception {
        ensureTestUser(377L);
        ensureTestUser(378L);
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (377, 'Books', 100, 'monthly')");
        Long id = budgetId(377L, "monthly", "Books");
        String request = """
                {"period":"monthly","expectedBudgets":[{"id":%d,"version":0}],
                 "budgets":[{"category":"Education","limit":500}]}
                """.formatted(id);

        mockMvc.perform(post("/api/budgets/apply-plan")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/budgets/apply-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 378L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertEquals(100, budgetLimit(id));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 378", Integer.class));
    }

    @Test
    void emptyPlanDeletesOnlySelectedPeriodBudgets() throws Exception {
        ensureTestUser(379L);
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (379, 'Books', 100, 'monthly')");
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (379, 'Education', 200, 'weekly')");
        Long monthlyId = budgetId(379L, "monthly", "Books");

        mockMvc.perform(post("/api/budgets/apply-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 379L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"period":"monthly","expectedBudgets":[{"id":%d,"version":0}],"budgets":[]}
                                """.formatted(monthlyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].period").value("weekly"));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 379 and period = 'monthly'", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where user_id = 379 and period = 'weekly'", Integer.class));
    }

    @Test
    void failureDuringPlanFlushRollsBackEveryChange() {
        ensureTestUser(380L);
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (380, 'Food & Dining', 100, 'monthly')");
        jdbcTemplate.update("insert into category_budgets (user_id, category, budget_limit, period) values (380, 'Other', 200, 'monthly')");
        Long foodId = budgetId(380L, "monthly", "Food & Dining");
        Long otherId = budgetId(380L, "monthly", "Other");
        jdbcTemplate.execute("ALTER TABLE category_budgets ADD CONSTRAINT chk_test_plan_failure CHECK (budget_limit <> 9999)");

        try {
            BudgetPlanApplyRequest request = new BudgetPlanApplyRequest("monthly", List.of(
                    new BudgetPlanItemRequest("Shopping", 150),
                    new BudgetPlanItemRequest("Education", 9999)), List.of(
                    new ExpectedBudgetVersion(foodId, 0), new ExpectedBudgetVersion(otherId, 0)));
            assertThrows(DataIntegrityViolationException.class, () -> budgetService.applyPlan(request, 380L));

            org.junit.jupiter.api.Assertions.assertEquals(100, budgetLimit(foodId));
            org.junit.jupiter.api.Assertions.assertEquals(200, budgetLimit(otherId));
            org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                    "select count(*) from category_budgets where user_id = 380 and category in ('Shopping', 'Education')",
                    Integer.class));
        } finally {
            jdbcTemplate.execute("ALTER TABLE category_budgets DROP CONSTRAINT chk_test_plan_failure");
        }
    }

    private Long budgetId(long userId, String period, String category) {
        return jdbcTemplate.queryForObject(
                "select id from category_budgets where user_id = ? and period = ? and category = ?",
                Long.class, userId, period, category);
    }

    private int budgetLimit(Long id) {
        return jdbcTemplate.queryForObject("select budget_limit from category_budgets where id = ?", Integer.class, id);
    }

    @Test
    void createsExpenseAndReturnsItForItsOwner() throws Exception {
        ensureTestUser(42L);
        ensureTestUser(99L);
        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Shared lunch",
                                  "category": "Food & Dining",
                                  "amount": 300.00,
                                  "paymentMethod": "UPI",
                                  "date": "2026-10-12",
                                  "splitDetails": "{\\\"mode\\\":\\\"equal\\\"}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value("Shared lunch"))
                .andExpect(jsonPath("$.category").value("Food & Dining"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.splitDetails").value("{\"mode\":\"equal\"}"));

        mockMvc.perform(get("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Shared lunch"));

        mockMvc.perform(get("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 99L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void expenseCreationWithSameIdempotencyKeyReturnsOriginalAndRejectsDifferentPayload() throws Exception {
        ensureTestUser(366L);
        String expenseJson = """
                {"description":"Campus lunch","category":"Food & Dining","amount":125.00,
                 "paymentMethod":"UPI","date":"2026-10-14"}
                """;
        String differentExpenseJson = """
                {"description":"Campus coffee","category":"Food & Dining","amount":125.00,
                 "paymentMethod":"UPI","date":"2026-10-14"}
                """;

        String original = mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 366L)))
                        .header("Idempotency-Key", "expense-retry-366")
                        .contentType(MediaType.APPLICATION_JSON).content(expenseJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String replay = mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 366L)))
                        .header("Idempotency-Key", "expense-retry-366")
                        .contentType(MediaType.APPLICATION_JSON).content(expenseJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer originalId = com.jayway.jsonpath.JsonPath.read(original, "$.id");
        Integer replayId = com.jayway.jsonpath.JsonPath.read(replay, "$.id");
        org.junit.jupiter.api.Assertions.assertEquals(originalId, replayId);
        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 366L)))
                        .header("Idempotency-Key", "expense-retry-366")
                        .contentType(MediaType.APPLICATION_JSON).content(differentExpenseJson))
                .andExpect(status().isConflict());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from expenses where user_id = 366 and idempotency_key = ?",
                Integer.class, "expense-retry-366");
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date,
                                      idempotency_key, idempotency_payload_hash)
                values (366, 'Direct duplicate', 'Food & Dining', 125.00, 'UPI', '2026-10-14',
                        'expense-retry-366', 'duplicate-hash')
                """));
    }

    @Test
    void concurrentExpenseRetriesWithSameKeyCreateOnlyOneRow() throws Exception {
        ensureTestUser(367L);
        String body = """
                {"description":"Bus to campus","category":"Transport","amount":45.00,
                 "paymentMethod":"UPI","date":"2026-10-14"}
                """;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> createIdempotentExpense(367L, "same-key-367", body));
            Future<Integer> second = executor.submit(() -> createIdempotentExpense(367L, "same-key-367", body));
            int firstStatus = first.get(10, TimeUnit.SECONDS);
            int secondStatus = second.get(10, TimeUnit.SECONDS);

            org.junit.jupiter.api.Assertions.assertTrue(List.of(201, 200).contains(firstStatus));
            org.junit.jupiter.api.Assertions.assertTrue(List.of(201, 200).contains(secondStatus));
            org.junit.jupiter.api.Assertions.assertEquals(201, firstStatus + secondStatus - 200);
        } finally {
            executor.shutdownNow();
        }

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from expenses where user_id = 367 and idempotency_key = ?",
                Integer.class, "same-key-367");
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    private int createIdempotentExpense(long userId, String key, String body) throws Exception {
        return mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", userId)))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void anotherUserCannotReadUpdateOrDeleteAnExpense() throws Exception {
        ensureTestUser(352L);
        ensureTestUser(353L);
        java.time.LocalDate today = java.time.LocalDate.now();
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (352, 'Private expense for ownership test', 'Education', 45.00, 'Cash', ?)
                """, today);
        Long expenseId = jdbcTemplate.queryForObject(
                "select id from expenses where description = ?", Long.class,
                "Private expense for ownership test");
        String updatedRequest = """
                {"description":"Changed by other account","category":"Education","amount":46.00,
                 "paymentMethod":"Cash","date":"%s"}
                """.formatted(today);

        mockMvc.perform(get("/api/expenses/{id}", expenseId)
                        .with(jwt().jwt(token -> token.claim("userId", 353L))))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/expenses/{id}", expenseId)
                        .with(jwt().jwt(token -> token.claim("userId", 353L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedRequest))
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/expenses/{id}", expenseId)
                        .with(jwt().jwt(token -> token.claim("userId", 353L))))
                .andExpect(status().isNotFound());

        Integer stillOwned = jdbcTemplate.queryForObject(
                "select count(*) from expenses where id = ? and user_id = 352", Integer.class, expenseId);
        org.junit.jupiter.api.Assertions.assertEquals(1, stillOwned);
    }

    @Test
    void expenseCreationRejectsNonpositiveAmountsAndMalformedDates() throws Exception {
        ensureTestUser(354L);
        String validDate = java.time.LocalDate.now().toString();
        for (String amount : new String[] {"0", "-1"}) {
            mockMvc.perform(post("/api/expenses")
                            .with(jwt().jwt(token -> token.claim("userId", 354L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"description":"Invalid amount","category":"Other","amount":%s,
                                     "paymentMethod":"Cash","date":"%s"}
                                    """.formatted(amount, validDate)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 354L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Invalid date","category":"Other","amount":1,
                                 "paymentMethod":"Cash","date":"not-a-date"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or has invalid JSON or date values"));
    }

    @Test
    void expenseAndIncomeAmountsMatchDecimalTwelveTwoPrecision() throws Exception {
        ensureTestUser(355L);
        String today = java.time.LocalDate.now().toString();

        for (String amount : new String[] {"1234.56", "9999999999.99"}) {
            mockMvc.perform(post("/api/expenses")
                            .with(jwt().jwt(token -> token.claim("userId", 355L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"description":"Valid amount %s","category":"Other","amount":%s,
                                     "paymentMethod":"Cash","date":"%s"}
                                    """.formatted(amount, amount, today)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(Double.parseDouble(amount)));

            mockMvc.perform(post("/api/income")
                            .with(jwt().jwt(token -> token.claim("userId", 355L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"source":"Valid amount %s","amount":%s,"date":"%s","frequency":"ONCE"}
                                    """.formatted(amount, amount, today)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(Double.parseDouble(amount)));
        }

        for (String amount : new String[] {"12.345", "10000000000.00", "0", "-1"}) {
            mockMvc.perform(post("/api/expenses")
                            .with(jwt().jwt(token -> token.claim("userId", 355L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"description":"Invalid amount","category":"Other","amount":%s,
                                     "paymentMethod":"Cash","date":"%s"}
                                    """.formatted(amount, today)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.path").value("/api/expenses"));

            mockMvc.perform(post("/api/income")
                            .with(jwt().jwt(token -> token.claim("userId", 355L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"source":"Invalid amount","amount":%s,"date":"%s","frequency":"ONCE"}
                                    """.formatted(amount, today)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.path").value("/api/income"));
        }
    }

    @Test
    void expenseImportCannotBypassDecimalAmountValidation() throws Exception {
        ensureTestUser(356L);

        mockMvc.perform(post("/api/expenses/import")
                        .with(jwt().jwt(token -> token.claim("userId", 356L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactions":[{"rowId":"row-1","description":"Imported item",
                                 "category":"Other","amount":12.345,"paymentMethod":"Cash","date":"2026-10-15"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/expenses/import"));

        Integer savedRows = jdbcTemplate.queryForObject(
                "select count(*) from expenses where user_id = 356", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, savedRows);
    }

    @Test
    void anotherUserCannotReadUpdateOrDeleteIncomeOrBudgetRecords() throws Exception {
        ensureTestUser(361L);
        ensureTestUser(362L);
        java.time.LocalDate today = java.time.LocalDate.now();
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (361, 'Private income ownership test', 500.00, ?, 'ONCE')
                """, today);
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (361, 'Private budget ownership test', 700, 'monthly')
                """);
        Long incomeId = jdbcTemplate.queryForObject(
                "select id from income_entries where source = ?", Long.class,
                "Private income ownership test");
        Long budgetId = jdbcTemplate.queryForObject(
                "select id from category_budgets where category = ?", Long.class,
                "Private budget ownership test");
        String incomeRequest = """
                {"source":"Changed by other account","amount":600.00,"date":"%s","frequency":"ONCE"}
                """.formatted(today);
        String budgetRequest = """
                {"category":"Changed by other account","limit":800,"period":"monthly"}
                """;

        mockMvc.perform(get("/api/income/{id}", incomeId)
                        .with(jwt().jwt(token -> token.claim("userId", 362L))))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/income/{id}", incomeId)
                        .with(jwt().jwt(token -> token.claim("userId", 362L)))
                        .contentType(MediaType.APPLICATION_JSON).content(incomeRequest))
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/income/{id}", incomeId)
                        .with(jwt().jwt(token -> token.claim("userId", 362L))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", 362L))))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", 362L)))
                        .contentType(MediaType.APPLICATION_JSON).content(budgetRequest))
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", 362L))))
                .andExpect(status().isNotFound());

        Integer incomeStillOwned = jdbcTemplate.queryForObject(
                "select count(*) from income_entries where id = ? and user_id = 361", Integer.class, incomeId);
        Integer budgetStillOwned = jdbcTemplate.queryForObject(
                "select count(*) from category_budgets where id = ? and user_id = 361", Integer.class, budgetId);
        org.junit.jupiter.api.Assertions.assertEquals(1, incomeStillOwned);
        org.junit.jupiter.api.Assertions.assertEquals(1, budgetStillOwned);
    }

    @Test
    void staleBudgetUpdateReturnsConflictAndIncludesVersionOnRead() throws Exception {
        ensureTestUser(364L);
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (364, 'Books', 1000, 'monthly')
                """);
        Long budgetId = jdbcTemplate.queryForObject(
                "select id from category_budgets where user_id = 364 and category = 'Books'", Long.class);

        mockMvc.perform(get("/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", 364L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", 364L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Books","limit":1200,"period":"monthly","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .with(jwt().jwt(token -> token.claim("userId", 364L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Books","limit":1500,"period":"monthly","version":0}
                                """))
                .andExpect(status().isConflict());

        Integer savedLimit = jdbcTemplate.queryForObject(
                "select budget_limit from category_budgets where id = ?", Integer.class, budgetId);
        org.junit.jupiter.api.Assertions.assertEquals(1200, savedLimit);
    }

    @Test
    void simultaneousUpdatesToSameBudgetAllowOnlyOneStaleWriter() throws Exception {
        ensureTestUser(365L);
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (365, 'Transport', 1000, 'monthly')
                """);
        Long budgetId = jdbcTemplate.queryForObject(
                "select id from category_budgets where user_id = 365 and category = 'Transport'", Long.class);
        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch allowUpdates = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> updateBudgetConcurrently(
                    budgetId, 365L, 1300, bothRead, allowUpdates));
            Future<Boolean> second = executor.submit(() -> updateBudgetConcurrently(
                    budgetId, 365L, 1400, bothRead, allowUpdates));

            org.junit.jupiter.api.Assertions.assertTrue(bothRead.await(5, TimeUnit.SECONDS),
                    "Both transactions should load the same budget version before either writes");
            allowUpdates.countDown();

            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            org.junit.jupiter.api.Assertions.assertEquals(1, successes);
        } finally {
            allowUpdates.countDown();
            executor.shutdownNow();
        }

        Integer version = jdbcTemplate.queryForObject(
                "select version from category_budgets where id = ?", Integer.class, budgetId);
        Integer limit = jdbcTemplate.queryForObject(
                "select budget_limit from category_budgets where id = ?", Integer.class, budgetId);
        org.junit.jupiter.api.Assertions.assertEquals(1, version);
        org.junit.jupiter.api.Assertions.assertTrue(List.of(1300, 1400).contains(limit));
    }

    private boolean updateBudgetConcurrently(Long budgetId, Long userId, int newLimit,
                                              CountDownLatch bothRead, CountDownLatch allowUpdates) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            transaction.executeWithoutResult(status -> {
                Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId).orElseThrow();
                bothRead.countDown();
                try {
                    if (!allowUpdates.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent update");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Concurrent update was interrupted", exception);
                }
                budget.updateDetails(budget.getCategory(), newLimit, budget.getPeriod());
                budgetRepository.saveAndFlush(budget);
            });
            return true;
        } catch (OptimisticLockingFailureException conflict) {
            return false;
        }
    }

    @Test
    void assistantChatRejectsRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"How much did I spend this month?","history":[]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void monthlySummaryAggregatesOnlyCurrentUsersCurrentMonthExpenses() throws Exception {
        ensureTestUser(142L);
        ensureTestUser(143L);
        String today = java.time.LocalDate.now().toString();
        java.time.LocalDate todayDate = java.time.LocalDate.parse(today);
        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 142L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","category":"Food & Dining","amount":120.00,"paymentMethod":"UPI","date":"%s"}
                                """.formatted(today)))
                .andExpect(status().isCreated());

        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (142, 'Previous month expense', 'Other', 700.00, 'Cash', ?)
                """, todayDate.minusMonths(1));
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (143, 'Private expense', 'Other', 900.00, 'Cash', ?)
                """, todayDate);
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (142, 'Old one-time income', 700.00, ?, 'ONCE')
                """, todayDate.minusYears(2));
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (142, 'Recurring income', 100.00, ?, 'MONTHLY')
                """, todayDate.withDayOfMonth(1).minusYears(2));
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (142, 'Current one-time income', 50.00, ?, 'ONCE')
                """, todayDate);
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (142, 'Future one-time income', 900.00, ?, 'ONCE')
                """, todayDate.plusYears(1));

        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 142L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Snack","category":"Food & Dining","amount":30.00,"paymentMethod":"Cash","date":"%s"}
                                """.formatted(today)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 142L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Notebook","category":"Education","amount":200.00,"paymentMethod":"Cash","date":"%s"}
                                """.formatted(today)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/summary/monthly")
                        .with(jwt().jwt(token -> token.claim("userId", 142L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.income").value(150.00))
                .andExpect(jsonPath("$.expenses").value(350.00))
                .andExpect(jsonPath("$.categorySpending", hasSize(2)))
                .andExpect(jsonPath("$.categorySpending[0].category").value("Education"))
                .andExpect(jsonPath("$.categorySpending[0].amount").value(200.00))
                .andExpect(jsonPath("$.categorySpending[1].category").value("Food & Dining"))
                .andExpect(jsonPath("$.categorySpending[1].amount").value(150.00));

        mockMvc.perform(get("/api/summary/monthly")
                        .with(jwt().jwt(token -> token.claim("userId", 143L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses").value(900.00))
                .andExpect(jsonPath("$.categorySpending", hasSize(1)))
                .andExpect(jsonPath("$.categorySpending[0].category").value("Other"));
    }

    @Test
    void assistantContextUsesOwnerScopedAggregatesAndOnlyRelevantIncomeSchedules() {
        ensureTestUser(820L);
        ensureTestUser(821L);
        java.time.LocalDate today = java.time.LocalDate.now();
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (820, 'Current expense', 'Food & Dining', 25.00, 'Cash', ?)
                """, today);
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (820, 'Out of assistant range', 'Other', 750.00, 'Cash', ?)
                """, today.minusYears(2));
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (821, 'Another user expense', 'Food & Dining', 800.00, 'Cash', ?)
                """, today);
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (820, 'Expired one-time', 700.00, ?, 'ONCE')
                """, today.minusYears(2));
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (820, 'Recurring schedule', 100.00, ?, 'MONTHLY')
                """, today.withDayOfMonth(1).minusYears(2));
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (820, 'Current one-time', 50.00, ?, 'ONCE')
                """, today);
        jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (820, 'Future one-time', 900.00, ?, 'ONCE')
                """, today.plusYears(1));

        java.util.Map<?, ?> ownerContext = assistantFinancialContextService.buildContext(820L);
        java.util.Map<?, ?> periods = (java.util.Map<?, ?>) ownerContext.get("periods");
        java.util.Map<?, ?> monthly = (java.util.Map<?, ?>) periods.get("monthly");
        java.util.Map<?, ?> current = (java.util.Map<?, ?>) monthly.get("current");
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("25.00"), current.get("expenseTotal"));
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("150.00"),
                current.get("scheduledIncomeEstimate"));
        org.junit.jupiter.api.Assertions.assertEquals("Food & Dining",
                ((java.util.Map<?, ?>) ((java.util.List<?>) current.get("spendingByCategory")).get(0)).get("category"));

        java.util.Map<?, ?> otherUserContext = assistantFinancialContextService.buildContext(821L);
        java.util.Map<?, ?> otherPeriods = (java.util.Map<?, ?>) otherUserContext.get("periods");
        java.util.Map<?, ?> otherMonthly = (java.util.Map<?, ?>) otherPeriods.get("monthly");
        java.util.Map<?, ?> otherCurrent = (java.util.Map<?, ?>) otherMonthly.get("current");
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("800.00"),
                otherCurrent.get("expenseTotal"));
        org.junit.jupiter.api.Assertions.assertEquals(java.math.BigDecimal.ZERO,
                otherCurrent.get("scheduledIncomeEstimate"));
    }

    @Test
    void periodSummaryCalculatesCurrentAndPreviousWeeksForAuthenticatedUser() throws Exception {
        ensureTestUser(242L);
        ensureTestUser(243L);
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY));
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (242, 'This week', 'Food & Dining', 125.00, 'Cash', ?)
                """, weekStart.plusDays(1));
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (242, 'Last week', 'Education', 80.00, 'Cash', ?)
                """, weekStart.minusDays(1));
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (243, 'Another user', 'Food & Dining', 900.00, 'Cash', ?)
                """, weekStart.plusDays(1));
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (242, 'Food & Dining', 200, 'weekly')
                """);
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (242, 'Education', 500, 'monthly')
                """);
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (243, 'Food & Dining', 1000, 'weekly')
                """);

        mockMvc.perform(get("/api/summary/period")
                        .param("period", "weekly")
                        .with(jwt().jwt(token -> token.claim("userId", 242L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("weekly"))
                .andExpect(jsonPath("$.startDate").value(weekStart.toString()))
                .andExpect(jsonPath("$.expenses").value(125.00))
                .andExpect(jsonPath("$.previousExpenses").value(80.00))
                .andExpect(jsonPath("$.balance").value(-125.00))
                .andExpect(jsonPath("$.trends", hasSize(6)))
                .andExpect(jsonPath("$.trends[4].expenses").value(80.00))
                .andExpect(jsonPath("$.trends[5].expenses").value(125.00))
                .andExpect(jsonPath("$.categorySpending[0].category").value("Food & Dining"))
                .andExpect(jsonPath("$.budgetUsage", hasSize(1)))
                .andExpect(jsonPath("$.budgetUsage[0].limit").value(200))
                .andExpect(jsonPath("$.budgetUsage[0].spent").value(125.00))
                .andExpect(jsonPath("$.budgetUsage[0].remaining").value(75.00));
    }

    @Test
    void periodSummaryRejectsUnsupportedPeriod() throws Exception {
        ensureTestUser(244L);
        mockMvc.perform(get("/api/summary/period")
                        .param("period", "daily")
                        .with(jwt().jwt(token -> token.claim("userId", 244L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aiSuggestionEndpointsRejectInvalidBodiesWithApiError() throws Exception {
        mockMvc.perform(post("/api/budgets/suggest-category")
                        .with(jwt().jwt(token -> token.claim("userId", 244L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"monthly\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/budgets/suggest-category"));

        mockMvc.perform(post("/api/budgets/suggest-plan")
                        .with(jwt().jwt(token -> token.claim("userId", 244L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"daily\",\"income\":12.345,\"savingsGoal\":0,\"savingsPurpose\":\"\",\"categories\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/budgets/suggest-plan"));
    }

    @Test
    void invalidQueryParameterUsesApiErrorFormat() throws Exception {
        mockMvc.perform(get("/api/expenses/page")
                        .param("size", "not-a-number")
                        .with(jwt().jwt(token -> token.claim("userId", 244L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("One or more request parameters are missing or invalid"));

        mockMvc.perform(get("/api/summary/period")
                        .with(jwt().jwt(token -> token.claim("userId", 244L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/summary/period"));
    }

    @Test
    void oversizedLoginAndExpenseOptionalFieldsReturnBadRequest() throws Exception {
        String email = "a".repeat(255) + "@example.com";
        String password = "x".repeat(73);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));

        String splitDetails = "x".repeat(16001);
        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 244L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Lunch\",\"category\":\"Food & Dining\","
                                + "\"amount\":10.00,\"paymentMethod\":\"Cash\",\"date\":\"2026-10-18\","
                                + "\"splitDetails\":\"" + splitDetails + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/expenses"));
    }

    @Test
    void periodSummaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/summary/period").param("period", "monthly"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void periodSummaryUsesCalendarMonthAndYearBoundaries() throws Exception {
        ensureTestUser(245L);
        java.time.LocalDate today = java.time.LocalDate.now();
        mockMvc.perform(get("/api/summary/period")
                        .param("period", "monthly")
                        .with(jwt().jwt(token -> token.claim("userId", 245L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(today.withDayOfMonth(1).toString()))
                .andExpect(jsonPath("$.endDate").value(today.withDayOfMonth(today.lengthOfMonth()).toString()))
                .andExpect(jsonPath("$.trends", hasSize(6)));

        mockMvc.perform(get("/api/summary/period")
                        .param("period", "yearly")
                        .with(jwt().jwt(token -> token.claim("userId", 245L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(java.time.LocalDate.of(today.getYear(), 1, 1).toString()))
                .andExpect(jsonPath("$.endDate").value(java.time.LocalDate.of(today.getYear(), 12, 31).toString()))
                .andExpect(jsonPath("$.trends", hasSize(6)));
    }

    @Test
    void softBudgetAllowsExpenseAndReportsTheOverrun() throws Exception {
        ensureTestUser(246L);
        java.time.LocalDate today = java.time.LocalDate.now();
        jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (246, 'Food & Dining', 100, 'weekly')
                """);

        mockMvc.perform(post("/api/expenses")
                        .with(jwt().jwt(token -> token.claim("userId", 246L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Expense above soft budget","category":"Food & Dining",
                                 "amount":125.00,"paymentMethod":"Cash","date":"%s"}
                                """.formatted(today)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/summary/period")
                        .param("period", "weekly")
                        .with(jwt().jwt(token -> token.claim("userId", 246L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetUsage[0].spent").value(125.00))
                .andExpect(jsonPath("$.budgetUsage[0].remaining").value(-25.00));
    }

    @Test
    void importPreviewFlagsExistingAndRepeatedRowsAndImportSkipsDuplicates() throws Exception {
        ensureTestUser(247L);
        String batch = """
                {"transactions":[
                  {"rowId":"r1","description":"Campus cafe","category":"Food & Dining","amount":120.00,"paymentMethod":"UPI","date":"2026-10-14"},
                  {"rowId":"r2","description":"Campus cafe","category":"Food & Dining","amount":120.00,"paymentMethod":"UPI","date":"2026-10-14"},
                  {"rowId":"r3","description":"Notebook","category":"Education","amount":90.00,"paymentMethod":"Cash","date":"2026-10-14"}
                ]}
                """;

        mockMvc.perform(post("/api/expenses/import/preview")
                        .with(jwt().jwt(token -> token.claim("userId", 247L)))
                        .contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.rows[1].duplicate").value(true));

        mockMvc.perform(post("/api/expenses/import")
                        .with(jwt().jwt(token -> token.claim("userId", 247L)))
                        .contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.duplicateCount").value(1));

        mockMvc.perform(post("/api/expenses/import")
                        .with(jwt().jwt(token -> token.claim("userId", 247L)))
                        .contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(0))
                .andExpect(jsonPath("$.duplicateCount").value(3));

        Integer savedCount = jdbcTemplate.queryForObject("select count(*) from expenses where user_id = 247", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(2, savedCount);
    }

    @Test
    void concurrentImportsForOneUserSerializeAndSaveOnlyOneMatchingExpense() throws Exception {
        ensureTestUser(249L);
        ExpenseImportRequest row = new ExpenseImportRequest("same-row", "Concurrent cafe purchase",
                "Food & Dining", new java.math.BigDecimal("44.00"), "UPI", java.time.LocalDate.now(), null);
        ExpenseImportBatchRequest batch = new ExpenseImportBatchRequest(List.of(row));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ExpenseImportResult> first = executor.submit(() -> {
                ready.countDown(); start.await(); return expenseService.importBatch(batch, 249L);
            });
            Future<ExpenseImportResult> second = executor.submit(() -> {
                ready.countDown(); start.await(); return expenseService.importBatch(batch, 249L);
            });
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            ExpenseImportResult resultOne = first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            ExpenseImportResult resultTwo = second.get(10, java.util.concurrent.TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertEquals(1, resultOne.importedCount() + resultTwo.importedCount());
            org.junit.jupiter.api.Assertions.assertEquals(1, resultOne.duplicateCount() + resultTwo.duplicateCount());
        } finally {
            executor.shutdownNow();
        }
        Integer savedCount = jdbcTemplate.queryForObject("select count(*) from expenses where user_id = 249", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, savedCount);
    }

    @Test
    void expensePagesFilterByOwnerCategoryAndSearchWithStableSorting() throws Exception {
        ensureTestUser(250L);
        ensureTestUser(251L);
        java.time.LocalDate today = java.time.LocalDate.now();
        jdbcTemplate.update("insert into expenses (user_id, description, category, amount, payment_method, date) values (250, 'Book one', 'Education', 10, 'Cash', ?)", today);
        jdbcTemplate.update("insert into expenses (user_id, description, category, amount, payment_method, date) values (250, 'Book two', 'Education', 20, 'Cash', ?)", today);
        jdbcTemplate.update("insert into expenses (user_id, description, category, amount, payment_method, date) values (250, 'Lunch', 'Food & Dining', 30, 'UPI', ?)", today);
        jdbcTemplate.update("insert into expenses (user_id, description, category, amount, payment_method, date) values (251, 'Book private', 'Education', 40, 'Cash', ?)", today);

        mockMvc.perform(get("/api/expenses/page")
                        .param("page", "0").param("size", "1")
                        .param("category", "Education").param("search", "book")
                        .with(jwt().jwt(token -> token.claim("userId", 250L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].description").value("Book two"));

        mockMvc.perform(get("/api/expenses/page").param("page", "0").param("size", "101")
                        .with(jwt().jwt(token -> token.claim("userId", 250L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void firstRegistrationDoesNotClaimLegacyExpensesWithoutAnOwner() throws Exception {
        jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (null, 'Legacy row', 'Other', 45.00, 'Cash', ?)
                """, java.time.LocalDate.now());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"New user","email":"new-budgetbuddy-user@example.com","password":"StrongPass9!"}
                                """))
                .andExpect(status().isOk());

        Integer stillUnowned = jdbcTemplate.queryForObject(
                "select count(*) from expenses where user_id is null", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, stillUnowned);
    }

    @Test
    void financeRowsCannotReferenceAMissingUser() {
        java.time.LocalDate today = java.time.LocalDate.now();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                insert into expenses (user_id, description, category, amount, payment_method, date)
                values (999999, 'Orphan', 'Other', 1.00, 'Cash', ?)
                """, today));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                insert into income_entries (user_id, source, amount, date, frequency)
                values (999999, 'Orphan', 1.00, ?, 'ONCE')
                """, today));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                insert into category_budgets (user_id, category, budget_limit, period)
                values (999999, 'Other', 1, 'monthly')
                """));
    }

    private void ensureTestUser(long id) {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from app_users where id = ?", Integer.class, id);
        if (existing == null || existing == 0) {
            jdbcTemplate.update("""
                    insert into app_users (id, full_name, email, password_hash)
                    values (?, ?, ?, ?)
                    """, id, "Test user", "test-user-" + id + "@example.com", "not-used-in-jwt-test");
        }
    }
}
