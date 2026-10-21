


import java.sql.Connection;
import org.junit.jupiter.api.Test;

import com.DBConnection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DBConnectionTest {

    @Test
    void shouldConnectToTiDB() throws Exception {
        try (Connection connection = DBConnection.getConnection()) {
            assertNotNull(connection);
            System.out.println("Connected to TiDB successfully!");
        }
    }
}
