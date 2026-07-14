package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__Ai_conversations extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (tableExists(connection, "ai_conversations")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE ai_conversations (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        title VARCHAR(120) NOT NULL,
                        user_message VARCHAR(1000) NOT NULL,
                        ai_message VARCHAR(4000) NOT NULL,
                        request_payload TEXT,
                        response_payload TEXT,
                        created_at DATETIME NOT NULL,
                        PRIMARY KEY (id),
                        INDEX idx_ai_conversations_user_created_at (user_id, created_at),
                        CONSTRAINT fk_ai_conversations_user
                            FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);
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
}
