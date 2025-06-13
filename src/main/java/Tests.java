import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
    public void testConnTest(){
        setOut();
        loadProps();
        Main.testConn();

        Assert.assertEquals(outputStreamCaptor.toString().trim(), "Соединение к БД прошло успешно");
    }

    @Test
    public void testSignIn0(){
        setIn("Y\nsa\n\n");
        Main.signIn();
        Assert.assertEquals(Main.admMode, true);
    }

    @Test
    public void testSignIn1(){
        setIn("N\n");
        Main.signIn();
        Assert.assertEquals(Main.admMode, false);
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
        Assert.assertEquals(outputStreamCaptor.toString().trim(),
                "Ошибка: недостаточно прав для выполнения команды DROP");
    }

    @Test
    public void testAcceptRequests2(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("CREATE");
        Assert.assertEquals(outputStreamCaptor.toString().trim(),
                "Ошибка: недостаточно прав для выполнения команды CREATE");
    }

    @Test
    public void testAcceptRequests3(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("abacaba");
        Assert.assertTrue(outputStreamCaptor.toString().trim().startsWith("Команду abacaba не удалось выполнить\n" +
                "Синтаксическая ошибка: "));
    }

    @Test
    public void testAcceptRequests4(){
        setOut();
        loadProps();
        Main.admMode = false;
        Main.acceptRequests("INSERT INTO TABLE404");
        Assert.assertTrue(outputStreamCaptor.toString().trim().startsWith("Команду INSERT не удалось выполнить\n" +
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
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Команда SELECT выполнена"));
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
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Показано 3/3 записей"));
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
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Ivanov"));
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Smirnov"));
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Kozlov"));
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
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Ivanov"));
        Assert.assertFalse(outputStreamCaptor.toString().trim().contains("Smirnov"));
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Kozlov"));
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
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Ivanov"));
        Assert.assertFalse(outputStreamCaptor.toString().trim().contains("Smirnov"));
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Kozlov"));
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
        Assert.assertTrue(outputStreamCaptor.toString().trim().contains("Показано 10/23 записей"));
    }
}
