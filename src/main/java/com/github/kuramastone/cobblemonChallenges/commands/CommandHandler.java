package com.github.kuramastone.cobblemonChallenges.commands;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeListGUI;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeMenuGUI;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.PermissionUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

public class CommandHandler {

    private static CobbleChallengeAPI api;

    public static void register() {
        api = CobbleChallengeMod.instance.getAPI();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> challenges = Commands.literal("challenges")
                .requires(source -> hasPermission(source, "challenges.commands.challenge"))
                .executes(CommandHandler::handleChallengeBaseCommand);

            challenges.then(
                Commands.literal("list")
                    .executes(CommandHandler::handleChallengeBaseCommand)
                    .then(Commands.argument("list", StringArgumentType.word())
                        .suggests(CommandHandler::handleListSuggestions)
                        .executes(CommandHandler::handleChallengeListCommand))
            );

            challenges.then(
                Commands.literal("info")
                    .requires(source -> hasPermission(source, "challenges.commands.info"))
                    .executes(CommandHandler::handleInfoCommand)
            );

            /* challenges.then(
                Commands.literal("reload")
                    .requires(source -> hasPermission(source, "challenges.commands.admin.reload"))
                    .executes(CommandHandler::handleReloadCommand)
            ); */

            challenges.then(
                Commands.literal("reset")
                    .requires(source -> hasPermission(source, "challenges.commands.admin.restart"))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(CommandHandler::handleRestartCommand))
            );

            challenges.then(
                Commands.literal("migrate")
                    .requires(source -> hasPermission(source, "challenges.commands.admin.migrate"))
                    .executes(CommandHandler::handleMigrateLegacyCommand)
            );

            challenges.then(
                Commands.literal("resetall")
                    .requires(source -> hasPermission(source, "challenges.commands.admin.restartall"))
                    .executes(CommandHandler::handleRestartAllCommand)
            );

            dispatcher.register(challenges);
        });
    }

    private static int handleRestartCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = EntityArgument.getPlayer(context, "player");
        PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID(), false);

        profile.AddDefaultSlotChallenges();
        profile.resetMissingChallenges();
        profile.resetChallenges();

        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMessage("commands.restart")));
        return 1;
    }

    private static int handleRestartAllCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        for (PlayerProfile profile : CobbleChallengeMod.instance.getAPI().getProfiles()) {
            profile.AddDefaultSlotChallenges();
            profile.resetMissingChallenges();
            profile.resetChallenges();
        }

        CobbleChallengeMod.instance.getAPI().saveProfiles();
        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMessage("commands.restartall")));
        return 1;
    }

    private static boolean hasPermission(CommandSourceStack source, String perm) {
        return source.hasPermission(2) || (source.isPlayer() && PermissionUtils.hasPermission(source.getPlayer(), perm));
    }

    private static CompletableFuture<Suggestions> handleListSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Define available options for completion
        for (ChallengeList cl : api.getChallengeLists()) {
            // Add each option to the suggestions
            builder.suggest(cl.getName());
        }

        return builder.buildFuture();
    }

    private static int handleReloadCommand(CommandContext<CommandSourceStack> context) {
        api.reloadConfig();
        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMessage("commands.reload")));
        return 1;
    }

    private static int handleChallengeBaseCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            if (!source.isPlayer()) {
                source.sendSystemMessage(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            ServerPlayer player = (ServerPlayer) source.getEntity();

            if (api.getConfigOptions().isUsingPools()) {
                PlayerProfile profile = api.getOrCreateProfile(player.getUUID(), false);
                if (profile.getAvailableSlotChallenges() == null || profile.getAvailableSlotChallenges().isEmpty())
                {
                    profile.AddDefaultSlotChallenges();
                }
            }

            ChallengeMenuGUI gui = new ChallengeMenuGUI(api, api.getOrCreateProfile(player.getUUID(), false));
            gui.open();
            if (!player.hasContainerOpen())
                player.displayClientMessage(FabricAdapter.adapt(api.getMessage("commands.opening-base-gui")), false);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int handleChallengeListCommand(CommandContext<CommandSourceStack> context) {
        try {
            String listName = StringArgumentType.getString(context, "list");

            CommandSourceStack source = context.getSource();

            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            ServerPlayer player = source.getPlayer();
            ChallengeList challengeList = api.getChallengeList(listName);

            if (challengeList == null) {
                source.sendFailure(FabricAdapter.adapt(api.getMessage("issues.unknown_challenge_list", "{challenge_list}", listName)));
                return 1;
            }

            PlayerProfile profile = api.getOrCreateProfile(player.getUUID(), false);
            if (!profile.containsWindowGUIForList(listName)) {
                profile.setWindowGUI(listName, new ChallengeListGUI(api, profile, challengeList, api.getConfigOptions().getChallengeGuiConfig(challengeList.getName())));
            }
            profile.openWindowGUIForList(listName);
            
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int handleInfoCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            String box = ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "========================" + "\n"
                    + ChatFormatting.RESET + "" + ChatFormatting.GOLD + " Cobblemon Challenges "
                    + ChatFormatting.GRAY + "v" + FabricLoader.getInstance().getModContainer("cobblemonchallenges").orElseThrow().getMetadata().getVersion().getFriendlyString() + "\n"
                    + ChatFormatting.WHITE + " Fork maintained by: " + ChatFormatting.YELLOW + "pioavenger " + ChatFormatting.WHITE + "!\n"
                    + ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "========================";

            Component message = Component.literal(box);
            source.sendSystemMessage(message);
        } catch (NoSuchElementException e) {
            CobbleChallengeMod.logger.error("Could not return plugin version: %s".formatted(e.getMessage()));
            e.printStackTrace();
            return 1;     
        }

        return 0;
    }

    private static int handleMigrateLegacyCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            if (source.isPlayer()) {
                source.sendSystemMessage(Component.literal("Players can not use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            if (!api.getConfigOptions().isUsingPools()) {
                source.sendSystemMessage(Component.literal("Migration only applies when using slot-based mode.").withStyle(ChatFormatting.RED));
                return 1;
            }

            api.loadLegacyProfiles();
            List<PlayerProfile> profiles = api.getProfiles();

            int migratedCount = 0;
            for (PlayerProfile profile : profiles) {
                List<ChallengeList> allLists = new ArrayList<>(api.getChallengeLists());

                for (ChallengeList list : allLists) {
                    String listName = list.getName();

                    List<ChallengeProgress> legacyProgressList = profile.getActiveChallengesMap().getOrDefault(listName, Collections.emptyList());
                    Set<Integer> migratedSlots = new HashSet<>();

                    for (ChallengeProgress legacyProgress : legacyProgressList) {
                        Challenge legacyChallenge = legacyProgress.getActiveChallenge();
                        if (legacyChallenge == null) continue;

                        for (Map.Entry<Integer, List<Challenge>> slotEntry : list.getSlotPools().entrySet()) {
                            int slot = slotEntry.getKey();
                            List<Challenge> pool = slotEntry.getValue();

                            if (migratedSlots.contains(slot)) continue;

                            if (pool.stream().anyMatch(c -> c.getName().equals(legacyChallenge.getName()))) {
                                Challenge newChallenge = list.getChallenge(legacyChallenge.getName());
                                if (newChallenge == null) continue;

                                ChallengeProgress newProgress = list.buildNewProgressForQuest(newChallenge, profile);

                                for (Pair<String, Progression<?>> legacyProgPair : legacyProgress.getProgressionMap()) {
                                    for (Pair<String, Progression<?>> progressPair : newProgress.getProgressionMap()) {
                                        if (progressPair.getKey().equals(legacyProgPair.getKey())) {
                                            Progression<?> targetProg = progressPair.getValue();
                                            if (targetProg != null) {
                                                copyProgressionData(legacyProgPair.getValue(), targetProg);
                                                break;
                                            }
                                        }
                                    }
                                }

                                profile.setProgressForSlot(listName, slot, newProgress);
                                migratedSlots.add(slot);
                                migratedCount++;
                                break;
                            }
                        }
                    }

                    Map<Integer, Challenge> available = new LinkedHashMap<>();
                    for (Map.Entry<Integer, List<Challenge>> slotPool : list.getSlotPools().entrySet()) {
                        int availableSlot = slotPool.getKey();
                        List<Challenge> availablePool = slotPool.getValue();

                        Challenge chosen;

                        // Use migrated challenge if present
                        ChallengeProgress prog = profile.getProgressForSlot(listName, availableSlot);
                        if (prog != null && prog.getActiveChallenge() != null) {
                            chosen = prog.getActiveChallenge();
                        } else {
                            // Otherwise default to first in pool
                            chosen = availablePool.isEmpty() ? null : availablePool.get(0);
                        }

                        if (chosen != null) {
                            available.put(availableSlot, chosen);
                        }
                    }

                    // Store available map in profile
                    profile.setAvailableChallengesForList(listName, available);

                    // Clear old legacy entries for this list now that we’ve migrated
                    profile.getActiveChallengesMap().remove(listName);
                }
            }
            api.reloadConfig();

            source.sendSystemMessage(Component.literal("Migrated " + migratedCount + " legacy challenges to slot-based mode.").withStyle(ChatFormatting.GREEN));

            context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMessage("commands.migrate")));
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static void copyProgressionData(Progression<?> oldProg, Progression<?> newProg) {
        try {
            Field[] fields = oldProg.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(oldProg);

                Field targetField = newProg.getClass().getDeclaredField(field.getName());
                targetField.setAccessible(true);
                targetField.set(newProg, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}