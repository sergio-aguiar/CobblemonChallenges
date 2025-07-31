package com.github.kuramastone.cobblemonChallenges.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

// Call this when the server is starting, not after
public interface RegisterRequirementsEvent {

    Event<RegisterRequirementsEvent> EVENT = EventFactory.createArrayBacked(RegisterRequirementsEvent.class, (callbacks) -> () -> {
        for (RegisterRequirementsEvent event : callbacks) {
            event.onRegistration();
        }

    });

    void onRegistration();

}
