package com.github.kuramastone.cobblemonChallenges.utils;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.bUtilities.configs.ItemConfig;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;

import java.awt.*;

public class FabricAdapter {

    public static ItemStack toItemStack(ItemConfig config) {
        return ItemUtils.createItemStack(config);
    }

    public static MutableComponent adapt(ComponentEditor componentEditor) {
        return adapt(componentEditor.build());
    }

    public static MutableComponent adapt(net.kyori.adventure.text.Component component) {
        if (component instanceof TextComponent) {
            // Handle basic text component
            String content = ((TextComponent) component).content();
            MutableComponent textComponent = Component.literal(content);

            // Apply styles from Kyori component
            applyStyle(component, textComponent);

            for (net.kyori.adventure.text.Component child : component.children()) {
                textComponent.append(adapt(child));
            }

            return textComponent;
        }


        throw new NotImplementedException("Cannot adapt this type of component yet.");
    }

    private static void applyStyle(net.kyori.adventure.text.Component source, MutableComponent target) {
        target.setStyle(target.getStyle().withColor(Color.WHITE.getRGB()));
        target.setStyle(target.getStyle().withBold(false));
        target.setStyle(target.getStyle().withItalic(false));
        target.setStyle(target.getStyle().withUnderlined(false));
        target.setStyle(target.getStyle().withStrikethrough(false));
        target.setStyle(target.getStyle().withObfuscated(false));

        if (source.color() != null) {
            TextColor color = source.color();
            if (color != null) {
                target.setStyle(target.getStyle().withColor(net.minecraft.network.chat.TextColor.fromRgb(color.value())));
            }
        }

        if (source.hasDecoration(TextDecoration.BOLD)) {
            target.setStyle(target.getStyle().withBold(true));
        }
        if (source.hasDecoration(TextDecoration.ITALIC)) {
            target.setStyle(target.getStyle().withItalic(true));
        }
        if (source.hasDecoration(TextDecoration.UNDERLINED)) {
            target.setStyle(target.getStyle().withUnderlined(true));
        }
        if (source.hasDecoration(TextDecoration.STRIKETHROUGH)) {
            target.setStyle(target.getStyle().withStrikethrough(true));
        }
        if (source.hasDecoration(TextDecoration.OBFUSCATED)) {
            target.setStyle(target.getStyle().withObfuscated(true));
        }
    }
}
