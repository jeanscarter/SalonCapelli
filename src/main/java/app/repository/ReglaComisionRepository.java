package app.repository;

import app.exception.DatabaseException;
import app.model.ReglaComision;
import java.util.List;

public interface ReglaComisionRepository {
    void create(ReglaComision regla) throws DatabaseException;
    void update(ReglaComision regla) throws DatabaseException;
    void delete(int id) throws DatabaseException;
    List<ReglaComision> findAll() throws DatabaseException;
    ReglaComision findById(int id) throws DatabaseException;
}
