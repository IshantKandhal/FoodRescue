import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DBconnection {
    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/food_rescue_db",
            "root",
            "*************"
        );
        return con;
    }
}