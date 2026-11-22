package db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class V3EnsureQueryIndexesTest {

    @Test
    void baselinesAnExistingSchemaThenAppliesOwnershipAndIndexMigrations() throws Exception {
        String url = "jdbc:h2:mem:existing-schema-baseline;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE app_users (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE expenses (user_id BIGINT, date DATE)");
            statement.execute("CREATE TABLE income_entries (user_id BIGINT, date DATE)");
            statement.execute("CREATE TABLE category_budgets (user_id BIGINT, period VARCHAR(12), category VARCHAR(60))");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertEquals("7", flyway.info().current().getVersion().getVersion());
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            assertEquals(1, indexCount(connection, "CATEGORY_BUDGETS",
                    "UK_CATEGORY_BUDGETS_USER_PERIOD_CATEGORY"));
        }
    }

    @Test
    void createsQueryIndexesWhenLegacyTablesDoNotHaveThem() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:legacy-index-migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE expenses (user_id BIGINT, date DATE)");
            statement.execute("CREATE TABLE income_entries (user_id BIGINT, date DATE)");
            statement.execute("CREATE TABLE category_budgets (user_id BIGINT, period VARCHAR(12), category VARCHAR(60))");

            Context context = mock(Context.class);
            when(context.getConnection()).thenReturn(connection);
            new V3__ensure_query_indexes().migrate(context);

            assertEquals(1, indexCount(connection, "EXPENSES", "IDX_EXPENSES_USER_DATE"));
            assertEquals(1, indexCount(connection, "INCOME_ENTRIES", "IDX_INCOME_USER_DATE"));
            assertEquals(1, indexCount(connection, "CATEGORY_BUDGETS", "IDX_BUDGETS_USER_PERIOD_CATEGORY"));
        }
    }

    private int indexCount(Connection connection, String table, String index) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES
                     WHERE TABLE_NAME = '""" + table + "' AND INDEX_NAME = '" + index + "'")) {
            result.next();
            return result.getInt(1);
        }
    }
}
