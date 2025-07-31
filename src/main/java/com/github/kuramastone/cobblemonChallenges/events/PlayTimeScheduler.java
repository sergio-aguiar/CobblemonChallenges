package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class PlayTimeScheduler {

    private static long currentTick = 0;

    public static void onServerTick(MinecraftServer minecraftServer) {
        try {
            // We only want to run this on the END phase to ensure everything is processed in one tick
            currentTick++;

            // trigger every 30 seconds
            if (currentTick % 20 == 0) {
                oneSecondTick();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void oneSecondTick() {
        for (ServerPlayer player : CobbleChallengeMod.getMinecraftServer().getPlayerList().getPlayers()) {
            PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());
            ChallengeListener.on1SecondPlayed(new Played1SecondEvent(profile));
        }
    }

}
