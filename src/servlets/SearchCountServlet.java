package servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

// Declaring a WebServlet called MovieServlet, which maps to url "/api/search"
@WebServlet(name = "SearchCountServlet", urlPatterns = "/api/searchCount")
public class SearchCountServlet extends HttpServlet {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("ENTERED SEARCH COUNT SERVLET SUCCESS");
        response.setContentType("application/json"); // Response mime type
        String search = request.getParameter("search");

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {

            String query = "SELECT DISTINCT\n" +
                    "    m.id, \n" +
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
                    "JOIN ratings r ON m.id = r.movieId \n" +
                    "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId \n" + // Join stars_in_movies table for substring search
                    "LEFT JOIN stars s ON sim.starId = s.id \n" +             // Join stars table for substring search
                    "WHERE 1=1 \n";

            if (search != null && !search.isEmpty()) {
                query += ("AND (MATCH (m.title, m.yearChar, m.director) AGAINST (? IN BOOLEAN MODE) COLLATE utf8mb4_0900_ai_ci AND MATCH (s.name) AGAINST (? IN BOOLEAN MODE) COLLATE utf8mb4_0900_ai_ci) \n");
            }

            // Declare statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Set dynamic parameters (replace ? with actual values)
            // parse search, add '+' in front of each and '*' behind
            String[] tokens = search.split("\\s+");
            StringBuilder booleanQuery = new StringBuilder();

            for (String token : tokens) {
                booleanQuery.append("+").append(token).append("* ").append(" ");
            }

            String searchQuery = booleanQuery.toString().trim();

            if (search != null && !search.isEmpty()) {
                statement.setString(1, searchQuery);
                statement.setString(2, searchQuery);
            }

            // Perform the query
            ResultSet rs = statement.executeQuery();

            // Parse into JSON
            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();

                String movieId = rs.getString("id");
                jsonObject.addProperty("movie_id", movieId);

                // add it to the jsonArray
                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();

            System.out.println("Json array to string:" + jsonArray.toString());
            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        }
        catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
        // Always remember to close db connection after usage. Here it's done by try-with-resources
    }
}

