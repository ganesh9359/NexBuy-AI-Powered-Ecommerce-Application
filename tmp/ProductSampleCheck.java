import java.sql.*;
public class ProductSampleCheck {
  public static void main(String[] args) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/nexbuy_db", "root", "ganesh")) {
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select p.id, p.title, p.slug, c.name as category from products p left join categories c on c.id = p.category_id order by p.id")) {
        while (rs.next()) {
          System.out.println(rs.getLong("id") + " | " + rs.getString("title") + " | " + rs.getString("slug") + " | " + rs.getString("category"));
        }
      }
    }
  }
}
