package org.statefulj.framework.core.model.impl;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import org.statefulj.framework.core.model.Finder;

public class CrudRepositoryFinderImpl<T, CT> implements Finder<T, CT> {
    private final CrudRepository<T, Serializable> repo;

    public CrudRepositoryFinderImpl(final CrudRepository<T, Serializable> repo) {
        this.repo = repo;
    }

    public T find(final Class<T> clazz, final Object id, final String event, final CT context) {
        return repo.findOne((Serializable) id);
    }

    public T find(final Class<T> clazz, final String event, final CT context) {
        return null;
    }
}
