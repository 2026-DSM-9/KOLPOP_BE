package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class V1__Chat_rooms_by_listing extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "chat_rooms")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "chat_rooms", "listing_id")) {
                statement.execute("ALTER TABLE chat_rooms ADD COLUMN listing_id BIGINT NULL");
            }
            if (indexExists(connection, "chat_rooms", "uk_chat_rooms_founder_landlord")) {
                statement.execute("ALTER TABLE chat_rooms DROP INDEX uk_chat_rooms_founder_landlord");
            }
            if (indexExists(connection, "chat_rooms", "uk_chat_rooms_founder_landlord_listing")) {
                statement.execute("ALTER TABLE chat_rooms DROP INDEX uk_chat_rooms_founder_landlord_listing");
            }
            if (!indexExists(connection, "chat_rooms", "uk_chat_rooms_founder_listing")) {
                statement.execute("ALTER TABLE chat_rooms ADD CONSTRAINT uk_chat_rooms_founder_listing "
                        + "UNIQUE (founder_id, listing_id)");
            }
            if (tableExists(connection, "listings") && !foreignKeyExists(connection, "chat_rooms", "fk_chat_rooms_listing")) {
                statement.execute("ALTER TABLE chat_rooms ADD CONSTRAINT fk_chat_rooms_listing "
                        + "FOREIGN KEY (listing_id) REFERENCES listings (id)");
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, "%")) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String foreignKeyName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet keys = metadata.getImportedKeys(connection.getCatalog(), null, tableName)) {
            while (keys.next()) {
                if (foreignKeyName.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
