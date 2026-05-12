package app.repository;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite del repositorio de Productos
 */
public class ProductoRepositorySQLite implements ProductoRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductoRepositorySQLite.class);

    public ProductoRepositorySQLite() {
    }

    private static final String SQL_CREATE =
        "INSERT INTO productos (nombre, descripcion, marca_id, precio_compra, precio_venta, stock_actual, stock_minimo, unidad_medida, activo) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
        "UPDATE productos SET nombre=?, descripcion=?, marca_id=?, precio_compra=?, precio_venta=?, " +
        "stock_actual=?, stock_minimo=?, unidad_medida=?, activo=? WHERE id=?";

    private static final String SQL_DELETE =
        "DELETE FROM productos WHERE id=?";

    private static final String SQL_FIND_ALL =
        "SELECT p.*, m.nombre AS marca_nombre FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id ORDER BY p.nombre";

    private static final String SQL_FIND_ALL_ACTIVOS =
        "SELECT p.*, m.nombre AS marca_nombre FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id WHERE p.activo=1 ORDER BY p.nombre";

    private static final String SQL_FIND_BY_ID =
        "SELECT p.*, m.nombre AS marca_nombre FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id WHERE p.id=?";

    private static final String SQL_FIND_BY_MARCA =
        "SELECT p.*, m.nombre AS marca_nombre FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id WHERE p.marca_id=? ORDER BY p.nombre";

    private static final String SQL_SEARCH_BY_NOMBRE =
        "SELECT p.*, m.nombre AS marca_nombre FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id WHERE p.nombre LIKE ? ORDER BY p.nombre";

    private static final String SQL_UPDATE_STOCK =
        "UPDATE productos SET stock_actual = stock_actual + ? WHERE id=?";

    private static final String SQL_FIND_STOCK_BAJO =
        "SELECT p.*, m.nombre AS marca_nombre FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id WHERE p.activo=1 AND p.stock_actual <= p.stock_minimo ORDER BY p.stock_actual";

    private static final String SQL_COUNT =
        "SELECT COUNT(*) FROM productos WHERE activo=1";

    @Override
    public void create(Producto producto) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, producto.getNombre());
            pstmt.setString(2, producto.getDescripcion());
            pstmt.setInt(3, producto.getMarcaId());
            pstmt.setDouble(4, producto.getPrecioCompra());
            pstmt.setDouble(5, producto.getPrecioVenta());
            pstmt.setInt(6, producto.getStockActual());
            pstmt.setInt(7, producto.getStockMinimo());
            pstmt.setString(8, producto.getUnidadMedida());
            pstmt.setInt(9, producto.isActivo() ? 1 : 0);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    producto.setId(rs.getInt(1));
                }
            }
            logger.debug("Producto creado: {} (ID={})", producto.getNombre(), producto.getId());
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("CREATE PRODUCTO", e);
        }
    }

    @Override
    public void update(Producto producto) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {

            pstmt.setString(1, producto.getNombre());
            pstmt.setString(2, producto.getDescripcion());
            pstmt.setInt(3, producto.getMarcaId());
            pstmt.setDouble(4, producto.getPrecioCompra());
            pstmt.setDouble(5, producto.getPrecioVenta());
            pstmt.setInt(6, producto.getStockActual());
            pstmt.setInt(7, producto.getStockMinimo());
            pstmt.setString(8, producto.getUnidadMedida());
            pstmt.setInt(9, producto.isActivo() ? 1 : 0);
            pstmt.setInt(10, producto.getId());

            pstmt.executeUpdate();
            logger.debug("Producto actualizado: {} (ID={})", producto.getNombre(), producto.getId());
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("UPDATE PRODUCTO", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.debug("Producto eliminado (ID={})", id);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("DELETE PRODUCTO", e);
        }
    }

    @Override
    public List<Producto> findAll() throws DatabaseException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_ALL PRODUCTOS", e);
        }
        return lista;
    }

    @Override
    public List<Producto> findAllActivos() throws DatabaseException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL_ACTIVOS)) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_ALL_ACTIVOS PRODUCTOS", e);
        }
        return lista;
    }

    @Override
    public Producto findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProducto(rs);
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_ID PRODUCTO", e);
        }
        return null;
    }

    @Override
    public List<Producto> findByMarca(int marcaId) throws DatabaseException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_FIND_BY_MARCA)) {

            pstmt.setInt(1, marcaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_BY_MARCA PRODUCTOS", e);
        }
        return lista;
    }

    @Override
    public List<Producto> searchByNombre(String nombre) throws DatabaseException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SEARCH_BY_NOMBRE)) {

            pstmt.setString(1, "%" + nombre + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("SEARCH_BY_NOMBRE PRODUCTOS", e);
        }
        return lista;
    }

    @Override
    public void actualizarStock(int productoId, int cantidad) throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_STOCK)) {

            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, productoId);
            pstmt.executeUpdate();
            logger.debug("Stock actualizado para producto ID={}: {} unidades", productoId, cantidad > 0 ? "+" + cantidad : cantidad);
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("UPDATE_STOCK PRODUCTO", e);
        }
    }

    @Override
    public List<Producto> findStockBajo() throws DatabaseException {
        List<Producto> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_STOCK_BAJO)) {

            while (rs.next()) {
                lista.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("FIND_STOCK_BAJO PRODUCTOS", e);
        }
        return lista;
    }

    @Override
    public int count() throws DatabaseException {
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw DatabaseException.queryFailed("COUNT PRODUCTOS", e);
        }
        return 0;
    }

    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setId(rs.getInt("id"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setMarcaId(rs.getInt("marca_id"));
        producto.setPrecioCompra(rs.getDouble("precio_compra"));
        producto.setPrecioVenta(rs.getDouble("precio_venta"));
        producto.setStockActual(rs.getInt("stock_actual"));
        producto.setStockMinimo(rs.getInt("stock_minimo"));
        producto.setUnidadMedida(rs.getString("unidad_medida"));
        producto.setActivo(rs.getInt("activo") == 1);
        producto.setFechaCreacion(rs.getString("fecha_creacion"));

        // Cargar la marca si está disponible en el JOIN
        try {
            String marcaNombre = rs.getString("marca_nombre");
            if (marcaNombre != null) {
                var marca = new app.model.Marca();
                marca.setId(producto.getMarcaId());
                marca.setNombre(marcaNombre);
                producto.setMarca(marca);
            }
        } catch (SQLException ignored) {
            // marca_nombre no está en el resultset, no hacer nada
        }

        return producto;
    }
}
