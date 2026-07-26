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
import java.util.Objects;

@WebServlet(name = "AddDataServlet", urlPatterns = "/api/add-data")
public class AddDataServlet extends HttpServlet {
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
        String type = request.getParameter("type");

        try (Connection conn = dataSource.getConnection()) {
            System.out.println(type);
            String message = "";
            JsonObject jsonResponse = new JsonObject();
            if (Objects.equals(type, "star")) {
                String getMaxIdQuery = "SELECT id FROM stars ORDER BY id DESC LIMIT 1";
                PreparedStatement getMaxIdStmt = conn.prepareStatement(getMaxIdQuery);
                ResultSet rs = getMaxIdStmt.executeQuery();

                String newId;
                if (rs.next()) {
                    // Step 2: Extract numeric part, increment it
                    String lastId = rs.getString("id"); // e.g., "nm9423080"
                    int numPart = Integer.parseInt(lastId.substring(2)); // Extracts "9423080" and converts to int
                    newId = "nm" + (numPart + 1); // Increment and reformat
                } else {
                    // No existing records, start from a base value
                    newId = "nm1000000"; // Change to your preferred starting ID
                }

                String name = request.getParameter("name");
                if (name.equals("")) {
                    throw new Exception("Name is null");
                }
                String birthYear = request.getParameter("birthYear");
                String query = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, newId);
                statement.setString(2, name);
                if (!birthYear.equals("")) {
                    statement.setInt(3, Integer.parseInt(birthYear));
                } else {
                    statement.setNull(3, java.sql.Types.INTEGER);
                }
                message += "starId: " + newId + " name: " + name + " birthYear: " + birthYear;
                statement.executeUpdate();
                statement.close();
            }
            else if (Objects.equals(type, "movie")) {
                String title = request.getParameter("title");
                String year = request.getParameter("year");
                String director = request.getParameter("director");
                String star = request.getParameter("star");
                String birthYear = request.getParameter("birthYear");
                String genre = request.getParameter("genre");
                System.out.println("movie input: " + title + " " + year + " " + director + " " + star + " " + birthYear + " " + genre);


                // Load MySQL JDBC Driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Prepare the stored procedure call
                String query = "CALL add_movie(?, ?, ?, ?, ?, ?)";

                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, title);
                statement.setInt(2, Integer.parseInt(year));
                statement.setString(3, director);
                System.out.println("star name: " + star);
                if (!star.equals("")) {
                    statement.setString(4, star);
                    statement.setInt(5, Integer.parseInt(birthYear));
                } else {
                    statement.setNull(4, java.sql.Types.VARCHAR);
                    statement.setNull(5, java.sql.Types.INTEGER);
                }
                if (!genre.equals("")) {
                    statement.setString(6, genre);
                } else {
                    statement.setNull(6, java.sql.Types.VARCHAR);
                }
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    message += rs.getString("message");
                    System.out.println("Message: " + rs.getString("message"));
                }
                System.out.println("printed");
                rs.close();
                conn.close();
            }
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", message);
            out.write(jsonResponse.toString());
            response.setStatus(200);
            System.out.println("success");
        } catch (Exception e) {
            System.out.println("fail: " + e.getMessage());
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500); // Internal Server Error
        } finally {
            System.out.println("finally");
            out.close();
        }
    }
}
