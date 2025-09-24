package com.github.kuramastone.cobblemonChallenges.events;

public class PlayerVoteEvent {

    private final String username;
    private final String service;

    public PlayerVoteEvent(String username, String service) {
        this.username = username;
        this.service = service;
    }

    public String getUsername() {
        return username;
    }

    public String getService() {
        return service;
    }
}
