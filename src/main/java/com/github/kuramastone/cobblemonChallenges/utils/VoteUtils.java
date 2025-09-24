package com.github.kuramastone.cobblemonChallenges.utils;

import net.fabricmc.loader.api.FabricLoader;

public class VoteUtils {

    public static boolean isVotifierLoaded() {
        return FabricLoader.getInstance().isModLoaded("nuvotifier-fabric");
    }
}
