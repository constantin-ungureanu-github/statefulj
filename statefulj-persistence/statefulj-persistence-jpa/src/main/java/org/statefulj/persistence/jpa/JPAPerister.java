package org.statefulj.persistence.jpa;

import java.lang.reflect.Field;
import java.util.List;

import javax.persistence.EmbeddedId;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.statefulj.common.utils.ReflectionUtils;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.common.AbstractPersister;

public class JPAPerister<T> extends AbstractPersister<T> implements Persister<T> {

    private static final Logger logger = LoggerFactory.getLogger(JPAPerister.class);

    private EntityManager entityManager;

    private PlatformTransactionManager transactionManager;

    public JPAPerister(final List<State<T>> states, final State<T> start, final Class<T> clazz, final EntityManagerFactoryInfo entityManagerFactory,
            final PlatformTransactionManager transactionManager) {
        this(states, null, start, clazz, entityManagerFactory.getNativeEntityManagerFactory().createEntityManager(), transactionManager);
    }

    public JPAPerister(final List<State<T>> states, final String stateFieldName, final State<T> start, final Class<T> clazz, final EntityManager entityManager,
            final PlatformTransactionManager transactionManager) {
        super(states, stateFieldName, start, clazz);
        this.transactionManager = transactionManager;
        this.entityManager = entityManager;
    }

    @Override
    public void setCurrent(final T stateful, final State<T> current, final State<T> next) throws StaleStateException {
        try {
            final Object id = getId(stateful);
            if ((id != null) && entityManager.contains(stateful)) {
                updateStateInDB(stateful, current, next, id);
                setState(stateful, next.getName());
            } else {
                updateStateInMemory(stateful, current, next);
            }
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateStateInMemory(final T stateful, final State<T> current, final State<T> next) throws NoSuchFieldException, IllegalAccessException, StaleStateException {
        synchronized (stateful) {
            String state = getState(stateful);
            state = (state == null) ? getStart().getName() : state;
            if (state.equals(current.getName())) {
                setState(stateful, next.getName());
            } else {
                throwStaleState(current, next);
            }
        }
    }

    private void updateStateInDB(final T stateful, final State<T> current, final State<T> next, final Object id) throws NoSuchFieldException, IllegalAccessException, StaleStateException {
        final Query update = buildUpdate(id, stateful, current, next, getIdField(), getStateField());

        if (update.executeUpdate() == 0) {
            final Query query = buildQuery(id, stateful);
            String state = getStart().getName();
            try {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                state = tt.execute(status -> (String) query.getSingleResult());
            } catch (final NoResultException nre) {
            }

            JPAPerister.logger.warn("Stale State, expected={}, actual={}", current.getName(), state);

            setState(stateful, state);
            throwStaleState(current, next);
        }
    }

    protected Query buildUpdate(final Object id, final T stateful, final State<T> current, final State<T> next, final Field idField, final Field stateField)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        final CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        final CriteriaUpdate<T> cu = cb.createCriteriaUpdate(getClazz());
        final Root<T> t = cu.from(getClazz());

        final Path<?> idPath = t.get(getIdField().getName());
        final Path<String> statePath = t.get(getStateField().getName());

        cu.set(statePath, next.getName());

        final Predicate statePredicate = (current.equals(getStart())) ? cb.or(cb.equal(statePath, current.getName()), cb.equal(statePath, cb.nullLiteral(String.class)))
                : cb.equal(statePath, current.getName());

        cu.where(cb.and(cb.equal(idPath, getId(stateful)), statePredicate));

        final Query query = entityManager.createQuery(cu);
        if (JPAPerister.logger.isDebugEnabled()) {
            JPAPerister.logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
        }
        return query;
    }

    @Override
    protected boolean validStateField(final Field stateField) {
        return (stateField.getType().equals(String.class));
    }

    @Override
    protected Field findIdField(final Class<?> clazz) {
        Field idField = null;
        idField = ReflectionUtils.getReferencedField(clazz, Id.class);
        if (idField == null) {
            idField = ReflectionUtils.getReferencedField(clazz, EmbeddedId.class);
        }
        return idField;
    }

    @Override
    protected Class<?> getStateFieldType() {
        return String.class;
    }

    private Query buildQuery(final Object id, final T stateful) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        final CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        final CriteriaQuery<String> cq = cb.createQuery(String.class);

        final Root<T> t = cq.from(getClazz());
        final Path<?> idPath = t.get(getIdField().getName());
        final Path<String> statePath = t.get(getStateField().getName());

        cq.select(statePath);

        cq.where(cb.equal(idPath, getId(stateful)));

        final Query query = entityManager.createQuery(cq);
        if (JPAPerister.logger.isDebugEnabled()) {
            JPAPerister.logger.debug(query.unwrap(org.hibernate.Query.class).getQueryString());
        }
        return query;
    }
}
