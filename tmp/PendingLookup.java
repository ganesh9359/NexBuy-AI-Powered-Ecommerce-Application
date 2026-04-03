import java.sql.*;
public class PendingLookup {
  public static void main(String[] args) throws Exception {
    String email = args[0];
    try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/nexbuy_db", "root", "ganesh")) {
      try (PreparedStatement ps = c.prepareStatement("select id, email, phone, otp_code from pending_registrations where email = ?")) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println(rs.getLong("id") + "|" + rs.getString("email") + "|" + rs.getString("phone") + "|" + rs.getString("otp_code"));
          }
        }
      }
    }
  }
}