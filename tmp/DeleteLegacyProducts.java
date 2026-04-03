import java.sql.*;
public class DeleteLegacyProducts {
  public static void main(String[] args) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/nexbuy_db", "root", "ganesh")) {
      try (PreparedStatement ps = c.prepareStatement("delete from products where slug in (?, ?)"); Statement s = c.createStatement()) {
        ps.setString(1, "samsung-galaxy-m14-5g");
        ps.setString(2, "oneplus-nord-buds-3");
        int deleted = ps.executeUpdate();
        System.out.println("deleted=" + deleted);
      }
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from products")) {
        rs.next();
        System.out.println("products=" + rs.getInt(1));
      }
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select c.name, count(*) from products p join categories c on c.id = p.category_id group by c.name order by c.name")) {
        while (rs.next()) {
          System.out.println(rs.getString(1) + "=" + rs.getInt(2));
        }
      }
    }
  }
}
