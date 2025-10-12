package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

public interface Requirement {

    Requirement load(YamlConfig section);

    String getName();

    Progression<?> buildProgression(PlayerProfile profile, Challenge parentChallenge);


}
