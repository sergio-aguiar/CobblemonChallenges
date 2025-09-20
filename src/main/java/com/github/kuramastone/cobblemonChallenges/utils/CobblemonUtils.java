package com.github.kuramastone.cobblemonChallenges.utils;

public class CobblemonUtils {

    public static boolean doesDaytimeMatch(long time, String phrase) {
        long normalizedTime = time % 24000;

        // Convert the phrase to lowercase to handle case-insensitive comparison
        phrase = phrase.toLowerCase();

        for(String sub : phrase.split("/")) {
            switch (sub) {
                case "any":
                    return true;  // If 'any', always return true
                case "dawn":
                    if (normalizedTime >= 0 && normalizedTime < 1000) return true;
                    break;
                case "day":
                    if (normalizedTime >= 1000 && normalizedTime < 12000) return true;
                    break;
                case "dusk":
                    if (normalizedTime >= 12000 && normalizedTime < 13000) return true;
                    break;
                case "night":
                    if (normalizedTime >= 13000 && normalizedTime < 24000) return true;
                    break;
            }
        }
        return false;
    }
}

