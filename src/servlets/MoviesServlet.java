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
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

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
        HttpSession session = request.getSession();
        response.setContentType("application/json"); // Response mime type

//        // The log message can be found in localhost log
        String sort = request.getParameter("sort");
        String limitStr = request.getParameter("limit");
        String pageStr = request.getParameter("page");
        String htmlURL = request.getHeader("Referer");
        session.setAttribute("previousMoviesPage", htmlURL);
        String previousMoviesPage = (String) session.getAttribute("previousMoviesPage");

        // get session variables if they exist in the session
        if (limitStr == null) {
            String sessionLimit = (String) session.getAttribute("limit");
            if (sessionLimit != null) {
                limitStr = sessionLimit;
            }
            else {
                limitStr = "10";}
        }
        // else is input limitStr
        session.setAttribute("limit", limitStr);
        String limit = " LIMIT " + limitStr;
        String sortStr = sort;

        System.out.println("sort: " + sort);
        if (sort == null || sort.equals("TARA")) {
            String sessionSort =  (String) session.getAttribute("sort");
            if (sessionSort != null) {
                sort = sessionSort;
                sortStr =  (String) session.getAttribute("sortStr");
            }
            else {
                sort = "m.title ASC, r.rating ASC";
                sortStr = "TARA";
            }
        }
        else if (sort.equals("TARD")) {
            sort = "m.title ASC, r.rating DESC";
        }
        else if (sort.equals("TDRD")) {
            sort = "m.title DESC, r.rating DESC";
        }
        else if (sort.equals("TDRA")) {
            sort = "m.title DESC, r.rating ASC";
        }
        else if (sort.equals("RATA")) {
            sort = "r.rating ASC, m.title ASC";
        }
        else if (sort.equals("RATD")) {
            sort = "r.rating ASC, m.title DESC";
        }
        else if (sort.equals("RDTD")) {
            sort = "r.rating DESC, m.title DESC";
        }
        else if (sort.equals("RDTA")) {
            sort = "r.rating DESC, m.title ASC";
        }
        System.out.println("sort: " + sort);
        session.setAttribute("sort", sort);
        session.setAttribute("sortStr", sortStr);

        String offsetStr = "";
        String sessionPage = (String) session.getAttribute("page");
        if ( pageStr != null ) {
            int page = Integer.parseInt(pageStr);
            int offset = ( page - 1 ) * Integer.parseInt(limitStr);
            offsetStr = " OFFSET " +  String.valueOf(offset);
        }
        else if (sessionPage != null) {
            pageStr = sessionPage;
        }
        session.setAttribute("page", pageStr);


        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        // Get a connection from dataSource
        try (Connection conn = dataSource.getConnection()) {
            // Declare our statement
            // Statement statement = conn.createStatement();


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
                    "ORDER BY " + sort + "\n" +
                    limit + offsetStr;


            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();
            ArrayList<JsonArray> data = new ArrayList<>();

            // Iterate through each row of rs
            while (rs.next()) {

                String movieId = rs.getString("id");
                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieDirector = rs.getString("director");
                String stars = rs.getString("stars");
//                String starId = rs.getString("starId");
                String genres = rs.getString("genres");
                 String genreId = rs.getString("genreId");
                String rating = rs.getString("rating");
                String moviePrice = rs.getString("price");



                // Create a JsonObject based on the data we retrieve from rs

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
                jsonObject.addProperty("star_name", stars);
//                jsonObject.addProperty("star_id", starId);
                jsonObject.addProperty("genre_name", genres);
                jsonObject.addProperty("genre_id", genreId);
                jsonObject.addProperty("rating", rating);
                jsonObject.addProperty("movie_price", moviePrice);



                jsonArray.add(jsonObject);
            }

            // Object to send filter data to js
            JsonArray filterData = new JsonArray();
            JsonObject filterObjData = new JsonObject();
            filterObjData.addProperty("sort", sortStr);
            filterObjData.addProperty("filter", limitStr);
            filterObjData.addProperty("page", pageStr);
            filterObjData.addProperty("listPage", previousMoviesPage);
            filterData.add(filterObjData);



            data.add(jsonArray);
            data.add(filterData);


            rs.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");


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
