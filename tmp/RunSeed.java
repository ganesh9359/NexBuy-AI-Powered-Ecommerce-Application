import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class RunSeed {
    public static void main(String[] args) throws Exception {
        String sql = Files.readString(Path.of("db/seed.sql"));
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : sql.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            current.append(rawLine).append('\n');
            if (line.endsWith(";")) {
                statements.add(current.toString());
                current.setLength(0);
            }
        }
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/nexbuy_db", "root", "ganesh")) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                for (String statement : statements) {
                    s.execute(statement);
                }
            }
            c.commit();
            printCount(c, "categories");
            printCount(c, "brands");
            printCount(c, "products");
            printCount(c, "product_variants");
            printCount(c, "inventory");
            printCount(c, "product_media");
            printCount(c, "product_tags");
        }
    }

    private static void printCount(Connection c, String table) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from " + table)) {
            rs.next();
            System.out.println(table + "=" + rs.getInt(1));
        }
    }
}
