package app.repository;

import app.exception.DatabaseException;
import app.model.CuentaPorCobrar;
import java.util.List;
import java.util.Optional;

public interface CuentaPorCobrarRepository {
    CuentaPorCobrar save(CuentaPorCobrar cxc) throws DatabaseException;
    Optional<CuentaPorCobrar> findById(Integer id) throws DatabaseException;
    List<CuentaPorCobrar> findAll() throws DatabaseException;
    CuentaPorCobrar update(CuentaPorCobrar cxc) throws DatabaseException;
    void delete(Integer id) throws DatabaseException;
    
    List<CuentaPorCobrar> findByClienteId(int clienteId) throws DatabaseException;
    List<CuentaPorCobrar> findPendientes() throws DatabaseException;
    Optional<CuentaPorCobrar> findByVentaId(int ventaId) throws DatabaseException;
}
