package app.repository;

import app.exception.DatabaseException;
import app.model.CuentaReceptora;

import java.util.List;

/**
 * Interfaz del repositorio de Cuentas Receptoras del Salón.
 */
public interface CuentaReceptoraRepository {

    List<CuentaReceptora> findAll() throws DatabaseException;

    List<CuentaReceptora> findActivas() throws DatabaseException;

    /**
     * Filtra cuentas receptoras por plataforma (ej: "Zelle", "Transferencia", "Pago Móvil").
     */
    List<CuentaReceptora> findByPlataforma(String bancoPlataforma) throws DatabaseException;

    CuentaReceptora findById(int id) throws DatabaseException;

    void save(CuentaReceptora cuenta) throws DatabaseException;

    void update(CuentaReceptora cuenta) throws DatabaseException;

    void delete(int id) throws DatabaseException;
}
