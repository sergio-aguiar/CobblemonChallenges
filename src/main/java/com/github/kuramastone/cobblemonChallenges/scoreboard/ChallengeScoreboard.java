package com.github.kuramastone.cobblemonChallenges.scoreboard;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ChallengeScoreboard {
    private static final String BASE_OBJECTIVE_NAME = "challenges";
    private static final ConcurrentHashMap<UUID, ChallengeProgress> playerTrackedChallenges = new ConcurrentHashMap<>();

    public static boolean showForPlayer(ServerPlayer player, PlayerProfile profile) {
        Scoreboard board = player.getServer().getScoreboard();
        String playerObjectiveName = getPlayerObjectiveName(player);

        Objective old = board.getObjective(playerObjectiveName);
        if (old != null) board.removeObjective(old);

        Objective objective = board.addObjective(
            playerObjectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal("§e§l     Challenge Tracking     "),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            (NumberFormat) null
        );
        player.getScoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, objective);

        ChallengeProgress trackedChallengeProgress = playerTrackedChallenges.get(player.getUUID());
        if (trackedChallengeProgress == null) {
            clearForPlayer(player);
            return false;
        }

        Challenge trackedChallenge = trackedChallengeProgress.getActiveChallenge();
        if (trackedChallenge == null) {
            clearForPlayer(player);
            return false;
        }

        List<String> lines = trackedChallengeProgress.getProgressLinesForScoreboard();

        String challengeName = trackedChallenge.getDisplayName();
        List<String> wrappedNameLines = wrapScoreboardText(challengeName, 36);

        int score = wrappedNameLines.size() + lines.size();
        
        addLine(board, objective, "  ", score--, false);

        for (String nameLine : wrappedNameLines) {
            addLine(board, objective, nameLine, score--, false);
        }

        for (String line : lines) {
            addLine(board, objective, line, score--, true);
        }

        return true;
    }

    private static void addLine(Scoreboard board, Objective obj, String text, int score, boolean progressLine) {
        String visibleText = text;
        String rawKey = net.minecraft.ChatFormatting.stripFormatting(visibleText);

        int id = score & 0xFF;
        String suffix = "§" + Integer.toHexString((id >> 4) & 0xF) + "§" + Integer.toHexString(id & 0xF);
        rawKey = "§6" + (progressLine ? "│ §f" : "") + rawKey + suffix;

        board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(rawKey), obj).set(score);
    }

    public static void clearForPlayer(ServerPlayer player) {
        Scoreboard board = player.getServer().getScoreboard();
        String playerObjectiveName = getPlayerObjectiveName(player);

        Objective objective = board.getObjective(playerObjectiveName);
        if (objective != null) {
            board.removeObjective(objective);
        }
    }

    public static String getPlayerObjectiveName(ServerPlayer player) {
        return BASE_OBJECTIVE_NAME + "_" + player.getUUID().toString().substring(0, 8);
    }

    public static boolean setTrackedChallenge(ServerPlayer player, ChallengeProgress progress) {
        if (progress == null) {
            playerTrackedChallenges.remove(player.getUUID());
            clearForPlayer(player);
            return false;
        }

        playerTrackedChallenges.put(player.getUUID(), progress);
        showForPlayer(player, progress.getProfile());
        return true;
    }

    @Nullable
    public static ChallengeProgress getTrackedChallenge(ServerPlayer player) {
        return player == null ? null : playerTrackedChallenges.get(player.getUUID());
    }

    public static void updateIfTracking(PlayerProfile profile, String challengeName) {
        if (profile == null || profile.getPlayerEntity() == null) return;

        ServerPlayer player = profile.getPlayerEntity();
        UUID uuid = player.getUUID();

        ChallengeProgress tracked = playerTrackedChallenges.get(uuid);

        if (tracked != null && tracked.getActiveChallenge().getName().equals(challengeName) && player.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) != null) {
            showForPlayer(player, profile);
        }
    }

    private static List<String> wrapScoreboardText(String text, int maxLength) {
        List<String> wrapped = new java.util.ArrayList<>();
        String remaining = text;
        String lastColor = "";

        while (remaining.length() > maxLength) {
            int cut = maxLength;

            if (remaining.charAt(cut - 1) == '§') {
                cut--;
            }

            int lastSpace = -1;
            for (int i = cut - 1; i >= 0; i--) {
                char c = remaining.charAt(i);
                if (Character.isWhitespace(c) || c == '_' || c == '-') {
                    lastSpace = i;
                    break;
                }
            }

            if (lastSpace > maxLength / 2) {
                cut = lastSpace + 1;
            }

            String line = remaining.substring(0, cut);
            remaining = remaining.substring(cut).trim();

            wrapped.add(line);

            for (int i = line.length() - 2; i >= 0; i--) {
                if (line.charAt(i) == '§') {
                    lastColor = line.substring(i, i + 2);
                    break;
                }
            }

            if (!remaining.isEmpty() && !lastColor.isEmpty() && !remaining.startsWith("§")) {
                remaining = lastColor + remaining;
            }
        }

        if (!remaining.isEmpty()) wrapped.add(remaining);
        return wrapped;
    }
}