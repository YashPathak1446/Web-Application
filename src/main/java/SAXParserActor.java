package main.java;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAXParserActor extends DefaultHandler {

    private Connection conn;
    private String tempVal;
    private Star tempStar;
    private List<Star> starList;
    private HashMap<String, String> starInfo; // stageName: starId -> import to cast parser
    private Integer maxStarId;
    private int insertedStarsCount = 0;
    private int inconsistentStarsCount = 0;
    private int duplicateStarCount = 0;

    // Number of threads you want to use for parallel parsing
    private static final int NUM_THREADS = 4;

    public SAXParserActor() {

        starList = new ArrayList<>();
        maxStarId = generateStarId();

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            String starInsertQuery = "SELECT * FROM stars";

            PreparedStatement starStmt = conn.prepareStatement(starInsertQuery);
            ResultSet rs = starStmt.executeQuery(starInsertQuery);
            starInfo = new HashMap<>();

            while (rs.next()) {
                String tempStarName = rs.getString("name");
                String tempStarId = rs.getString("id");
                starInfo.put(tempStarName, tempStarId); // stageName : starId
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseFile(File file) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
//            saxParser.parse(new InputSource(new InputStreamReader(
//                    new FileInputStream(new File("mains243.xml")), "ISO-8859-1")), this);
            saxParser.parse(new InputSource(new InputStreamReader(
                    new FileInputStream(file), "ISO-8859-1")), this); // Use your existing SAX handler
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runParser(File file) {
//        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
//        executor.submit(() -> parseFile(new File("actors63.xml")));
//        executor.shutdown();
//        try {
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS); // Wait for all threads to finish
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        parseFile(file);
        // printData();
        insertDataIntoDB();
    }

//    private void printData() {
//
//        System.out.println("Total parsed " + starList.size() + " stars");
//
//        for (Star star : starList) {
//            System.out.println("\t" + star.toString());
//        }
//    }
//
//    private void parseDocument() {
//        try {
//            SAXParserFactory factory = SAXParserFactory.newInstance();
//            SAXParser saxParser = factory.newSAXParser();
//            // saxParser.parse(new File("actors63.xml"), this);
//            saxParser.parse(new InputSource(new InputStreamReader(
//                    new FileInputStream(new File("xmlFiles/actors63.xml")), "ISO-8859-1")), this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // Called at the start of each XML element
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = "";

        if (qName.equalsIgnoreCase("actor")) {
            tempStar = new Star();
        }
    }

    // Called when characters are found inside an XML tag
    public void characters(char[] ch, int start, int length) {
        tempVal = new String(ch, start, length).trim();
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < tempVal.length(); i++) {
            char c = tempVal.charAt(i);
            if (c < 256)
                ret.append(c);
            else {
                try {
                    FileWriter file = new FileWriter("inconsistencies.txt", true);
                    file.write("Ignoring character: " + c + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        tempVal = ret.toString();
    }

    // Called at the end of each XML element
    public void endElement(String uri, String localName, String qName) {
        try {
            if (qName.equalsIgnoreCase("actor")) {
                if (tempStar.getStageName() == null || tempStar.getBirthYear() == null || tempStar.getStageName().isEmpty() || tempStar.getBirthYear() == 0 ) {
//                    System.out.println("No star found");
                    FileWriter file = new FileWriter("inconsistencies.txt", true);
                    file.write("Element Name: Actor, Node Value: " + tempVal + "\n"); // "Missing actor data from xml file with data: " + tempStar.getStageName() + tempStar.getBirthYear() + "\n");
                    file.close();
                    ++inconsistentStarsCount;
                }
                else {
                    if (!(starInfo.containsKey(tempStar.getStageName()) && starInfo.get(tempStar.getStageName()).equals(tempStar.getId()))) {
                        String id = "nm" + String.format("%07d",maxStarId);
                        ++maxStarId;
                        tempStar.setId(id);
                        starList.add(tempStar);
                        starInfo.put(tempStar.getStageName(), id);
                    }
                    else { // star already exists
                        FileWriter file = new FileWriter("inconsistencies.txt", true);
                        file.write("Skipping duplicate star with Element Name: m, Node Value: " + tempVal + "\n"); // : " + tempStar.getStageName() + " " + tempStar.getId() + ".\n");
                        file.close();
                        ++duplicateStarCount;
                        ++inconsistentStarsCount;
                    }
                }
            }
            else if (qName.equalsIgnoreCase("stagename")) {
                if (tempVal.matches("[ \\p{L}.-]+")) {
                    tempStar.setStageName(tempVal);
                }
                else {
                    FileWriter file = new FileWriter("inconsistencies.txt", true);
                    file.write("\"Element Name: stagename, Node Value: " + tempVal + "\n");
                    file.close();
                    tempStar.setStageName("");
                    ++inconsistentStarsCount;
                }
            }
            else if (qName.equalsIgnoreCase("dob")) {
                try {
                    tempStar.setBirthYear(Integer.parseInt(tempVal));
                } catch (NumberFormatException e) {
                    tempStar.setBirthYear(0);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing element: " + qName + " -> " + e.getMessage());
        }
    }

    private void insertDataIntoDB() {
        try {
            int count = 0;
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            String starInsertQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";

            PreparedStatement starStmt = conn.prepareStatement(starInsertQuery);

            for (Star star : starList) {
                starStmt.setString(1, star.getId());
                starStmt.setString(2, star.getStageName());
                starStmt.setInt(3, star.getBirthYear());
                starStmt.addBatch();
                ++insertedStarsCount;
                // System.out.println(star);
                count++;

                if (count >= 1000) {
                    // System.out.println("executing with count " + count);
                    starStmt.executeBatch();
                    conn.commit();
                    count = 0;
                }
            }
            starStmt.executeBatch();
            conn.commit();
            System.out.println("Inserted " + insertedStarsCount + " stars.");
            System.out.println(inconsistentStarsCount + " stars inconsistent.");
            System.out.println(duplicateStarCount + " stars duplicate.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

//    private boolean validName(String first, String last) {
//        return !tempStar.getFirstName().isEmpty() && !tempStar.getLastName().isEmpty() && Character.isUpperCase((tempStar.getFirstName().charAt(0))) && Character.isUpperCase((tempStar.getLastName().charAt(0)));
//    }

    private Integer generateStarId() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            String starInsertQuery = "SELECT LPAD(CAST(SUBSTRING((MAX(stars.id)), 3) AS UNSIGNED) + 1, 7, '0') FROM stars";

            PreparedStatement starStmt = conn.prepareStatement(starInsertQuery);
            ResultSet rs = starStmt.executeQuery(starInsertQuery);

            if (rs.next()){
                return rs.getInt(1);
            }
            return null; // title.replaceAll("\\s+", "_") + "_" + year;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeDictionaryToFile(HashMap<String, String> dictionary) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("starInfo.txt"))) {
            for (Map.Entry<String, String> entry : dictionary.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, String> getStarInfo() { return starInfo; }


//    public static void main(String[] args) {
//        SAXParserActor parser = new SAXParserActor();
//        parser.runParser();
//        writeDictionaryToFile(parser.getStarInfo());
//    }
}

