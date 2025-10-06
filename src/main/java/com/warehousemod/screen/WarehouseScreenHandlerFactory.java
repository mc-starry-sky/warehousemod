package com.warehousemod.screen;

import com.warehousemod.WarehouseMod;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

public class WarehouseScreenHandlerFactory implements NamedScreenHandlerFactory {
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new WarehouseScreenHandler(syncId, inv, WarehouseMod.warehouseManager.getInventory());
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("公共仓库");
    }
}