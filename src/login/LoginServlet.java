package login;

import com.google.gson.JsonObject;
import common.JwtUtil;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //HttpSession session = request.getSession();
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        PrintWriter out = response.getWriter();

//        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
//        System.out.println("gRecaptchaResponse=" + gRecaptchaResponse);

        // Verify reCAPTCHA
        try {
//            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
//            System.out.println("VERIFIED RECAPTCHA RESPONSE=" + gRecaptchaResponse);

            try (Connection conn = dataSource.getConnection()) {
                // Fetch only the stored encrypted password for the given email
                String userQuery = "SELECT id, password FROM customers WHERE email = ?";

                PreparedStatement userStatement = conn.prepareStatement(userQuery);
                userStatement.setString(1, username);
                ResultSet rs = userStatement.executeQuery();

                JsonObject responseJsonObject = new JsonObject();
                if (rs.next()) {
                    String storedEncryptedPassword = rs.getString("password");
                    // Use VerifyPassword utility to check the entered password
//                    if (VerifyPassword.checkPassword(password, storedEncryptedPassword)) {
                    if (password.equals(storedEncryptedPassword)) {}
                        System.out.println("LOGIN SUCCESSFUL");
                        // set this user into the session
                        // session.setAttribute("user", new User(username));
                        String subject = rs.getString("id");
                        // session.setAttribute("userId", userId);
                        System.out.println("userId:" + subject);
//                        String token = JwtUtil.generateToken(subject, new HashMap<>());
                        HashMap<String, Object> claims = new HashMap<>();
                        claims.put("userId", Integer.parseInt(subject));
                        String token = JwtUtil.generateToken(subject, claims);
                        JwtUtil.updateJwtCookie(request, response, token);

                        // responseJsonObject.addProperty("userId", userId);
                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "success");

                    }
                    else {
                        // reference for self
                        System.out.println("LOGIN FAILED: Incorrect password");
//                        // Login fail
                        responseJsonObject.addProperty("status", "fail");
//                        // Log to localhost log
//                        request.getServletContext().log("Login failed");
//                        // Clear any existing session to reset login attempts
//                        // session.invalidate();
//
//                        // sample error messages. in practice, it is not a good idea to tell user which one is incorrect/not exist.
                        if (username.isEmpty()) {
                            responseJsonObject.addProperty("message", "username not specified.");
                        }
                        else if (password.isEmpty()) {
                            responseJsonObject.addProperty("message", "password not specified.");
                        } else {
                            responseJsonObject.addProperty("message", "incorrect username or password");
                        }
                    }
//                else {
//                    // reference for self
//                    System.out.println("LOGIN FAILED: Incorrect password");
//                    // Login fail
//                    responseJsonObject.addProperty("status", "fail");
//                    // Log to localhost log
//                    request.getServletContext().log("Login failed");
//                    // sample error messages. in practice, it is not a good idea to tell user which one is incorrect/not exist.
//                    if (username.isEmpty()) {
//                        responseJsonObject.addProperty("message", "username not specified.");
//                    }
//                    else if (password.isEmpty()) {
//                        responseJsonObject.addProperty("message", "password not specified.");
//                    } else {
//                        responseJsonObject.addProperty("message", "incorrect username or password");
//                    }
//                }
                response.getWriter().write(responseJsonObject.toString());
            }
            catch (Exception e) { // FAILURE IF CANNOT FIND A MATCHING CREDIT CARD IN DATABASE
                // Write error message JSON object to output
                System.out.println("catch");
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("errorMessage", e.getMessage());
                out.write(jsonObject.toString());
                e.printStackTrace();

                // Log error to localhost log
                request.getServletContext().log("Error:", e);
                // Set response status to 500 (Internal Server Error)
                response.setStatus(500);
            } finally {
                System.out.println("finally");
                out.close();
            }
        } catch (Exception e) {
            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("message", "CONFIRM YOU ARE NOT A ROBOT");
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error, please try again.");
            response.setStatus(500);
            out.write(responseJsonObject.toString());
            out.close();
            response.sendRedirect("login.html?error=server_error");
        }
    }
}
