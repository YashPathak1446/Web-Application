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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.ArrayList;

@WebServlet(
        name = "BrowseServlet",
        urlPatterns = {"/api/browse"}
)
public class BrowseServlet extends HttpServlet {
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
        System.out.println("ENTERED BROWSE SERVLET SUCCESS");
        HttpSession session = request.getSession();
        response.setContentType("application/json");
        String genre = request.getParameter("genre");
        String prefix = request.getParameter("prefix");
        // Debug statement

        // Save session
        // String currentUrl = request.getRequestURL().toString();
        String htmlURL = request.getHeader("Referer");
        session.setAttribute("previousMoviesPage", htmlURL);
        System.out.println("genre 55: " + genre);
        if (genre == null) {
            genre = "";
        }

        if (prefix == null) {
            prefix = "";
        }

        // session code
        String previousMoviesPage = (String) session.getAttribute("previousMoviesPage");

        // pagination code
        String sort = request.getParameter("sort");
        String limitStr = request.getParameter("limit");
        String pageStr = request.getParameter("page");

        // get session variables if they exist in the session
        if (limitStr == null) {
            String sessionLimit = (String) session.getAttribute("browseLimit");
            if (sessionLimit != null) {
                limitStr = sessionLimit;
            }
            else {
                limitStr = "10";}
        }
        // else is input limitStr
        session.setAttribute("browseLimit", limitStr);
        String limit = " LIMIT " + limitStr;

        String sortStr = sort;
        System.out.println("sort: " + sort);
        if (sort == null || sort.equals("TARA")) {
            String sessionSort =  (String) session.getAttribute("browseSort");
            if (sessionSort != null) {
                sort = sessionSort;
                sortStr =  (String) session.getAttribute("browseSortStr");
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
        session.setAttribute("browseSort", sort);
        session.setAttribute("browseSortStr", sortStr);

        String offsetStr = "";
        String sessionPage = (String) session.getAttribute("browsePage");
        if ( pageStr != null ) {
            int page = Integer.parseInt(pageStr);
            int offset = ( page - 1 ) * Integer.parseInt(limitStr);
            offsetStr = " OFFSET " +  String.valueOf(offset);
        }
        else if (sessionPage != null) {
            pageStr = sessionPage;
        }
        session.setAttribute("browsePage", pageStr);



        PrintWriter out = response.getWriter();
        System.out.println("11: ");
        System.out.println(genre);
        try (Connection conn = dataSource.getConnection()) {
            // Parse into JSON
            System.out.println("12");
            if (!genre.isEmpty()) {
                System.out.println("genre");
                String genreQuery = "SELECT DISTINCT\n" +
                        "    m.id, \n" +
                        "    m.title, \n" +
                        "    m.year, \n" +
                        "    m.director,\n" +
                        "    m.price, \n" + // Added price column to be included from the movies table.
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
                        "JOIN genres_in_movies gim on gim.movieId = m.id \n" +
                        "JOIN genres g on g.id = gim.genreId \n" +
                        "WHERE g.id = ? \n" +
                        "ORDER BY " + sort + "\n" +
                        limit + offsetStr;

                PreparedStatement genreStatement = conn.prepareStatement(genreQuery);
                genreStatement.setString(1, genre);

                // Perform the query
                ResultSet rs = genreStatement.executeQuery();
                JsonArray jsonArray = new JsonArray();
                ArrayList<JsonArray> data = new ArrayList<>();
                System.out.println("13");
                while (rs.next()) {
                    // Create a JsonObject based on the data we retrieve from rs

                    JsonObject jsonObject = new JsonObject();
                    String movieId = rs.getString("id");
                    String movieTitle = rs.getString("title");
                    String movieYear = rs.getString("year");
                    String movieDirector = rs.getString("director");
                    String stars = rs.getString("stars");
                    //String starId = rs.getString("starId");
                    String genres = rs.getString("genres");
                    String genreId = rs.getString("genreId");
                    String rating = rs.getString("rating");
                    String moviePrice = rs.getString("price");


                    jsonObject.addProperty("movie_id", movieId);
                    jsonObject.addProperty("movie_title", movieTitle);
                    jsonObject.addProperty("movie_year", movieYear);
                    jsonObject.addProperty("movie_director", movieDirector);
                    jsonObject.addProperty("star_name", stars);
                    //jsonObject.addProperty("star_id", starId);
                    jsonObject.addProperty("genre_name", genres);
                    jsonObject.addProperty("genre_id", genreId);
                    jsonObject.addProperty("rating", rating);
                    jsonObject.addProperty("movie_price", moviePrice);
                    jsonArray.add(jsonObject);
                    System.out.println("14");
                }
                // Object to send filter data to js
                JsonArray filterData = new JsonArray();
                JsonObject filterObjData = new JsonObject();
                filterObjData.addProperty("sort", sortStr);
                filterObjData.addProperty("filter", limitStr);
                filterObjData.addProperty("page", pageStr);
                filterObjData.addProperty("listPage", previousMoviesPage);
                filterData.add(filterObjData);
                System.out.println("15");

                data.add(jsonArray);
                data.add(filterData);
                System.out.println("16");

                rs.close();
                genreStatement.close();

                // Debug statement
                System.out.println("Json array to string:" + jsonArray.toString());

                request.getServletContext().log("getting " + jsonArray.size() + " results");
                out.write(data.toString());
                System.out.println("rawr");
            } else if (!prefix.isEmpty()) {
                System.out.println("Fetching movies by prefix");
                String titleQuery = "SELECT DISTINCT\n" +
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
                        "WHERE 1=1 \n";
                if ("*".equals(prefix)) {
                    titleQuery = titleQuery + "AND m.title REGEXP '^[^a-z0-9]+' \n";
                } else {
                    titleQuery = titleQuery + " AND m.title LIKE ? \n";
                }

                titleQuery += "ORDER BY " + sort + "\n" +
                        limit + offsetStr;
                PreparedStatement titleStatement = conn.prepareStatement(titleQuery);
                if (!"*".equals(prefix)) {
                    titleStatement.setString(1, prefix + "%");
                }

                ResultSet rs = titleStatement.executeQuery();
                JsonArray jsonArray = new JsonArray();
                ArrayList<JsonArray> data = new ArrayList<>();

                while (rs.next()) {
                    JsonObject jsonObject = new JsonObject();
                    String movieId = rs.getString("id");
                    String movieTitle = rs.getString("title");
                    String movieYear = rs.getString("year");
                    String movieDirector = rs.getString("director");
                    String stars = rs.getString("stars");
                    //String starId = rs.getString("starId");
                    String genres = rs.getString("genres");
                    String genreId = rs.getString("genreId");
                    String rating = rs.getString("rating");
                    String moviePrice = rs.getString("price");


                    jsonObject.addProperty("movie_id", movieId);
                    jsonObject.addProperty("movie_title", movieTitle);
                    jsonObject.addProperty("movie_year", movieYear);
                    jsonObject.addProperty("movie_director", movieDirector);
                    jsonObject.addProperty("star_name", stars);
                    //jsonObject.addProperty("star_id", starId);
                    jsonObject.addProperty("genre_name", genres);
                    jsonObject.addProperty("genre_id", genreId);
                    jsonObject.addProperty("rating", rating);
                    jsonObject.addProperty("movie_price", moviePrice);

                    // add it to the jsonArray
                    jsonArray.add(jsonObject);
                }
                // Object to send filter data to js
                JsonArray filterData = new JsonArray();
                JsonObject filterObjData = new JsonObject();
                filterObjData.addProperty("sort", sortStr);
                filterObjData.addProperty("filter", limitStr);
                filterObjData.addProperty("page", pageStr);
                filterData.add(filterObjData);

                data.add(jsonArray);
                data.add(filterData);

                rs.close();
                titleStatement.close();
                System.out.println("Json array to string:" + jsonArray.toString());
                request.getServletContext().log("getting " + jsonArray.size() + " results");
                out.write(data.toString());
            }
            System.out.println("success");
            response.setStatus(200);
        }
        catch (Exception e) {
            // Write error message JSON object to output
            System.out.println("fail");
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
//            JsonArray jsonArray = new JsonArray();
//            ArrayList<JsonArray> data = new ArrayList<>();
//            jsonArray.add(jsonObject);
//            data.add(jsonArray);
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            System.out.println("finally");
            out.close();
        }
        // Always remember to close db connection after usage. Here it's done by try-with-resources
    }
}
