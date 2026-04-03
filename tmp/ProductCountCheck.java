import java.sql.*;
public class ProductCountCheck {
  public static void main(String[] args) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/nexbuy_db", "root", "ganesh")) {
      printCount(c, "categories");
      printCount(c, "brands");
      printCount(c, "products");
      printCount(c, "product_variants");
      printCount(c, "inventory");
      printCount(c, "product_media");
      printCount(c, "product_tags");
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select id,title,slug from products order by id limit 10")) {
        System.out.println("-- sample products --");
        while (rs.next()) {
          System.out.println(rs.getLong(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
        }
      }
    }
  }
  static void printCount(Connection c, String table) throws Exception {
    try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from " + table)) {
      rs.next();
      System.out.println(table + "=" + rs.getInt(1));
    }
  }
}
