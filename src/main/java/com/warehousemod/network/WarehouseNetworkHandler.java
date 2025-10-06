package com.warehousemod.network;

import net.minecraft.util.Identifier;

public class WarehouseNetworkHandler {
    // 使用新的 Identifier.of 方法
    public static final Identifier WAREHOUSE_UPDATE_PACKET = Identifier.of("warehousemod", "warehouse_update");

    public static void register() {
        // 可以在这里注册客户端和服务器之间的网络通信
        // 用于实时更新仓库界面等
    }
}