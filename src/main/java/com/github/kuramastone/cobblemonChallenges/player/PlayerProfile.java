package com.github.kuramastone.cobblemonChallenges.player;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.CompletedChallenge;
import com.github.kuramastone.cobblemonChallenges.challenges.reward.Reward;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeListGUI;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayerProfile {

    private CobbleChallengeAPI api;
    private MinecraftServer server;
    private UUID uuid;

    private @Nullable ServerPlayer playerEntity;
    private Map<String, List<ChallengeProgress>> activeChallenges; // active challenges per list
    private Map<String, Map<Integer, ChallengeProgress>> activeSlotChallenges; // active challenges per list per slot
    private Map<String, Map<Integer, Challenge>> availableSlotChallenges; // challenges available to do for each slot
    private Map<String, ChallengeListGUI> windowGUIMap;
    private List<CompletedChallenge> completedChallenges;
    private List<Reward> rewardsToGive;

    public PlayerProfile(CobbleChallengeAPI api, UUID uuid) {
        this.api = api;
        this.uuid = uuid;

        activeChallenges = Collections.synchronizedMap(new HashMap<>());
        activeSlotChallenges = Collections.synchronizedMap(new LinkedHashMap<>());
        availableSlotChallenges = Collections.synchronizedMap(new HashMap<>());
        windowGUIMap = Collections.synchronizedMap(new HashMap<>());
        completedChallenges = Collections.synchronizedList(new ArrayList<>());
        rewardsToGive = Collections.synchronizedList(new ArrayList<>());

        server = CobbleChallengeMod.getMinecraftServer();
        syncPlayer(); // try syncing player object
    }

    public void setWindowGUI(String listName, ChallengeListGUI gui) {
        if (listName == null || gui == null) return;
        windowGUIMap.put(listName, gui);
    }

    public void openWindowGUIForList(String listName) {
        ChallengeListGUI gui = windowGUIMap.get(listName);
        if (gui != null) gui.open();
    }
    
    public boolean containsWindowGUIForList(String listName) { 
        return windowGUIMap.containsKey(listName); 
    }

    public boolean isOnline() {
        syncPlayer();
        return playerEntity != null;
    }

    public void setCompletedChallenges(List<CompletedChallenge> completedChallenges) {
        this.completedChallenges = completedChallenges;
    }

    public void syncPlayer() {
        playerEntity = server.getPlayerList().getPlayer(uuid);
    }

    public UUID getUUID() {
        return uuid;
    }

    public Set<ChallengeProgress> getActiveChallenges() {
        Set<ChallengeProgress> set = new HashSet<>();

        if (api.getConfigOptions().isUsingPools()) {
            for (Map<Integer, ChallengeProgress> slotMap : activeSlotChallenges.values()) {
                set.addAll(slotMap.values());
            }
        } else {
            for (List<ChallengeProgress> value : activeChallenges.values()) {
                set.addAll(value);
            }
        }

        return set;
    }

    public Map<Integer, ChallengeProgress> getActiveSlotChallenges(String listName) {
        return activeSlotChallenges.computeIfAbsent(listName, k -> new LinkedHashMap<>());
    }

    public Map<String, List<ChallengeProgress>> getActiveChallengesMap() {
        return activeChallenges;
    }

    public Map<String, Map<Integer, ChallengeProgress>> getActiveSlotChallengesMap()
    {
        return activeSlotChallenges;
    }

    /**
     * Challenges that dont require selection are added to player's progression if they dont exist
     */
    public void addUnrestrictedChallenges() {

        for (ChallengeList challengeList : new ArrayList<>(api.getChallengeLists())) {
            if (api.getConfigOptions().isUsingPools()) {
                for (Map.Entry<Integer, List<Challenge>> slotPool : challengeList.getSlotPools().entrySet()) {
                    for (Challenge challenge : slotPool.getValue()) {
                        if (!challenge.doesNeedSelection()) {
                            if (!isChallengeCompleted(challenge.getName())) {
                                if (!isChallengeInProgress(challenge.getName())) {
                                    addActiveChallenge(challengeList, challenge, challenge.getSlot());
                                }
                            }
                        }
                    }
                }
            } else {
                for (Challenge challenge : new ArrayList<>(challengeList.getChallengeMap())) {
                    if (!challenge.doesNeedSelection()) {
                        if (!isChallengeCompleted(challenge.getName())) {
                            if (!isChallengeInProgress(challenge.getName())) {
                                addActiveChallenge(challengeList, challenge, 0);
                            }
                        }
                    }
                }
            }
            checkCompletion(challengeList);
        }

    }

    public void addActiveChallenge(ChallengeList list, Challenge challenge, int slot) {
        if (api.getConfigOptions().isUsingPools()) {
            if (!list.getChallengesForSlot(slot).contains(challenge)) {
                throw new RuntimeException(String.format("This challenge '%s' is not contained by this challenge list '%s' on slot '%d'.", list.getName(), challenge.getName(), slot));
            }

            if (slot <= 0) {
                throw new RuntimeException("Challenge " + challenge.getName() + " has invalid slot: " + slot);
            }

            Map<Integer, ChallengeProgress> slots = activeSlotChallenges.computeIfAbsent(list.getName(), k -> new LinkedHashMap<>());

            int max = list.getMaxChallengesPerPlayer();
            if (slots.size() >= max) {
                Integer firstKey = slots.keySet().iterator().next();
                if (firstKey != null) slots.remove(firstKey);
            }

            ChallengeProgress progress = list.buildNewProgressForQuest(challenge, this);
            slots.put(slot, progress);
        } else {
            if (!list.getChallengeMap().contains(challenge)) {
                throw new RuntimeException(String.format("This challenge '%s' is not contained by this challenge list '%s'.", list.getName(), challenge.getName()));
            }

            List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(list.getName(), (key) -> new ArrayList<>());
            if (!progressInList.isEmpty() && getChallengesInProgress() >= list.getMaxChallengesPerPlayer())
                progressInList.removeFirst();

            progressInList.add(list.buildNewProgressForQuest(challenge, this));
        }
    }



    private int getChallengesInProgress() {
        int count = 0;
        for (List<ChallengeProgress> cpList : this.activeChallenges.values()) {
            for (ChallengeProgress challengeProgress : cpList) {
                if (challengeProgress.getActiveChallenge().doesNeedSelection()) {
                    count++;
                }
            }
        }
        return count;
    }

    public void addActiveChallenge(ChallengeProgress cp) {
        List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(cp.getParentList().getName(), (key) -> new ArrayList<>());
        // only add if they dont have it already
        if (progressInList.stream().noneMatch(it -> it.getActiveChallenge().getName().equalsIgnoreCase(cp.getActiveChallenge().getName())))
            progressInList.add(cp);
    }

    public ServerPlayer getPlayerEntity() {
        if (playerEntity == null || playerEntity.isRemoved()) {
            syncPlayer();
        }

        return playerEntity;
    }

    public List<Reward> getRewardsToGive() {
        return rewardsToGive;
    }

    public void completeChallenge(ChallengeList list, Challenge challenge) {
        //double check that it isnt already completed
        if (isChallengeCompleted(challenge.getName()))
            return;

        rewardsToGive.addAll(challenge.getRewards());

        dispenseRewards();
        List<String> linesToSend = List.of(StringUtils.splitByLineBreak(api.getMessage("challenges.completed", "{challenge}", challenge.getDisplayName(), "{challenge-description}", challenge.getDescription()).getText()));
        List<String> formattedLines = StringUtils.centerStringListTags(linesToSend);
        for (String line : formattedLines) {
            sendMessage(ComponentEditor.decorateComponent(line));
        }
        CobbleChallengeMod.logger.info("{} has completed the {} challenge!",
                isOnline() ? playerEntity.getName().getString() : uuid.toString(),
                challenge.getName());

        addCompletedChallenge(list, challenge);
    }

    private void dispenseRewards() {
        syncPlayer();
        if (playerEntity != null) {
            for (Reward reward : rewardsToGive) {
                try {
                    if (reward != null)
                        reward.applyTo(playerEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            rewardsToGive.clear();
        }
    }

    public void sendMessage(Collection<String> text) {
        for (String s : text) {
            sendMessage(s);
        }
    }

    public void sendMessage(String text, Object... replacements) {
        if (isOnline()) {
            ComponentEditor editor = new ComponentEditor(text);
            for (int i = 0; i < replacements.length; i += 2) {
                editor.replace(replacements[i].toString(), replacements[i + 1].toString());
            }
            playerEntity.displayClientMessage(FabricAdapter.adapt(editor.build()), false);
        }
    }

    public void sendMessage(Component comp) {
        if (isOnline()) {
            playerEntity.displayClientMessage(FabricAdapter.adapt(comp), false);
        }
    }

    public void removeActiveChallenge(ChallengeProgress challengeProgress) {
        String listName = challengeProgress.getParentList().getName();

        if (api.getConfigOptions().isUsingPools()) {
            Map<Integer, ChallengeProgress> slots = activeSlotChallenges.get(listName);
            if (slots != null) {
                slots.values().removeIf(cp -> cp.equals(challengeProgress));
            }
        } else {
            List<ChallengeProgress> list = activeChallenges.get(listName);
            if (list != null) {
                list.remove(challengeProgress);
            }
        }
    }

    public boolean isChallengeCompleted(String challengeID) {
        for (CompletedChallenge completedChallenge : this.completedChallenges) {
            if (completedChallenge.challengeID().equalsIgnoreCase(challengeID)) {
                return true;
            }
        }
        return false;
    }

    public void addCompletedChallenge(ChallengeList list, Challenge challenge) {
        if (!isChallengeCompleted(challenge.getName())) {
            completedChallenges.add(new CompletedChallenge(list.getName(), challenge.getName(), System.currentTimeMillis()));
        }
    }

    public boolean isChallengeInProgress(String challengeName) {
        return getActiveChallengeProgress(challengeName) != null;
    }

    public ChallengeProgress getActiveChallengeProgress(String challengeName) {
        for (ChallengeProgress challenge : getActiveChallenges()) {
            if (challenge.getActiveChallenge().getName().equals(challengeName)) {
                return challenge;
            }
        }
        return null;
    }

    public List<CompletedChallenge> getCompletedChallenges() {
        return completedChallenges;
    }

    public void checkCompletion(ChallengeList challengeList) {
        List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(challengeList.getName(), (key) -> new ArrayList<>());
        // progress initially to see if it is auto-done
        for (ChallengeProgress cp : new ArrayList<>(progressInList)) {
            cp.progress(null);
        }

    }

    /**
     * Remove {@link CompletedChallenge}s if they are repeatable and the repeat time has been reached
     */
    public void refreshRepeatableChallenges() {
        for (CompletedChallenge data : new ArrayList<>(completedChallenges)) {
            ChallengeList challengeList = api.getChallengeList(data.challengeListID());
            if (challengeList != null) {
                Challenge challenge = challengeList.getChallenge(data.challengeID());
                if (challenge != null) {
                    if (challenge.isRepeatable()) {
                        long timeSinceCompleted = System.currentTimeMillis() - data.timeCompleted();
                        if (timeSinceCompleted >= challenge.getRepeatableEveryMilliseconds()) {
                            completedChallenges.remove(data);
                        }
                    }
                }
            }
        }
    }

    public void resetExpiredChallenges() {
        for (ChallengeList challengeList : new ArrayList<>(api.getChallengeLists())) {
            for (ChallengeProgress cp : new ArrayList<>(activeChallenges.getOrDefault(challengeList.getName(), Collections.emptyList()))) {
                if (cp.hasTimeRanOut()) {
                    CobbleChallengeMod.logger.info("Resetting expired challenge {} for player {}", cp.getActiveChallenge().getName(), uuid);
                    activeChallenges.get(challengeList.getName()).remove(cp);
                }
            }
        }
    }

    public void resetChallenges() {
        resetProgress();
        completedChallenges.clear();
        rewardsToGive.clear();

        addUnrestrictedChallenges();
    }

    private void resetProgress() {
        activeChallenges.clear();
        activeSlotChallenges.clear();
    }

    public ChallengeProgress getProgressForSlot(String listName, int slot) {
        return activeSlotChallenges
                .getOrDefault(listName, Collections.emptyMap())
                .get(slot);
    }

    public void setProgressForSlot(String listName, int slot, ChallengeProgress progress) {
        activeSlotChallenges
            .computeIfAbsent(listName, k -> new LinkedHashMap<>())
            .put(slot, progress);
    }

    public void removeProgressForSlot(String listName, int slot) {
        Map<Integer, ChallengeProgress> slots = activeSlotChallenges.get(listName);
        if (slots != null) {
            slots.remove(slot);
        }
    }

    public void setSlotChallenge(String list, int slot, Challenge challenge) {
        availableSlotChallenges
            .computeIfAbsent(list, k -> new HashMap<>())
            .put(slot, challenge);
    }

    public Map<String, Map<Integer, Challenge>> getAvailableSlotChallenges() {
        return availableSlotChallenges;
    }

    public Map<Integer, Challenge> getAvailableSlotChallengesForList(String listName) {
        return availableSlotChallenges.getOrDefault(listName, Collections.emptyMap());
    }

    public Challenge getAvailableSlotChallenge(String listName, int slot) {
        return availableSlotChallenges
            .computeIfAbsent(listName, k -> new HashMap<>())
            .get(slot);
    }

    public void setAvailableSlotChallenge(String listName, int slot, Challenge challenge) {
        availableSlotChallenges
            .computeIfAbsent(listName, k -> new HashMap<>())
            .put(slot, challenge);
    }

    public void setAvailableChallengesForList(String listName, Map<Integer, Challenge> availableChallenges) {
        availableSlotChallenges.put(listName, availableChallenges);
    }

    public void AddDefaultSlotChallenges(PlayerProfile profile)
    {
        if (api.getConfigOptions().isUsingPools()) {
            for (ChallengeList list : api.getChallengeLists()) {
                String listName = list.getName();
                for (Map.Entry<Integer, List<Challenge>> slotPool : list.getSlotPools().entrySet()) {
                    int slot = slotPool.getKey();

                    if (profile.getAvailableSlotChallenge(listName, slot) == null &&
                        profile.getProgressForSlot(listName, slot) == null) {
                        List<Challenge> pool = slotPool.getValue();
                        if (!pool.isEmpty()) {
                            profile.setAvailableSlotChallenge(listName, slot, pool.getFirst());
                        }
                    }
                }
            }
        }
    }
}
