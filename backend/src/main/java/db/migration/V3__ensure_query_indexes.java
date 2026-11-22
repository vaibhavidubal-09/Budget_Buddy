package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds indexes declared by the entity mappings without duplicating existing indexes. */
public class V3__ensure_query_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureIndex(connection, "expenses", "idx_expenses_user_date", "user_id, date");
        ensureIndex(connection, "income_entries", "idx_income_user_date", "user_id, date");
        ensureIndex(connection, "category_budgets", "idx_budgets_user_period_category",
                "user_id, period, category");
    }

    private void ensureIndex(Connection connection, String table, String index, String columns) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        String databaseTable = findTableName(connection, metadata, table);
        if (databaseTable == null) {
            throw new IllegalStateException("Expected table does not exist: " + table);
        }

        try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, databaseTable, false, false)) {
            while (indexes.next()) {
                String existingName = indexes.getString("INDEX_NAME");
                if (existingName != null && existingName.toLowerCase(Locale.ROOT).equals(index.toLowerCase(Locale.ROOT))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX " + index + " ON " + databaseTable + " (" + columns + ")");
        }
    }

    private String findTableName(Connection connection, DatabaseMetaData metadata, String expectedName) throws Exception {
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, null, new String[] { "TABLE" })) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName != null && tableName.equalsIgnoreCase(expectedName)) {
                    return tableName;
                }
            }
        }
        return null;
    }
}
