import java.sql.*;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/nexbuy_db";
        String user = "root";
        String pass = "ganesh";
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            printCount(c, "users");
            printCount(c, "user_profiles");
            printCount(c, "addresses");
            printCount(c, "pending_registrations");
            printLatest(c, "select id,email,phone,status,role,created_at from users order by id desc limit 5", "latest users");
            printLatest(c, "select user_id,first_name,last_name,created_at from user_profiles order by user_id desc limit 5", "latest profiles");
            printLatest(c, "select user_id,label,line1,city,state,postal_code,country,created_at from addresses order by id desc limit 5", "latest addresses");
            printLatest(c, "select id,email,phone,first_name,line1,city,state,postal_code,country,created_at from pending_registrations order by id desc limit 5", "latest pending registrations");
        }
    }

    private static void printCount(Connection c, String table) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select count(*) from " + table)) {
            rs.next();
            System.out.println(table + "=" + rs.getInt(1));
        }
    }

    private static void printLatest(Connection c, String sql, String label) throws Exception {
        System.out.println("-- " + label + " --");
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) row.append(" | ");
                    row.append(md.getColumnLabel(i)).append('=').append(rs.getString(i));
                }
                System.out.println(row);
            }
        }
    }
}