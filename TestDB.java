import com.turismo.api.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Connected to database successfully.");
            DatabaseMetaData meta = conn.getMetaData();
            // Check if table TURISTA exists
            ResultSet tables = meta.getTables(null, null, "TURISTA", new String[]{"TABLE"});
            if (tables.next()) {
                System.out.println("Table TURISTA exists.");
                // Get columns
                ResultSet cols = meta.getColumns(null, null, "TURISTA", null);
                System.out.println("Columns:");
                while (cols.next()) {
                    String columnName = cols.getString("COLUMN_NAME");
                    String typeName = cols.getString("TYPE_NAME");
                    int columnSize = cols.getInt("COLUMN_SIZE");
                    System.out.printf("  %s (%s, size %s)%n", columnName, typeName, columnSize);
                }
                cols.close();
            } else {
                System.out.println("Table TURISTA does NOT exist.");
            }
            tables.close();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}