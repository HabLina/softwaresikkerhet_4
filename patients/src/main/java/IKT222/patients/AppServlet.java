package IKT222.patients;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

  private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";
  // Implemented SQL queries in the respective functions bellow, removed the private static variables AUTH_QUERY and SEARCH_QUERY
    // private static final String AUTH_QUERY = "select * from user where username='%s' and password='%s'";
    // private static final String SEARCH_QUERY = "select * from patient where surname like '%s'";

  private final Configuration fm = new Configuration(Configuration.VERSION_2_3_28);
  private Connection database;

  @Override
  public void init() throws ServletException {
    configureTemplateEngine();
    connectToDatabase();
  }

  private void configureTemplateEngine() throws ServletException {
    try {
      fm.setDirectoryForTemplateLoading(new File("./templates"));
      fm.setDefaultEncoding("UTF-8");
      fm.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
      fm.setLogTemplateExceptions(false);
      fm.setWrapUncheckedExceptions(true);
    }
    catch (IOException error) {
      throw new ServletException(error.getMessage());
    }
  }

  private void connectToDatabase() throws ServletException {
    try {
      database = DriverManager.getConnection(CONNECTION_URL);
    }
    catch (SQLException error) {
      throw new ServletException(error.getMessage());
    }
  }

  // Use LoginService here
  private final LoginService loginService = new LoginService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {
    try {
      Template template = fm.getTemplate("login.html");
      template.process(null, response.getWriter());
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (TemplateException error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {

     // Get form parameters
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String surname = request.getParameter("surname");

    try {

      // Check if the user is locked out, if so display a message
      if (loginService.isLocked(username)) {
        response.setContentType("text/html");
        response.getWriter().write("<h1>Account locked. Please try again later.</h1>");
        return;
      }

      if (authenticated(username, password)) {
        
        // Reset attempts on successful login
        loginService.resetAttempts(username);

        // Get search results and merge with template
        Map<String, Object> model = new HashMap<>();
        model.put("records", searchResults(surname));
        Template template = fm.getTemplate("details.html");
        template.process(model, response.getWriter());
      }
      else {

        // Record failed attempt
        loginService.recordFailedAttempt(username);
        Template template = fm.getTemplate("invalid.html");
        template.process(null, response.getWriter());
      }
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (Exception error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
    }
  }

  private boolean authenticated(String username, String password) throws SQLException {
    // New SQL query using placeholder ? to let the query remain static
    String query = "SELECT * FROM user WHERE username = ? AND password = ?";

    // Using PreparedStatement instead of Statement as it treats user input as data, escaping special characters.
    // Need to make sure the PreparedStatement and ResultSet is closed automatically, so i put them in try-with-resources. Avoids resource leaks.
    try (PreparedStatement stmt = database.prepareStatement(query)) {

      // Set the username parameter to replace the placeholder
      stmt.setString(1, username);

      // Set the password parameter to replace the placeholder
      stmt.setString(2, password);
      try (ResultSet results = stmt.executeQuery()){
        return results.next();
      }
    }
  }

  private List<Record> searchResults(String surname) throws SQLException {
    List<Record> records = new ArrayList<>();
    // New SQL query using placeholder ? to let the query remain static
    String query = "SELECT * FROM patient WHERE surname LIKE ?";

    // Using PreparedStatement instead of Statement as it treats user input as data, escaping special characters.
    // Need to make sure the PreparedStatement and ResultSet is closed automatically, so i put them in try-with-resources. Avoids resource leaks.
    try (PreparedStatement stmt = database.prepareStatement(query)) {

      // Set the surname parameter to replace the placeholder
      stmt.setString(1, surname);

      try (ResultSet results = stmt.executeQuery()) {
        while (results.next()) {
          Record rec = new Record();
          rec.setSurname(results.getString(2));
          rec.setForename(results.getString(3));
          rec.setAddress(results.getString(4));
          rec.setDateOfBirth(results.getString(5));
          rec.setDoctorId(results.getString(6));
          rec.setDiagnosis(results.getString(7));
          records.add(rec);
        }
      }
    }
    return records;
  }
}