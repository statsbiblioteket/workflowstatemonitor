/*
 * #%L
 * Workflow state monitor
 * %%
 * Copyright (C) 2012 The State and University Library, Denmark
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package dk.statsbiblioteket.mediaplatform.workflowstatemonitor;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A state manager backed by a hibernated database. */
public class HibernatedStateManager implements StateManager {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void addState(String entityName, State state) {
        try {
            log.trace("Enter addState(entityName='{}',state='{}')", entityName, state);
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();

                Entity entity = (Entity) session.createQuery("from Entity where name = '" + entityName + "'")
                        .uniqueResult();
                if (entity == null) {
                    entity = new Entity();
                    entity.setName(entityName);
                    session.save(entity);
                }
                state.setEntity(entity);
                if (state.getDate() == null) {
                    state.setDate(new Date());
                }
                session.save(state);
                session.getTransaction().commit();
                log.debug("Added state '{}'", state);
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
            log.trace("Exit addState(entityName='{}',state='{}')", entityName, state);
        } catch (RuntimeException e) {
            log.error("Failed addState(entityName='{}',state='{}')", new Object[]{entityName, state, e});
            throw e;
        }
    }

    @Override
    public List<Entity> listEntities() {
        try {
            log.trace("Enter listEntities()");
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            List<Entity> entities;
            try {
                session.beginTransaction();

                entities = session.createQuery("from Entity").list();
                session.getTransaction().commit();
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
            log.trace("Exit listEntities()->entities='{}'", entities.toString());
            return entities;
        } catch (RuntimeException e) {
            log.error("Failed listEntities(): '{}'", e);
            throw e;
        }
    }

    @Override
    public List<State> listStates(String entityName, boolean onlyLast, List<String> includes, List<String> excludes,
                                  Date startDate, Date endDate) {
        try {
            log.trace("Enter listStates(entityName='{}')", entityName);
            List<State> states = queryStates(entityName, onlyLast, includes, excludes, startDate, endDate);
            log.trace("Exit listStates(entityName='{}')->states='{}'", entityName, states);
            return states;
        } catch (RuntimeException e) {
            log.error("Failed listStates(entityName='{}')", entityName, e);
            throw e;
        }
    }

    @Override
    public List<State> listStates(boolean onlyLast, List<String> includes, List<String> excludes, Date startDate,
                                  Date endDate) {
        try {
            log.trace("Enter listStates(onlyLast='{}', includes='{}', excludes='{}')",
                      new Object[]{onlyLast, includes, excludes});
            List<State> states = queryStates(null, onlyLast, includes, excludes, startDate, endDate);
            log.trace("Exit listStates(onlyLast='{}', includes='{}', excludes='{}') -> states='{}'",
                      new Object[]{onlyLast, includes, excludes, states});
            return states;
        } catch (RuntimeException e) {
            log.error("Failed listStates(onlyLast='{}', includes='{}', excludes='{}')",
                      new Object[]{onlyLast, includes, excludes, e});
            throw e;
        }
    }

    private List<State> queryStates(String entityName, boolean onlyLast, List<String> includes, List<String> excludes,
                                    Date startDate, Date endDate) {

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        List<State> states;
        try {
            session.beginTransaction();
            states = buildQuery(session, entityName, onlyLast, includes, excludes, startDate, endDate).list();
            session.getTransaction().commit();
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
        return states;
    }

    private Query buildQuery(Session session, String entityName, boolean onlyLast, List<String> includes,
                             List<String> excludes, Date startDate, Date endDate) {
        StringBuilder query = new StringBuilder();
        Map<String, Object> parameters = new HashMap<String, Object>();
        //TODO: Escape SQL
        if (entityName != null) {
            initNextClause(query);
            query.append("s.entity.name = :entityName");
            parameters.put("entityName", entityName);
        }

        if (onlyLast) {
            initNextClause(query);
            query.append("s.date = (SELECT MAX(s2.date) FROM State s2 WHERE s.entity.id = s2.entity.id)");
        }

        if (includes != null && includes.size() != 0) {
            initNextClause(query);
            query.append("s.stateName IN (:includes)");
            parameters.put("includes", includes);
        }

        if (excludes != null && excludes.size() != 0) {
            initNextClause(query);
            query.append("NOT s.stateName IN (:excludes)");
            parameters.put("excludes", excludes);
        }

        if (startDate != null) {
            initNextClause(query);
            query.append("s.date > :startDate");
            parameters.put("startDate", startDate);
        }

        if (endDate != null) {
            initNextClause(query);
            query.append("s.date < :endDate");
            parameters.put("endDate", endDate);
        }

        log.debug("Query: '{}' Parameters: '{}'", query, parameters);
        Query sessionQuery = session.createQuery("SELECT s FROM State s " + query.toString());
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            if (parameter.getValue() instanceof Collection) {
                sessionQuery.setParameterList(parameter.getKey(), (Collection) parameter.getValue());
            } else {
                sessionQuery.setParameter(parameter.getKey(), parameter.getValue());
            }
        }
        return sessionQuery;
    }

    private void initNextClause(StringBuilder query) {
        if (query.length() > 0) {
            query.append(" AND ");
        } else {
            query.append("WHERE ");
        }
    }
}
