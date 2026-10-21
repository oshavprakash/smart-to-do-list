# Smart To-Do List

A Java-based task management web application that helps users organize their daily activities, manage priorities, and track deadlines.

**Live Demo:** https://smart-to-do-list-jnft.onrender.com

## Features

- User registration and login
- Secure password hashing
- Create, view, update, and delete tasks
- Task priorities and due dates
- Task status and filtering
- User-specific task management
- Persistent storage using TiDB Cloud
- Responsive dark-themed interface

## Tech Stack

- **Frontend:** HTML, CSS, JavaScript
- **Backend:** Java Servlets, Jakarta Servlet API
- **Database:** TiDB Cloud (MySQL-compatible)
- **Database Connectivity:** JDBC, MySQL Connector/J
- **Build Tool:** Apache Maven
- **Web Server:** Apache Tomcat
- **Deployment:** Render using Docker

## Project Structure

```text
SmartTodoList/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   └── DBConnection.java
│   │   │   └── com/smarttodo/
│   │   │       ├── AuthServlet.java
│   │   │       └── HelloServlet.java
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       ├── index.html
│   │       ├── index.jsp
│   │       └── login.html
│   └── test/
├── .gitignore
├── Dockerfile
├── pom.xml
├── smartsql.sql
└── README.md
```

## Prerequisites

- Java Development Kit (JDK)
- Apache Maven
- Apache Tomcat 11
- A TiDB Cloud database

## Database Configuration

Configure the following environment variables before running the application:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL for the `smart_todo` database |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |

The JDBC URL should use the format:

```text
jdbc:mysql://YOUR_TIDB_HOST:4000/smart_todo?sslMode=VERIFY_IDENTITY
```

Replace the hostname with your database endpoint. Never commit actual credentials to GitHub.

Use the SQL statements in `smartsql.sql` to create the required database tables if they are not already present.

## Build

Clone the repository and navigate to the project directory:

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
cd SmartTodoList
```

Build the WAR file with Maven:

```bash
mvn clean package
```

The generated WAR file will be available in the `target/` directory.

## Deployment

The application is deployed on Render using the included `Dockerfile`. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the Render environment settings before deploying.

## Security

- Passwords are stored as hashes rather than plaintext.
- Database credentials are supplied through environment variables.
- Users should access only their own tasks.
- Never publish database passwords, access tokens, or other secrets in source code.

## Future Improvements

- Task search and sorting
- Recurring tasks and reminders
- Calendar-based task planning
- Enhanced dashboard analytics

## Author

Developed as a Java web application project.
