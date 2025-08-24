package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.gui.ItemProvider;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.ItemUtils;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChallengeItem implements ItemProvider {

    private SimpleWindow window;
    private CobbleChallengeAPI api;
    private PlayerProfile profile;
    private Challenge challenge;

    public ChallengeItem(SimpleWindow window, PlayerProfile profile, Challenge challenge) {
        this.window = window;
        this.profile = profile;
        this.challenge = challenge;
        api = CobbleChallengeMod.instance.getAPI();
    }

    public Challenge getChallenge() {
        return challenge;
    }

    @Override
    public ItemStack build() {

        ItemStack item = FabricAdapter.toItemStack(challenge.getDisplayConfig());

        //format lore
        List<String> lore = new ArrayList<>();
        for (String line : challenge.getDisplayConfig().getLore()) {
            // Handle {tracking-tag} separately if it exists in the line
            if (line.contains("{tracking-tag}")) {
                List<String> tagLines = new ArrayList<>();

                if (!challenge.doesNeedSelection()) {
                    // No selection → no timer tag
                    tagLines = List.of(); // or skip
                } else if (profile.isChallengeCompleted(challenge.getName())) {
                    // Challenge completed → check cooldown
                    long lastCompleted = profile.getCompletedChallenges().stream()
                        .filter(c -> c.challengeID().equals(challenge.getName()))
                        .findFirst()
                        .map(c -> c.timeCompleted())
                        .orElse(0L);
                    long elapsed = System.currentTimeMillis() - lastCompleted;
                    long cooldown = challenge.getRepeatableEveryMilliseconds();
                    long remaining = cooldown - elapsed;

                    if (remaining > 0) {
                        tagLines = List.of(StringUtils.splitByLineBreak(
                            api.getMessage("challenges.tracking-tag.cooldown", "{time-remaining}", StringUtils.formatSecondsToString(remaining / 1000)).getText()
                        ));
                    } else {
                        tagLines = List.of(StringUtils.splitByLineBreak(
                            api.getMessage("challenges.tracking-tag.before-starting").getText()
                        ));
                    }

                } else if (profile.isChallengeInProgress(challenge.getName())) {
                    long remaining = profile.getActiveChallengeProgress(challenge.getName()).getTimeRemaining();
                    tagLines = List.of(StringUtils.splitByLineBreak(
                        api.getMessage("challenges.tracking-tag.after-starting", "{time-remaining}", StringUtils.formatSecondsToString(remaining / 1000)).getText()
                    ));
                } else {
                    long remaining = challenge.getMaxTimeInMilliseconds();
                    tagLines = List.of(StringUtils.splitByLineBreak(
                        api.getMessage("challenges.tracking-tag.before-starting", "{time-remaining}", StringUtils.formatSecondsToString(remaining / 1000)).getText()
                    ));
                }

                lore.addAll(tagLines);
                continue;
            }

            // Handle other replacements
            String[] replacements = {
                "{progression_status}", null,
                "{description}", challenge.getDescription()
            };

            if (profile.isChallengeCompleted(challenge.getName())) {
                replacements[1] = api.getMessage("challenges.progression_status.post-completion").getText();
            } else if (profile.isChallengeInProgress(challenge.getName())) {
                String progressLines = profile.getActiveChallengeProgress(challenge.getName()).getProgressListAsString();
                replacements[1] = api.getMessage("challenges.progression_status.during-attempt").getText() + "\n" + progressLines;
            } else {
                replacements[1] = api.getMessage("challenges.progression_status.before-attempt").getText();
            }

            for (int i = 0; i < replacements.length; i += 2) {
                if (replacements[i] != null && replacements[i + 1] != null)
                    line = line.replace(replacements[i], replacements[i + 1]);
            }

            lore.addAll(List.of(StringUtils.splitByLineBreak(line)));
        }
        

        lore = StringUtils.centerStringListTags(lore);

        ItemUtils.setLore(item, lore);

        if (profile.isChallengeCompleted(challenge.getName())) {
            item = ItemUtils.setItem(item, api.getConfigOptions().getCompletedChallengeItem().getItem());
        }
        else if (profile.isChallengeInProgress(challenge.getName()) && challenge.doesNeedSelection()) {
            item = ItemUtils.setItem(item, api.getConfigOptions().getActiveChallengeItem().getItem());
        }

        item.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        return item;
    }

    @Override
    public ItemProvider copy() {
        return new ChallengeItem(window, profile, challenge);
    }
}
