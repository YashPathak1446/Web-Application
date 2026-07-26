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

@WebServlet(name = "GenreServlet", urlPatterns = "/api/genres")
public class GenreServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // DataSource object for database connection
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Set response type to JSON
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            // Query to fetch all genres sorted alphabetically
            String query = "SELECT name, id FROM genres ORDER BY id ASC";
            PreparedStatement statement = conn.prepareStatement(query);

            ResultSet rs = statement.executeQuery();

            // Convert the result set into a JSON array
            JsonArray genresArray = new JsonArray();
            while (rs.next()) {
                JsonObject genreObject = new JsonObject();
                genreObject.addProperty("id", rs.getString("id"));
                genreObject.addProperty("name", rs.getString("name"));
                genresArray.add(genreObject);
            }

            System.out.println(genresArray.toString());

            rs.close();
            statement.close();

            // Write the JSON array to the response
            out.write(genresArray.toString());
            response.setStatus(200); // OK status

        } catch (Exception e) {
            // Handle exceptions
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500); // Internal Server Error
        } finally {
            out.close();
        }
    }
}