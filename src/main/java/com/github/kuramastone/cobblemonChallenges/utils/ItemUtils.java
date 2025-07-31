package com.github.kuramastone.cobblemonChallenges.utils;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.bUtilities.configs.ItemConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemUtils {

    public static ItemStack createItemStack(String material, int amount, @Nullable Component name, @Nullable List<Component> lore, @Nullable Map<String, Integer> enchants, int customModelData) {

        // Validate and retrieve the material from the item registry
        String materialName = material.toLowerCase();
        Item materialItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(materialName));
        if (materialItem == Items.AIR) {
            throw new IllegalArgumentException("Invalid material: " + materialName);
        }

        // Create the ItemStack with the specified amount
        ItemStack itemStack = new ItemStack(materialItem, amount);

        if (name != null)
            itemStack.set(DataComponents.CUSTOM_NAME, name);

        if (customModelData != 0) {
            itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
        }

        // Set lore if provided
        if (lore != null) {
            List<Component> loreText = new ArrayList<>();
            for (Component loreLine : lore) {
                loreText.add(loreLine);
            }
            itemStack.set(DataComponents.LORE, new ItemLore(loreText));
        }

        // Add enchantments if specified
        if (enchants != null) {
            for (Map.Entry<String, Integer> enchantEntry : enchants.entrySet()) {
                String enchantName = enchantEntry.getKey().startsWith("minecraft:") ? enchantEntry.getKey() : "minecraft:" + enchantEntry.getKey();
                int enchantLevel = enchantEntry.getValue();
                ResourceLocation resourceLocation = ResourceLocation.parse(enchantName);

                Optional<Holder.Reference<Enchantment>> enchantment = CobbleChallengeMod.getMinecraftServer().overworld().registryAccess().lookup(Registries.ENCHANTMENT)
                        .orElseThrow().get(ResourceKey.create(Registries.ENCHANTMENT, resourceLocation));

                itemStack.enchant(enchantment.get(), enchantLevel);
            }
        }

        return itemStack;
    }

    public static ItemStack createItemStack(ItemConfig config) {
        return createItemStack(config.getMaterial(), config.getAmount(), FabricAdapter.adapt(ComponentEditor.decorateComponent("&r" + config.getName())),
                config.getLore() == null ? null : config.getLore().stream()
                        .map(str -> ComponentEditor.decorateComponent("&r" + str))
                        .map(FabricAdapter::adapt)
                        .map(mu -> (Component) mu).collect(Collectors.toUnmodifiableList()),
                config.getEnchants(),
                config.getCustommodeldata());
    }

    public static void setLore(ItemStack item, List<String> lore) {
        List<Component> loreText = new ArrayList<>();
        for (String loreLine : lore) {
            loreText.add(FabricAdapter.adapt(ComponentEditor.decorateComponent("&r" + loreLine)));
        }
        setLoreComponents(item, loreText);
    }

    public static void setLoreComponents(ItemStack item, List<Component> lore) {
        item.set(DataComponents.LORE, new ItemLore(lore));
    }

    public static ItemStack setItem(ItemStack itemstack, Item item) {
        Map<String, Integer> enchants = new HashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> set : itemstack.getEnchantments().entrySet()) {
            String enchant = set.getKey().getRegisteredName();
            int level = set.getIntValue();
            enchants.put(enchant, level);
        }

        int cmd = 0;
        if (itemstack.has(DataComponents.CUSTOM_MODEL_DATA))
            cmd = itemstack.get(DataComponents.CUSTOM_MODEL_DATA).value();

        return createItemStack(BuiltInRegistries.ITEM.getKey(item).toString(), itemstack.getCount(), itemstack.get(DataComponents.CUSTOM_NAME),
                itemstack.get(DataComponents.LORE) == null ? null : itemstack.get(DataComponents.LORE).lines(),
                enchants, cmd);
    }

}
