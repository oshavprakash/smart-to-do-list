
package com.smarttodo;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {
        "/auth/register",
        "/auth/login",
        "/auth/logout",
        "/auth/session"
})
public class AuthServlet extends HttpServlet {

    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static final SecureRandom RANDOM = new SecureRandom();
@Override
protected void doGet(HttpServletRequest request,
                     HttpServletResponse response)
        throws IOException {

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    HttpSession session = request.getSession(false);

    if (session != null
            && session.getAttribute("userId") instanceof Number) {

        String username = String.valueOf(
                session.getAttribute("username"));

        String safeUsername = username
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        response.getWriter().write(
                "{\"authenticated\":true,"
                + "\"username\":\"" + safeUsername + "\"}");
    } else {
        response.getWriter().write(
                "{\"authenticated\":false}");
    }
}
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();

        try {
            switch (path) {
                case "/auth/register":
                    register(request, response);
                    break;

                case "/auth/login":
                    login(request, response);
                    break;

                case "/auth/logout":
                    logout(request, response);
                    break;

                default:
                    response.setStatus(
                            HttpServletResponse.SC_NOT_FOUND);
                    sendJson(response, false, "Endpoint not found.");
            }
        } catch (SQLException e) {
            getServletContext().log(
                    "Authentication database error", e);

            if (!response.isCommitted()) {
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                sendJson(response, false,
                        "A database error occurred.");
            }
        } catch (Exception e) {
            System.err.println("========== AUTHENTICATION DATABASE ERROR ==========");
                e.printStackTrace(System.err);

            if (!response.isCommitted()) {
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                sendJson(response, false,
                        "An unexpected error occurred.");
            }
        }
    }

    private void register(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {

        String username = trim(request.getParameter("username"));
        String email = trim(request.getParameter("email"))
                .toLowerCase(java.util.Locale.ROOT);
        String password = request.getParameter("password");

        if (username == null || username.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, false,
                    "All fields are required.");
            return;
        }

        if (username.length() > 100
                || email.length() > 255
                || !email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, false,
                    "Enter a valid username and email.");
            return;
        }

        if (password.length() < 8
                || password.length() > 128) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, false,
                    "Password must be 8 to 128 characters.");
            return;
        }

        String passwordHash = hashPassword(password);

        String sql = "INSERT INTO users "
                + "(username, email, password_hash) "
                + "VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, passwordHash);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long userId = keys.getLong(1);
                    startSession(request, userId, username);
                } else {
                    response.setStatus(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    sendJson(response, false,
                            "Account created, but login could not start.");
                    return;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                response.setStatus(
                        HttpServletResponse.SC_CONFLICT);
                sendJson(response, false,
                        "An account with this email already exists.");
                return;
            }
            throw e;
        }

        sendJson(response, true,
                "Account created successfully.");
    }

    private void login(HttpServletRequest request,
                       HttpServletResponse response)
            throws Exception {

        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");

        if (email == null || password == null
                || email.isBlank() || password.isEmpty()) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);
            sendJson(response, false,
                    "Email and password are required.");
            return;
        }

        String sql = "SELECT id, username, password_hash "
                + "FROM users WHERE email = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1,
                    email.toLowerCase(java.util.Locale.ROOT));

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()
                        || !verifyPassword(
                                password,
                                result.getString("password_hash"))) {
                    response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED);
                    sendJson(response, false,
                            "Invalid email or password.");
                    return;
                }

                startSession(
                        request,
                        result.getLong("id"),
                        result.getString("username"));
            }
        }

        sendJson(response, true, "Login successful.");
    }

    private void logout(HttpServletRequest request,
                    HttpServletResponse response)
        throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        sendJson(response, true, "Logged out successfully.");
    }

    private void startSession(HttpServletRequest request,
                              long userId,
                              String username) {

        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setMaxInactiveInterval(30 * 60);
    }

    private String hashPassword(String password)
            throws Exception {

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH);

        try {
            byte[] hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            Base64.Encoder encoder = Base64.getEncoder();

            return "pbkdf2$" + ITERATIONS + "$"
                    + encoder.encodeToString(salt) + "$"
                    + encoder.encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }

    private boolean verifyPassword(String password,
                                   String storedHash)
            throws Exception {

        if (storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split("\\$");

        if (parts.length != 4
                || !"pbkdf2".equals(parts[0])) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);

        if (iterations < 100000 || iterations > 1000000) {
            return false;
        }

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt = decoder.decode(parts[2]);
        byte[] expectedHash = decoder.decode(parts[3]);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                expectedHash.length * 8);

        try {
            byte[] actualHash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            return MessageDigest.isEqual(
                    expectedHash, actualHash);
        } finally {
            spec.clearPassword();
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void sendJson(HttpServletResponse response,
                          boolean success,
                          String message)
            throws IOException {

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        response.getWriter().write(
                "{\"success\":" + success
                        + ",\"message\":\"" + safeMessage + "\"}");
    }
}
