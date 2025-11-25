package app.db;

import app.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor de conexiones a la base de datos SQLite
 * Patrón: Singleton + Lazy Initialization
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String URL = "jdbc:sqlite:salon_capelli.db";
    private static volatile Connection instance;
    private static final Object lock = new Object();
    
    // Bandera para saber si ya se inicializó el driver
    private static boolean driverLoaded = false;

    /**
     * Carga el driver JDBC de SQLite (solo una vez)
     */
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            driverLoaded = true;
            logger.info("Driver SQLite cargado exitosamente");
        } catch (ClassNotFoundException e) {
            logger.error("CRÍTICO: Driver SQLite no encontrado. Verifique las dependencias Maven", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Obtiene una conexión a la base de datos (Singleton con doble verificación)
     * 
     * @return Conexión activa a la base de datos
     * @throws DatabaseException si no se puede conectar
     */
    public static Connection connect() throws DatabaseException {
        if (!driverLoaded) {
            throw DatabaseException.connectionFailed(
                new IllegalStateException("Driver SQLite no está cargado")
            );
        }
        
        try {
            if (instance == null || instance.isClosed()) {
                synchronized (lock) {
                    if (instance == null || instance.isClosed()) {
                        logger.debug("Creando nueva conexión a la base de datos: {}", URL);
                        instance = DriverManager.getConnection(URL);
                        instance.setAutoCommit(true);
                        
                        // Configuraciones de SQLite para mejor rendimiento
                        try (Statement stmt = instance.createStatement()) {
                            stmt.execute("PRAGMA journal_mode=WAL");  // Write-Ahead Logging
                            stmt.execute("PRAGMA foreign_keys=ON");
                            stmt.execute("PRAGMA synchronous=NORMAL");
                            logger.debug("Configuraciones de SQLite aplicadas (WAL, FK, SYNC)");
                        }
                        
                        logger.info("Conexión a la base de datos establecida exitosamente");
                    }
                }
            }
            return instance;
            
        } catch (SQLException e) {
            logger.error("Error al conectar con la base de datos: {}", e.getMessage(), e);
            throw DatabaseException.connectionFailed(e);
        }
    }

    /**
     * Inicializa la base de datos creando las tablas necesarias
     * 
     * @throws DatabaseException si no se puede inicializar
     */
    public static void initDatabase() throws DatabaseException {
        logger.info("Iniciando proceso de inicialización de la base de datos");
        
        String sql = """
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cedula TEXT UNIQUE NOT NULL,
                    nombre_completo TEXT NOT NULL,
                    telefono TEXT,
                    direccion TEXT,
                    tipo_cabello TEXT,
                    tipo_extensiones TEXT,
                    fecha_cumpleanos TEXT,
                    fecha_ultimo_tinte TEXT,
                    fecha_ultimo_quimico TEXT,
                    fecha_ultima_keratina TEXT,
                    fecha_ultimo_mantenimiento TEXT,
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP,
                    fecha_modificacion TEXT DEFAULT CURRENT_TIMESTAMP
                );
                
                CREATE INDEX IF NOT EXISTS idx_clientes_cedula ON clientes(cedula);
                CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre_completo);
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            logger.debug("Ejecutando script de creación de tablas");
            stmt.execute(sql);
            
            logger.info("✓ Base de datos SQLite inicializada correctamente");
            logger.info("✓ Tabla 'clientes' verificada/creada");
            logger.info("✓ Índices creados/verificados");
            
        } catch (SQLException e) {
            logger.error("Error crítico al inicializar la base de datos", e);
            throw DatabaseException.initializationFailed(e);
        }
    }

    /**
     * Cierra la conexión a la base de datos
     */
    public static void close() {
        if (instance != null) {
            try {
                logger.info("Cerrando conexión a la base de datos...");
                instance.close();
                instance = null;
                logger.info("✓ Conexión cerrada exitosamente");
            } catch (SQLException e) {
                logger.error("Error al cerrar la conexión a la base de datos", e);
            }
        }
    }

    /**
     * Verifica si hay una conexión activa
     * 
     * @return true si hay conexión activa, false en caso contrario
     */
    public static boolean isConnected() {
        try {
            return instance != null && !instance.isClosed();
        } catch (SQLException e) {
            logger.warn("Error verificando estado de conexión", e);
            return false;
        }
    }
    
    /**
     * Hook para cerrar la conexión al finalizar la aplicación
     */
    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Ejecutando shutdown hook de la base de datos");
            close();
        }, "Database-Shutdown-Hook"));
        
        logger.debug("Shutdown hook registrado para la base de datos");
    }
}