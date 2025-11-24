package app.repository;

import app.model.Cliente;
import java.util.List;

public interface ClienteRepository {
    void create(Cliente cliente) throws Exception;
    void update(Cliente cliente) throws Exception;
    void delete(int id) throws Exception;
    List<Cliente> findAll() throws Exception;
    Cliente findByCedula(String cedula) throws Exception;
}