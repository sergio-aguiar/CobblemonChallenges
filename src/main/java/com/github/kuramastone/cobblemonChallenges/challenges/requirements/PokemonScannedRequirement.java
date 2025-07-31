package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.pokedex.scanning.PokemonScannedEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import java.util.UUID;

public class PokemonScannedRequirement implements Requirement {

    public static final String ID = "pokemon_scanned";

    @YamlKey("amount")
    private int amount = 1; // Number of Pokédex entries to complete
    @YamlKey(value = "pokename", required = false)
    private String pokename = "any"; // Number of Pokédex entries to complete

    public PokemonScannedRequirement() {
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
    public Progression<?> buildProgression(PlayerProfile profile) {
        /*
        Do not ask the player to scan more pokemon than possible
         */

        CompletePokedexEntriesProgression ccp = new CompletePokedexEntriesProgression(profile, this);
        int maxPossibleToGain = this.amount;
        try {
            int currentAmount = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(profile.getUUID()).getSpeciesRecords().size(); // 99
            int maxPokedexEntries = PokemonSpecies.INSTANCE.count(); // 100
            maxPossibleToGain = Math.max(0, maxPokedexEntries - currentAmount); // 1
        } catch (Exception e) {
            CobbleChallengeMod.logger.error("Unable to read cobblemon nbt data for player '{}' for the PokemonScannedRequirement. Ignoring their data. Is it corrupted? Error type is: '{}'",
                    profile.getUUID(), e.getClass().getSimpleName());
        }

        // cant ask them to gain more than the max
        if (this.amount > maxPossibleToGain)
            ccp.progressAmount = this.amount - maxPossibleToGain;

        return ccp;
    }

    // Progression class to track progress
    public static class CompletePokedexEntriesProgression implements Progression<PokemonScannedEvent> {

        private PlayerProfile profile;
        private PokemonScannedRequirement requirement;
        private int progressAmount;

        public CompletePokedexEntriesProgression(PlayerProfile profile, PokemonScannedRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<PokemonScannedEvent> getType() {
            return PokemonScannedEvent.class;
        }

        @Override
        public boolean meetsCriteria(PokemonScannedEvent event) {

            if (!StringUtils.doesStringContainCategory(requirement.pokename.split("/"), event.getScannedPokemonEntityData().getPokemon().getSpecies().getName())) {
                return false;
            }

            return true;
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria(getType().cast(obj))) {
                    progressAmount++;
                    progressAmount = Math.min(progressAmount, this.requirement.amount);
                }
            }
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
