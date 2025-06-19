import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;

public class Tests {

    private ByteArrayOutputStream outputStreamCaptor;

    public void setOut() {
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    public void setIn(String str){
        System.setIn(new ByteArrayInputStream(str.getBytes()));
    }

    @Test
    public void testLoadProps(){
        Main.loadProps();

        Assert.assertEquals(Main.jdbcUrl, "jdbc:h2:~/db_console");
        Assert.assertEquals(Main.username, "sa");
        Assert.assertEquals(Main.password, "");
        Assert.assertEquals(Main.initScript, "CREATE TABLE IF NOT EXISTS STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
    }

    public void loadProps(){
        Main.jdbcUrl = "jdbc:h2:~/db_console";
        Main.username = "sa";
        Main.password = "";
        Main.initScript = "CREATE TABLE IF NOT EXISTS STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))";
    }

    @Test
    public void testConnTest0(){
        setOut();
        loadProps();
        Main.testConn();

        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Соединение к БД прошло успешно");
    }

    @Test
    public void testConnTest1(){
        setOut();
        loadProps();
        Main.jdbcUrl = "404";
        Main.testConn();

        Assert.assertTrue(outputStreamCaptor.toString().startsWith("Соединиться к БД не получилось"));
    }

    @Test
    public void testSignIn0(){
        setIn("Y\nsa\n\n");
        Scanner in = new Scanner(System.in);
        Main.signIn(in);
        Assert.assertTrue(Main.admMode);
    }

    @Test
    public void testSignIn1(){
        setIn("N\n");
        Scanner in = new Scanner(System.in);
        Main.signIn(in);
        Assert.assertFalse(Main.admMode);
    }

    @Test
    public void testSignIn2(){
        setIn("Y\nqa\n");
        setOut();
        Scanner in = new Scanner(System.in);
        Main.signIn(in);
        Assert.assertFalse(Main.admMode);
        Assert.assertTrue(outputStreamCaptor.toString().contains("Логин неверный"));
    }

    @Test
    public void testSignIn3(){
        setIn("Y\nsa\npassword\n");
        setOut();
        Scanner in = new Scanner(System.in);
        Main.signIn(in);
        Assert.assertFalse(Main.admMode);
        Assert.assertTrue(outputStreamCaptor.toString().contains("Пароль неверный"));
    }

    @Test
    public void testAcceptRequests0(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("QUIT");
        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Работа завершена");
    }

    @Test
    public void testAcceptRequests1(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("DROP");
        Assert.assertTrue(outputStreamCaptor.toString().trim()
                .contains("Ошибка: недостаточно прав для выполнения команды DROP"));
    }

    @Test
    public void testAcceptRequests2(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("CREATE");
        Assert.assertTrue(outputStreamCaptor.toString().trim()
                .contains("Ошибка: недостаточно прав для выполнения команды CREATE"));
    }

    @Test
    public void testAcceptRequests3(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("abacaba");
        Assert.assertTrue(outputStreamCaptor.toString().startsWith("Команду abacaba не удалось выполнить\n" +
                "Синтаксическая ошибка: "));
    }

    @Test
    public void testAcceptRequests4(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("INSERT INTO TABLE404");
        Assert.assertTrue(outputStreamCaptor.toString().startsWith("Команду INSERT не удалось выполнить\n" +
                "Ошибка: "));
    }

    @Test
    public void testAcceptRequestsInsert0(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("CREATE TABLE IF NOT EXISTS STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Команда CREATE выполнена");
    }

    @Test
    public void testAcceptRequestsInsert1(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Команда DROP выполнена\n" +
                "Команда CREATE выполнена");
    }

    @Test
    public void testAcceptRequestsInsert2(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov')");
        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Команда INSERT выполнена");
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsInsert3(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "INSERT INTO STUDENTS VALUES(3, 'Dobrov')");
        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Команда INSERT выполнена\n" +
                "Команда INSERT выполнена\n" +
                "Команда INSERT выполнена");
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsInsert4(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(1, 'Smirnov')");
        Assert.assertTrue(outputStreamCaptor.toString().startsWith("Команда INSERT выполнена\n" +
                "Команду INSERT не удалось выполнить\n" +
                "Ошибка: "));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsInsert5(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "UPDATE STUDENTS SET NAME = 'Kozlov' WHERE ID = 2; " +
                "UPDATE STUDENTS SET NAME = 'Kozlov' WHERE ID = 3");
        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Команда INSERT выполнена\n" +
                "Команда INSERT выполнена\n" +
                "Команда UPDATE выполнена\n" +
                "Команда UPDATE выполнена");
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsSelect0(){
        setOut();
        loadProps();
        Main.acceptRequests("SELECT * * * FRRR STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Команду SELECT не удалось выполнить"));
    }

    @Test
    public void testAcceptRequestsSelect1(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "INSERT INTO STUDENTS VALUES(3, 'Dobrov'); " +
                "SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Команда SELECT выполнена"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsSelect2(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "INSERT INTO STUDENTS VALUES(3, 'Dobrov'); " +
                "SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Показано 3/3 записей"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsSelect3(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "INSERT INTO STUDENTS VALUES(3, 'Kozlov'); " +
                "SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Ivanov"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("Smirnov"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("Kozlov"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsSelect4(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "UPDATE STUDENTS SET NAME = 'Kozlov' WHERE ID = 2; " +
                "SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Ivanov"));
        Assert.assertFalse(outputStreamCaptor.toString().contains("Smirnov"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("Kozlov"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsSelect5(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Smirnov'); " +
                "INSERT INTO STUDENTS VALUES(3, 'Kozlov'); " +
                "DELETE STUDENTS WHERE NAME = 'Smirnov'; " +
                "SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Ivanov"));
        Assert.assertFalse(outputStreamCaptor.toString().contains("Smirnov"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("Kozlov"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsSelect6(){
        setOut();
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        setOut();
        for(int i = 0; i < 23; i++){
            Main.acceptRequests("INSERT INTO STUDENTS VALUES(" + i + ", '" + i +"')");
        }
        setOut();
        Main.acceptRequests("SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Показано 10/23 записей"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testMain1(){
        setIn("Y\nsa\n\n" +
                "DROP TABLE STUDENTS\n" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))\n" +
                "INSERT INTO STUDENTS VALUES(1, 'IVAN')\n" +
                "INSERT INTO STUDENTS VALUES(2, 'ANDREY')\n" +
                "INSERT INTO STUDENTS VALUES(3, 'VASILIY')\n" +
                "DELETE STUDENTS WHERE ID=3\n" +
                "UPDATE STUDENTS SET NAME = 'ANDREW' WHERE ID = 2\n" +
                "SELECT * FROM STUDENTS\n" +
                "QUIT\n");
        setOut();
        Main.main(new String[0]);
        Assert.assertTrue(outputStreamCaptor.toString().contains("Работа завершена"));
        Assert.assertFalse(outputStreamCaptor.toString().contains("не удалось выполнить"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testCasheIn() {
        loadProps();
        Main.admMode = true;
        Main.acceptRequests("DROP TABLE SYSTEM;" +
                "CREATE TABLE IF NOT EXISTS SYSTEM(NAME VARCHAR(100), COUNT INTEGER)");
        Main.acceptRequests("INSERT INTO SYSTEM VALUES('TABLE1', 1);" +
                "INSERT INTO SYSTEM VALUES('TABLE2', 10);" +
                "INSERT INTO SYSTEM VALUES('TABLE3', 100);");
        Main.cashe = new HashMap<>();
        Main.casheIn();
        Assert.assertTrue(Main.cashe.containsKey("TABLE1"));
        Assert.assertEquals(Main.cashe.get("TABLE1"), 1);
        Assert.assertTrue(Main.cashe.containsKey("TABLE2"));
        Assert.assertEquals(Main.cashe.get("TABLE2"), 10);
        Assert.assertTrue(Main.cashe.containsKey("TABLE3"));
        Assert.assertEquals(Main.cashe.get("TABLE3"), 100);
        Main.acceptRequests("DROP TABLE SYSTEM");
    }

    @Test
    public void testCasheOut() {
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();
        Main.cashe.put("TABLE1", 24);
        Main.cashe.put("TABLE2", 10);
        Main.cashe.put("TABLE3", 100);
        Main.acceptRequests("DROP TABLE SYSTEM;" +
                "CREATE TABLE IF NOT EXISTS SYSTEM(NAME VARCHAR(100), COUNT INTEGER)");
        Main.casheOut();
        setOut();
        Main.acceptRequests("SELECT * FROM SYSTEM");
        Assert.assertTrue(outputStreamCaptor.toString().contains("TABLE1"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("TABLE2"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("TABLE3"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("24"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("10"));
        Assert.assertTrue(outputStreamCaptor.toString().contains("100"));
        Main.acceptRequests("DROP TABLE SYSTEM");
    }

    @Test
    public void testAcceptRequestsWithCashe1(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();

        Main.acceptRequests("CREATE TABLE TEST");
        Assert.assertTrue(Main.cashe.containsKey("TEST"));
        Main.acceptRequests("DROP TABLE TEST");
    }

    @Test
    public void testAcceptRequestsWithCashe2(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();

        Main.acceptRequests("CREATE TABLE TEST");
        Main.cashe = new HashMap<>();
        Main.acceptRequests("CREATE TABLE IF NOT EXISTS TEST");
        Assert.assertFalse(Main.cashe.containsKey("TEST"));
        Main.acceptRequests("DROP TABLE TEST");
    }

    @Test
    public void testAcceptRequestsWithCashe3(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();

        Main.acceptRequests("CREATE TABLE TEST(ID INTEGER)");
        Assert.assertTrue(Main.cashe.containsKey("TEST"));
        Main.acceptRequests("DROP TABLE TEST");
    }

    @Test
    public void testAcceptRequestsWithCashe4(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();

        Main.acceptRequests("CREATE TABLE TEST");
        Main.acceptRequests("DROP TABLE TEST");
        Assert.assertFalse(Main.cashe.containsKey("TEST"));
    }

    @Test
    public void testAcceptRequestsWithCashe5(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();

        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        Main.cashe = new HashMap<>();
        Main.cashe.put("STUDENTS", 100);
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov')");
        Assert.assertEquals(Main.cashe.get("STUDENTS"), 101);
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsWithCashe6(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        Main.acceptRequests("INSERT INTO STUDENTS VALUES(1, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(2, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(3, 'Ivanov'); " +
                "INSERT INTO STUDENTS VALUES(4, 'Smirnov'); " +
                "INSERT INTO STUDENTS VALUES(5, 'Kozlov'); ");
        Main.cashe = new HashMap<>();
        Main.cashe.put("STUDENTS", 100);
        Main.acceptRequests("DELETE STUDENTS WHERE NAME = 'Ivanov'");
        Assert.assertEquals(Main.cashe.get("STUDENTS"), 97);
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    public void fillStudents(int cnt){
        for(int i = 1; i <= cnt; i++){
            Main.acceptRequests("INSERT INTO STUDENTS VALUES(" + i + ", '" + i + "')");
        }
    }

    @Test
    public void testAcceptRequestsWithCashe7(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        fillStudents(23);
        Main.cashe.replace("STUDENTS", 23, 100);
        setOut();
        Main.acceptRequests("SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Показано 10/100 записей"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsWithCashe8(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        fillStudents(23);
        Main.cashe.replace("STUDENTS", 23, 100);
        setOut();
        Main.acceptRequests("SELECT * FROM STUDENTS LIMIT 19");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Показано 10/19 записей"));
        Main.acceptRequests("DROP TABLE STUDENTS");
    }

    @Test
    public void testAcceptRequestsWithCashe9(){
        loadProps();
        Main.admMode = true;
        Main.cashe = new HashMap<>();
        Main.acceptRequests("DROP TABLE STUDENTS;" +
                "CREATE TABLE STUDENTS(ID INTEGER PRIMARY KEY, NAME VARCHAR(50))");
        fillStudents(23);
        Main.cashe.remove("STUDENTS");
        setOut();
        Main.acceptRequests("SELECT * FROM STUDENTS");
        Assert.assertTrue(outputStreamCaptor.toString().contains("Показано 10/23 записей"));
        Assert.assertEquals(Main.cashe.get("STUDENTS"), 23);
        Main.acceptRequests("DROP TABLE STUDENTS");
    }
}
