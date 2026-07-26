package servlets;

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
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

@WebServlet(name = "TomcatPoolingServlet", urlPatterns = "/api/pooling")
public class TomcatPoolingServlet extends HttpServlet {
    // Create a dataSource which registered in web.xml
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public String getServletInfo() {
        return "Servlet connects to MySQL database and displays result of a SELECT using connection pooling";
    }

    // Use HTTP GET
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("text/html"); // Response mime type

        // Output stream to STDOUT

        // the following line is to get a connection from a data source configured as a connection pool
        try (PrintWriter out = response.getWriter();
             Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement("SELECT id, name, birthYear FROM stars LIMIT ?")) {


            // the following commented lines are direct connections without pooling, which is the old way
            // Class.forName("org.gjt.mm.mysql.Driver");
            // Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            // try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
            // Set the parameter for the prepared statement

            // Removed the redundant query string String query = "SELECT * from stars limit 10";
            // Used statement.setInt(1, 10) to set the parameter value
            statement.setInt(1, 10);


            out.println("<HTML><HEAD><TITLE>MovieDB</TITLE></HEAD>");
            out.println("<BODY><H1>MovieDB (with some changes)</H1>");

            if (conn == null) {
                out.println("conn is null.");
                return;
            }
            else{
                // Execute the prepared statement directly - don't create a new query string
                ResultSet rs = statement.executeQuery();

                out.println("<TABLE border>");

                while (rs.next()) {
                    String m_id = rs.getString("id");
                    String m_LN = rs.getString("name");
                    String m_dob = rs.getString("birthYear");
                    out.println("<tr>" + "<td>" + m_id + "</td>" + "<td>" + m_LN + "</td>" + "<td>" + m_dob + "</td>"
                            + "</tr>");

            }
                out.println("</TABLE>");

                rs.close();
            }
        }
        catch (Exception e) {
            response.setStatus(500);
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Error</title></head>");
            out.println("<body>");
            out.println("<h1>Error</h1>");
            out.println("<p>A database error occurred: " + e.getMessage() + "</p>");
            out.println("</body></html>");
            e.printStackTrace();
        }
    }
}


