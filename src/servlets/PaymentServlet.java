package servlets;

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
import java.sql.Statement;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Claims;

@WebServlet(
        name = "PaymentServlet",
        urlPatterns = {"/api/payment"}
)
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("ENTERED PAYMENT SERVLET SUCCESS");
        response.setContentType("application/json");

        String first = request.getParameter("first");
        String last = request.getParameter("last");
        String cardNumber = request.getParameter("number");
        String expiration = request.getParameter("expiration");

        // Parse shopping cart data sent from frontend SessionStorage
        String jsonData = request.getParameter("shoppingCart"); // request.getReader().lines()
        // .reduce("", (accumulator, actual) -> accumulator + actual);
        System.out.println("SHOPPING CART DATA 2: " + jsonData);
        System.out.println("making data");
        Gson gson = new Gson();
        System.out.println("making data2");
        Map<String, Map<String, Object>> movieData = gson.fromJson(jsonData, new TypeToken<Map<String, Map<String, Object>>>(){}.getType());
        System.out.println("made data");
        // Save session
        // String currentUrl = request.getRequestURL().toString();
        // movies.movieIds are varchar

        PrintWriter out = response.getWriter();
        System.out.println("got writer");
        Map<String, Integer> movieResults = new HashMap<>();
        System.out.println("try connect");
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("connecting");
            // JsonArray jsonArray = new JsonArray();

            String genreQuery = "SELECT * FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND expiration = ?";
            System.out.println("prep query");
            PreparedStatement genreStatement = conn.prepareStatement(genreQuery);
            genreStatement.setString(1, cardNumber);
            genreStatement.setString(2, first);
            genreStatement.setString(3, last);
            genreStatement.setString(4, expiration);
            // Perform the query
            System.out.println("executing query");
            ResultSet rs = genreStatement.executeQuery();

            // get user id from session
            Integer userId = null;
            try {
                // get user id from session
                System.out.println("About to get claims");
                Object claimsObj = request.getAttribute("claims");
                System.out.println("Claims object: " + (claimsObj == null ? "null" : claimsObj.getClass().getName()));

                if (claimsObj == null) {
                    throw new Exception("Claims not found in request attributes");
                }

                Claims claims = (Claims) claimsObj;
                System.out.println("Claims content: " + claims);

                // Object userIdObj = claims.get("userId");
                Object userIdObj = claims.get("sub");
                if (userIdObj == null) {
                    throw new Exception("User ID not found in JWT claims");
                }

                // Assign to the variable declared outside the try block
                if (userIdObj instanceof Integer) {
                    userId = (Integer) userIdObj;
                } else if (userIdObj instanceof String) {
                    userId = Integer.parseInt((String) userIdObj);
                } else {
                    throw new Exception("User ID is in unexpected format: " + userIdObj.getClass().getName());
                }

                System.out.println("UserID: " + userId);
            // Continue with your existing code...
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Exception message: " + e.getMessage());

            // Handle the exception...
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage() != null ? e.getMessage() : "Unknown error");
            out.write(jsonObject.toString());
            response.setStatus(500);
            System.out.println("fail");
        }

            // String id = (String) request.getSession().getAttribute("userId");
            // int userId = Integer.parseInt(id);
            if (rs.next()) {
                System.out.println("credit card info right");
                // Insert into sales database
                String saleQuery = "INSERT INTO sales (customerId, movieId, saleDate) " +
                        "VALUES (?, ?, NOW())";

                PreparedStatement saleStatement = conn.prepareStatement(saleQuery, Statement.RETURN_GENERATED_KEYS);
                System.out.println("prep sales statement");
                for (String movieId : movieData.keySet()) {
                    for ( int i = 0 ; i < ((Double) movieData.get(movieId).get("quantity")).intValue() ; i++ ) {
                        System.out.println("new sales statement");
                        saleStatement.setInt(1, userId);
                        saleStatement.setString(2, movieId);
                        // ResultSet rs2 = saleStatement.executeQuery();
                        saleStatement.executeUpdate();
                        ResultSet rs2 = saleStatement.getGeneratedKeys();
                        if (rs2.next()) {
                            int dbSaleId = rs2.getInt(1);
                            movieResults.put(movieId, dbSaleId);
                        }
                    }
                }
            }
            else {
                throw new Exception("Credit card information not found in database");
            }
            System.out.println("movieResults4" + movieResults);
            JsonObject salesData = new JsonObject();
            for (Map.Entry<String, Integer> entry : movieResults.entrySet()) {
                salesData.addProperty(entry.getKey(), entry.getValue());
            }
            genreStatement.close();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("success", "success");
            ArrayList<Object> result = new ArrayList<>();
            System.out.println("movieResults5" + movieResults);
            result.add(jsonObject);
            result.add(salesData);
            Gson gson2 = new Gson();
            out.write(gson2.toJson(result));
            System.out.println("result" + result);
            response.setStatus(200);
            System.out.println("PAYMENT SERVLET SUCCESS");
        }
        catch (Exception e) { // FAILURE IF CANNOT FIND A MATCHING CREDIT CARD IN DATABASE
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
            System.out.println("fail");
        } finally {
            out.close();
        }
        // Always remember to close db connection after usage. Here it's done by try-with-resources
    }
}
