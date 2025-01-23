package servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

// Declaring a WebServlet called MovieServlet, which maps to url "/api/movies"
@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
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

        response.setContentType("application/json"); // Response mime type

//        // Retrieve parameter id from url request.
//        String id = request.getParameter("id");

//        // The log message can be found in localhost log
//        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        // Get a connection from dataSource
        try (Connection conn = dataSource.getConnection()) {
            // Declare our statement
            Statement statement = conn.createStatement();

            // Construct a query to display required fields
            String query = "SELECT \n" +
                    "m.id, \n" +
                    "m.title, \n" +
                    "m.year, \n" +
                    "m.director, \n" +
                    "(SELECT SUBSTRING_INDEX(GROUP_CONCAT(s.name SEPARATOR ', '), ', ', 3) \n" +
                    "FROM stars_in_movies sim \n" +
                    "JOIN stars s ON sim.starId = s.id \n" +
                    "WHERE sim.movieId = m.id) AS starName, \n" +
                    "(SELECT SUBSTRING_INDEX(GROUP_CONCAT(s.id SEPARATOR ', '), ', ', 3) \n" +
                    "FROM stars_in_movies sim \n" +
                    "JOIN stars s ON sim.starId = s.id \n" +
                    "WHERE sim.movieId = m.id) AS starId, \n" +
                    "(SELECT SUBSTRING_INDEX(GROUP_CONCAT(g.name SEPARATOR ', '), ', ', 3) \n" +
                    "FROM genres_in_movies gim \n" +
                    "JOIN genres g on gim.genreId = g.id \n" +
                    "WHERE gim.movieId = m.id) AS genreName, \n" +
                    "r.rating \n" +
                    "FROM \n" +
                    "movies m \n" +
                    "JOIN \n" +
                    "ratings r ON m.id = r.movieId \n" +
                    "ORDER BY \n" +
                    "r.rating DESC \n" +
                    "LIMIT 20";


            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            // statement.setString(1, id);

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {

                String movieId = rs.getString("id");
                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieDirector = rs.getString("director");
                String starName = rs.getString("starName");
                String starId = rs.getString("starId");
                String genreName = rs.getString("genreName");
                String rating = rs.getString("rating");



                // Create a JsonObject based on the data we retrieve from rs

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
                jsonObject.addProperty("star_name", starName);
                jsonObject.addProperty("star_id", starId);
                jsonObject.addProperty("genre_name", genreName);
                jsonObject.addProperty("rating", rating);



                jsonArray.add(jsonObject);
            }
            rs.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");

            // Write JSON string to output
            out.write(jsonArray.toString());
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
