package com.github.kuramastone.cobblemonChallenges.player;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.CompletedChallenge;
import com.github.kuramastone.cobblemonChallenges.challenges.reward.Reward;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.gui.WindowItem;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeItem;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeListGUI;
import com.github.kuramastone.cobblemonChallenges.scoreboard.ChallengeScoreboard;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

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

    public PlayerProfile(CobbleChallengeAPI api, UUID uuid) {
        this.api = api;
        this.uuid = uuid;

        activeChallenges = Collections.synchronizedMap(new HashMap<>());
        activeSlotChallenges = Collections.synchronizedMap(new LinkedHashMap<>());
        availableSlotChallenges = Collections.synchronizedMap(new HashMap<>());
        windowGUIMap = Collections.synchronizedMap(new HashMap<>());
        completedChallenges = Collections.synchronizedList(new ArrayList<>());

        server = CobbleChallengeMod.getMinecraftServer();
        syncPlayer(); // try syncing player object
    }

    public Map<String, ChallengeListGUI> getWindowGUIMap() {
        return windowGUIMap;
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

    public Set<ChallengeProgress> getActiveChallenges(String listName) {
        Set<ChallengeProgress> set = new HashSet<>();

        if (api.getConfigOptions().isUsingPools()) {
            for (Map<Integer, ChallengeProgress> slotMap : activeSlotChallenges.values()) {
                for (ChallengeProgress progress : slotMap.values()) {
                    if (progress.getParentList().getName().equals(listName)) {
                        set.add(progress);
                    }
                }
            }
        } else {
            for (List<ChallengeProgress> value : activeChallenges.values()) {
                for (ChallengeProgress progress : value) {
                    if (progress.getParentList().getName().equals(listName)) {
                        set.add(progress);
                    }
                }
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
                                    Map<Integer, Challenge> slotChallenges = availableSlotChallenges.getOrDefault(challengeList.getName(), Collections.emptyMap());
                                    Challenge slotChallenge = slotChallenges.get(challenge.getSlot());
                                    if (slotChallenge != null && slotChallenge.getName().equals(challenge.getName())) {
                                        CobbleChallengeMod.logger.info("Player %s started unrestricted challenge %s.".formatted(playerEntity.getName().getString(), challenge.getName()));
                                        addActiveChallenge(challengeList, challenge, challenge.getSlot());
                                    }
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

            boolean found = false;
            for (Challenge slotChallenge : list.getChallengesForSlot(slot)) {
                if (slotChallenge.getName().equals(challenge.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new RuntimeException(String.format("This challenge '%s' is not contained by this challenge list '%s' on slot '%d'.", challenge.getName(), list.getName(), slot));
            }

            if (slot <= 0) {
                throw new RuntimeException("Challenge " + challenge.getName() + " has invalid slot: " + slot);
            }

            Map<Integer, ChallengeProgress> slots = activeSlotChallenges.computeIfAbsent(list.getName(), k -> new LinkedHashMap<>());

            if (challenge.doesNeedSelection()) {
                if (!slots.isEmpty() && getSlotChallengesInProgress(list.getName()) >= list.getMaxChallengesPerPlayer()) {
                    for (Iterator<Map.Entry<Integer, ChallengeProgress>> it = slots.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<Integer, ChallengeProgress> entry = it.next();
                        if (entry.getValue().getActiveChallenge().doesNeedSelection()) {
                            ChallengeProgress progress = ChallengeScoreboard.getTrackedChallenge(playerEntity);

                            if (progress != null) {
                                Challenge progressChallenge = progress.getActiveChallenge();
                                
                                if (progressChallenge != null && progressChallenge.getName().equals(entry.getValue().getActiveChallenge().getName())) {
                                    ChallengeScoreboard.clearForPlayer(playerEntity, true);
                                }
                            }

                            it.remove();
                            break;
                        }
                    }
                }
            }

            ChallengeProgress progress = list.buildNewProgressForQuest(challenge, this);
            slots.put(slot, progress);
        } else {
            boolean found = false;
            for (Challenge legacyChallenge : list.getChallengeMap()) {
                if (legacyChallenge.getName().equals(challenge.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new RuntimeException(String.format("This challenge '%s' is not contained by this challenge list '%s'.", list.getName(), challenge.getName()));
            }

            List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(list.getName(), (key) -> new ArrayList<>());

            if (challenge.doesNeedSelection()) {
                if (!progressInList.isEmpty() && getChallengesInProgress(list.getName()) >= list.getMaxChallengesPerPlayer()) {
                    for (Iterator<ChallengeProgress> it = progressInList.iterator(); it.hasNext(); ) {
                        ChallengeProgress cp = it.next();
                        if (cp.getActiveChallenge().doesNeedSelection()) {
                            if (ChallengeScoreboard.getTrackedChallenge(playerEntity).getActiveChallenge().getName().equals(cp.getActiveChallenge().getName())) {
                                ChallengeScoreboard.clearForPlayer(playerEntity, true);
                            }

                            it.remove();
                            break;
                        }
                    }
                }
            }

            progressInList.add(list.buildNewProgressForQuest(challenge, this));
        }

        handleOverflowingActiveChallenges();
    }



    private int getChallengesInProgress(String listName) {
        int count = 0;
        
        List<ChallengeProgress> challenges = activeChallenges.get(listName);
        if (challenges == null) {
            return 0;
        }

        for (ChallengeProgress challengeProgress : challenges) {
            if (challengeProgress.getActiveChallenge().doesNeedSelection()) {
                count++;
            }
        }
        
        return count;
    }

    private int getSlotChallengesInProgress(String listName) {
        int count = 0;

        Map<Integer, ChallengeProgress> slotMap = activeSlotChallenges.get(listName);
        if (slotMap == null) {
            return 0;
        }

        for (ChallengeProgress challengeProgress : this.activeSlotChallenges.get(listName).values()) {
            if (challengeProgress.getActiveChallenge().doesNeedSelection()) {
                count++;
            }
        }
        return count;
    }

    public void addActiveChallenge(ChallengeProgress cp) {
        if (api.getConfigOptions().isUsingPools()) {
            Map<Integer, ChallengeProgress> progressInSlots = this.activeSlotChallenges.computeIfAbsent(cp.getParentList().getName(), (key) -> new LinkedHashMap<>());
            // only add if they dont have it already
            if (progressInSlots.values().stream().noneMatch(it -> it.getActiveChallenge().getName().equalsIgnoreCase(cp.getActiveChallenge().getName())))
                progressInSlots.put(cp.getActiveChallenge().getSlot(), cp);
        } else {
            List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(cp.getParentList().getName(), (key) -> new ArrayList<>());
            // only add if they dont have it already
            if (progressInList.stream().noneMatch(it -> it.getActiveChallenge().getName().equalsIgnoreCase(cp.getActiveChallenge().getName())))
                progressInList.add(cp);
        }

        handleOverflowingActiveChallenges();
    }

    private void handleOverflowingActiveChallenges() {
        for (ChallengeList list : api.getChallengeLists()) {
            if (api.getConfigOptions().isUsingPools()) {
                while (!activeSlotChallenges.isEmpty() && getSlotChallengesInProgress(list.getName()) > list.getMaxChallengesPerPlayer()) {
                    activeSlotChallenges.remove(activeSlotChallenges.keySet().iterator().next());
                }
            } else {
                while (!activeChallenges.isEmpty() && getChallengesInProgress(list.getName()) > list.getMaxChallengesPerPlayer()) {
                    activeChallenges.get(list.getName()).remove(0);
                }
            }
        }
    }

    public ServerPlayer getPlayerEntity() {
        if (playerEntity == null || playerEntity.isRemoved()) {
            syncPlayer();
        }

        return playerEntity;
    }

    public synchronized void completeChallenge(ChallengeList list, Challenge challenge) {
        if (isChallengeCompleted(challenge.getName()))
            return;

        addCompletedChallenge(list, challenge);

        dispenseRewards(challenge.getRewards());

        List<String> linesToSend = List.of(StringUtils.splitByLineBreak(api.getMessage("challenges.completed", "{challenge}", challenge.getDisplayName(), "{challenge-description}", challenge.getDescription()).getText()));
        List<String> formattedLines = StringUtils.centerStringListTags(linesToSend);
        for (String line : formattedLines) {
            sendMessage(ComponentEditor.decorateComponent(line));
        }
        CobbleChallengeMod.logger.info("{} has completed the {} challenge!",
                isOnline() ? playerEntity.getName().getString() : uuid.toString(),
                challenge.getName());
    }

    private synchronized void dispenseRewards(List<Reward> rewardsToGive) {
        try {
            syncPlayer();
            if (playerEntity == null) return;

            for (Reward reward : rewardsToGive) {
                if (reward != null) reward.applyTo(playerEntity);
            }
        } catch (Exception e) {
            CobbleChallengeMod.logger.info("Failed to distribute rewards to player %s (%s): %s".formatted(getUUID().toString(), getPlayerEntity().getName().getString(), e.getMessage()));
            e.printStackTrace();
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
        if (api.getConfigOptions().isUsingPools()) {
            for (Map<Integer, ChallengeProgress> slotMap : activeSlotChallenges.values()) {
                for (ChallengeProgress challenge : slotMap.values()) {
                    if (challenge.getActiveChallenge().getName().equals(challengeName)) {
                        return challenge;
                    }
                }
            }
            return null;
        } else {
            for (ChallengeProgress challenge : getActiveChallenges()) {
                if (challenge.getActiveChallenge().getName().equals(challengeName)) {
                    return challenge;
                }
            }
            return null;
        }
    }

    public List<CompletedChallenge> getCompletedChallenges() {
        return completedChallenges;
    }

    public void checkCompletion(ChallengeList challengeList) {
        if (api.getConfigOptions().isUsingPools()) {
            Map<Integer, ChallengeProgress> slotProgressMap = this.activeSlotChallenges.computeIfAbsent(challengeList.getName(), k -> new LinkedHashMap<>());
            for (ChallengeProgress cp : new ArrayList<>(slotProgressMap.values())) {
                cp.progress(null);
            }
        } else {
            List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(challengeList.getName(), (key) -> new ArrayList<>());
            for (ChallengeProgress cp : new ArrayList<>(progressInList)) {
                cp.progress(null);
            }
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
        if (api.getConfigOptions().isUsingPools()) {
            for (ChallengeList challengeList : new ArrayList<>(api.getChallengeLists())) {
                Map<Integer, ChallengeProgress> slotProgressMap = activeSlotChallenges.getOrDefault(challengeList.getName(), Collections.emptyMap());
                for (ChallengeProgress cp : new ArrayList<>(slotProgressMap.values())) {
                    if (cp.hasTimeRanOut()) {
                        CobbleChallengeMod.logger.info("Resetting expired challenge {} for player {}", cp.getActiveChallenge().getName(), uuid);
                        slotProgressMap.values().remove(cp);
                    }
                }
            }
        } else {
            for (ChallengeList challengeList : new ArrayList<>(api.getChallengeLists())) {
                for (ChallengeProgress cp : new ArrayList<>(activeChallenges.getOrDefault(challengeList.getName(), Collections.emptyList()))) {
                    if (cp.hasTimeRanOut()) {
                        CobbleChallengeMod.logger.info("Resetting expired challenge {} for player {}", cp.getActiveChallenge().getName(), uuid);
                        activeChallenges.get(challengeList.getName()).remove(cp);
                    }
                }
            }
        }
    }

    public void resetChallenges() {
        resetProgress();
        completedChallenges.clear();

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

    public void resetAvailableSlotChallenges() {
        availableSlotChallenges.clear();
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

        if (!challenge.doesNeedSelection()) {
            ChallengeProgress loaded = getProgressForSlot(listName, slot);
            if (loaded != null) {
                if (getActiveChallengeProgress(loaded.getActiveChallenge().getName()) == null) {
                    addActiveChallenge(loaded);
                }
            } else {
                if (!isChallengeInProgress(challenge.getName())) {
                    addActiveChallenge(api.getChallengeList(listName), challenge, challenge.getSlot());
                }
            }
        }

        if (windowGUIMap == null) return;
        ChallengeListGUI challengeListGUI = windowGUIMap.get(listName);
        if (challengeListGUI == null) return;

        challengeListGUI.refreshChallengeAtSlot(slot, challenge);
    }

    public void setAvailableChallengesForList(String listName, Map<Integer, Challenge> availableChallenges) {
        availableSlotChallenges.put(listName, availableChallenges);
    }

    public void AddDefaultSlotChallenges()
    {
        if (api.getConfigOptions().isUsingPools()) {
            for (ChallengeList list : api.getChallengeLists()) {
                String listName = list.getName();
                for (Map.Entry<Integer, List<Challenge>> slotPool : list.getSlotPools().entrySet()) {
                    int slot = slotPool.getKey();

                    if (getAvailableSlotChallenge(listName, slot) == null) {
                        List<Challenge> pool = slotPool.getValue();
                        if (!pool.isEmpty()) {
                            setAvailableSlotChallenge(listName, slot, pool.getFirst());
                        }
                    }
                }
            }
        }
    }

    public void resetMissingChallenges() {
        if (api.getConfigOptions().isUsingPools()) {
            for (ChallengeList list : api.getChallengeLists()) {
                String listName = list.getName();
                Map<Integer, Challenge> listOfAvailableSlots = availableSlotChallenges.getOrDefault(listName, Collections.emptyMap());

                for (Map.Entry<Integer, Challenge> challengeSlotEntry : listOfAvailableSlots.entrySet()) {
                    int slot = challengeSlotEntry.getKey();
                    Challenge availableChallenge = challengeSlotEntry.getValue();

                    boolean found = false;
                    List<Challenge> listOfPossibleChallenges = list.getSlotPools().getOrDefault(slot, Collections.emptyList());
                    for (Challenge challengeForSlot : listOfPossibleChallenges) {
                        if (availableChallenge.getName().equals(challengeForSlot.getName())) {
                            found = true;
                            break;
                        }
                    }

                    if (!found && !listOfPossibleChallenges.isEmpty()) {
                        Challenge defaultChallenge = listOfPossibleChallenges.getFirst();
                        listOfAvailableSlots.put(slot, defaultChallenge);

                        ChallengeListGUI listGUI = windowGUIMap.get(listName);
                        if (listGUI != null) {
                            listGUI.refreshChallengeAtSlot(slot, defaultChallenge);
                        }
                    }
                }

                Iterator<CompletedChallenge> iterator = completedChallenges.iterator();
                while (iterator.hasNext()) {
                    CompletedChallenge completedChallenge = iterator.next();
                    if (!completedChallenge.challengeListID().equals(listName)) continue;

                    Challenge comChallenge = list.getChallenge(completedChallenge.challengeID());

                    if (comChallenge == null) {
                        iterator.remove();
                        continue;
                    }

                    int comChallengeSlot = comChallenge.getSlot();
                    List<Challenge> listOfPossibleChallenges = list.getSlotPools().getOrDefault(comChallengeSlot, Collections.emptyList());

                    boolean found = false;
                    for (Challenge ch : listOfPossibleChallenges) {
                        if (ch.getName().equals(comChallenge.getName())) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        iterator.remove();
                    }
                }
            }
        } else {
            for (ChallengeList list : api.getChallengeLists()) {
                String listName = list.getName();
                List<Challenge> challengeMap = list.getChallengeMap();

                if (windowGUIMap.containsKey(listName)) {
                    ChallengeListGUI listGUI = windowGUIMap.get(listName);

                    if (listGUI != null) {
                        Map<Integer, WindowItem> slotItems = listGUI.getWindow().getItemsPerSlot();
                        int challengeIndex = 0;

                        for (Map.Entry<Integer, WindowItem> entry : slotItems.entrySet()) {
                            int slot = entry.getKey();

                            if (challengeIndex < challengeMap.size()) {
                                Challenge newChallenge = challengeMap.get(challengeIndex++);
                                String oldChallengeName = entry.getValue().getChallengeName();

                                if (oldChallengeName == null) {
                                    listGUI.refreshChallengeAtSlot(slot + 1, newChallenge);
                                } else {
                                    if (!oldChallengeName.equals(newChallenge.getName())) {
                                        listGUI.refreshChallengeAtSlot(slot + 1, newChallenge);
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }

                List<ChallengeProgress> activeChallengesForList = activeChallenges.get(listName);
                if (activeChallengesForList != null) {
                    Iterator<ChallengeProgress> activeChallengesIt = activeChallengesForList.iterator();
                    while (activeChallengesIt.hasNext()) {
                        ChallengeProgress activeChallenge = activeChallengesIt.next();

                        boolean found = false;
                        for (Challenge challenge : challengeMap) {
                            if (activeChallenge.getActiveChallenge().getName().equals(challenge.getName())) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            activeChallengesIt.remove();
                        }
                    }
                }
            }
        }
    }

    public void resetUnneededSlots() {
        for (ChallengeList list : api.getChallengeLists()) {
            String listName = list.getName();

            ChallengeListGUI gui = windowGUIMap.get(listName);
            if (gui == null) continue;
            SimpleWindow window = gui.getWindow();
            if (window == null) continue;

            String itemsPerSlotString = window.getItemsPerSlot().entrySet().stream()
                .map(e -> {
                    WindowItem item = e.getValue();
                    if (item == null) {
                        return "ItemsPerSlot: <" + e.getKey() + ", (0, null)>";
                    } else {
                        return "ItemsPerSlot: <" + e.getKey() + ", (" + item.getChallengeSlot() + ", " + item.getChallengeName() + ")>";
                    }
                })
                .collect(Collectors.joining(", "));
            if (isOnline()) CobbleChallengeMod.logger.info(itemsPerSlotString);

            Map<Integer, WindowItem> itemsPerSlot = window.getItemsPerSlot();
            List<Integer> realContentIndexes = window.getContentSlots();

            for (int realIndex : realContentIndexes) {
                int contentIndex = window.getContentIndex(realIndex);
                List<Challenge> slotChallenges = list.getSlotPools().getOrDefault(contentIndex, Collections.emptyList());

                if (isOnline()) {
                    String slotChallengesString = slotChallenges.stream()
                        .map(c -> "(" + c.getSlot() + ", " + c.getName() + ")")
                        .collect(Collectors.joining(", "));
                    CobbleChallengeMod.logger.info("<%d (content=%d), %s>".formatted(realIndex, contentIndex, slotChallengesString.isEmpty() ? "(?, null)" : slotChallengesString));
                }

                WindowItem slotWindowItem = itemsPerSlot.get(realIndex);
                if (slotChallenges.isEmpty()) {
                    CobbleChallengeMod.logger.info("Replace with empty at realSlot=%d (content=%d)".formatted(realIndex, contentIndex));
                    if (slotWindowItem != null) {
                        slotWindowItem.setBuilder(new ChallengeItem(window, this, null));
                    } else {
                        slotWindowItem = new WindowItem(window, new ChallengeItem(window, this, null));
                        itemsPerSlot.put(realIndex, slotWindowItem);
                    }
                    window.updateSlot(slotWindowItem, true);
                }
            }

            /* int initialRealSlotID = window.getFirstRealSlot();
            int getLastRealSlotID = window.getLastRealSlot();
            window.getItemsPerSlot().replaceAll((slot, windowItem) -> {
                CobbleChallengeMod.logger.info("initialRealSlotID=%d , getLastRealSlotID=%d".formatted(initialRealSlotID, getLastRealSlotID));
                if (slot < initialRealSlotID || slot > getLastRealSlotID) return windowItem;

                List<Challenge> slotChallenges = list.getSlotPools().getOrDefault(window.getContentIndex(slot), Collections.emptyList());

                if (isOnline()) {
                    String slotChallengesString = slotChallenges.stream()
                        .map(c -> "(" + c.getSlot() + ", " + c.getName() + ")")
                        .collect(Collectors.joining(", "));
                    CobbleChallengeMod.logger.info("<%d, %s>".formatted(slot, slotChallengesString.isEmpty() ? "(?, null)" : slotChallengesString));

                    CobbleChallengeMod.logger.info("Checking slot %d, pool size=%d, item=%s"
                        .formatted(slot, slotChallenges.size(), 
                                windowItem == null ? "null" : windowItem.getChallengeName()));
                }

                if (slotChallenges.isEmpty()) {
                    CobbleChallengeMod.logger.info("Replace with empty: <?, null>");
                    if (windowItem != null) {
                        windowItem.setBuilder(new ChallengeItem(window, this, null));
                        window.updateSlot(windowItem, true);
                    }
                } else {
                    if (windowItem != null && windowItem.getChallengeName() != null && windowItem.getChallengeSlot() > 0) {
                        CobbleChallengeMod.logger.info("Dont replace: <%d, %s>".formatted(windowItem.getChallengeSlot(), windowItem.getChallengeName()));
                    }
                }

                return windowItem; 
            });

            window.buildInventory(); */
        }
    }
}
