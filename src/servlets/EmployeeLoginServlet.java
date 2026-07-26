package servlets;

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

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/_dashboard/api/employee-login")
public class EmployeeLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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
        doPost(request, response);
    }


    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        System.out.println("Employee login: " + email + " " + password);

        // Always set response content type first
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        JsonObject responseJsonObject = new JsonObject();

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        System.out.println("gRecaptchaResponse=" + gRecaptchaResponse);

        // Verify reCAPTCHA
        try {
            if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Please complete the reCAPTCHA verification");
                out.write(responseJsonObject.toString());
                return;
            }

            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
            System.out.println("VERIFIED RECAPTCHA RESPONSE=" + gRecaptchaResponse);

            try (Connection conn = dataSource.getConnection()) {
                // Fetch only the stored encrypted password for the given email
                String userQuery = "SELECT password FROM employees WHERE email = ?";

                PreparedStatement userStatement = conn.prepareStatement(userQuery);
                userStatement.setString(1, email);
                // userStatement.setString(2, password);
                ResultSet rs = userStatement.executeQuery();
                System.out.println("1, " +userQuery);

                if (rs.next()) {
                    System.out.println("2");
                    // Login success:
                    String storedEncryptedPassword = rs.getString("password");
                    // Use VerifyPassword utility to check the entered password
                    System.out.println("storedEncryptedPassword=" + storedEncryptedPassword);
                    System.out.println("password=" + password);
                    if (VerifyPassword.checkPassword(password, storedEncryptedPassword)) {
                        System.out.println("EMPLOYEE LOGIN SUCCESSFUL");
                        session.setAttribute("employee", email);
                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "success");

                    }
                    else {
                        // reference for self
                        System.out.println("LOGIN FAILED: Incorrect password");
                        // Login fail
                        responseJsonObject.addProperty("status", "fail");
                        // Log to localhost log
                        request.getServletContext().log("Login failed");
                        // Clear any existing session to reset login attempts
                        session.invalidate();

                        // sample error messages. in practice, it is not a good idea to tell user which one is incorrect/not exist.
                        if (email.isEmpty()) {
                            responseJsonObject.addProperty("message", "username not specified.");
                        }
                        else if (password.isEmpty()) {
                            responseJsonObject.addProperty("message", "password not specified.");
                        } else {
                            responseJsonObject.addProperty("message", "incorrect username or password");
                        }
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
                out.write(responseJsonObject.toString());
//                response.getWriter().write(responseJsonObject.toString());
            }
            catch (Exception e) { // FAILURE IF CANNOT FIND A MATCHING CREDIT CARD IN DATABASE
                // Write error message JSON object to output
                System.out.println("Database error: " + e.getMessage());
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Server error, please try again.");
                out.write(responseJsonObject.toString());
                e.printStackTrace();
                request.getServletContext().log("Error:", e);
                response.setStatus(500);
            }
        }catch (Exception e) {
            // reCAPTCHA verification failed
            System.out.println("reCAPTCHA verification failed: " + e.getMessage());
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "CONFIRM YOU ARE NOT A ROBOT");
            out.write(responseJsonObject.toString());
            response.setStatus(400);
        } finally {
            out.close();
        }
    }
}