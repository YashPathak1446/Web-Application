package servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;


// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-Movie"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
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
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");
//        String limit = request.getParameter("limit");
//
//        if ( limit == null || limit.equals("") ) {
//            limit = "";
//        }
//        else {
//            limit = " LIMIT " + limit;
//        }

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);
        String previousMoviesPage = (String) session.getAttribute("previousMoviesPage");

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            JsonArray jsonArray = new JsonArray();
            ArrayList<JsonArray> data = new ArrayList<>();
            // Get a connection from dataSource

            // Construct a query with parameter represented by "?"
            String query = "SELECT \n" +
                    "    m.id,\n" +
                    "    m.title,\n" +
                    "    m.year,\n" +
                    "    m.director,\n" +
                    "    m.price,\n" +
                    "    COALESCE(r.rating, 0) AS rating,\n" +
                    "    GROUP_CONCAT(\n" +
                    "        (SELECT GROUP_CONCAT(CONCAT(g.id, ':', g.name) ORDER BY g.name ASC)\n" +
                    "        FROM genres g\n" +
                    "        JOIN genres_in_movies gim ON g.id = gim.genreId\n" +
                    "        WHERE gim.movieId = m.id)\n" +
                    "    ) AS genres,\n" +
                    "    GROUP_CONCAT(\n" +
                    "        CONCAT(s.id, ':', s.name) \n" +
                    "        ORDER BY \n" +
                    "            (SELECT COUNT(*) FROM stars_in_movies sim2 WHERE sim2.starId = s.id) DESC,\n" +
                    "            s.name ASC\n" +
                    "    ) AS stars\n" +
                    "FROM movies m\n" +
                    "LEFT JOIN ratings r ON m.id = r.movieId\n" +
                    "LEFT JOIN stars_in_movies sim ON sim.movieId = m.id\n" +
                    "LEFT JOIN stars s ON s.id = sim.starId\n" +
                    "WHERE m.id = ?\n" +
                    "GROUP BY m.id;";

//                    "SELECT \n" +
//                            "    m.id, \n" +
//                            "    m.title,\n" +
//                            "    m.year,\n" +
//                            "    m.director,\n" +
//                            "    m.price, \n" +
//                            "    r.rating,\n" +
//                            "    (SELECT GROUP_CONCAT(g.name ORDER BY g.name ASC) \n" +
//                            "     FROM genres g\n" +
//                            "     JOIN genres_in_movies gim ON g.id = gim.genreId\n" +
//                            "     WHERE gim.movieId = m.id) AS genres,\n" +
//                            "     GROUP_CONCAT(CONCAT(starData.starId,':',s.name) ORDER BY starData.movieCount DESC, s.name ASC) as stars \n" +
//                            "FROM movies m, stars s, \n" +
//                            "   (SELECT sim.starId, COUNT(sim.movieId) AS movieCount\n" +
//                            "       FROM stars_in_movies sim " +
//                            "       WHERE sim.starId IN \n" +
//                            "           (SELECT starId \n" +
//                            "           FROM stars_in_movies \n" +
//                            "           WHERE movieId = ?)\n" +
//                            "       GROUP BY sim.starId" +
//                            "   ) as starData \n" +
//                            "LEFT JOIN ratings r ON m.id = r.movieId \n" +
//                            "WHERE s.id = starData.starId AND m.id = r.movieId AND m.id = ?\n" +
//                            "GROUP BY starData.starId " +
//                            "ORDER BY starData.movieCount DESC, s.name ASC";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            statement.setString(1, id);
//            statement.setString(2, id);

            // Perform the query
            ResultSet rs = statement.executeQuery();
            // Iterate through each row of rs
            while (rs.next()) {
                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieDirector = rs.getString("director");
//                String starId = rs.getString("star_id");
                String stars = rs.getString("stars");
                String genres = rs.getString("genres");
                String rating = rs.getString("rating");
                String moviePrice = rs.getString("price");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
//                jsonObject.addProperty("star_id", starId);
                jsonObject.addProperty("stars", stars);
                jsonObject.addProperty("genres", genres);
                jsonObject.addProperty("rating", rating);
                jsonObject.addProperty("movie_price", moviePrice);

                jsonArray.add(jsonObject);
            }
            // Object to send filter data to js
            JsonArray filterData = new JsonArray();
            JsonObject filterObjData = new JsonObject();
            filterObjData.addProperty("listPage", previousMoviesPage);
            filterData.add(filterObjData);



            data.add(jsonArray);
            data.add(filterData);

            rs.close();
            statement.close();

            // Write JSON string to output
            out.write(data.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
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
