package com.warehousemod.mixin;

import com.warehousemod.WarehouseMod;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity player, CallbackInfo ci) {
        // 玩家离开时自动保存仓库
        if (WarehouseMod.warehouseManager != null) {
            WarehouseMod.warehouseManager.saveWarehouse();
        }
    }
}