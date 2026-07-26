package servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "LogoutServlet", urlPatterns = "/api/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Invalidate the session
        request.getSession().invalidate();


//        // Set response headers to prevent caching
//        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
//        response.setHeader("Pragma", "no-cache");
//        response.setDateHeader("Expires", 0);

        // Send a JSON response indicating success
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":\"success\"}"); // JSON response indicating successful logout
    }
}
