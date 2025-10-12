package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.events.PlayerVoteEvent;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.scoreboard.ChallengeScoreboard;

import java.util.UUID;

public class VoteRequirement implements Requirement {
    public static final String ID = "Player_Vote";

    @YamlKey("amount")
    private int amount = 1;

    @YamlKey("service")
    private String service = "any";

    public VoteRequirement() {}

    @Override
    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile, Challenge parentChallenge) {
        return new VoteProgression(profile, this, parentChallenge);
    }

    public static class VoteProgression implements Progression<PlayerVoteEvent> {
        private final PlayerProfile profile;
        private final VoteRequirement requirement;
        private int progressAmount;
        private Challenge parentChallenge;

        public VoteProgression(PlayerProfile profile, VoteRequirement requirement, Challenge parentChallenge) {
            this.profile = profile;
            this.requirement = requirement;
            this.parentChallenge = parentChallenge;
            this.progressAmount = 0;
        }

        @Override
        public Class<PlayerVoteEvent> getType() {
            return PlayerVoteEvent.class;
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                PlayerVoteEvent event = getType().cast(obj);

                if (!event.getUsername().equalsIgnoreCase(profile.getPlayerEntity().getGameProfile().getName())) {
                    return;
                }

                if (meetsCriteria(event)) {
                    progressAmount++;

                    ChallengeScoreboard.updateIfTracking(profile, parentChallenge.getName());
                }
            }
        }

        @Override
        public boolean meetsCriteria(PlayerVoteEvent event) {
            return requirement.service.equalsIgnoreCase("any") 
                || (event.getService() != null && event.getService().equalsIgnoreCase(requirement.service));
        }

        @Override
        public boolean matchesMethod(Object obj) {
            return getType().isInstance(obj);
        }

        @Override
        public double getPercentageComplete() {
            return (double) progressAmount / requirement.amount;
        }

        @Override
        public Progression loadFrom(UUID uuid, YamlConfig configurationSection) {
            this.progressAmount = configurationSection.getInt("progressAmount");
            return this;
        }

        @Override
        public void writeTo(YamlConfig configurationSection) {
            configurationSection.set("progressAmount", progressAmount);
        }

        @Override
        public String getProgressString() {
            return CobbleChallengeMod.instance.getAPI().getMessage("challenges.progression-string",
                    "{current}", String.valueOf(this.progressAmount),
                    "{target}", String.valueOf(this.requirement.amount)).getText();
        }
    }
}
