package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockPlaceEvent {

    private BlockState blockState;
    private BlockPos blockPos;
    private Level level;
    private Player player;

    public BlockPlaceEvent(BlockState blockState, BlockPos blockPos, Level level, Player player) {
        this.blockState = blockState;
        this.blockPos = blockPos;
        this.level = level;
        this.player = player;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public Level getLevel() {
        return level;
    }

    public Player getPlayer() {
        return player;
    }

    public static void register() {
        UseBlockCallback.EVENT.register(BlockPlaceEvent::trigger);
    }

    private static InteractionResult trigger(Player player, Level level, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        try {
            if (!level.isClientSide) {
                ItemStack stack = player.getItemInHand(interactionHand);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                    BlockPos placePos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                    BlockState stateToPlace = blockItem.getBlock().defaultBlockState();

                    ChallengeListener.onBlockPlace(
                        new BlockPlaceEvent(stateToPlace, placePos, level, player)
                    );
                }
            }

            return InteractionResult.PASS;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
