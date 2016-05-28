package org.statefulj.persistence.jpa.embedded;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class EmbeddedOrderId implements Serializable {
    private static final long serialVersionUID = 3402769544634761745L;
    private long id;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }
}
