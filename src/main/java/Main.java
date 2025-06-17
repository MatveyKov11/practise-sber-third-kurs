import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {

    public static String jdbcUrl;
    public static String username;
    public static String password;
    public static String initScript;
    public static boolean admMode = false;
    private static String[] admComands = {"CREATE", "ALTER", "DROP", "GRANT", "REVOKE", "DENY"};

    public static void main(String[] args){
        loadProps();

        Scanner in = new Scanner(System.in);
        String str;
        System.out.println("Управление БД с помощью консоли");
        System.out.println();

        if(testConn()) {
            signIn(in);

            System.out.println("Введите SQL-запрос, или завершите работу командой QUIT:");
            str = in.nextLine();
            while (acceptRequests(str)) {
                System.out.println("Введите SQL-запрос, или завершите работу командой QUIT:");
                str = in.nextLine();
            }
        }
    }

    public static void loadProps(){
        Properties props = new Properties();
        InputStream inputStream = Main.class.getResourceAsStream("config.properties");
        try {
            props.load(inputStream);
            //props.load(Files.newInputStream(Paths.get("src/main/resources/config.properties")));
        }catch (IOException e){
            System.out.println("Ошибка программиста");
        }

        jdbcUrl = props.getProperty("jdbcUrl");
        username = props.getProperty("username");
        password = props.getProperty("password").substring(1);
        initScript = props.getProperty("initScript");
    }

    public static boolean testConn(){
        try {
            queryDB(initScript);
            System.out.println("Соединение к БД прошло успешно");
            return true;
        } catch (SQLException e){
            String msg = e.getMessage();
            System.out.println("Соединиться к БД не получилось");
            if (msg.startsWith("База данных уже используется")){
                System.out.println("Ошибка: БД уже используется другим пользователем или программой");
            }else {
                System.out.println("Ошибка: " + msg);
            }
            System.out.println("Попробуйте подключиться ещё раз через некоторое время");
            return false;
        }
    }

    public static void signIn(Scanner in){;
        System.out.print("Войти в режим администратора? (Y/N, значение по умолчанию - N) ");
        String str = in.nextLine();
        if(str.equalsIgnoreCase("Y")){
            System.out.print("Введите логин: ");
            String log = in.nextLine();
            if(log.equals(username)){
                System.out.print("Введите пароль: ");
                String pass = in.nextLine();
                if(pass.equals(password)){
                    admMode = true;
                    System.out.println("Вы вошли как администратор");
                }else {
                    System.out.println("Пароль неверный");
                    System.out.println("Вы вошли как обычный пользователь");
                    admMode = false;
                }
            }else {
                System.out.println("Логин неверный");
                System.out.println("Вы вошли как обычный пользователь");
                admMode = false;
            }
        }else {
            System.out.println("Вы вошли как обычный пользователь");
            admMode = false;
        }
        System.out.println();
    }

    public static boolean acceptRequests(String requestString){
        String[] listQuery = requestString.split(";");
        int i = 0;
        while (i < listQuery.length && !listQuery[i].equalsIgnoreCase("QUIT")){
            String comString = listQuery[i].trim();
            if(!admMode && Arrays.stream(admComands).anyMatch(Predicate.isEqual(
                    comString.split(" ")[0].toUpperCase()))){
                System.out.println("Ошибка: недостаточно прав для выполнения команды " + comString.split(" ")[0]);
            }else {
                try {
                    if(comString.split(" ")[0].toUpperCase().equals("SELECT")){
                        selectDB(comString);
                    }else {
                        queryDB(comString);
                    }
                    System.out.print("Команда " + comString.split(" ")[0] + " выполнена\n");
                } catch (SQLException e) {
                    System.out.print("Команду " + comString.split(" ")[0] + " не удалось выполнить\n");
                    String msg = e.getMessage();
                    if (msg.startsWith("Синтаксическая ошибка")) {
                        System.out.println("Синтаксическая ошибка: " +
                                msg.substring("Синтаксическая ошибка ".length(), msg.indexOf("\n")));
                    } else {
                        System.out.println("Ошибка: " + msg.substring(0, msg.indexOf("\n")));
                    }
                }
            }
            i++;
        }
        if (i < listQuery.length){
            System.out.println("Работа завершена");
            return false;
        }
        return true;
    }

    public static void queryDB(String queryString) throws SQLException{
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);

            Statement stmt = conn.createStatement();
            stmt.execute(queryString);
            conn.close();
        }catch (SQLException e){
            throw e;
        }
    }

    private static int maxLenInt = 10;
    private static int maxLenString = 50;

    public static void selectDB(String queryString) throws SQLException{
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);

            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(queryString);
            ResultSetMetaData data = res.getMetaData();
            int cnt = data.getColumnCount();
            for(int i = 1; i <= cnt; i++){
                String type = data.getColumnTypeName(i);
                int d = maxLenString;
                switch (type){
                    case "INTEGER":
                        d = maxLenInt;
                        break;
                }
                String clmName = data.getColumnName(i);
                System.out.print(clmName);
                for(int j = clmName.length(); j < d; j++){
                    System.out.print(" ");
                }
                System.out.print("|");
            }
            System.out.println();
            for(int i = 1; i <= cnt; i++){
                String type = data.getColumnTypeName(i);
                int d = maxLenString;
                switch (type){
                    case "INTEGER":
                        d = maxLenInt;
                        break;
                }
                for(int j = 0; j < d; j++){
                    System.out.print("-");
                }
                System.out.print("+");
            }
            System.out.println();
            int k = 0;
            while(k < 10 && res.next()){
                for(int i = 1; i <= cnt; i++){
                    String type = data.getColumnTypeName(i);
                    int d = maxLenString;
                    switch (type){
                        case "INTEGER":
                            d = maxLenInt;
                            break;
                    }
                    String blData = res.getString(i);
                    System.out.print(blData);
                    for(int j = blData.length(); j < d; j++){
                        System.out.print(" ");
                    }
                    System.out.print("|");
                }
                System.out.println();
                k++;
            }
            int total = k;
            while (res.next()){
                total++;
            }
            System.out.println("Показано " + k + "/" + total + " записей");
            conn.close();
        }catch (SQLException e){
            throw e;
        }
    }
}
