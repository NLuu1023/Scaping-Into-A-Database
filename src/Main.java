import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    static Connection conn;
    static Statement stmt;
    static ResultSet resultSet;

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        String website = "https://aps2.missouriwestern.edu/schedule/Default.asp?tck=201910";
        String connectStmt = "jdbc:sqlite:Schedule.db";

        ArrayList<Subject> myDepartment = new ArrayList<>();
        ArrayList<Subject> myDiscipline = new ArrayList<>();
        Document doc = null;
        try {
            doc = Jsoup.connect(website).get();
            Elements subject = doc.select("select#subject > option");
            for(Element option: subject) {
                String dis_abbrev = option.attr("value");
                String dis_name = option.select("[value]").text();
                myDiscipline.add(new Subject(dis_abbrev, dis_name));
            }

            Elements departments = doc.select("select#department > option");
            for (Element option : departments) {
                String dep_abbrev = option.attr("value");
                String dep_name = option.select("[value]").text();
                myDepartment.add(new Subject(dep_abbrev, dep_name));
            }

            conn = DriverManager.getConnection(connectStmt);
            stmt = conn.createStatement();
            menu(myDiscipline, myDepartment, website, connectStmt);
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void menu(ArrayList<Subject> myDis, ArrayList<Subject> myDep, String website, String connectStmt){
        char ch;
        do{
            System.out.println("\nMENU");
            System.out.print("A = Erase and build subject table.\n" +
                    "B = Erase and build department table.\n" +
                    "C = Print subject table.\n" +
                    "D = Print department table.\n" +
                    "E = Print report of discipline by department\n" +
                    "Q = Quit.\n");
            System.out.println("Please type in your choice:");
            Scanner input = new Scanner(System.in);
            String choice = input.next().toUpperCase().trim();
            ch = (choice.length() > 0)? choice.charAt(0): 'x';
            switch(ch){
                case 'A':
                    eraseAndBuildTable(myDis,"disciplines");
                    break;
                case 'B':
                    eraseAndBuildTable(myDep, "departments");
                    break;
                case 'C':
                    printTable("disciplines", "dis_abbrev", "dis_name");
                    break;
                case 'D':
                    printTable("departments", "dep_abbrev", "dep_name");
                    break;
                case 'E':
                    String topic;
                    do{
                        System.out.println("Please enter in the abbreviation of the department you want to search.");
                        System.out.println("Enter R if you want to stop searching");
                        Scanner dep_abbrev = new Scanner(System.in);
                        topic = dep_abbrev.next().toUpperCase().trim();
                        switch(topic){
                            case "R":
                                break;
                            default:
                                printDisciplinesByDepartment(topic, website);
                        }
                    }while(!topic.equals("R"));
                    break;
                case 'Q':
                    break;
                default:
                    System.out.println("You still have to type a letter from the menu!!");
            }
        }while(ch != 'Q');
    }

    public static void eraseAndBuildTable(ArrayList<Subject> myList, String tableName){
        try {
            stmt.execute("delete from '" + tableName + "'");

            for(int i=1; i<myList.size(); i++){
                stmt.executeUpdate("insert into " + tableName + " values('" + myList.get(i).getAbbrev() + "', '"  + myList.get(i).getName() + "')");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Erasing and building " + tableName + " table successful.");
    }

    public static void printTable(String tableName, String colunm1Name, String column2Name){
        try {
            stmt.execute("select * from " + tableName);
            resultSet = stmt.getResultSet();

            System.out.println("\n" + tableName.toUpperCase() + " TABLE");
            System.out.printf("%s   %s\n", colunm1Name, column2Name);

            while(resultSet.next()) {
                String abbrev = resultSet.getString(colunm1Name);
                String name = resultSet.getString(column2Name);
                System.out.printf("%-15s %s\n", abbrev, name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Printing " + tableName + " table complete.");
    }

    public static void printDisciplinesByDepartment(String dep_abbrev, String website){
        System.out.println("Searching discipline by department:");
        ArrayList<String> myDis = new ArrayList<>();

        try {
            stmt.execute("select dep_name from departments where dep_abbrev like '" + dep_abbrev + "'");
            resultSet = stmt.getResultSet();
            if(resultSet.next()) {
                String dep_name = resultSet.getString("dep_name");
                System.out.printf("%s - %s\n", dep_abbrev, dep_name);
                org.jsoup.Connection.Response response = Jsoup.connect(website)
                        .timeout(60 * 1000)
                        .method(org.jsoup.Connection.Method.POST)
                        .data("course_number", "")
                        .data("subject", "ALL")
                        .data("department", dep_abbrev)
                        .data("display_closed", "yes")
                        .data("course_type", "ALL")
                        .followRedirects(true)
                        .execute();
                Document doc = response.parse();
                Elements course = doc.select("a");
                for (int i = 0; i < course.size(); i++) {
                    String discipline = course.get(i).select("[href]").text().trim();
                    if (discipline.length() == 6) {
                        myDis.add(discipline.substring(0,3));
                    }
                }
                for(int n=0; n<deleteCopy(myDis).size(); n++) {
                    stmt.execute("select dis_name from disciplines where dis_abbrev like '" + deleteCopy(myDis).get(n) + "'");
                    resultSet = stmt.getResultSet();
                    String dis_name = resultSet.getString("dis_name");
                    System.out.printf("\t%s - %s\n", deleteCopy(myDis).get(n), dis_name);
                }
            }
            else {
                System.out.println("There is no such abbreviation of department.");
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> deleteCopy(ArrayList<String> myList){
        ArrayList<String> myDis = new ArrayList<>();
        for(int i=0; i<myList.size(); i++){
            if(myDis.size() == 0){
                myDis.add(myList.get(i));
            }
            else{
                if(!myDis.contains(myList.get(i))){
                    myDis.add(myList.get(i));
                }
            }
        }
        return myDis;
    }
}
