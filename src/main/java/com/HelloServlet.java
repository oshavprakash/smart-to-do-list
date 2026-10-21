
package com.smarttodo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    private Long getUserId(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please log in to continue.");
            return null;
        }

        Object value = session.getAttribute("userId");

        if (!(value instanceof Number)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please log in to continue.");
            return null;
        }

        return ((Number) value).longValue();
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(message);
    }

    private void sendSuccess(
            HttpServletResponse response,
            String message) throws IOException {

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(message);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long userId = getUserId(request, response);
        if (userId == null) {
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String sql = """
            SELECT id, title, description, priority, due_date, status
            FROM tasks
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (result.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;

                    Date dueDate = result.getDate("due_date");

                    json.append("{")
                        .append("\"id\":")
                        .append(result.getInt("id")).append(",")
                        .append("\"title\":")
                        .append(jsonString(result.getString("title")))
                        .append(",")
                        .append("\"description\":")
                        .append(jsonString(
                                result.getString("description")))
                        .append(",")
                        .append("\"priority\":")
                        .append(jsonString(
                                result.getString("priority")))
                        .append(",")
                        .append("\"dueDate\":")
                        .append(jsonString(
                                dueDate == null
                                        ? null : dueDate.toString()))
                        .append(",")
                        .append("\"status\":")
                        .append(jsonString(
                                result.getString("status")))
                        .append("}");
                }

                json.append("]");
                response.getWriter().print(json);
            }

        } catch (SQLException e) {
            getServletContext().log("Failed to load tasks.", e);
            sendError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not load tasks.");
        }
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder("\"");

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(
                                String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }

        return escaped.append('"').toString();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long userId = getUserId(request, response);
        if (userId == null) {
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");

        if (title == null || title.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Task title is required.");
            return;
        }

        String sql = """
            INSERT INTO tasks
                (user_id, title, description, priority, due_date)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setString(2, title.trim());
            statement.setString(3, description);
            statement.setString(4,
                    priority == null || priority.isBlank()
                            ? "Medium" : priority);

            if (dueDate == null || dueDate.isBlank()) {
                statement.setNull(5, java.sql.Types.DATE);
            } else {
                statement.setDate(5, Date.valueOf(dueDate));
            }

            statement.executeUpdate();

            response.setStatus(HttpServletResponse.SC_CREATED);
            sendSuccess(response, "Task saved successfully!");

        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid due date. Use YYYY-MM-DD.");
        } catch (SQLException e) {
            getServletContext().log("Failed to save task.", e);
            sendError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not save the task.");
        }
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long userId = getUserId(request, response);
        if (userId == null) {
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String idValue = request.getParameter("id");

        if (idValue == null || idValue.isBlank()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Task ID is required.");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(idValue);
            if (id <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid task ID.");
            return;
        }

        String status = request.getParameter("status");

        try (Connection connection = DBConnection.getConnection()) {

            if (status != null) {
                if (!status.equals("Pending")
                        && !status.equals("Completed")) {
                    sendError(response,
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid task status.");
                    return;
                }

                String sql = """
                    UPDATE tasks
                    SET status = ?
                    WHERE id = ? AND user_id = ?
                    """;

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, status);
                    statement.setInt(2, id);
                    statement.setLong(3, userId);

                    if (statement.executeUpdate() == 0) {
                        sendError(response,
                                HttpServletResponse.SC_NOT_FOUND,
                                "Task not found.");
                        return;
                    }
                }

                sendSuccess(response, "Task status updated.");
                return;
            }

            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String priority = request.getParameter("priority");
            String dueDate = request.getParameter("dueDate");

            if (title == null || title.trim().isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Task title is required.");
                return;
            }

            String sql = """
                UPDATE tasks
                SET title = ?, description = ?, priority = ?, due_date = ?
                WHERE id = ? AND user_id = ?
                """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, title.trim());
                statement.setString(2, description);
                statement.setString(3,
                        priority == null || priority.isBlank()
                                ? "Medium" : priority);

                if (dueDate == null || dueDate.isBlank()) {
                    statement.setNull(4, java.sql.Types.DATE);
                } else {
                    statement.setDate(4, Date.valueOf(dueDate));
                }

                statement.setInt(5, id);
                statement.setLong(6, userId);

                if (statement.executeUpdate() == 0) {
                    sendError(response,
                            HttpServletResponse.SC_NOT_FOUND,
                            "Task not found.");
                    return;
                }
            }

            sendSuccess(response, "Task updated successfully.");

        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid due date. Use YYYY-MM-DD.");
        } catch (SQLException e) {
            getServletContext().log("Failed to update task.", e);
            sendError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not update the task.");
        }
    }

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long userId = getUserId(request, response);
        if (userId == null) {
            return;
        }

        String idValue = request.getParameter("id");

        if (idValue == null || idValue.isBlank()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Task ID is required.");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(idValue);
            if (id <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid task ID.");
            return;
        }

        String sql = """
            DELETE FROM tasks
            WHERE id = ? AND user_id = ?
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.setLong(2, userId);

            if (statement.executeUpdate() == 0) {
                sendError(response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Task not found.");
                return;
            }

            sendSuccess(response, "Task deleted successfully.");

        } catch (SQLException e) {
            getServletContext().log("Failed to delete task.", e);
            sendError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not delete the task.");
        }
    }
}
