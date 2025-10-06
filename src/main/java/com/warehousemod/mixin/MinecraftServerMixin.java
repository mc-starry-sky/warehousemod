package com.warehousemod.mixin;

import com.warehousemod.WarehouseMod;
import com.warehousemod.manager.SchematicManager;
import com.warehousemod.manager.WarehouseManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void onServerStart(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        WarehouseMod.warehouseManager = new WarehouseManager(server);
        WarehouseMod.schematicManager = new SchematicManager(server);
        WarehouseMod.LOGGER.info("通过 Mixin 初始化仓库管理器");
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void onServerShutdown(CallbackInfo ci) {
        if (WarehouseMod.warehouseManager != null) {
            WarehouseMod.warehouseManager.saveWarehouse();
        }
    }
}