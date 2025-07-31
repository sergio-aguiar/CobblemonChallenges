package com.github.kuramastone.cobblemonChallenges.gui;

import com.github.kuramastone.bUtilities.configs.ItemConfig;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;

import java.util.*;

public class GuiConfig  {

    @YamlKey("window-name")
    private String windowName = "Simple Window";

    @YamlKey("structure")
    private ArrayList<String> structure;

    @YamlKey("ingredients")
    private HashMap<Character, WindowItem> ingredients;

    private HashMap<Character, YamlConfig> miscIngredientInfo; // used to store item data

    public GuiConfig() {
        miscIngredientInfo = new HashMap<>();
    }

    public GuiConfig(String windowName, ArrayList<String> structure, HashMap<Character, WindowItem> ingredients) {
        this.windowName = windowName;
        this.structure = structure;
        this.ingredients = ingredients;
        miscIngredientInfo = new HashMap<>();
    }

    public GuiConfig copy() {
        // Create a new array for the structure
        ArrayList<String> copiedStructure = new ArrayList<>(structure.size());
        this.structure.forEach(line -> copiedStructure.add(String.valueOf(line)));

        // Create a new map for the ingredients
        HashMap<Character, WindowItem> copiedIngredients = new LinkedHashMap<>();
        for (Map.Entry<Character, WindowItem> entry : ingredients.entrySet()) {
            copiedIngredients.put(entry.getKey(), entry.getValue().copy());
        }

        // Create a new map for the ingredients
        HashMap<Character, YamlConfig> copiedIngredientInfo = new LinkedHashMap<>();
        for (Map.Entry<Character, YamlConfig> entry : this.miscIngredientInfo.entrySet()) {
            copiedIngredientInfo.put(entry.getKey(), entry.getValue().shallowCopy());
        }

        // Return a new GuiConfig instance with copied values
        GuiConfig copy =  new GuiConfig(windowName, copiedStructure, copiedIngredients);
        copy.miscIngredientInfo = copiedIngredientInfo;

        return copy;
    }

    public String getWindowName() {
        return windowName;
    }

    public List<String> getStructure() {
        return structure;
    }

    public Map<Character, WindowItem> getIngredients() {
        return ingredients;
    }

    public static GuiConfig load(YamlConfig section) {
        GuiConfig loaded = YamlConfig.loadFromYaml(new GuiConfig(), section, (obj, key, sub) -> {
            if (key.equals("ingredients")) {
                try {
                    //for each character entry, load an itemconfig
                    Map<Character, WindowItem> map = new HashMap<>();
                    for (String string : sub.getKeys("", false)) {
                        char c = string.charAt(0);
                        YamlConfig subsection = sub.getSection(string);

                        ItemConfig item = new ItemConfig(subsection);
                        WindowItem wi = new WindowItem(null, new ItemProvider.ItemWrapper(FabricAdapter.toItemStack(item)));

                        if(subsection.containsKey("commands")) {
                            wi.addCommandsOnClick(subsection.getStringList("commands"));
                        }
                        if(subsection.get("back-page", false)) {
                            wi.setPagesToTurn(-1);
                        }
                        if(subsection.get("next-page", false)) {
                            wi.setPagesToTurn(1);
                        }

                        map.put(c, wi);
                        ((GuiConfig) obj).miscIngredientInfo.put(c, subsection); // casting to self, we know this will always work safely
                    }

                    return map;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
            return null;
        });

        Objects.requireNonNull(loaded.structure, "Structure for gui cannot be null.");

        return loaded;
    }

    public YamlConfig getDataForIngredient(char c) {
        return this.miscIngredientInfo.get(c);
    }

    /**
     * Update each WindowItem in the ingredients list to apply to this window
     * @param simpleWindow
     * @return
     */
    public GuiConfig apply(SimpleWindow simpleWindow) {
        for(WindowItem item : this.ingredients.values()) {
            item.setWindow(simpleWindow);
        }

        return this;
    }
}
