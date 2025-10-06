package com.warehousemod;

import com.warehousemod.manager.SchematicManager;
import com.warehousemod.manager.WarehouseManager;
import com.warehousemod.command.WarehouseCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarehouseMod implements ModInitializer {
    public static final String MOD_ID = "warehousemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 添加管理器实例
    public static WarehouseManager warehouseManager;
    public static SchematicManager schematicManager;

    @Override
    public void onInitialize() {
        LOGGER.info("初始化公共仓库模组");
        LOGGER.info("当前环境: {}", FabricLoader.getInstance().getEnvironmentType());

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LOGGER.info("注册仓库命令，环境: {}", environment);
            WarehouseCommands.register(dispatcher);
        });

        // 使用事件系统初始化管理器
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("服务器启动中，初始化仓库管理器");
            warehouseManager = new WarehouseManager(server);
            schematicManager = new SchematicManager(server);
            LOGGER.info("仓库管理器已初始化");
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("服务器已启动，仓库管理器状态: {}", warehouseManager != null ? "已初始化" : "未初始化");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (warehouseManager != null) {
                warehouseManager.saveWarehouse();
                LOGGER.info("服务器关闭，仓库数据已保存");
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (warehouseManager != null) {
                warehouseManager.saveWarehouse();
                LOGGER.info("玩家离开，仓库数据已保存");
            }
        });
    }
}