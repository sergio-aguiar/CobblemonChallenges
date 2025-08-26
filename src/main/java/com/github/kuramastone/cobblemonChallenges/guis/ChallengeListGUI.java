package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.MilestoneTimePlayedRequirement;
import com.github.kuramastone.cobblemonChallenges.gui.GuiConfig;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.gui.WindowItem;

import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChallengeListGUI {

    private final CobbleChallengeAPI api;
    private final PlayerProfile profile;
    private final ChallengeList challengeList;

    private final SimpleWindow window;

    public ChallengeListGUI(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList challengeList, GuiConfig config) {
        this.api = api;
        this.profile = profile;
        this.challengeList = challengeList;
        window = new SimpleWindow(config);
        build();
    }

    private void build() {
        List<WindowItem> contents = new ArrayList<>();
        boolean usingPools = api.getConfigOptions().isUsingPools();

        if (usingPools) {
            Map<Integer, Challenge> assignedSlots = profile.getAvailableSlotChallengesForList(challengeList.getName());

            for (Map.Entry<Integer, Challenge> slotChallenge : assignedSlots.entrySet()) {
                int slot = slotChallenge.getKey();
                Challenge challenge = slotChallenge.getValue();

                if (challenge == null || slot <= 0) continue;

                WindowItem item = new WindowItem(window, new ChallengeItem(window, profile, challenge));
                item.setRunnableOnClick(onChallengeClick(challenge, item, slot));

                if (challenge.doesNeedSelection() && profile.isChallengeInProgress(challenge.getName())) {
                    item.setAutoUpdate(15, () ->
                            challenge.getRequirements().stream().anyMatch(it -> it instanceof MilestoneTimePlayedRequirement)
                                    || profile.isChallengeInProgress(challenge.getName()));
                }

                contents.add(item);
            }
        } else {
            //window is already built aesthetically, but now we need to insert each challenge
            for (Challenge challenge : challengeList.getChallengeMap()) {
                WindowItem item = new WindowItem(window, new ChallengeItem(window, profile, challenge));
                if (challenge.doesNeedSelection() && profile.isChallengeInProgress(challenge.getName()))
                    item.setAutoUpdate(15, () ->
                            // check if this challenge requirement should auto-update
                            challenge.getRequirements().stream().anyMatch(it -> it instanceof MilestoneTimePlayedRequirement)
                                    // check if challenge has a timer that needs ticking
                                    || profile.isChallengeInProgress(challenge.getName())
                    );
                item.setRunnableOnClick(onChallengeClick(challenge, item, -1));
                contents.add(item);
            }
        }

        window.setContents(contents);
    }

    private Runnable onChallengeClick(Challenge challenge, WindowItem item, int slot) {
        return () -> {
            if (!profile.isChallengeInProgress(challenge.getName()) && !profile.isChallengeCompleted(challenge.getName()) && challenge.doesNeedSelection()) {
                profile.addActiveChallenge(challengeList, challenge, slot);
                profile.checkCompletion(challengeList);
                item.setAutoUpdate(10, () -> true); // set to auto update to allow timer to keep updating
                item.notifyWindow();
            }
        };
    }

    public void open() {
        window.show(profile.getPlayerEntity());
    }

    public void refreshChallengeAtSlot(int slot, Challenge challenge) {
        if (window == null) return;

        WindowItem windowItem = window.getItemAtContentSlot(slot);
        if (windowItem != null) {
            windowItem.setBuilder(new ChallengeItem(window, profile, challenge));
            windowItem.setRunnableOnClick(onChallengeClick(challenge, windowItem, slot));
            window.updateSlot(windowItem);
        }
    }

    public void refreshChallenge(Challenge challenge, int challengeSlot) {
        if (window == null) return;

        for (Map.Entry<Integer, WindowItem> entry : window.getItemsPerSlot().entrySet()) {
            int slot = entry.getKey();
            WindowItem windowItem = entry.getValue();

            if (windowItem != null && slot == challengeSlot) {
                windowItem.setBuilder(new ChallengeItem(window, profile, challenge));
                windowItem.setRunnableOnClick(onChallengeClick(challenge, windowItem, slot));
                window.updateSlot(windowItem);
                break;
            }
        }
    }
}




















