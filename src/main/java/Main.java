import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Predicate;

public class Main {

    public static String jdbcUrl;
    public static String username;
    public static String password;
    public static String initScript;
    public static boolean admMode = false;
    // В кэше мы храним количество записей в различных таблицах, чтобы при отображении результата команды SELECT
    // не приходилось просматривать все записи в ResultSet
    public static HashMap<String, Integer> cashe = new HashMap<>();

    public static void main(String[] args){
        loadProps();

        Scanner in = new Scanner(System.in);
        String str;
        System.out.println("Управление БД с помощью консоли");
        System.out.println();

        if(testConn()) {
            casheIn();
            signIn(in);

            System.out.println("Введите SQL-запрос, или завершите работу командой QUIT:");
            str = in.nextLine();
            while (acceptRequests(str)) {
                System.out.println("Введите SQL-запрос, или завершите работу командой QUIT:");
                str = in.nextLine();
            }
            casheOut();
        }
    }

    // Считывание конфигурационного файла
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

    // Тестовое соединение к БД
    public static boolean testConn(){
        try {
            queryDB(initScript);
            System.out.println("Соединение к БД прошло успешно");
            return true;
        } catch (SQLException e){
            // Вывод ошибки
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

    // Регистрация пользователя
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

    private static String[] admComands = {"CREATE", "ALTER", "DROP", "GRANT", "REVOKE", "DENY"};

    // Обработка строкового запроса
    public static boolean acceptRequests(String requestString){
        // Разбиение строки на команды
        String[] listQuery = requestString.split(";");
        int i = 0;
        while (i < listQuery.length && !listQuery[i].equalsIgnoreCase("QUIT")){
            String comString = listQuery[i].trim();
            if(comString.toUpperCase().contains("SYSTEM")){
                System.out.println("Предупреждение: SYSTEM - зарезервированная таблица");
                System.out.println("Все записи в этой таблице в конце работы будут удалены");
            }
            // Проверка наличия прав для команд DDL и DCL
            if(!admMode && Arrays.stream(admComands).anyMatch(Predicate.isEqual(
                    comString.split(" ")[0].toUpperCase()))){
                System.out.print("Команду " + comString.split(" ")[0] + " не удалось выполнить\n");
                System.out.println("Ошибка: недостаточно прав для выполнения команды " + comString.split(" ")[0]);
            }else {
                try {
                    // Обработка случаев: команда SELECT/остальные команды
                    if(comString.split(" ")[0].equalsIgnoreCase("SELECT")){
                        selectDB(comString);
                    }else {
                        queryDB(comString);
                    }
                    System.out.print("Команда " + comString.split(" ")[0] + " выполнена\n");
                } catch (SQLException e) {
                    // Вывод ошибки
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
            // Случай: команда QUIT
            System.out.println("Работа завершена");
            return false;
        }
        return true;
    }

    // Обработка команды не SELECT
    public static void queryDB(String queryString) throws SQLException{
        try {
            // Подключение
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            // Выполнение
            Statement stmt = conn.createStatement();
            // Работа с кэшем
            int cnt = stmt.executeUpdate(queryString);
            if(queryString.toUpperCase().startsWith("CREATE TABLE") && !queryString.toUpperCase().contains("IF NO")){
                // Случай: команда CREATE
                String tabName = queryString.split(" ")[2];
                if(tabName.contains("(")){
                    tabName = tabName.substring(0, tabName.indexOf("("));
                }
                cashe.put(tabName, 0);
            } else if (queryString.toUpperCase().startsWith("DROP TABLE")) {
                // Случай: команда DROP
                String tabName = queryString.split(" ")[2];
                cashe.remove(tabName);
            } else if (queryString.toUpperCase().startsWith("INSERT INTO")){
                // Случай: команда INSERT
                String tabName = queryString.split(" ")[2];
                if(tabName.contains("(")){
                    tabName = tabName.substring(0, tabName.indexOf("("));
                }
                if(cashe.containsKey(tabName)){
                    cashe.replace(tabName, cashe.get(tabName), Integer.parseInt(cashe.get(tabName).toString())+1);
                }
            } else if (queryString.toUpperCase().startsWith("DELETE")){
                // Случай: команда DELETE
                String tabName = queryString.split(" ")[1];
                if(cashe.containsKey(tabName)) {
                    cashe.replace(tabName, cashe.get(tabName), Integer.parseInt(cashe.get(tabName).toString()) - cnt);
                }
            }
            conn.close();
        }catch (SQLException e){
            throw e;
        }
    }

    private static final int maxLenInt = 10;
    private static final int maxLenString = 50;

    // Обработка команды SELECT
    public static void selectDB(String queryString) throws SQLException{
        queryString = queryString.toUpperCase();
        try {
            // Подключение
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            // Выполнение
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(queryString);
            ResultSetMetaData data = res.getMetaData();

            // Рисование таблицы
            int cnt = data.getColumnCount();
            // Вывод заголовков
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
            // Вывод границы
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
            // Вывод записей
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
            // Подсчёт количества всех записей
            if(queryString.substring(queryString.indexOf("FROM")).split(" ").length > 2){
                // Случай: у запроса есть параметры WHERE, LIMIT, OFFSET, ... (кэш нельзя использовать)
                int total = k;
                while (res.next()){
                    total++;
                }
                System.out.println("Показано " + k + "/" + total + " записей");
            }else{
                String tabName = queryString.split(" ")[queryString.split(" ").length-1];
                if(cashe.containsKey(tabName)){
                    // Случай: таблица находится в кэше
                    System.out.println("Показано " + k + "/" + cashe.get(tabName) + " записей");
                }else{
                    // Случай: таблица не находится в кэше
                    int total = k;
                    while (res.next()){
                        total++;
                    }
                    System.out.println("Показано " + k + "/" + total + " записей");
                    cashe.put(tabName, total);
                }
            }
            conn.close();
        }catch (SQLException e){
            throw e;
        }
    }

    // Загрузка данных о количествах записей в таблицах из БД в программу
    public static void casheIn(){
        try {
            // Соединение
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            // Считывание информации о числе записей в доступных таблицах
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS SYSTEM(NAME VARCHAR(100), COUNT INTEGER)");
            ResultSet res = stmt.executeQuery("SELECT * FROM SYSTEM");
            while (res.next()){
                // Сохранение информации в кэш
                cashe.put(res.getString(1).toUpperCase(), res.getInt(2));
            }
        }catch (SQLException e){
            System.out.println("Ошибка программиста");
        }
    }

    // Загрузка данных о количествах записей в таблицах из программы в БД
    public static void casheOut(){
        try {
            // Соединение
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            // Обновление таблицы SYSTEM
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE SYSTEM");
            stmt.execute("CREATE TABLE SYSTEM(NAME VARCHAR(100), COUNT INTEGER)");
            stmt.close();
            cashe.forEach((k, v) -> {
                // Добавление информации в таблицу
                try {
                    PreparedStatement prst = conn.prepareStatement("INSERT INTO SYSTEM VALUES(?, ?)");
                    prst.setString(1, k);
                    prst.setInt(2, v);
                    prst.execute();
                } catch (SQLException e) {
                    System.out.println("Ошибка программиста");
                }
            });
        }catch (SQLException e){
            System.out.println("Ошибка программиста");
        }
    }
}
