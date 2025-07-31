package com.github.kuramastone.cobblemonChallenges.utils;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.bUtilities.configs.ItemConfig;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlObject;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.events.RegisterMessagesEvent;
import com.github.kuramastone.cobblemonChallenges.gui.GuiConfig;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigOptions {

    public Map<String, ComponentEditor> messages;

    @YamlObject("challenges.active-challenge-item")
    private ItemConfig activeChallengeItem;
    @YamlObject("challenges.completed-challenge-item")
    private ItemConfig completedChallengeItem;

    private GuiConfig menuConfig; // base challenge menu
    private Map<String, GuiConfig> challengeConfigs; // config per challenge

    public ConfigOptions() {

    }

    public void load() {
        YamlConfig config = new YamlConfig(CobbleChallengeMod.defaultDataFolder(), null, null,
                "config.yml", getClass());

        YamlConfig.loadFromYaml(this, config);
        menuConfig = GuiConfig.load(config.getSection("base-menu"));
        loadMessages(config);
        loadGuiConfigPerChallenge();
    }

    private void loadGuiConfigPerChallenge() {
        File parentFolder = new File(CobbleChallengeMod.defaultDataFolder(), "challenges");

        if (!parentFolder.exists()) {
            FileUtils.copyDirectoryFromJar("/challenges", parentFolder, new String[]{"daily.yml", "weekly.yml", "monthly.yml"});
        }

        challengeConfigs = new HashMap<>();
        for (File yamlFile : YamlConfig.getYamlFiles(parentFolder)) {
            YamlConfig config = new YamlConfig(yamlFile.getParentFile(), null, null, yamlFile.getName(), getClass());

            // remove ending
            String name = yamlFile.getName();
            if (name.contains(".")) {
                name = name.substring(0, name.lastIndexOf('.')); // remove extension if possible
            }

            GuiConfig gui = GuiConfig.load(config.getSection("gui"));
            challengeConfigs.put(name, gui);
        }

    }

    /**
     * Loads all keys under "messages" and stores them for easy retrieval elsewhere
     */
    public void loadMessages(YamlConfig config) {
        messages = new HashMap<>();

        for (String subkey : config.getKeys("Messages", true)) {
            String key = "Messages." + subkey;
            if (config.isSection(key)) {
                continue;
            }

            String string;
            Object obj = config.getObject(key);
            if (obj instanceof List<?> list) {
                string = String.join("\n", list.toArray(new String[0]));
            }
            else if (obj instanceof String objStr) {
                string = objStr;
            }
            else {
                string = obj.toString();
            }

            this.messages.put(subkey, new ComponentEditor(string));
        }

        RegisterMessagesEvent.EVENT.invoker().onRegistration();
    }

    public ItemStack getCompletedChallengeItem() {
        return FabricAdapter.toItemStack(completedChallengeItem);
    }

    public ItemStack getActiveChallengeItem() {
        return FabricAdapter.toItemStack(activeChallengeItem);
    }

    public GuiConfig getMenuGuiConfig() {
        return menuConfig;
    }

    public GuiConfig getChallengeGuiConfig(String name) {
        return challengeConfigs.get(name);
    }
}
