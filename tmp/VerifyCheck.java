import java.sql.*;
public class VerifyCheck {
  public static void main(String[] args) throws Exception {
    String email = args[0];
    try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/nexbuy_db", "root", "ganesh")) {
      try (PreparedStatement user = c.prepareStatement("select id,email,phone,status,role from users where email = ?")) {
        user.setString(1, email);
        try (ResultSet rs = user.executeQuery()) {
          while (rs.next()) {
            long userId = rs.getLong("id");
            System.out.println("USER=" + userId + "|" + rs.getString("email") + "|" + rs.getString("phone") + "|" + rs.getString("status") + "|" + rs.getString("role"));
            try (PreparedStatement profile = c.prepareStatement("select first_name,last_name from user_profiles where user_id = ?")) {
              profile.setLong(1, userId);
              try (ResultSet prs = profile.executeQuery()) {
                while (prs.next()) {
                  System.out.println("PROFILE=" + prs.getString("first_name") + "|" + prs.getString("last_name"));
                }
              }
            }
            try (PreparedStatement address = c.prepareStatement("select line1,city,state,postal_code,country from addresses where user_id = ?")) {
              address.setLong(1, userId);
              try (ResultSet ars = address.executeQuery()) {
                while (ars.next()) {
                  System.out.println("ADDRESS=" + ars.getString("line1") + "|" + ars.getString("city") + "|" + ars.getString("state") + "|" + ars.getString("postal_code") + "|" + ars.getString("country"));
                }
              }
            }
          }
        }
      }
      try (PreparedStatement pending = c.prepareStatement("select count(*) from pending_registrations where email = ?")) {
        pending.setString(1, email);
        try (ResultSet rs = pending.executeQuery()) {
          if (rs.next()) {
            System.out.println("PENDING_COUNT=" + rs.getInt(1));
          }
        }
      }
    }
  }
}