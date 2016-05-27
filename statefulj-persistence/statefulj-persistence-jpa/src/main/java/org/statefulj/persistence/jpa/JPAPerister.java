/***
 *
 * Copyright 2014 Andrew Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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

    /**
     * Set the current State. This method will ensure that the state in the db matches the expected current state. If not, it will throw a StateStateException
     *
     * @param stateful
     *            Stateful Entity
     * @param current
     *            Expected current State
     * @param next
     *            The value of the next State
     * @throws StaleStateException
     *             thrown if the value of the State does not equal to the provided current State
     */
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

    /**
     * @param stateful
     * @param current
     * @param next
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws StaleStateException
     */
    private void updateStateInMemory(final T stateful, final State<T> current, final State<T> next) throws NoSuchFieldException, IllegalAccessException, StaleStateException {
        // The Entity hasn't been persisted to the database - so it exists only this Application memory.
        // So, serialize the qualified update to prevent concurrency conflicts.
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

    /**
     * @param stateful
     * @param current
     * @param next
     * @param id
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws StaleStateException
     */
    private void updateStateInDB(final T stateful, final State<T> current, final State<T> next, final Object id) throws NoSuchFieldException, IllegalAccessException, StaleStateException {
        // Entity is in the database - perform qualified update based off
        // the current State value
        //
        final Query update = buildUpdate(id, stateful, current, next, getIdField(), getStateField());

        // Successful update?
        //
        if (update.executeUpdate() == 0) {

            // If we aren't able to update - it's most likely that we are out of sync.
            // So, fetch the latest value and update the Stateful object. Then throw a RetryException
            // This will cause the event to be reprocessed by the FSM
            //
            final Query query = buildQuery(id, stateful);
            String state = getStart().getName();
            try {
                final TransactionTemplate tt = new TransactionTemplate(transactionManager);
                state = tt.execute(new TransactionCallback<String>() {
                    public String doInTransaction(final TransactionStatus status) {
                        return (String) query.getSingleResult();
                    }

                });
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

        // update <class>
        //
        final CriteriaUpdate<T> cu = cb.createCriteriaUpdate(getClazz());
        final Root<T> t = cu.from(getClazz());

        final Path<?> idPath = t.get(getIdField().getName());
        final Path<String> statePath = t.get(getStateField().getName());

        // set state=<next_state>
        //
        cu.set(statePath, next.getName());

        // where id=<id> and state=<current_state>
        //
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
