package com.github.kuramastone.cobblemonChallenges.gui;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

public class CustomMenu extends ChestMenu {

    private CustomContainer container;

    public CustomMenu(MenuType<?> menuType, int containerID, Inventory inventory, CustomContainer container, int rows) {
        super(menuType, containerID, inventory, container, rows);
        this.container = container;
    }

    @Override
    public void clicked(int slot, int button, ClickType clickType, Player player) {
        try {
            container.handleClick(slot, button,  clickType, player);
        } catch (Exception var8) {
            Exception exception = var8;
            CrashReport crashReport = CrashReport.forThrowable(exception, "Container click");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Click info");
            crashReportCategory.setDetail("Menu Class", () -> {
                return this.getClass().getCanonicalName();
            });
            crashReportCategory.setDetail("Slot Count", this.slots.size());
            crashReportCategory.setDetail("Slot", slot);
            crashReportCategory.setDetail("Button", button);
            crashReportCategory.setDetail("Type", clickType);
            throw new ReportedException(crashReport);
        }
    }
}
