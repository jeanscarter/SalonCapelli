package app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // Ruta de la base de datos
    private static final String URL = "jdbc:sqlite:salon_capelli.db";

    public static Connection connect() throws SQLException {
        try {
            // CORRECCIÓN: Forzamos la carga del driver explícitamente
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el Driver de SQLite. Revisa tu pom.xml");
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL);
    }

    public static void initDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cedula TEXT UNIQUE,
                    nombre_completo TEXT NOT NULL,
                    telefono TEXT,
                    direccion TEXT,
                    tipo_cabello TEXT,
                    tipo_extensiones TEXT,
                    fecha_cumpleanos TEXT,
                    fecha_ultimo_tinte TEXT,
                    fecha_ultimo_quimico TEXT,
                    fecha_ultima_keratina TEXT,
                    fecha_ultimo_mantenimiento TEXT
                );
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Base de datos SQLite inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error inicializando la BD: " + e.getMessage());
        }
    }
}