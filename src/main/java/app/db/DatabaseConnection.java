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
 * 
 * Esquema completo:
 * - clientes (Módulo Clientes)
 * - trabajadoras (Módulo Trabajadoras)
 * - cuentas_bancarias (Módulo Trabajadoras - Cuentas)
 * - servicios (Módulo Servicios)
 * - reglas_comision (Comisiones simples por categoría - LEGACY compat)
 * - reglas_comision_detalladas (Comisiones avanzadas - reemplaza PayrollService
 * hardcoded)
 * - marcas (Módulo Inventario)
 * - productos (Módulo Inventario)
 * - inventario_movimientos (Módulo Inventario - Auditoría)
 * - ventas (Módulo Transaccional)
 * - venta_items (Módulo Transaccional)
 * - venta_pagos (Módulo Transaccional)
 * - propinas (Módulo Transaccional)
 * - app_settings (Configuración)
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
                    new IllegalStateException("Driver SQLite no está cargado"));
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
                            stmt.execute("PRAGMA journal_mode=WAL"); // Write-Ahead Logging
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

        // =====================================================================
        // MÓDULO: CLIENTES
        // =====================================================================

        String sqlClientes1 = """
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
                    saldo_favor REAL DEFAULT 0.0,
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP,
                    fecha_modificacion TEXT DEFAULT CURRENT_TIMESTAMP
                )""";

        String sqlClientes2 = "CREATE INDEX IF NOT EXISTS idx_clientes_cedula ON clientes(cedula)";
        String sqlClientes3 = "CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre_completo)";

        // =====================================================================
        // MÓDULO: TRABAJADORAS
        // =====================================================================

        String sqlTrabajadoras1 = """
                CREATE TABLE IF NOT EXISTS trabajadoras (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cedula TEXT UNIQUE NOT NULL,
                    nombres TEXT NOT NULL,
                    apellidos TEXT NOT NULL,
                    telefono TEXT,
                    correo TEXT,
                    foto BLOB,
                    bono_activo INTEGER DEFAULT 0,
                    monto_bono REAL DEFAULT 0.0,
                    razon_bono TEXT DEFAULT '',
                    metodo_pago_preferido TEXT DEFAULT 'BANCO',
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP
                )""";

        String sqlTrabajadoras2 = "CREATE INDEX IF NOT EXISTS idx_trabajadoras_cedula ON trabajadoras(cedula)";
        String sqlTrabajadoras3 = "CREATE INDEX IF NOT EXISTS idx_trabajadoras_nombre ON trabajadoras(nombres, apellidos)";

        String sqlCuentas1 = """
                CREATE TABLE IF NOT EXISTS cuentas_bancarias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    trabajadora_id INTEGER NOT NULL,
                    banco TEXT NOT NULL,
                    tipo_cuenta TEXT NOT NULL,
                    numero_cuenta TEXT NOT NULL,
                    es_principal INTEGER DEFAULT 0,
                    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras(id) ON DELETE CASCADE
                )""";

        String sqlCuentas2 = "CREATE INDEX IF NOT EXISTS idx_cuentas_trabajadora ON cuentas_bancarias(trabajadora_id)";

        // =====================================================================
        // MÓDULO: SERVICIOS
        // =====================================================================

        String sqlServicios1 = """
                CREATE TABLE IF NOT EXISTS servicios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    categoria TEXT,
                    precio_corto REAL DEFAULT 0.0,
                    precio_mediano REAL DEFAULT 0.0,
                    precio_largo REAL DEFAULT 0.0,
                    precio_extensiones REAL DEFAULT 0.0,
                    permite_cliente_producto INTEGER DEFAULT 0,
                    precio_cliente_producto REAL DEFAULT 0.0,
                    is_active INTEGER DEFAULT 1,
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP
                )""";

        String sqlServicios2 = "CREATE INDEX IF NOT EXISTS idx_servicios_nombre ON servicios(nombre)";
        String sqlServicios3 = "CREATE INDEX IF NOT EXISTS idx_servicios_active ON servicios(is_active)";

        // =====================================================================
        // MÓDULO: COMISIONES (Simple - Legacy compat)
        // =====================================================================

        String sqlComisiones1 = """
                CREATE TABLE IF NOT EXISTS reglas_comision (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    trabajadora_id INTEGER NOT NULL,
                    categoria_servicio TEXT NOT NULL,
                    porcentaje_comision REAL DEFAULT 0.0,
                    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras(id) ON DELETE CASCADE,
                    UNIQUE(trabajadora_id, categoria_servicio)
                )""";

        String sqlComisiones2 = "CREATE INDEX IF NOT EXISTS idx_reglas_comision_trabajadora ON reglas_comision(trabajadora_id)";

        // =====================================================================
        // MÓDULO: COMISIONES DETALLADAS (Reemplaza PayrollService hardcoded)
        // =====================================================================

        String sqlComisionesDetalladas1 = """
                CREATE TABLE IF NOT EXISTS reglas_comision_detalladas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    trabajadora_id INTEGER,
                    servicio_id INTEGER,
                    categoria_servicio TEXT,
                    cliente_trae_producto INTEGER,
                    tipo_comision TEXT NOT NULL DEFAULT 'PORCENTAJE',
                    valor_comision REAL NOT NULL DEFAULT 0.0,
                    precio_condicion REAL,
                    prioridad INTEGER NOT NULL DEFAULT 10,
                    activo INTEGER NOT NULL DEFAULT 1,
                    descripcion TEXT,
                    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras(id) ON DELETE CASCADE,
                    FOREIGN KEY (servicio_id) REFERENCES servicios(id) ON DELETE CASCADE
                )""";

        String sqlComisionesDetalladas2 = "CREATE INDEX IF NOT EXISTS idx_rcd_trabajadora ON reglas_comision_detalladas(trabajadora_id)";
        String sqlComisionesDetalladas3 = "CREATE INDEX IF NOT EXISTS idx_rcd_servicio ON reglas_comision_detalladas(servicio_id)";
        String sqlComisionesDetalladas4 = "CREATE INDEX IF NOT EXISTS idx_rcd_categoria ON reglas_comision_detalladas(categoria_servicio)";
        String sqlComisionesDetalladas5 = "CREATE INDEX IF NOT EXISTS idx_rcd_prioridad ON reglas_comision_detalladas(prioridad DESC)";

        // =====================================================================
        // MÓDULO: INVENTARIO
        // =====================================================================

        String sqlMarcas1 = """
                CREATE TABLE IF NOT EXISTS marcas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT UNIQUE NOT NULL,
                    ruta_imagen TEXT,
                    descripcion TEXT,
                    activa INTEGER DEFAULT 1
                )""";

        String sqlMarcas2 = "CREATE INDEX IF NOT EXISTS idx_marcas_nombre ON marcas(nombre)";

        String sqlProductos1 = """
                CREATE TABLE IF NOT EXISTS productos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    descripcion TEXT,
                    marca_id INTEGER NOT NULL,
                    precio_compra REAL DEFAULT 0.0,
                    precio_venta REAL DEFAULT 0.0,
                    stock_actual INTEGER DEFAULT 0,
                    stock_minimo INTEGER DEFAULT 1,
                    unidad_medida TEXT DEFAULT 'unidad',
                    activo INTEGER DEFAULT 1,
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (marca_id) REFERENCES marcas(id) ON DELETE RESTRICT
                )""";

        String sqlProductos2 = "CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre)";
        String sqlProductos3 = "CREATE INDEX IF NOT EXISTS idx_productos_marca ON productos(marca_id)";
        String sqlProductos4 = "CREATE INDEX IF NOT EXISTS idx_productos_activo ON productos(activo)";

        String sqlMovimientos1 = """
                CREATE TABLE IF NOT EXISTS inventario_movimientos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    producto_id INTEGER NOT NULL,
                    tipo_movimiento TEXT NOT NULL,
                    cantidad INTEGER NOT NULL,
                    motivo TEXT,
                    venta_id INTEGER,
                    fecha_movimiento TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE,
                    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE SET NULL
                )""";

        String sqlMovimientos2 = "CREATE INDEX IF NOT EXISTS idx_inv_mov_producto ON inventario_movimientos(producto_id)";
        String sqlMovimientos3 = "CREATE INDEX IF NOT EXISTS idx_inv_mov_fecha ON inventario_movimientos(fecha_movimiento)";

        // =====================================================================
        // MÓDULO: TRANSACCIONAL (Ventas, Pagos, Propinas)
        // Migrado desde LEGACY: sales, sale_items, sale_payments, tips
        // =====================================================================

        String sqlVentas1 = """
                CREATE TABLE IF NOT EXISTS ventas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cliente_id INTEGER,
                    fecha_venta TEXT DEFAULT CURRENT_TIMESTAMP,
                    subtotal REAL NOT NULL,
                    tipo_descuento TEXT,
                    monto_descuento REAL DEFAULT 0.0,
                    monto_iva REAL DEFAULT 0.0,
                    total REAL NOT NULL,
                    tasa_bcv REAL DEFAULT 0.0,
                    numero_correlativo TEXT,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )""";

        String sqlVentas2 = "CREATE INDEX IF NOT EXISTS idx_ventas_fecha ON ventas(fecha_venta)";
        String sqlVentas3 = "CREATE INDEX IF NOT EXISTS idx_ventas_cliente ON ventas(cliente_id)";
        String sqlVentas4 = "CREATE INDEX IF NOT EXISTS idx_ventas_correlativo ON ventas(numero_correlativo)";

        String sqlVentaItems1 = """
                CREATE TABLE IF NOT EXISTS venta_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venta_id INTEGER NOT NULL,
                    servicio_id INTEGER NOT NULL,
                    trabajadora_id INTEGER NOT NULL,
                    precio_venta REAL NOT NULL,
                    cliente_trajo_producto INTEGER DEFAULT 0,
                    producto_id INTEGER,
                    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
                    FOREIGN KEY (servicio_id) REFERENCES servicios(id),
                    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras(id),
                    FOREIGN KEY (producto_id) REFERENCES productos(id)
                )""";

        String sqlVentaItems2 = "CREATE INDEX IF NOT EXISTS idx_venta_items_venta ON venta_items(venta_id)";
        String sqlVentaItems3 = "CREATE INDEX IF NOT EXISTS idx_venta_items_trabajadora ON venta_items(trabajadora_id)";

        String sqlVentaPagos1 = """
                CREATE TABLE IF NOT EXISTS venta_pagos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venta_id INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    moneda TEXT NOT NULL,
                    metodo_pago TEXT NOT NULL,
                    destino_pago TEXT,
                    referencia_pago TEXT,
                    tasa_bcv_al_pago REAL DEFAULT 0.0,
                    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE
                )""";

        String sqlVentaPagos2 = "CREATE INDEX IF NOT EXISTS idx_venta_pagos_venta ON venta_pagos(venta_id)";

        String sqlPropinas1 = """
                CREATE TABLE IF NOT EXISTS propinas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venta_id INTEGER,
                    trabajadora_id INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
                    FOREIGN KEY (trabajadora_id) REFERENCES trabajadoras(id)
                )""";

        String sqlPropinas2 = "CREATE INDEX IF NOT EXISTS idx_propinas_venta ON propinas(venta_id)";
        String sqlPropinas3 = "CREATE INDEX IF NOT EXISTS idx_propinas_trabajadora ON propinas(trabajadora_id)";

        // =====================================================================
        // MÓDULO: CUENTAS POR COBRAR (Fase 4.5)
        // =====================================================================

        String sqlCuentasPorCobrar1 = """
                CREATE TABLE IF NOT EXISTS cuentas_por_cobrar (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cliente_id INTEGER NOT NULL,
                    venta_id INTEGER NOT NULL,
                    monto_original REAL NOT NULL,
                    monto_pendiente REAL NOT NULL,
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP,
                    fecha_ultimo_abono TEXT,
                    estatus TEXT DEFAULT 'PENDIENTE', -- PENDIENTE, PARCIAL, PAGADA
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE
                )""";

        String sqlCuentasPorCobrar2 = "CREATE INDEX IF NOT EXISTS idx_cxc_cliente ON cuentas_por_cobrar(cliente_id)";

        // =====================================================================
        // MÓDULO: SEGURIDAD (Fase 6)
        // =====================================================================

        String sqlUsuarios1 = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    rol TEXT NOT NULL,
                    activo INTEGER DEFAULT 1,
                    fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP
                )""";

        String sqlUsuarios2 = "CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios(username)";

        // =====================================================================
        // MÓDULO: CONFIGURACIÓN
        // =====================================================================

        String sqlSettings1 = """
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY NOT NULL,
                    setting_value TEXT NOT NULL
                )""";

        // =====================================================================
        // EJECUCIÓN
        // =====================================================================

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {

            logger.debug("Ejecutando script de creación de tablas");

            // Clientes
            stmt.execute(sqlClientes1);
            stmt.execute(sqlClientes2);
            stmt.execute(sqlClientes3);

            try {
                stmt.execute("ALTER TABLE clientes ADD COLUMN intercambio_activo INTEGER DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE clientes ADD COLUMN fecha_vencimiento_intercambio TEXT");
            } catch (SQLException e) {
            }

            // Trabajadoras
            stmt.execute(sqlTrabajadoras1);

            // Migraciones para DB legacy (CapelliSalesWindow -> SalonCapelli)
            try {
                stmt.execute("ALTER TABLE trabajadoras RENAME COLUMN numero_ci TO cedula");
                logger.info("Migración: Columna 'numero_ci' renombrada a 'cedula' en trabajadoras");
            } catch (SQLException e) {
                // Ignorar si la columna no existe o ya se renombró
            }
            try {
                stmt.execute("ALTER TABLE trabajadoras ADD COLUMN bono_activo INTEGER DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE trabajadoras ADD COLUMN monto_bono REAL DEFAULT 0.0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE trabajadoras ADD COLUMN razon_bono TEXT DEFAULT ''");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE trabajadoras ADD COLUMN metodo_pago_preferido TEXT DEFAULT 'BANCO'");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE trabajadoras ADD COLUMN fecha_creacion TEXT DEFAULT CURRENT_TIMESTAMP");
            } catch (SQLException e) {
            }

            stmt.execute(sqlTrabajadoras2);
            stmt.execute(sqlTrabajadoras3);

            // Cuentas Bancarias
            stmt.execute(sqlCuentas1);
            stmt.execute(sqlCuentas2);

            // Servicios
            stmt.execute(sqlServicios1);
            stmt.execute(sqlServicios2);
            stmt.execute(sqlServicios3);

            // Comisiones (Simple)
            stmt.execute(sqlComisiones1);
            stmt.execute(sqlComisiones2);

            // Comisiones Detalladas
            stmt.execute(sqlComisionesDetalladas1);
            stmt.execute(sqlComisionesDetalladas2);
            stmt.execute(sqlComisionesDetalladas3);
            stmt.execute(sqlComisionesDetalladas4);
            stmt.execute(sqlComisionesDetalladas5);

            // Inventario: Marcas
            stmt.execute(sqlMarcas1);
            stmt.execute(sqlMarcas2);

            // Inventario: Productos
            stmt.execute(sqlProductos1);
            stmt.execute(sqlProductos2);
            stmt.execute(sqlProductos3);
            stmt.execute(sqlProductos4);

            // Ventas (antes de movimientos, por la FK)
            stmt.execute(sqlVentas1);
            stmt.execute(sqlVentas2);
            stmt.execute(sqlVentas3);
            stmt.execute(sqlVentas4);

            try {
                stmt.execute("ALTER TABLE ventas ADD COLUMN estatus TEXT DEFAULT 'PAGADA'");
            } catch (SQLException e) {
            }

            // Venta Items
            stmt.execute(sqlVentaItems1);
            stmt.execute(sqlVentaItems2);
            stmt.execute(sqlVentaItems3);

            // Venta Pagos
            stmt.execute(sqlVentaPagos1);
            stmt.execute(sqlVentaPagos2);

            // Propinas
            stmt.execute(sqlPropinas1);
            stmt.execute(sqlPropinas2);
            stmt.execute(sqlPropinas3);

            // Cuentas por Cobrar
            stmt.execute(sqlCuentasPorCobrar1);
            stmt.execute(sqlCuentasPorCobrar2);

            // Inventario: Movimientos (después de ventas por FK)
            stmt.execute(sqlMovimientos1);
            stmt.execute(sqlMovimientos2);
            stmt.execute(sqlMovimientos3);

            // App Settings
            stmt.execute(sqlSettings1);

            // Usuarios
            stmt.execute(sqlUsuarios1);
            stmt.execute(sqlUsuarios2);

            // Insertar admin por defecto si no existe (pass: admin123)
            // Hash SHA-256 de "admin123" es "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"
            stmt.execute("INSERT OR IGNORE INTO usuarios (username, password_hash, rol) VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN')");

            // Inicializar configuración por defecto
            stmt.execute("INSERT OR IGNORE INTO app_settings (setting_key, setting_value) VALUES ('correlativo', '1')");
            stmt.execute("INSERT OR IGNORE INTO app_settings (setting_key, setting_value) VALUES ('tasa_bcv', '0.0')");

            logger.info("✓ Base de datos SQLite inicializada correctamente");
            logger.info("✓ Tablas verificadas/creadas: clientes, trabajadoras, cuentas_bancarias, " +
                    "servicios, reglas_comision, reglas_comision_detalladas, marcas, productos, " +
                    "inventario_movimientos, ventas, venta_items, venta_pagos, propinas, app_settings");
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