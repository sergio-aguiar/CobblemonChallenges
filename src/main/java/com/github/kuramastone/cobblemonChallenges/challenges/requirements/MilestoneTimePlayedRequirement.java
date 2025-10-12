package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.events.Played1SecondEvent;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.scoreboard.ChallengeScoreboard;

import java.util.UUID;

public class MilestoneTimePlayedRequirement implements Requirement {

    public static final String ID = "milestone_time_played";

    @YamlKey("total-seconds")
    private int totalTime = 1;

    public MilestoneTimePlayedRequirement() {
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile, Challenge parentChallenge) {
        return new MilestoneTimePlayedProgression(profile, this, parentChallenge);
    }

    // Progression class to track time played progress
    public static class MilestoneTimePlayedProgression implements Progression<Played1SecondEvent> {

        private PlayerProfile profile;
        private MilestoneTimePlayedRequirement requirement;
        private int progressAmount;
        private Challenge parentChallenge;

        public MilestoneTimePlayedProgression(PlayerProfile profile, MilestoneTimePlayedRequirement requirement, Challenge parentChallenge) {
            this.profile = profile;
            this.requirement = requirement;
            this.parentChallenge = parentChallenge;
            this.progressAmount = 0;
        }

        @Override
        public Class<Played1SecondEvent> getType() {
            return Played1SecondEvent.class;
        }

        @Override
        public boolean meetsCriteria(Played1SecondEvent event) {
            // Each event fired counts for 30 seconds of playtime
            return true;
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.totalTime;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria(getType().cast(obj))) {
                    progressAmount += 1;
                    progressAmount = Math.min(progressAmount, this.requirement.totalTime);

                    ChallengeScoreboard.updateIfTracking(profile, parentChallenge.getName());
                }
            }
        }

        @Override
        public boolean matchesMethod(Object obj) {
            return getType().isInstance(obj);
        }

        @Override
        public double getPercentageComplete() {
            return (double) progressAmount / requirement.totalTime;
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
                    "{target}", String.valueOf(this.requirement.totalTime)).getText();
        }
    }
}