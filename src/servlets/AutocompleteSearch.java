package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@WebServlet("/api/autocomplete")
public class AutocompleteSearch extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate the appropriate edit distance threshold based on query length
     * @param query The Search query
     * @return The appropriate threshold value
     */
    private int calculateThreshold(String query) {
        // For short queries, allow fewer mistakes
        // the reference value is 14, as the average length of each
        // title and name from movies and stars came out to be 14 combined.
        // SELECT AVG(CHAR_LENGTH(name)) AS avg_combined_length
        //FROM (
        //    SELECT title AS name FROM movies
        //    UNION ALL
        //    SELECT name FROM stars
        //) AS combined;
        // For longer queries, allow reasonably more mistakes
        // took 6 instead of 5 for a reasonable distance
        int length = query.length();
        if (length <= 5){
            return 1; // Very short queries, allow 1 mistake
        }
        else if (length <= 10) {
            return 2; // Short queries allowing 2 mistakes
        }
        else if (length <= 16) {
            return 3; // Medium queries - allow 3 mistakes for a reasonably average query
        }
        else {
            return 4; // Long queries - allow 4 mistakes, for relevancy
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JsonArray jsonArray = new JsonArray();
            String search = request.getParameter("query");
            System.out.println("search: " + search);

            // return the empty json array if query is null or empty
            if (search == null || search.trim().isEmpty()) {
                response.getWriter().write(jsonArray.toString());
                return;
            }

            PrintWriter out = response.getWriter();
            Set<String> processedMovieIds = new HashSet<>(); // To avoid duplicates

            try (Connection conn = dataSource.getConnection()) {
                // Part 1: full text search query
                String query = "SELECT DISTINCT\n" +
                        "    m.id, \n" +
                        "    m.title, \n" +
                        "    m.year, \n" +
                        "    m.director,\n" +
                        "    m.price, \n" +
                        "(SELECT GROUP_CONCAT(CONCAT(star_info.starId, ':', star_info.name) ORDER BY star_info.movieCount DESC, star_info.name ASC)\n" +
                        "   FROM (\n" +
                        "       SELECT sim.starId, s.name, starData.movieCount\n" +
                        "       FROM stars_in_movies sim\n" +
                        "       JOIN stars s ON sim.starId = s.id\n" +
                        "       JOIN (\n" +
                        "           SELECT sim.starId, COUNT(sim.movieId) AS movieCount\n" +
                        "           FROM stars_in_movies sim\n" +
                        "           GROUP BY sim.starId\n" +
                        "           ) AS starData ON sim.starId = starData.starId\n" +
                        "        WHERE sim.movieId = m.id\n" +
                        "       ORDER BY starData.movieCount DESC, s.name ASC\n" +
                        "       LIMIT 3\n" +
                        "    ) star_info) AS stars, \n" +
                        "    (SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.name SEPARATOR ', '), ', ', 3) \n" +
                        "     FROM genres_in_movies gim\n" +
                        "     JOIN genres g ON gim.genreId = g.id \n" +
                        "     WHERE gim.movieId = m.id) AS genres,\n" +
                        "(SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.id SEPARATOR ', '), ', ',  3) \n" +
                        " FROM genres_in_movies gim \n" +
                        " JOIN genres g ON gim.genreId = g.id \n" +
                        " WHERE gim.movieId = m.id) as genreId, \n" +
                        "    r.rating\n" +
                        "FROM movies m \n" +
                        "LEFT JOIN ratings r ON m.id = r.movieId \n" +
                        "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId \n" + // Join stars_in_movies table for substring search
                        "LEFT JOIN stars s ON sim.starId = s.id \n" +             // Join stars table for substring search
                        "WHERE 1=1 \n";

                if (search != null && !search.isEmpty()) {
                    query += ("AND MATCH (m.title) AGAINST (? IN BOOLEAN MODE) COLLATE utf8mb4_0900_ai_ci\n");
                }

                query += "LIMIT 10";

                PreparedStatement statement = conn.prepareStatement(query);

                // parse search, add '+' in front of each and '*' behind
                String[] tokens = search.split("\\s+");
                StringBuilder booleanQuery = new StringBuilder();

                for (String token : tokens) {
                    booleanQuery.append("+").append(token).append("* ").append(" ");
                }

                String searchQuery = booleanQuery.toString().trim();
                if (search != null && !search.isEmpty()) {
                    statement.setString(1, searchQuery);
                }
                ResultSet rs = statement.executeQuery();
                // process the resultset based off the fulltextsearch first
                processResultSet(rs, jsonArray, processedMovieIds);

                // part 2: Fuzzy Search with Like
                // Start by lowecasing the search query to make the search case-insensitive
                String lowercaseSearch = search.toLowerCase();
                // We will use lowerCaseSearch for LEDA as well
                String likeQuery = "SELECT DISTINCT\n" +
                        "    m.id, \n" +
                        "    m.title, \n" +
                        "    m.year, \n" +
                        "    m.director,\n" +
                        "    m.price, \n" +
                        "(SELECT GROUP_CONCAT(CONCAT(star_info.starId, ':', star_info.name) ORDER BY star_info.movieCount DESC, star_info.name ASC)\n" +
                        "   FROM (\n" +
                        "       SELECT sim.starId, s.name, starData.movieCount\n" +
                        "       FROM stars_in_movies sim\n" +
                        "       JOIN stars s ON sim.starId = s.id\n" +
                        "       JOIN (\n" +
                        "           SELECT sim.starId, COUNT(sim.movieId) AS movieCount\n" +
                        "           FROM stars_in_movies sim\n" +
                        "           GROUP BY sim.starId\n" +
                        "           ) AS starData ON sim.starId = starData.starId\n" +
                        "        WHERE sim.movieId = m.id\n" +
                        "       ORDER BY starData.movieCount DESC, s.name ASC\n" +
                        "       LIMIT 3\n" +
                        "    ) star_info) AS stars, \n" +
                        "    (SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.name SEPARATOR ', '), ', ', 3) \n" +
                        "     FROM genres_in_movies gim\n" +
                        "     JOIN genres g ON gim.genreId = g.id \n" +
                        "     WHERE gim.movieId = m.id) AS genres,\n" +
                        "(SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.id SEPARATOR ', '), ', ',  3) \n" +
                        " FROM genres_in_movies gim \n" +
                        " JOIN genres g ON gim.genreId = g.id \n" +
                        " WHERE gim.movieId = m.id) as genreId, \n" +
                        "    r.rating\n" +
                        "FROM movies m \n" +
                        "LEFT JOIN ratings r ON m.id = r.movieId \n" +
                        "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId \n" +
                        "LEFT JOIN stars s ON sim.starId = s.id \n" +
                        "WHERE LOWER(m.title) LIKE ? OR LOWER(s.name) LIKE ?\n" +
                        "LIMIT 10";

                PreparedStatement likeStatement = conn.prepareStatement(likeQuery);
                likeStatement.setString(1, "%" + lowercaseSearch + "%");
                likeStatement.setString(2, "%" + lowercaseSearch + "%");

                rs = likeStatement.executeQuery();
                processResultSet(rs, jsonArray, processedMovieIds);


                // Part 3: Fuzzy search using edth (Levenshtein Edit Distance Algorithm)
                // First calculate the threshold based on the length of the search
                int threshold = calculateThreshold(lowercaseSearch);
                String edthQuery = "SELECT DISTINCT\n" +
                        "    m.id, \n" +
                        "    m.title, \n" +
                        "    m.year, \n" +
                        "    m.director,\n" +
                        "    m.price, \n" +
                        "(SELECT GROUP_CONCAT(CONCAT(star_info.starId, ':', star_info.name) ORDER BY star_info.movieCount DESC, star_info.name ASC)\n" +
                        "   FROM (\n" +
                        "       SELECT sim.starId, s.name, starData.movieCount\n" +
                        "       FROM stars_in_movies sim\n" +
                        "       JOIN stars s ON sim.starId = s.id\n" +
                        "       JOIN (\n" +
                        "           SELECT sim.starId, COUNT(sim.movieId) AS movieCount\n" +
                        "           FROM stars_in_movies sim\n" +
                        "           GROUP BY sim.starId\n" +
                        "           ) AS starData ON sim.starId = starData.starId\n" +
                        "        WHERE sim.movieId = m.id\n" +
                        "       ORDER BY starData.movieCount DESC, s.name ASC\n" +
                        "       LIMIT 3\n" +
                        "    ) star_info) AS stars, \n" +
                        "    (SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.name SEPARATOR ', '), ', ', 3) \n" +
                        "     FROM genres_in_movies gim\n" +
                        "     JOIN genres g ON gim.genreId = g.id \n" +
                        "     WHERE gim.movieId = m.id) AS genres,\n" +
                        "(SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.id SEPARATOR ', '), ', ',  3) \n" +
                        " FROM genres_in_movies gim \n" +
                        " JOIN genres g ON gim.genreId = g.id \n" +
                        " WHERE gim.movieId = m.id) as genreId, \n" +
                        "    r.rating\n" +
                        "FROM movies m \n" +
                        "LEFT JOIN ratings r ON m.id = r.movieId \n" +
                        "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId \n" +
                        "LEFT JOIN stars s ON sim.starId = s.id \n" +
                        "WHERE edth(LOWER(m.title), ?, ?) = 1 OR edth(LOWER(s.name), ?, ?) = 1\n" +
                        "LIMIT 10";

                PreparedStatement edthStatement = conn.prepareStatement(edthQuery);
                // For edth(LOWER(m.title), lowercaseSearch, threshold) = 1
                edthStatement.setString(1, lowercaseSearch);
                edthStatement.setInt(2, threshold);

                // For edth(LOWER(s.name), lowercaseSearch, threshold) = 1
                edthStatement.setString(3, lowercaseSearch);
                edthStatement.setInt(4, threshold);

                rs = edthStatement.executeQuery();
                processResultSet(rs, jsonArray, processedMovieIds);
                // this example only does a substring match
                // TODO: in project 4, you should do full text search with MySQL to find the matches on movies and stars

//                while (rs.next()) {
//                    JsonObject jsonObject = new JsonObject();
//
//                    String movieId = rs.getString("id");
//                    String movieTitle = rs.getString("title");
//                    String movieYear = rs.getString("year");
//                    String movieDirector = rs.getString("director");
//                    String stars = rs.getString("stars");
//                    String genres = rs.getString("genres");
//                    String genreId = rs.getString("genreId");
//                    String rating = rs.getString("rating");
//                    String moviePrice = rs.getString("price");
//
//                    jsonObject.addProperty("movie_id", movieId);
//                    jsonObject.addProperty("movie_title", movieTitle);
//                    jsonObject.addProperty("movie_year", movieYear);
//                    jsonObject.addProperty("movie_director", movieDirector);
//                    jsonObject.addProperty("star_name", stars);
//                    jsonObject.addProperty("genre_name", genres);
//                    jsonObject.addProperty("genre_id", genreId);
//                    jsonObject.addProperty("rating", rating);
//                    jsonObject.addProperty("movie_price", moviePrice);
//                    JsonObject suggestion = new JsonObject();
//                    suggestion.addProperty("value", movieTitle);
//                    suggestion.add("data", jsonObject);
//
//                    jsonArray.add(suggestion);
//                }
                System.out.println("autocomplete returns: " + jsonArray.toString());
                response.getWriter().write(jsonArray.toString());
            } catch (Exception e) {
                System.out.println(e);
                response.sendError(500, e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to process ResultSet and add items to JsonArray (as we do it twice)
     */
    private void processResultSet(ResultSet rs, JsonArray jsonArray, Set<String> processedMovieIds) throws SQLException {
        while (rs.next()) {
            // get movieId first to check if it has already been processed
            String movieId = rs.getString("id");

            // Skip if we've already processed this movie ID
            if (processedMovieIds.contains(movieId)) {
                continue;
            }

            processedMovieIds.add(movieId);
            JsonObject jsonObject = new JsonObject();

            String movieTitle = rs.getString("title");
            String movieYear = rs.getString("year");
            String movieDirector = rs.getString("director");
            String stars = rs.getString("stars");
            String genres = rs.getString("genres");
            String genreId = rs.getString("genreId");
            String rating = rs.getString("rating");
            String moviePrice = rs.getString("price");

            jsonObject.addProperty("movie_id", movieId);
            jsonObject.addProperty("movie_title", movieTitle);
            jsonObject.addProperty("movie_year", movieYear);
            jsonObject.addProperty("movie_director", movieDirector);
            jsonObject.addProperty("star_name", stars);
            jsonObject.addProperty("genre_name", genres);
            jsonObject.addProperty("genre_id", genreId);
            jsonObject.addProperty("rating", rating);
            jsonObject.addProperty("movie_price", moviePrice);

            JsonObject suggestion = new JsonObject();
            suggestion.addProperty("value", movieTitle);
            suggestion.add("data", jsonObject);

            jsonArray.add(suggestion);
        }
    }
}
