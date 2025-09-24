package com.github.kuramastone.cobblemonChallenges;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.CompletedChallenge;
import com.github.kuramastone.cobblemonChallenges.commands.CommandHandler;
import com.github.kuramastone.cobblemonChallenges.events.*;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.listeners.TickScheduler;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import com.github.kuramastone.cobblemonChallenges.utils.VoteUtils;
import com.vexsoftware.votifier.fabric.event.VoteListener;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CobbleChallengeMod implements ModInitializer {

    public static String MODID = "cobblemonchallenges";

    public static CobbleChallengeMod instance;
    public static final Logger logger = LogManager.getLogger(MODID);
    private static MinecraftServer minecraftServer;
    private CobbleChallengeAPI api;

    @Override
    public void onInitialize() {
        instance = this;
        api = new CobbleChallengeAPI();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted); // capture minecraftserver
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onStopped());
        startSaveScheduler();
        startRepeatableScheduler();
        startExpirationScheduler();
        CommandHandler.register();
        registerTrackedEvents();
    }

    private void onServerStarted(MinecraftServer server) {
        minecraftServer = server;
        api.init();
    }


    private void startSaveScheduler() {
        TickScheduler.scheduleRepeating(20 * 60 * 30, () -> {
            CompletableFuture.runAsync(() -> api.saveProfiles(), ChallengeExecutors.CHALLENGE_EXECUTOR);
            return true;
        });
    }

    private void startRepeatableScheduler() {
        TickScheduler.scheduleRepeating(20, () -> {
            CompletableFuture.runAsync(() -> {
                List<Runnable> tasksToRun = new ArrayList<>();

                for (PlayerProfile profile : api.getProfiles()) {
                    if (profile.isOnline()) {
                        List<CompletedChallenge> challengesToRemove = new ArrayList<>();

                        for (CompletedChallenge completedChallenge : new ArrayList<>(profile.getCompletedChallenges())) {
                            ChallengeList challengeList = api.getChallengeList(completedChallenge.challengeListID());

                            if (challengeList != null) {
                                Challenge challenge = challengeList.getChallenge(completedChallenge.challengeID());

                                if (challenge != null && challenge.isRepeatable()) {
                                    long timeSinceCompleted = System.currentTimeMillis() - completedChallenge.timeCompleted();

                                    if (timeSinceCompleted >= challenge.getRepeatableEveryMilliseconds()) {
                                        challengesToRemove.add(completedChallenge);
                                    }
                                }
                            }
                        }

                        if (!challengesToRemove.isEmpty()) {
                            tasksToRun.add(() -> {
                                profile.getCompletedChallenges().removeAll(challengesToRemove);

                                for (CompletedChallenge cc : challengesToRemove) {
                                    ChallengeList cl = api.getChallengeList(cc.challengeListID());
                                    Challenge ch = cl != null ? cl.getChallenge(cc.challengeID()) : null;
                                    if (ch != null) {
                                        if (api.getConfigOptions().isUsingPools()) {
                                            int slot = ch.getSlot();
                                            Challenge replacement = cl.getRandomChallengeForSlot(slot, new java.util.Random());

                                            if (replacement != null) {
                                                profile.setAvailableSlotChallenge(cl.getName(), slot, replacement);

                                                if (!replacement.doesNeedSelection()) {
                                                    profile.addActiveChallenge(cl, replacement, replacement.getSlot());
                                                }

                                                List<String> lines = List.of(StringUtils.splitByLineBreak(
                                                        api.getMessage(
                                                                "challenges.randomized",
                                                                "{challenge}", replacement.getDisplayName(),
                                                                "{challenge-description}", replacement.getDescription()
                                                        ).getText()
                                                ));
                                                List<String> formatted = StringUtils.centerStringListTags(lines);
                                                for (String line : formatted) {
                                                    profile.sendMessage(ComponentEditor.decorateComponent(line));
                                                }
                                            } else {
                                                List<String> lines = List.of(StringUtils.splitByLineBreak(
                                                    api.getMessage(
                                                        "challenges.offcooldown",
                                                        "{challenge}", ch.getDisplayName(),
                                                        "{challenge-description}", ch.getDescription()
                                                    ).getText()
                                                ));
                                                List<String> formatted = StringUtils.centerStringListTags(lines);
                                                for (String line : formatted) {
                                                    profile.sendMessage(ComponentEditor.decorateComponent(line));
                                                }
                                            }
                                        } else {
                                            if (!ch.doesNeedSelection()) {
                                                profile.addActiveChallenge(cl.buildNewProgressForQuest(ch, profile));
                                            }

                                            List<String> lines = List.of(StringUtils.splitByLineBreak(
                                                api.getMessage(
                                                    "challenges.offcooldown",
                                                    "{challenge}", ch.getDisplayName(),
                                                    "{challenge-description}", ch.getDescription()
                                                ).getText()
                                            ));
                                            List<String> formatted = StringUtils.centerStringListTags(lines);
                                            for (String line : formatted) {
                                                profile.sendMessage(ComponentEditor.decorateComponent(line));
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

                CobbleChallengeMod.getMinecraftServer().execute(() -> {
                    for (Runnable task : tasksToRun) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            CobbleChallengeMod.logger.error("Error during repeatable challenge task execution.");
                            e.printStackTrace();
                        }
                    }
                });
            }, ChallengeExecutors.CHALLENGE_EXECUTOR);

            return true;
        });
    }

    private void startExpirationScheduler() {
        TickScheduler.scheduleRepeating(20, () -> {
            CompletableFuture.runAsync(() -> {
                List<Runnable> tasksToRun = new ArrayList<>();

                for (PlayerProfile profile : api.getProfiles()) {
                    if (profile.isOnline()) {

                        if (api.getConfigOptions().isUsingPools())
                        {
                            class ExpiredSlot 
                            {
                                final String listName;
                                final int slot;
                                final ChallengeProgress cp;
                                ExpiredSlot(String listName, int slot, ChallengeProgress cp) {
                                    this.listName = listName;
                                    this.slot = slot;
                                    this.cp = cp;
                                }
                            }
                            List<ExpiredSlot> expiredSlots = new ArrayList<>();

                            for (ChallengeList cl : new ArrayList<>(api.getChallengeLists())) {
                                String listName = cl.getName();
                                Map<Integer, ChallengeProgress> slots = new LinkedHashMap<>(
                                        profile.getActiveSlotChallengesMap().getOrDefault(listName, Collections.emptyMap())
                                );

                                for (Map.Entry<Integer, ChallengeProgress> e : slots.entrySet()) {
                                    ChallengeProgress cp = e.getValue();
                                    if (cp != null && cp.hasTimeRanOut()) {
                                        expiredSlots.add(new ExpiredSlot(listName, e.getKey(), cp));
                                    }
                                }
                            }

                            if (!expiredSlots.isEmpty()) {
                                tasksToRun.add(() -> {
                                    for (ExpiredSlot ex : expiredSlots) {
                                        profile.removeProgressForSlot(ex.listName, ex.slot);

                                        CobbleChallengeMod.logger.info("Resetting expired slot {} challenge {} for player {} in list {}",
                                                ex.slot, ex.cp.getActiveChallenge().getName(), profile.getUUID(), ex.listName);

                                        List<String> lines = List.of(StringUtils.splitByLineBreak(
                                                api.getMessage(
                                                        "challenges.expired",
                                                        "{challenge}", ex.cp.getActiveChallenge().getDisplayName(),
                                                        "{challenge-description}", ex.cp.getActiveChallenge().getDescription()
                                                ).getText()
                                        ));
                                        List<String> formatted = StringUtils.centerStringListTags(lines);
                                        for (String line : formatted) {
                                            profile.sendMessage(ComponentEditor.decorateComponent(line));
                                        }

                                        Challenge ch = ex.cp.getActiveChallenge();
                                        if (api.getConfigOptions().isRerollingOnExpiration()) {
                                            int slot = ch.getSlot();
                                            ChallengeList cl = api.getChallengeList(ex.listName);
                                            Challenge replacement = cl.getRandomChallengeForSlot(slot, new java.util.Random());

                                            if (replacement != null) {
                                                profile.setAvailableSlotChallenge(ex.listName, slot, replacement);

                                                if (!replacement.doesNeedSelection()) {
                                                    profile.addActiveChallenge(cl, replacement, replacement.getSlot());
                                                }

                                                List<String> randomizedLines = List.of(StringUtils.splitByLineBreak(
                                                        api.getMessage(
                                                                "challenges.randomized",
                                                                "{challenge}", replacement.getDisplayName(),
                                                                "{challenge-description}", replacement.getDescription()
                                                        ).getText()
                                                ));
                                                List<String> randomizeformatted = StringUtils.centerStringListTags(randomizedLines);
                                                for (String line : randomizeformatted) {
                                                    profile.sendMessage(ComponentEditor.decorateComponent(line));
                                                }
                                            }
                                        } else {
                                            if (!ch.doesNeedSelection()) {
                                                profile.addActiveChallenge(ex.cp.getParentList(), ch, ch.getSlot());
                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            Map<String, List<ChallengeProgress>> expiredChallengesLegacy = new HashMap<>();

                            for (ChallengeList challengeList : new ArrayList<>(api.getChallengeLists())) {
                                String listName = challengeList.getName();
                                List<ChallengeProgress> cps = new ArrayList<>(profile.getActiveChallengesMap().getOrDefault(listName, Collections.emptyList()));

                                for (ChallengeProgress cp : cps) {
                                    if (cp.hasTimeRanOut()) {
                                        expiredChallengesLegacy.computeIfAbsent(listName, k -> new ArrayList<>()).add(cp);
                                    }
                                }
                            }

                            if (!expiredChallengesLegacy.isEmpty()) {
                                tasksToRun.add(() -> {
                                    for (Map.Entry<String, List<ChallengeProgress>> entry : expiredChallengesLegacy.entrySet()) {
                                        List<ChallengeProgress> list = profile.getActiveChallengesMap().get(entry.getKey());
                                        if (list != null) {
                                            for (ChallengeProgress cp : entry.getValue()) {
                                                CobbleChallengeMod.logger.info("Resetting expired challenge {} for player {}", cp.getActiveChallenge().getName(), profile.getUUID());
                                                list.remove(cp);

                                                Challenge ch = cp.getActiveChallenge();

                                                if (!ch.doesNeedSelection()) {
                                                    profile.addActiveChallenge(cp.getParentList().buildNewProgressForQuest(ch, profile));
                                                }

                                                List<String> lines = List.of(StringUtils.splitByLineBreak(
                                                    api.getMessage(
                                                        "challenges.expired",
                                                        "{challenge}", ch.getDisplayName(),
                                                        "{challenge-description}", ch.getDescription()
                                                    ).getText()
                                                ));
                                                List<String> formatted = StringUtils.centerStringListTags(lines);
                                                for (String line : formatted) {
                                                    profile.sendMessage(ComponentEditor.decorateComponent(line));
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }

                CobbleChallengeMod.getMinecraftServer().execute(() -> {
                    for (Runnable task : tasksToRun) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            CobbleChallengeMod.logger.error("Error during repeatable challenge task execution.");
                            e.printStackTrace();
                        }
                    }
                });
            }, ChallengeExecutors.CHALLENGE_EXECUTOR);

            return true;
        });
    }


    private void onStopped() {
        api.saveProfiles();
        shutdownChallengeExecutor();
    }

    private void shutdownChallengeExecutor() {
        ChallengeExecutors.CHALLENGE_EXECUTOR.shutdown();
        try {
            if (!ChallengeExecutors.CHALLENGE_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                ChallengeExecutors.CHALLENGE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            ChallengeExecutors.CHALLENGE_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void registerTrackedEvents() {
        ServerTickEvents.START_SERVER_TICK.register(TickScheduler::onServerTick);

        ChallengeListener.register();
        BlockBreakEvent.register();
        BlockPlaceEvent.register();
        PlayerJoinEvent.register();
        ServerTickEvents.START_SERVER_TICK.register(PlayTimeScheduler::onServerTick);
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonCaptured);
        CobblemonEvents.POKEMON_SCANNED.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonPokedexScanned);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, ChallengeListener::onBattleVictory);
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.HIGHEST, ChallengeListener::onEvolution);
        CobblemonEvents.APRICORN_HARVESTED.subscribe(Priority.HIGHEST, ChallengeListener::onApricornHarvest);
        CobblemonEvents.BERRY_HARVEST.subscribe(Priority.HIGHEST, ChallengeListener::onBerryHarvest);
        CobblemonEvents.POKEMON_SEEN.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonPokedexSeen);
        CobblemonEvents.EXPERIENCE_CANDY_USE_POST.subscribe(Priority.HIGHEST, ChallengeListener::onRareCandyUsed);
        CobblemonEvents.HATCH_EGG_POST.subscribe(Priority.HIGHEST, ChallengeListener::onEggHatch);
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_POST.subscribe(Priority.HIGHEST, ChallengeListener::onExpGained);
        CobblemonEvents.LEVEL_UP_EVENT.subscribe(Priority.HIGHEST, ChallengeListener::onLevelUp);
        CobblemonEvents.TRADE_EVENT_POST.subscribe(Priority.HIGHEST, ChallengeListener::onTradeCompleted);
        CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.HIGHEST, ChallengeListener::onFossilRevived);

        if (VoteUtils.isVotifierLoaded()) {
            VoteListener.EVENT.register(event -> {
                PlayerVoteEvent voteEvent = new PlayerVoteEvent(event.getUsername(), event.getServiceName());
                ChallengeListener.onPlayerVote(voteEvent);
            });
        }
    }

    public CobbleChallengeAPI getAPI() {
        return api;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static File defaultDataFolder() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), MODID);
    }
}
