package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.commons.event.CatastropheEvent;

public interface EventPublisher {
    void publish(CatastropheEvent event);
}
