package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.api.events.berry.BerryHarvestEvent;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.scoreboard.ChallengeScoreboard;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class HarvestBerryRequirement implements Requirement {
    public static final String ID = "Harvest_Berry";

    @YamlKey("type")
    private String berryType = "any";
    @YamlKey("amount")
    private int amount = 1;

    public HarvestBerryRequirement() {
    }

    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    // The requirement name now returns the ID used to recognize it
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile, Challenge parentChallenge) {
        return new HarvestBerryProgression(profile, this, parentChallenge);
    }

    // Static nested Progression class
    public static class HarvestBerryProgression implements Progression<BerryHarvestEvent> {

        private PlayerProfile profile;
        private HarvestBerryRequirement requirement;
        private int progressAmount;
        private Challenge parentChallenge;

        public HarvestBerryProgression(PlayerProfile profile, HarvestBerryRequirement requirement, Challenge parentChallenge) {
            this.profile = profile;
            this.requirement = requirement;
            this.parentChallenge = parentChallenge;
            this.progressAmount = 0;
        }

        @Override
        public String getProgressString() {
            return CobbleChallengeMod.instance.getAPI().getMessage("challenges.progression-string",
                    "{current}", String.valueOf(this.progressAmount),
                    "{target}", String.valueOf(this.requirement.amount)).getText();
        }

        @Override
        public Class<BerryHarvestEvent> getType() {
            return BerryHarvestEvent.class;
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria(getType().cast(obj))) {
                    for (ItemStack drop : getType().cast(obj).getDrops()) {
                        progressAmount += drop.getCount();
                    }
                    progressAmount = Math.min(progressAmount, this.requirement.amount);

                    ChallengeScoreboard.updateIfTracking(profile, parentChallenge.getName());
                }
            }
        }

        @Override
        public boolean meetsCriteria(BerryHarvestEvent event) {

            String itemName = event.getBerry().item().berry().getIdentifier().toString();

            if (!StringUtils.doesStringContainCategory(requirement.berryType.split("/"), itemName)) {
                return false;
            }

            return true;
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
    }
}