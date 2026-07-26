package main.java;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SAXParserMain extends DefaultHandler {

    private Connection conn;
    private String tempVal;
    private Movie tempMovie;
    private String directorName;
    private List<Movie> movieList;
    private Set<String> genreSet;
    private HashMap<String, String> movieIdMap; // {fid : movieId}
    private HashMap<List<String>, List<String>> movieInfo; // {<title, year, director> : <fid, movieId>}
    private Integer maxMovieId;
    static HashMap<String, Integer> genreCount = new HashMap<>(); // {genre: count}
    static int moviesCount = 0;
    static int genresCount = 0;
    static int genresInMoviesCount = 0;
    static int duplicateMovieCount = 0;
    static int inconsistentMovieCount = 0;
    static int moviesNotFoundCount = 0;

    // Number of threads you want to use for parallel parsing
    private static final int NUM_THREADS = 3;

    public SAXParserMain() {
        movieList = new ArrayList<>();
        genreSet = new HashSet<>();
        movieIdMap = new HashMap<>();
        movieInfo = new HashMap<>();
        maxMovieId = generateMovieId();

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            String starInsertQuery = "SELECT * FROM movies";

            PreparedStatement starStmt = conn.prepareStatement(starInsertQuery);
            ResultSet rs = starStmt.executeQuery(starInsertQuery);

            while (rs.next()) {
                List<String> tempMovieKey = new ArrayList<>();
                String tempMovieId = rs.getString("id");
                String tempMovieTitle = rs.getString("title");
                String tempMovieYear = rs.getString("year");
                String tempMovieDirector = rs.getString("director");
                tempMovieKey.add(tempMovieId);
                tempMovieKey.add(tempMovieTitle);
                tempMovieKey.add(tempMovieYear);
                tempMovieKey.add(tempMovieDirector);
                List<String> tempMovieValue = new ArrayList<>();
                tempMovieValue.add("");
                tempMovieValue.add(tempMovieId);
                movieInfo.put(tempMovieKey, tempMovieValue); // {<title, year, director> : <fid, movieId>}
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
        parseFile(file);
        // printData();
        insertDataIntoDB();

    }

    private void printData() {

        System.out.println("Total parsed " + movieList.size() + " movies");

        for (Movie movie : movieList) {
            System.out.println("\t" + movie.toString());
        }
    }

    private void parseDocument() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(new InputStreamReader(
                    new FileInputStream(new File("mains243.xml")), "ISO-8859-1")), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Called at the start of each XML element
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempVal = "";

        if (qName.equalsIgnoreCase("film")) {
//            System.out.println("Film start tag");
            tempMovie = new Movie();
        }
    }

    // Called when characters are found inside an XML tag
    @Override
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
    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            if (qName.equalsIgnoreCase("film")) {
                if (tempMovie.getFID().isEmpty() || tempMovie.getFID() == null || tempMovie.getTitle().isEmpty() || tempMovie.getTitle() == null || tempMovie.getDirector().isEmpty() ||tempMovie.getDirector() == null || tempMovie.getYear() == 0 || tempMovie.getGenres().isEmpty() || tempMovie.getGenres() == null) {
                    // System.out.println("Skipping incomplete movie entry.");
                    FileWriter file = new FileWriter("inconsistencies.txt", true);
                    file.write("Element Name: film, Node Value: " + tempVal + "n");
                    inconsistentMovieCount++;
                }
                else {
                    // Add to movieList
                    movieList.add(tempMovie);
//                    movieIdMap.put(tempMovie.getFID(), movieId);
                }
            } else if (qName.equalsIgnoreCase("t")) {
                // tempMovie.setTitle(tempVal);
                if (tempVal.matches("[ \\p{L}.,:!'-0123456789?]+")) {
                    tempMovie.setTitle(tempVal);
                }
                else {
                    FileWriter file = new FileWriter("inconsistencies.txt", true);
                    file.write("\"Not valid movie title for Element Name: t, Node Value: " + tempVal + "\n");
                    file.close();
                    tempMovie.setTitle("");
                }
            }
            else if (qName.equalsIgnoreCase("fid")) {
                tempMovie.setFID(tempVal);
            }
            else if (qName.equalsIgnoreCase("year")) {
                try {
                    tempMovie.setYear(Integer.parseInt(tempVal));
                } catch (NumberFormatException e) {
                    tempMovie.setYear(0);
                }
            } else if (qName.equalsIgnoreCase("dirn")) {
                tempMovie.setDirector(tempVal);
            } else if (qName.equalsIgnoreCase("cat")) {
                // set the genre to lower case
                String tempValLowerCase = tempVal.toLowerCase();

                // Define a map with the genre prefixes and their corresponding genre names
                // Reference: http://infolab.stanford.edu/pub/movies/doc.html#CATS
                Map<String, String> categoryConversion = new HashMap<>();
                categoryConversion.put("^dram", "Drama");
                categoryConversion.put("^comd", "Comedy");
                categoryConversion.put("^susp", "Thriller");
                categoryConversion.put("^romt", "Romance");
                categoryConversion.put("^horr", "Horror");
                categoryConversion.put("^musc", "Musical");
                categoryConversion.put("^actn", "Action");
                categoryConversion.put("^advt", "Adventure");
                categoryConversion.put("^biop", "Biography");
                categoryConversion.put("^docu", "Documentary");
                categoryConversion.put("^west", "Western");
                categoryConversion.put("^scfi", "Sci-Fi");
                categoryConversion.put("^fant", "Fantasy");
                categoryConversion.put("^cnrb", "cops and robbers");
                categoryConversion.put("^cart", "Cartoon");
                categoryConversion.put("^epic", "Epic");
                categoryConversion.put("^myst", "Mystery");
                categoryConversion.put("^s\\.f", "Sci-Fi");
                categoryConversion.put("^noir", "Black");
                categoryConversion.put("^tv", "TV Series");

                // Track if a match is found
                boolean matched = false;


                // Loop through the map and check if tempVal matches any prefix
                for (Map.Entry<String, String> entry : categoryConversion.entrySet()) {
                    // Check if the category starts with a key, and has 0 or more characters after
                    if (tempValLowerCase.matches(entry.getKey() + ".*")) {
                        String genre = entry.getValue();
                        genreSet.add(genre);
                        tempMovie.addGenre(genre);
                        matched = true;

                        // Old code to visualize genres with their respective counts
//                        if (!genreCount.containsKey(genre)) {
//                            genreCount.put(genre, 1);
//                        }
//                        else{
//                            genreCount.put(genre, genreCount.get(genre) + 1);
//                        }
//                        System.out.println("Current genre counts: " + genreCount);
//
//                        return;  // Exit the loop itself once a match is found
                    }
                }

                // If not in the category, add it to the miscellaneous genre
                if (!matched) {
                    genreSet.add("Miscellaneous");
                    tempMovie.addGenre("Miscellaneous");

//                    // Old code to visualize genres with their respective counts
//                    if (!genreCount.containsKey("Miscellaneous")) {
//                        genreCount.put("Miscellaneous", 1);
//                    }
//                    else{
//                        genreCount.put("Miscellaneous", genreCount.get("Miscellaneous") + 1);
//                    }
//                    System.out.println("Current genre counts: " + genreCount);
                }

            }
        } catch (Exception e) {
            // System.err.println("Error processing element: " + qName + " -> " + e.getMessage());
        }
    }

    // function to load database as a hashmap
    public class MovieDatabase {
        public HashMap<List<String>, Movie> loadMoviesFromDB(Connection conn) throws SQLException {
            HashMap<List<String>, Movie> movieMap = new HashMap<>();
            String query = "SELECT id, title, year, director, price FROM movies";

            try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
                ResultSet rs = preparedStatement.executeQuery();

                while (rs.next()){
                    Movie movie = new Movie();
                    movie.setMovieId(rs.getString("id"));
                    movie.setTitle(rs.getString("title"));
                    movie.setYear(rs.getInt("year"));
                    movie.setDirector(rs.getString("director"));
                    movie.setPrice(rs.getDouble("price"));

                    // Use ["title", "year", "director"] as the key
                    List<String> key = Arrays.asList(movie.getTitle(), String.valueOf(movie.getYear()), movie.getDirector());

                     movieMap.put(key, movie);
                }
            }
            return movieMap;
        }
    }

    // Insert movie into db if not a duplicate
    public boolean insertMovieIfNotDuplicate(Connection conn, Movie movie, HashMap<List<String>, Movie> movieMap, PreparedStatement preparedStatement) throws SQLException, IOException {
        List<String> key = Arrays.asList(movie.getTitle(), String.valueOf(movie.getYear()), movie.getDirector());
        if (!movieMap.containsKey(key)) {

            String movieId = "tt" + String.format("%07d",maxMovieId);
            ++maxMovieId;
            movie.setMovieId(movieId);
            movieIdMap.put(movie.getFID(), movieId);

            String insertSQL = "INSERT INTO movies (id, title, year, director, price) VALUES (?, ?, ?, ?, ROUND(5 + (RAND() * 95), 2))";

            // try (PreparedStatement preparedStatement = conn.prepareStatement(insertSQL)) {
                preparedStatement.setString(1, movie.getMovieId());  // Use movie's actual ID
                preparedStatement.setString(2, movie.getTitle());
                preparedStatement.setInt(3, movie.getYear());
                preparedStatement.setString(4, movie.getDirector());
                // preparedStatement.setDouble(5, movie.getPrice());
                preparedStatement.executeUpdate();

                // Add the movie from xml to the movie map now that it has been inserted to the database
                movieMap.put(key, movie);
                // System.out.println("Added movie: " + movie.getMovieId());
                return false;
            //}
        }
        // return true if duplicate
        else {
            // System.out.println("Duplicate found: " + movie.getTitle() + " (" + movie.getYear() + ")");
            FileWriter file = new FileWriter("inconsistencies.txt", true);
            file.write("Element Name: film, Node Value: " + tempVal + "\n");
            return true;
        }
    }

    public class GenreDatabase {
        public HashMap<String, Integer> loadGenresFromDB(Connection conn) throws SQLException {
            HashMap<String, Integer> genreMap = new HashMap<>();
            String query = "SELECT id, name FROM genres";

            try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
                ResultSet rs = preparedStatement.executeQuery();

                while (rs.next()) {
                    genreMap.put(rs.getString("name"), rs.getInt("id"));
                }
            }
            return genreMap;
        }
    }

    public int insertGenreIfNotExists(Connection conn, String genreName, HashMap<String, Integer> genreMap) throws SQLException {
        if (genresCount == 0){
            genresCount = genreMap.size();
        }
        // Check in the in-memory map first
        if (genreMap.containsKey(genreName)) {
            return genreMap.get(genreName);  // Return existing genre ID
        }

        // If not found, insert a new genre
        String insertSQL = "INSERT INTO genres (name) VALUES (?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            // Increase genreCount by 1 for new genre
            genresCount += 1;
            insertStmt.setString(1, genreName);
            insertStmt.executeUpdate();

            ResultSet rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                int newGenreId = rs.getInt(1);
                genreMap.put(genreName, newGenreId); // Update the map with new genre
                return newGenreId;
            } else {
                throw new SQLException("Failed to insert genre, no ID obtained.");
            }
        }
    }

    public void insertGenreMovieRelation(Connection conn, int genreId, String movieId) throws SQLException {

        // Insert new genre-movie relation
        String insertRelationSQL = "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertRelationSQL)) {
            insertStmt.setInt(1, genreId);
            insertStmt.setString(2, movieId);
            insertStmt.executeUpdate();
            genresInMoviesCount++;
        }
    }


    private void insertDataIntoDB() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            MovieDatabase movieDB = new MovieDatabase();
            GenreDatabase genreDB = new GenreDatabase();
            HashMap<List<String>, Movie> movieMap = movieDB.loadMoviesFromDB(conn);
            HashMap<String, Integer> genreMap = genreDB.loadGenresFromDB(conn);

            String insertSQL = "INSERT INTO movies (id, title, year, director, price) VALUES (?, ?, ?, ?, ROUND(5 + (RAND() * 95), 2))";
            String insertGenreSQL = "INSERT INTO genres (name) VALUES (?)";
            PreparedStatement movieStmt = conn.prepareStatement(insertSQL);
            PreparedStatement genreStmt = conn.prepareStatement(insertGenreSQL, Statement.RETURN_GENERATED_KEYS);

//            writeMovieToFile(movieMap);
//            writeGenreToFile(genreMap);

            for (Movie movie : movieList) {
                boolean duplicate = insertMovieIfNotDuplicate(conn, movie, movieMap, movieStmt); // Ensures movie is inserted if not duplicate

                // Get movie ID after insertion to use for genres_in_movies insertion
                // String movieId = movie.getMovieId();
                if (!duplicate) {
                    moviesCount++;
                    for (String genre : movie.getGenres()) {
                        // genre is inserted if not a duplicate
                        int genreId = insertGenreIfNotExists(conn, genre, genreMap);
                        // Ensures genre-movie relation is inserted if not duplicate
                        insertGenreMovieRelation(conn, genreId, movie.getMovieId());
                    }
//                    writeMovieToFile(movieMap);
//                    writeGenreToFile(genreMap);
                }
                else{
                    duplicateMovieCount++;
                }
            }

            conn.commit();
            System.out.println("Movies Count: " + moviesCount + ".");
            System.out.println("Genres Count: " + genresCount + ".");
            System.out.println("genres_in_movies Count: " + genresInMoviesCount + ".");
            System.out.println("Movies Duplicate Count: " + duplicateMovieCount + ".");
            System.out.print("Movies Inconsistent Count: " + inconsistentMovieCount + ".");

            // System.out.println("Data inserted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private Integer generateMovieId() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "testuser", "testpassword");
            conn.setAutoCommit(false);

            String starInsertQuery = "SELECT LPAD(CAST(SUBSTRING((MAX(movies.id)), 3) AS UNSIGNED) + 1, 7, '0') FROM movies";

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

    public HashMap<String, String> getMovieInfo() { return movieIdMap; }

//    public static void writeDictionaryToFile(HashMap<String, String> dictionary) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("movieInfo.txt"))) {
//            for (Map.Entry<String, String> entry : dictionary.entrySet()) {
//                writer.write(entry.getKey() + "=" + entry.getValue());
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static void writeAllCategories(HashMap<String, Integer> dictionary) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("allGenres.txt"))) {
//            for (Map.Entry<String, Integer> entry : dictionary.entrySet()) {
//                writer.write(entry.getKey() + "=" + entry.getValue());
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static void writeMovieToFile(HashMap<List<String>, Movie> dictionary) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("movieDB.txt"))) {
//            for (Map.Entry<List<String>, Movie> entry : dictionary.entrySet()) {
//                writer.write(entry.getKey() + "=" + entry.getValue());
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void writeGenreToFile(HashMap<String, Integer> dictionary) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("genreDB.txt"))) {
//            for (Map.Entry<String, Integer> entry : dictionary.entrySet()) {
//                writer.write(entry.getKey() + "=" + entry.getValue());
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
//        SAXParserMain parserMain = new SAXParserMain();
//        SAXParserActor parserActor = new SAXParserActor();
//        SAXParserCast parserCast = new SAXParserCast(parserMain.getMovieInfo(), parserActor.getStarInfo());
//        parserMain.runParser();
//        parserActor.runParser();
//        parserCast.runParser();
//        writeDictionaryToFile(parserMain.getMovieInfo());
        try {

            FileWriter file = new FileWriter("output.txt", true);
            long startTime = System.nanoTime();
            SAXParserMain parserMain = new SAXParserMain();
            SAXParserActor parserActor = new SAXParserActor();
            SAXParserCast parserCast = new SAXParserCast(parserMain.getMovieInfo(), parserActor.getStarInfo());

            // Create a thread pool to run the parsers concurrently
            ExecutorService executor = Executors.newFixedThreadPool(3); // 3 threads for 3 parsers
//            saxParser.parse(new InputSource(new InputStreamReader(
//                    new FileInputStream(new File("mains243.xml")), "ISO-8859-1")), this);
            executor.submit(() -> parserMain.runParser(new File("mains243.xml")));
            executor.submit(() -> parserActor.runParser(new File("actors63.xml")));
            executor.submit(() -> parserCast.runParser(new File("casts124.xml")));
            executor.shutdown();

            // Time the parsing
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            double durationInMilliseconds = (double) duration / 1_000_000.0;

            file.write("Execution time: " + durationInMilliseconds + " milliseconds");
            file.flush();
            // Print out the logs


            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            // System.out.println("Error in Parser Main!");
            e.printStackTrace();
        }


//        // Writing genres and their counts to a text file for visualization purposes.
//        // sort the genreCount dictionary
//        // Sort the hashmap by values in descending order
//        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(genreCount.entrySet());
//
//        // Sort the list by value (descending order)
//        sortedList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
//
//        // Optionally, put the sorted entries back into a LinkedHashMap to preserve the order
//        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
//        for (Map.Entry<String, Integer> entry : sortedList) {
//            sortedMap.put(entry.getKey(), entry.getValue());
//        }
//
//        writeAllCategories(sortedMap);

    }
}

