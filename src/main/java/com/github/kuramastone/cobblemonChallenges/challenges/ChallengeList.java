package com.github.kuramastone.cobblemonChallenges.challenges;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Requirement;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ChallengeList {

    private CobbleChallengeAPI api;

    private String name;
    private List<Challenge> challengeMap; // legacy mode (no challenge pool)
    private Map<Integer, List<Challenge>> slotPools; // new challenge pool mode
    private int maxChallengesPerPlayer;

    public ChallengeList(CobbleChallengeAPI api, String name, List<Challenge> challengeMap, Map<Integer, List<Challenge>> slotPools, int maxChallengesPerPlayer) {
        this.api = api;
        this.name = name;
        this.challengeMap = challengeMap;
        this.slotPools = slotPools;
        this.maxChallengesPerPlayer = maxChallengesPerPlayer;
    }

    public static ChallengeList load(CobbleChallengeAPI api, String challengeListID, YamlConfig section) {
        Objects.requireNonNull(section, "Cannot load data from a null section.");

        List<Challenge> challengeList = new ArrayList<>();
        Map<Integer, List<Challenge>> slotPool = new LinkedHashMap<>();

        boolean usePools = api.getConfigOptions().isUsingPools();

        for (String challengeID : section.getKeys("challenges", false)) {
            Challenge challenge = Challenge.load(challengeID, section.getSection("challenges." + challengeID));
            if(challenge == null) continue; // something went wrong, skip

            if (usePools) {
                int slot = section.getInt("challenges." + challengeID + ".challenge-slot");

                boolean valid = api.registerChallenge(challenge, challengeListID, slot);
                if (!valid) {
                    CobbleChallengeMod.logger.error("Unable to load duplicate Challenge name '{}' for slot {}. Try renaming it! Ignoring this challenge.", challengeID, slot);
                    continue;
                }

                if (slot > 0) {
                    slotPool.computeIfAbsent(slot, k -> new ArrayList<>()).add(challenge);
                    // CobbleChallengeMod.logger.info("Challenge {} with slot {} has been added to list {}.", challengeID, slot, challengeListID);
                } else {
                    CobbleChallengeMod.logger.warn("Challenge {} is missing a valid slot number in pool mode. Skipping.", challengeID);
                }
            } else {
                boolean valid = api.registerChallenge(challenge, challengeListID, 0);
                if (!valid) {
                    CobbleChallengeMod.logger.error("Unable to load duplicate Challenge name '{}'. Try renaming it! Ignoring this challenge.", challengeID);
                    continue;
                }

                challengeList.add(challenge);
            }
        }
        int maxChallengesPerPlayer = section.get("maxChallengesPerPlayer", 1);

        return new ChallengeList(api, challengeListID, challengeList, slotPool, maxChallengesPerPlayer);
    }

    public String getName() {
        return name;
    }

    public List<Challenge> getChallengeMap() {
        return challengeMap;
    }

    public Map<Integer, List<Challenge>> getSlotPools() {
        return slotPools;
    }

    public boolean hasSlots() {
        return api.getConfigOptions().isUsingPools() && !slotPools.isEmpty();
    }

    public int getMaxChallengesPerPlayer() {
        return maxChallengesPerPlayer;
    }

    public Challenge getChallengeAt(Integer level) {
        return challengeMap.get(level);
    }

    public Challenge getRandomChallengeForSlot(int slot, Random random) {
        List<Challenge> pool = slotPools.get(slot);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    public List<Challenge> getChallengesForSlot(int slot) {
        return slotPools.getOrDefault(slot, Collections.emptyList());
    }

    /**
     * Create a progression for this challenge. This can be used to keep track of a player's progress
     *
     * @param challenge
     * @param profile
     * @return
     */
    public ChallengeProgress buildNewProgressForQuest(Challenge challenge, PlayerProfile profile) {

        List<Pair<String, Progression<?>>> progs = new ArrayList<>();
        for (Requirement requirement : challenge.getRequirements()) {
            progs.add(Pair.of(requirement.getName(), requirement.buildProgression(profile)));
        }

        return new ChallengeProgress(api, profile, this, challenge, progs, System.currentTimeMillis());
    }

    public Challenge getChallenge(String challengeName) {
        if (api.getConfigOptions().isUsingPools()) {
            for (List<Challenge> pool : this.slotPools.values()) {
                for (Challenge challenge : pool) {
                    if (challenge.getName().equals(challengeName)) {
                        return challenge;
                    }
                }
            }
        } else {
            for(Challenge challenge : this.challengeMap) {
                if (challenge.getName().equals(challengeName)) {
                    return challenge;
                }
            }
        }

        return null;
    }

    public Challenge getChallengeBySlot(int slot, String challengeName) {
        if (!api.getConfigOptions().isUsingPools()) {
            // In legacy mode, slots don't exist → just fall back to regular lookup
            return getChallenge(challengeName);
        }

        List<Challenge> pool = slotPools.get(slot);
        if (pool == null || pool.isEmpty()) return null;

        for (Challenge challenge : pool) {
            if (challenge.getName().equals(challengeName)) {
                return challenge;
            }
        }
        return null;
    }
}
