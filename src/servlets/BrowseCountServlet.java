//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@WebServlet(
        name = "BrowseCountServlet",
        urlPatterns = {"/api/browseCount"}
)
public class BrowseCountServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String genre = request.getParameter("genre");
        String prefix = request.getParameter("prefix");
        // Debug statement

        System.out.println("prefix: " + prefix);

        if (genre == null) {
            genre = "";
        }

        if (prefix == null) {
            prefix = "";
        }

        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            // Parse into JSON
            JsonArray jsonArray = new JsonArray();
            System.out.println("genre: " + genre);

            if (!genre.isEmpty()) {
                System.out.println("Fetching movies by genre");

                String genreQuery = "SELECT DISTINCT\n" +
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
                        "JOIN genres_in_movies gim on gim.movieId = m.id \n" +
                        "JOIN genres g on g.id = gim.genreId \n" +
                        "WHERE g.id = ? \n";

                PreparedStatement genreStatement = conn.prepareStatement(genreQuery);
                genreStatement.setString(1, genre);

                // Perform the query
                ResultSet rs = genreStatement.executeQuery();

                while (rs.next()) {
                    // Create a JsonObject based on the data we retrieve from rs

                    JsonObject jsonObject = new JsonObject();
                    String movieId = rs.getString("id");

                    jsonObject.addProperty("movie_id", movieId);


                    jsonArray.add(jsonObject);
                }

                rs.close();
                genreStatement.close();

                // Debug statement
                System.out.println("count -1 Json array to string:" + jsonArray.toString());

                request.getServletContext().log("getting " + jsonArray.size() + " results");

            } else if (!prefix.isEmpty()) {
                System.out.println("Fetching movies by prefix");
                String titleQuery = "SELECT DISTINCT\n" +
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
                        "WHERE 1=1 \n";

                if ("*".equals(prefix)) {
                    titleQuery = titleQuery + "AND m.title REGEXP '^[^a-z0-9]+' \n";
                } else {
                    titleQuery = titleQuery + " AND m.title LIKE ? \n";
                }

                PreparedStatement titleStatement = conn.prepareStatement(titleQuery);
                if (!"*".equals(prefix)) {
                    titleStatement.setString(1, prefix + "%");
                }

                ResultSet rs = titleStatement.executeQuery();

                while (rs.next()) {
                    JsonObject jsonObject = new JsonObject();
                    String movieId = rs.getString("id");
                    jsonObject.addProperty("movie_id", movieId);

                    // add it to the jsonArray
                    jsonArray.add(jsonObject);
                }

                rs.close();
                titleStatement.close();
                System.out.println("count 0 Json array to string:" + jsonArray.toString());
                request.getServletContext().log("getting " + jsonArray.size() + " results");
            }
            System.out.println("count 1 Json array to string:" + jsonArray.toString());
            out.write(jsonArray.toString());
            System.out.println("count 2 Json array to string:" + jsonArray.toString());
            response.setStatus(200);
            System.out.println("count 3 Json array to string:" + jsonArray.toString());
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
