package ua.dymohlo.moduledb;

import java.util.List;
import java.util.Optional;

public interface Dao<T> {
    T create(T entity);

    Optional<T> getById(Long id);

    Optional<T> update(T entity);

    void deleteById(Long id);

    List<T> getAll();
}