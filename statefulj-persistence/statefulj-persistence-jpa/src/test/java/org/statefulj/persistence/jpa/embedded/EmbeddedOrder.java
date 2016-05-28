package org.statefulj.persistence.jpa.embedded;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.statefulj.persistence.jpa.model.StatefulEntity;

@Entity
@Table(name = "EmbeddedOrders")
public class EmbeddedOrder extends StatefulEntity {
    @EmbeddedId
    private EmbeddedOrderId orderId;

    private int amount;

    public EmbeddedOrder() {
    }

    public EmbeddedOrder(final EmbeddedOrderId orderId) {
        this.orderId = orderId;
    }

    public EmbeddedOrderId getOrderId() {
        return orderId;
    }

    public void setOrderId(final EmbeddedOrderId orderId) {
        this.orderId = orderId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }
}
