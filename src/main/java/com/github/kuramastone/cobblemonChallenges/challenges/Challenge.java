package com.github.kuramastone.cobblemonChallenges.challenges;

import com.github.kuramastone.bUtilities.configs.ItemConfig;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.RequirementLoader;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Requirement;
import com.github.kuramastone.cobblemonChallenges.challenges.reward.CommandReward;
import com.github.kuramastone.cobblemonChallenges.challenges.reward.Reward;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Challenge {

    private String name;
    private ItemConfig displayItem;
    private List<Reward> rewards;
    private List<Requirement> requirements;
    private String description;

    private boolean needsSelection; // does it need to be selected for a player to make progress?
    private long maxTimeInMilliseconds; // how long does the player have to complete this? -1 for infinite time
    private long repeatableEveryMilliseconds = -1; // how often this challenge can be completed. -1 for only once

    public Challenge(String name, List<Reward> rewards, List<Requirement> requirements, ItemConfig displayItem,
                     boolean needsSelection, long maxTimeInMilliseconds, String description, long repeatableEveryMilliseconds) {
        this.name = name;
        this.rewards = rewards;
        this.requirements = requirements;
        this.displayItem = displayItem;
        this.needsSelection = needsSelection;
        this.maxTimeInMilliseconds = maxTimeInMilliseconds;
        this.description = description;
        this.repeatableEveryMilliseconds = repeatableEveryMilliseconds;
    }

    public static @Nullable Challenge load(String challengeID, YamlConfig section) {

        boolean needsSelection = section.get("needs-selection", false);
        long timeToComplete = StringUtils.stringToMilliseconds(section.get("time-limit", "-1"));
        ItemConfig displayItem = new ItemConfig(section.getSection("display-item"));
        long repeatableEveryMilliseconds = StringUtils.stringToMilliseconds(section.get("repeatable", "-1"));
        List<Requirement> requirementList = new ArrayList<>();
        List<Reward> rewards = new ArrayList<>();

        for (String keyID : section.getKeys("requirements", false)) {
            for (String requirementType : section.getKeys("requirements." + keyID, false)) {
                String friendlyReqType = requirementType.toLowerCase().replace("-", "_").replace(" ", "_");
                Requirement requirement = RequirementLoader.load(challengeID, friendlyReqType, section.getSection("requirements." + keyID + "." + requirementType));
                if (requirement != null) {
                    requirementList.add(requirement);
                }
            }
        }
        if(requirementList.isEmpty()) {
            CobbleChallengeMod.logger.warn("Challenge {} has no valid requirements. Check for earlier warnings for more details. Skipping this challenge!", challengeID);
            return null;
        }

        if (section.hasKey("rewards.commands")) {
            for (String commandString : section.getStringList("rewards.commands")) {
                rewards.add(new CommandReward(commandString));
            }
        }

        String description = StringUtils.collapseWithNextLines(section.getStringList("description"));

        return new Challenge(challengeID, rewards, requirementList, displayItem, needsSelection, timeToComplete, description, repeatableEveryMilliseconds);
    }

    public long getRepeatableEveryMilliseconds() {
        return repeatableEveryMilliseconds;
    }

    public boolean isRepeatable() {
        return repeatableEveryMilliseconds != -1;
    }

    public String getName() {
        return name;
    }

    public ItemConfig getDisplayConfig() {
        return displayItem;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public long getMaxTimeInMilliseconds() {
        return maxTimeInMilliseconds;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public Requirement getRequirement(String name) {
        for (Requirement req : this.requirements) {
            if (req.getName().equals(name)) {
                return req;
            }
        }

        return null;
    }

    public String getDescription() {
        return description;
    }

    public boolean doesNeedSelection() {
        return needsSelection;
    }
}
