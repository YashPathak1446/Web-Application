package servlets;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import jakarta.servlet.http.HttpSession;

@WebFilter(filterName="EmployeeLoginFilter", urlPatterns = {
        "/_dashboard/*", "/frontend/dashboard.html"
})
public class EmployeeLoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();

    public void init(FilterConfig fConfig) {
        allowedURIs.add("/_dashboard/login.html");
        allowedURIs.add("/_dashboard/api/employee-login");
        allowedURIs.add("/fabflix/_dashboard");
        allowedURIs.add("/fabflix/_dashboard/api/employee-login");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String requestURI = httpRequest.getRequestURI();
        System.out.println("EmployeeLoginFilter - requested URI: " + requestURI);

        // Check if session exists
        if (session == null) {
            System.out.println("No session found");
            if (this.isUrlAllowedWithoutLogin(requestURI)) {
                System.out.println("URL allowed without login");
                chain.doFilter(request, response);
                return;
            } else {
                System.out.println("Redirecting to login page");
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard/login.html");
                return;
            }
        }

        System.out.println("Employee attribute: " + session.getAttribute("employee"));

        if (this.isUrlAllowedWithoutLogin(requestURI)) {
            System.out.println("URL allowed without login");
            chain.doFilter(request, response);
            return;
        }

        if (session.getAttribute("employee") != null) {
            System.out.println("Employee logged in, proceeding to requested resource");
            chain.doFilter(request, response);
        } else {
            System.out.println("Not authenticated, redirecting to login page");

            // For AJAX requests, send a JSON response instead of redirecting
            if ("XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With"))) {
                httpResponse.setContentType("application/json");
                httpResponse.setStatus(401); // Unauthorized
                httpResponse.getWriter().write("{\"status\":\"fail\",\"message\":\"Please log in first\"}");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard/login.html");
            }
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void destroy() {
        // ignored.
    }
}