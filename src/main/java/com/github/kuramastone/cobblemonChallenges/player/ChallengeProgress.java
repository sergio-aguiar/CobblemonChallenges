package com.github.kuramastone.cobblemonChallenges.player;

import com.github.kuramastone.cobblemonChallenges.challenges.requirements.*;
import com.github.kuramastone.cobblemonChallenges.events.ChallengeCompletedEvent;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.scoreboard.ChallengeScoreboard;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Used to track a player's progress within a challenge
 */
public class ChallengeProgress {

    private CobbleChallengeAPI api;

    private PlayerProfile profile;
    private ChallengeList parentList;
    private Challenge activeChallenge;
    private List<Pair<String, Progression<?>>> progressionMap; // <requirement type name, RequirementProgress>
    private long startTime;

    public ChallengeProgress(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList parentList, Challenge activeChallenge, List<Pair<String, Progression<?>>> progressionMap, long startTime) {
        this.api = api;
        this.profile = profile;
        this.parentList = parentList;
        this.activeChallenge = activeChallenge;
        this.progressionMap = progressionMap;
        this.startTime = startTime;
    }

    public boolean hasTimeRanOut() {
        if (activeChallenge.getMaxTimeInMilliseconds() == -1) {
            return false;
        }
        return (startTime + activeChallenge.getMaxTimeInMilliseconds()) < System.currentTimeMillis();
    }

    public long getTimeRemaining() {
        if (activeChallenge.getMaxTimeInMilliseconds() == -1) {
            return -1;
        }
        return Math.max(0, (startTime + activeChallenge.getMaxTimeInMilliseconds()) - System.currentTimeMillis());
    }

    private synchronized void completedActiveChallenge() {
        if (activeChallenge == null) {
            return;
        }

        profile.removeActiveChallenge(this);
        profile.completeChallenge(parentList, activeChallenge);

        ChallengeListener.onChallengeCompleted(new ChallengeCompletedEvent(profile, parentList, activeChallenge));
    }

    public void progress(@Nullable Object obj) {
        if (this.activeChallenge == null) {
            return;
        }

        if (obj != null) {
            for (Pair<String, Progression<?>> pair : this.progressionMap) {
                Progression<?> prog = pair.getRight();
                try {
                    if (prog.matchesMethod(obj)) {
                        prog.progress(obj);
                    }
                } catch (Exception e) {
                    CobbleChallengeMod.logger.error("Error progressing challenge!");
                    e.printStackTrace();
                }
            }
        }

        //only play if this progression made it level up
        if (isCompleted()) {
            completedActiveChallenge();
        }
    }

    private boolean isCompleted() {
        for (Pair<String, Progression<?>> pair : this.progressionMap) {
            Progression<?> prog = pair.getRight();
            if (!prog.isCompleted()) {
                return false;
            }
        }

        ServerPlayer player = profile.getPlayerEntity();

        ChallengeProgress tracked = ChallengeScoreboard.getTrackedChallenge(player);
        if (tracked == null || tracked.getActiveChallenge() == null || tracked.getActiveChallenge().getName().equals(activeChallenge.getName())) {
            ChallengeScoreboard.clearForPlayer(player, true);
        }

        return true;
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    public ChallengeList getParentList() {
        return parentList;
    }

    public Challenge getActiveChallenge() {
        return activeChallenge;
    }

    public List<Pair<String, Progression<?>>> getProgressionMap() {
        return progressionMap;
    }

    public void timeRanOut() {
        profile.sendMessage(api.getMessage("challenges.failure.time-ran-out", "{challenge}", activeChallenge.getDisplayName()).build());
        profile.removeActiveChallenge(this);
    }

    public String getProgressListAsString() {
        StringBuilder sb = new StringBuilder();

        for (Pair<String, Progression<?>> set : this.progressionMap) {

            String pokename = "";
            String poketype = "";
            String blockData = "";
            if (set.getValue() instanceof CatchPokemonRequirement.CatchPokemonProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof EvolvePokemonRequirement.EvolvePokemonProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof DefeatBattlerRequirement.DefeatPokemonProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof EXPGainedRequirement.ExpGainedProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof FossilRevivedRequirement.FossilsRevivedProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof HatchEggRequirement.HatchEggProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof IncreaseLevelRequirement.IncreaseLevelProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof LevelUpToRequirement.LevelUpToProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof TradeCompletedRequirement.TradesCompletedProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof UseRareCandyRequirement.UseRareCandyOnProgression prog) {
                pokename = prog.requirement.pokename;
            }
            else if (set.getValue() instanceof MineBlockRequirement.MineBlockProgression mineBlockProgression)
                blockData = getPrettyBlockTypeOfFirst(mineBlockProgression.requirement.blockType) + " ";
            else if (set.getValue() instanceof PlaceBlockRequirement.PlaceBlockProgression placeBlockProgression)
                blockData = getPrettyBlockTypeOfFirst(placeBlockProgression.requirement.blockType) + " ";

            if (pokename.isEmpty() || pokename.trim().toLowerCase().startsWith("any")) {
                pokename = "";
            }

            if (poketype.isEmpty() || poketype.trim().toLowerCase().startsWith("any")) {
                poketype = "";
            }

            pokename = individualToUppers(pokename);
            poketype = individualToUppers(poketype);

            String reqTitle = api.getMessage("requirements.progression-shorthand.%s".formatted(set.getKey().toLowerCase())).getText()
                    .replace("{poketype}", poketype)
                    .replace("{pokename}", pokename)
                    .replace("{blockData}", blockData)
                    .replace("  ", " ");

            sb.append(api.getMessage("progression.progression-entry",
                    "{requirement-title}", reqTitle,
                    "{progression-string}", set.getValue().getProgressString()).getText().replace("{block_data?}", "")
            ).append("\n");
        }

        String result = sb.substring(0, sb.length() - "\n".length());
        return result; // substring out the final \n
    }

    private String individualToUppers(String original) {
        String[] divs = original.split("/");
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;

        for (String part : divs) {
            String checking = part.trim();
            if (checking.isEmpty()) continue;

            if (!isFirst) result.append("/");

            result.append(Character.toUpperCase(checking.charAt(0)));
            if (checking.length() > 1) {
                result.append(checking.substring(1));
            }

            isFirst = false;
        }
        return result.toString();
    }

    private String getPrettyBlockTypeOfFirst(String blockIdentifierGroup) {
        if (blockIdentifierGroup.toLowerCase().startsWith("any")) {
            return "Any";
        }

        String[] blockIdentifierArray = blockIdentifierGroup.split("/");
        StringBuilder builder = new StringBuilder();
        // only add first block to list
        for (int i = 0; i < blockIdentifierArray.length; i++) {
            String blockIdentifier = blockIdentifierArray[i];


            try {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockIdentifier));
                builder.append(block.getName().getString());
            } catch (Exception e) {
                builder.append(blockIdentifier);
            }

            break;
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "ChallengeProgress{" +
                "activeChallengeName=" + activeChallenge.getName() +
                '}';
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public List<String> getProgressLinesForScoreboard() {
        List<String> lines = new java.util.ArrayList<>();

        for (Pair<String, Progression<?>> set : this.progressionMap) {
            String key = set.getKey();
            Progression<?> progression = set.getValue();

            String pokename = "";
            String poketype = "";
            String blockData = "";
            if (set.getValue() instanceof CatchPokemonRequirement.CatchPokemonProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof EvolvePokemonRequirement.EvolvePokemonProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof DefeatBattlerRequirement.DefeatPokemonProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof EXPGainedRequirement.ExpGainedProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof FossilRevivedRequirement.FossilsRevivedProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof HatchEggRequirement.HatchEggProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof IncreaseLevelRequirement.IncreaseLevelProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof LevelUpToRequirement.LevelUpToProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof TradeCompletedRequirement.TradesCompletedProgression prog) {
                pokename = prog.requirement.pokename;

                if (!prog.requirement.type.equalsIgnoreCase("any")) {
                    poketype = prog.requirement.type;
                }
            }
            else if (set.getValue() instanceof UseRareCandyRequirement.UseRareCandyOnProgression prog) {
                pokename = prog.requirement.pokename;
            }
            else if (set.getValue() instanceof MineBlockRequirement.MineBlockProgression mineBlockProgression)
                blockData = getPrettyBlockTypeOfFirst(mineBlockProgression.requirement.blockType) + " ";
            else if (set.getValue() instanceof PlaceBlockRequirement.PlaceBlockProgression placeBlockProgression)
                blockData = getPrettyBlockTypeOfFirst(placeBlockProgression.requirement.blockType) + " ";

            if (pokename.isEmpty() || pokename.trim().toLowerCase().startsWith("any")) {
                pokename = "";
            }

            if (poketype.isEmpty() || poketype.trim().toLowerCase().startsWith("any")) {
                poketype = "";
            }

            pokename = individualToUppers(pokename);
            poketype = individualToUppers(poketype);

            String displayName = api.getMessage("requirements.progression-shorthand.%s".formatted(key.toLowerCase())).getText()
                .replace("{poketype}", poketype)
                .replace("{pokename}", pokename)
                .replace("{blockData}", blockData)
                .replace("  ", " ");

            String progressString = progression.getProgressString().replace('&', '§');
            String line = displayName + ": §e" + progressString;

            if (line.length() > 40) {
                line = line.substring(0, 37) + "...";
            }

            lines.add(line);
        }

        return lines;
    }
}
