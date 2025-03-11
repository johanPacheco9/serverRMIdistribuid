package distribuidos.documentconverters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private Connection connection;

    public DBConnection() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                "jdbc:mariadb://127.0.0.1:3306/convertdocuments",
                "root",
                "1098825894"
            );
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace(); // Manejo de errores
        }
    }

    public Connection getConnection() {
        return this.connection;
    }
}
