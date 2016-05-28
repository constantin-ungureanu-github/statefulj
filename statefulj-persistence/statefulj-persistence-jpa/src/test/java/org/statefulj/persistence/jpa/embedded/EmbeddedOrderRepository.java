package org.statefulj.persistence.jpa.embedded;

import org.springframework.data.repository.Repository;

public interface EmbeddedOrderRepository extends Repository<EmbeddedOrder, EmbeddedOrderId> {
    EmbeddedOrder save(EmbeddedOrder order);

    EmbeddedOrder findOne(EmbeddedOrderId id);
}
