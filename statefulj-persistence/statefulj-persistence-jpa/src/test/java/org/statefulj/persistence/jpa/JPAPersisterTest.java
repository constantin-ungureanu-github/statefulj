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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.StaleStateException;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.jpa.model.StatefulEntity;
import org.statefulj.persistence.jpa.utils.UnitTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/applicationContext-JPAPersisterTests.xml" })
public class JPAPersisterTest {

    @Resource
    Persister<Order> jpaPersister;

    @Resource
    OrderRepository orderRepo;

    @Resource
    JpaTransactionManager transactionManager;

    @Resource
    State<Order> stateA;

    @Resource
    State<Order> stateB;

    @Resource
    State<Order> stateC;

    @Test
    public void testValidStateChange() throws StaleStateException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        UnitTestUtils.startTransaction(transactionManager);

        // Verify that a new Order without a state set, we return the Start State
        //
        Order order = new Order();
        order.setAmount(20);
        order = this.orderRepo.save(order);

        State<Order> currentState = jpaPersister.getCurrent(order);
        assertEquals(stateA, currentState);

        // Verify that qualified a change in state works
        //
        jpaPersister.setCurrent(order, stateA, stateB);
        assertEquals(order.getState(), stateB.getName());

        Order dbOrder = orderRepo.findOne(order.getId());
        assertEquals(order.getState(), dbOrder.getState());

        jpaPersister.setCurrent(order, stateB, stateC);
        assertEquals(order.getState(), stateC.getName());

        dbOrder = orderRepo.findOne(order.getId());
        assertEquals(order.getState(), dbOrder.getState());

        // Verify that updating the state without going through the Persister doesn't wok
        //
        order = new Order();
        jpaPersister.setCurrent(order, stateA, stateB);
        order = this.orderRepo.save(order);

        dbOrder = orderRepo.findOne(order.getId());
        currentState = jpaPersister.getCurrent(dbOrder);
        assertEquals(stateB, currentState);

        Field stateField = StatefulEntity.class.getDeclaredField("state");
        stateField.setAccessible(true);
        stateField.set(dbOrder, "stateD");
        dbOrder.setAmount(100);
        this.orderRepo.save(dbOrder);

        UnitTestUtils.commitTransaction(transactionManager);
        UnitTestUtils.startTransaction(transactionManager);

        dbOrder = this.orderRepo.findOne(dbOrder.getId());
        currentState = jpaPersister.getCurrent(dbOrder);
        assertEquals(stateB.getName(), currentState.getName());
        assertEquals(100, dbOrder.getAmount());

        UnitTestUtils.commitTransaction(transactionManager);
    }

    @Test(expected = StaleStateException.class)
    public void testInvalidStateChange() throws StaleStateException {
        UnitTestUtils.startTransaction(transactionManager);
        Order order = new Order();
        order.setAmount(20);
        orderRepo.save(order);

        State<Order> currentState = jpaPersister.getCurrent(order);
        assertEquals(stateA, currentState);

        jpaPersister.setCurrent(order, stateB, stateC);
        UnitTestUtils.commitTransaction(transactionManager);
    }
}
