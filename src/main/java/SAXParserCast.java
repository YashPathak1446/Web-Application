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

public class SAXParserCast extends DefaultHandler {

    private Connection conn;
    private String tempVal;
    private SIM tempSIM;
    private List<SIM> simList;
    private HashMap<String, String> movieIdMap; // fid: movieId -> import from main parser
    private HashMap<String, String> starInfo; // starname: starId -> import from actor parser
    private HashMap<String, String> simMap; // starId: movieId
    // Number of threads you want to use for parallel parsing
    private static final int NUM_THREADS = 4;
    private int insertedSIMCount = 0;
    private int inconsistentSIMCount = 0;
    private int duplicateSIMCount = 0;
    private int movieNoStar = 0;
    private int movieNotFoundCount = 0;
    private int starNotFoundCount = 0;

    public SAXParserCast(HashMap<String, String> movieInfo, HashMap<String, String> starInformation) {
        simList = new ArrayList<>();
        movieIdMap = movieInfo;
        starInfo = starInformation;
        simMap = new HashMap<>();
//        readStarDictionaryFromFile();
//        readMovieDictionaryFromFile();

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            String starInsertQuery = "SELECT * FROM stars_in_movies";

            PreparedStatement starStmt = conn.prepareStatement(starInsertQuery);
            ResultSet rs = starStmt.executeQuery(starInsertQuery);

            while (rs.next()) {
                String tempStarId = rs.getString("starId");
                String tempMovieId = rs.getString("movieId");
                simMap.put(tempStarId, tempMovieId); // stageName : starId
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

//    private void readStarDictionaryFromFile() {
//        try (BufferedReader reader = new BufferedReader(new FileReader("starInfo.txt"))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.split("=", 2); // Split at '='
//                if (parts.length == 2) {
//                    starInfo.put(parts[0].trim(), parts[1].trim());
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void readMovieDictionaryFromFile() {
//        try (BufferedReader reader = new BufferedReader(new FileReader("movieInfo.txt"))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.split("=", 2); // Split at '='
//                if (parts.length == 2) {
//                    movieIdMap.put(parts[0].trim(), parts[1].trim());
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
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
//        executor.submit(() -> parseFile(new File("casts124.xml")));
//        executor.shutdown();
//        try {
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS); // Wait for all threads to finish
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        parseFile(file);
//        printData();
        insertDataIntoDB();
    }

    private void printData() {

        System.out.println("Total parsed " + simList.size() + " sims");

        for (SIM sim : simList) {
            System.out.println("\t" + sim.toString());
        }
    }

//    private void parseDocument() {
//        try {
//            SAXParserFactory factory = SAXParserFactory.newInstance();
//            SAXParser saxParser = factory.newSAXParser();
////            saxParser.parse(new File("casts124.xml"), this);
//            saxParser.parse(new InputSource(new InputStreamReader(
//                    new FileInputStream(new File("casts124.xml")), "ISO-8859-1")), this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // Called at the start of each XML element
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = "";

        if (qName.equalsIgnoreCase("m")) {
            tempSIM = new SIM();
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
            if (qName.equalsIgnoreCase("m")) {
                if (tempSIM.getStageName() == null || tempSIM.getStageName().isEmpty())
                    ++movieNoStar;
                if (tempSIM.getFid() == null || tempSIM.getStageName() == null || tempSIM.getMovieId() == null || tempSIM.getStarId() == null  || tempSIM.getFid().isEmpty() || tempSIM.getStageName().isEmpty() || tempSIM.getMovieId().isEmpty() || tempSIM.getStarId().isEmpty()) {
                    FileWriter file = new FileWriter("inconsistencies.txt", true);
                    file.write("Element Name: m, Node Value: " + tempVal); //+ tempSIM.getStageName() + " " + tempSIM.getStarId() + " " + tempSIM.getFid() + " " + tempSIM.getMovieId() + "\n");
                    file.close();
                    ++inconsistentSIMCount;
                }
                else {
                    // if not in simMap dict already
                    if (!(simMap.containsKey(tempSIM.getStarId()) && simMap.get(tempSIM.getStarId()).equals(tempSIM.getMovieId()))) {
                        simList.add(tempSIM);
                        simMap.put(tempSIM.getStarId(), tempSIM.getMovieId());
                    }
                    else  { // SIM relationship already exists
                        FileWriter file = new FileWriter("inconsistencies.txt", true);
                        file.write("Skipping duplicate SIM with Element Name: m, Node Value: " + tempVal);
                        file.close();
                        ++inconsistentSIMCount;
                        ++duplicateSIMCount;
                    }

                }
            }
            else if (qName.equalsIgnoreCase("f")) {
                tempSIM.setFid(tempVal);
                if (movieIdMap.containsKey(tempVal))
                    tempSIM.setMovieId(movieIdMap.get(tempVal));
                else
                    ++movieNotFoundCount;
            }
            else if (qName.equalsIgnoreCase("a")) {
                tempSIM.setStageName(tempVal);
                if (starInfo.containsKey(tempVal))
                    tempSIM.setStarId(starInfo.get(tempVal));
                else
                    ++starNotFoundCount;
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

            String movieInsertQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";

            PreparedStatement simStmt = conn.prepareStatement(movieInsertQuery);

            for (SIM sim : simList) {
                simStmt.setString(1, sim.getStarId());
                simStmt.setString(2, sim.getMovieId());
                simStmt.addBatch();
                count++;
                ++insertedSIMCount;

                if (count >= 1000) {
                    simStmt.executeBatch();
                    conn.commit();
                    count = 0;
                }
            }
            simStmt.executeBatch();
            conn.commit();
            System.out.println("Inserted " + insertedSIMCount + " stars_in_movies.");
            System.out.println(inconsistentSIMCount + " stars_in_movies inconsistent.");
            System.out.println(duplicateSIMCount + " stars_in_movies duplicate.");
            if (movieNotFoundCount > 0) {
                System.out.println(movieNoStar + " movies no star.");
            }
            System.out.println(movieNotFoundCount + " movies not found.");
            System.out.println(starNotFoundCount + " stars not found.");
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

//    public static void main(String[] args) {
//        SAXParserCast parser = new SAXParserCast();
//        parser.runParser();
//    }
}

