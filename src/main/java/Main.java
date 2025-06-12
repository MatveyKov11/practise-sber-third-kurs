import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {

    private static String jdbcUrl = "jdbc:h2:~/db_console";
    private static String username = "sa";
    private static String password = "";

    public static void main(String[] args) throws SQLException {
        Scanner in = new Scanner(System.in);
        System.out.println("Управление БД с помощью консоли");
        //System.out.println("Управление БД с помощью консоли");
        String str = in.nextLine();
        str = str.trim();
        while (!str.toUpperCase().equals("QUIT")){
            queryDB(str);
            str = in.next();
            str = str.trim();
        }

    }

    public static void queryDB(String queryString) throws SQLException{
        Connection conn = DriverManager.getConnection(jdbcUrl, username, password);

        Statement stmt = conn.createStatement();
        stmt.execute(queryString);

        conn.close();
    }
}
