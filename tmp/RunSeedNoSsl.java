import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RunSeedNoSsl {
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

        String url = "jdbc:mysql://localhost:3306/nexbuy_db?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection c = DriverManager.getConnection(url, "root", "ganesh")) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                for (String statement : statements) {
                    s.execute(statement);
                }
            }
            c.commit();

            printCount(c, "brands");
            printCount(c, "products");
            printCount(c, "product_media");
            printCount(c, "product_tags");
            printScalar(c, "products_with_three_media", "select count(*) from (select product_id from product_media group by product_id having count(*) >= 3) media_check");
            printScalar(c, "products_with_three_tags", "select count(*) from (select product_id from product_tags group by product_id having count(*) >= 3) tag_check");
        }
    }

    private static void printCount(Connection c, String table) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from " + table)) {
            rs.next();
            System.out.println(table + "=" + rs.getInt(1));
        }
    }

    private static void printScalar(Connection c, String label, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            System.out.println(label + "=" + rs.getInt(1));
        }
    }
}
