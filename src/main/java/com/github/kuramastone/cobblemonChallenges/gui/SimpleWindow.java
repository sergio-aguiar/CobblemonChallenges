package com.github.kuramastone.cobblemonChallenges.gui;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Create a chest inventory using data from a GuiConfig.
 * Each item
 */
public class SimpleWindow {

    private Character listCharacter = '.';

    private GuiConfig guiConfig; // details about how to build the config

    private CustomContainer gui; // gui object
    private CustomMenu menu; // menu object
    private int rows; // number of rows in the inventory
    private Map<Integer, WindowItem> itemPerSlot;  // item slot in the inventory and the WindowItem. Multiple slots can reference the same WindowItem

    private List<WindowItem> contents; // list of items to display in menus
    private int currentPage = 0;
    private int entriesPerPage = 0;
    private List<SlotID> contentSlots; // order to insert slots in. only in horizontal order for now.

    private Set<ServerPlayer> playersShown;

    public SimpleWindow(GuiConfig guiConfig) {
        Objects.requireNonNull(guiConfig, "Cannot load settings from a null GuiConfig.");
        this.guiConfig = guiConfig.copy().apply(this);
        validate();
        rows = this.guiConfig.getStructure().size();
        this.playersShown = new HashSet<>();
    }

    /**
     * If player is viewing an inventory, they must first close it before they can safely receive another. We do this by sending it on the next tick
     *
     * @param player
     */
    public void show(ServerPlayer player) {

        Objects.requireNonNull(player, "Player cannot be null");
        Component name = ComponentEditor.decorateComponent(guiConfig.getWindowName());
        int rows = guiConfig.getStructure().size();
        CustomContainer container = new CustomContainer(rows);
        container.setClickRunnable(this::handleClick);

        playersShown.add(player);
        this.gui = container;
        buildInventory();

        for (WindowItem windowItem : itemPerSlot.values()) {
            if (windowItem != null) {
                windowItem.restartAutoUpdate(15);
            }
        }

        player.openMenu(new CustomProvider(name, rows, container));
        this.menu = (CustomMenu) player.containerMenu;
    }

    public WindowItem getItemAtContentSlot(int contentIndex) {
        if (contentIndex <= 0 || contentIndex > contentSlots.size()) return null;
        int realSlot = contentSlots.get(contentIndex - 1).slotID;
        return itemPerSlot.get(realSlot);
    }

    public int getContentIndex(int realSlot) {
        if (realSlot < 0 || contentSlots == null) {
            return -1;
        }

        for (int i = 0; i < contentSlots.size(); i++) {
            if (contentSlots.get(i).slotID == realSlot) {
                return i + 1;
            }
        }
        return -1;
    }

    public int getRealSlot(int contentIndex) {
        if (contentIndex <= 0 || contentIndex > contentSlots.size()) { 
            return -1;
        } else {
            return contentSlots.get(contentIndex - 1).slotID;
        }
    }

    public int getFirstRealSlot() {
        return contentSlots.get(0).slotID;
    }

    public int getLastRealSlot() {
        CobbleChallengeMod.logger.info(contentSlots.stream()
            .map(slot -> String.valueOf(slot.slotID))
            .collect(Collectors.joining(", ", "[", "]")));
        return contentSlots.get(contentSlots.size() - 1).slotID;
    }

    public Map<Integer, WindowItem> getItemsPerSlot() {
        return itemPerSlot != null ? itemPerSlot : Collections.emptyMap();
    }

    private static MenuType<?> getMenuTypeForRows(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> throw new IllegalStateException("Unexpected value: " + rows);
        };
    }

    /**
     * Use the item builder to update the item in the slot
     *
     * @param item
     */
    public void updateSlot(WindowItem item, boolean updateEmpty) {
        Objects.requireNonNull(item, "Cannot update the slot of a null WindowItem");

        for (Map.Entry<Integer, WindowItem> set : this.itemPerSlot.entrySet()) {
            if (set.getValue() == item) {
                ItemStack display = item.getDisplayItem();
                if (display != null && (display != ItemStack.EMPTY || updateEmpty)) {
                    gui.setItem(set.getKey(), display);
                }
            }
        }


    }

    /**
     * Fill inventory with items by calling the windowitem's builders
     */
    public void buildInventory() {
        itemPerSlot = new HashMap<>();

        // insert regular items
        int row = 0;
        for (String line : guiConfig.getStructure()) {
            int col = 0;
            for (char c : line.toCharArray()) {
                int slotID = row * 9 + col;
                if (c == ' ')
                    continue;

                WindowItem item = guiConfig.getIngredients().get(c);


                if (item != null) {
                    itemPerSlot.put(slotID, item);
                }

                col++;
            }

            row++;
        }

        // insert contents
        if (this.contents != null) {
            for (int index = 0; index < this.contentSlots.size(); index++) {
                SlotID slot = this.contentSlots.get(index);
                int slotID = slot.slotID;

                int actualIndex = index + (entriesPerPage * currentPage);

                WindowItem wi = null;
                if(actualIndex < this.contents.size()) {
                    wi = this.contents.get(actualIndex);
                }

                itemPerSlot.put(slotID, wi);
            }
        }

        // use a set to avoid duplicate updates
        for (WindowItem item : new HashSet<>(this.itemPerSlot.values())) {
            if(item != null) {
                updateSlot(item, false);
            }
        }

    }

    public CustomContainer getSimpleGui() {
        return gui;
    }

    private ItemStack handleClick(ClickType type, int dragType, int pSlotID, Player player) {
        WindowItem item = itemPerSlot.get(pSlotID);

        if (item == null) {
            return ItemStack.EMPTY;
        }

        return item.handleClick(type, dragType, player);
    }

    public SimpleWindow setListCharacter(Character listCharacter) {
        this.listCharacter = listCharacter;
        return this;
    }

    public SimpleWindow setContents(List<WindowItem> contents) {
        this.contents = contents;

        if (this.contents != null) {
            for (WindowItem item : this.contents) {
                if (item != null) {
                    item.setAutoUpdate(20, () -> true);
                }
            }
        }

        return this;
    }

    private void validate() {
        Objects.requireNonNull(guiConfig);

        Objects.requireNonNull(guiConfig.getStructure(), "Structure cannot be null for gui.");
        Objects.requireNonNull(guiConfig.getIngredients(), "Ingredients cannot be null for gui.");

        if (guiConfig.getStructure().size() == 0)
            throw new RuntimeException("Structure cannot have a length of 0.");

        for (String line : guiConfig.getStructure()) {
            if ("# # # # # # # # #".length() != line.length()) {
                throw new RuntimeException(String.format("Invalid format for structure. '%s'", line));
            }
        }


        // count entries per page by checking for the list character
        contentSlots = new ArrayList<>();
        int listMarkerCount = 0;
        int row = 0;
        for (String line : guiConfig.getStructure()) {
            int col = 0;
            for (Character c : line.toCharArray()) {
                if (c == ' ')
                    continue;

                int slotID = row * 9 + col;
                if (c == listCharacter) {
                    listMarkerCount++;
                    contentSlots.add(new SlotID(slotID));
                }
                col++;
            }
            row++;
        }
        this.entriesPerPage = listMarkerCount;

    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, currentPage);
        buildInventory();
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public void notifyAllItems() {
        for (WindowItem value : this.itemPerSlot.values()) {
            if (value != null) {
                updateSlot(value, false);
            }
        }
    }

    private static class SlotID {
        public int slotID;

        public SlotID(int slotID) {
            this.slotID = slotID;
        }
    }

    public boolean isAnyoneViewing() {
        for(ServerPlayer player : this.playersShown) {
            if(player.containerMenu == menu) {
                return true;
            }
        }
        return false;
    }

    private static class CustomProvider implements MenuProvider {

        private Component name;
        private int rows;
        private CustomContainer container;

        public CustomProvider(Component name, int rows, CustomContainer container) {
            this.name = name;
            this.rows = rows;
            this.container = container;
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName() {
            return FabricAdapter.adapt(name);
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int inventoryID, Inventory inventory, Player player) {
            return new CustomMenu(getMenuTypeForRows(rows), inventoryID, inventory, container, rows);
        }
    }

    public List<Integer> getContentSlots() {
        return contentSlots.stream().map(slot -> slot.slotID).toList();
    }
}
