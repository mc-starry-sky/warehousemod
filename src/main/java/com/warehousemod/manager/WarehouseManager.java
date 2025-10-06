package com.warehousemod.manager;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import com.warehousemod.screen.WarehouseScreenHandlerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WarehouseManager {
    private static final String WAREHOUSE_FILE = "public_warehouse.dat";
    private SimpleInventory warehouseInventory;
    private Path warehouseFile;
    private MinecraftServer server;

    public WarehouseManager(MinecraftServer server) {
        this.server = server;
        this.warehouseInventory = new SimpleInventory(54);
        loadWarehouse();
    }

    public void openWarehouse(ServerPlayerEntity player) {
        // 创建 NamedScreenHandlerFactory
        NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {
            @Override
            public net.minecraft.screen.ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, net.minecraft.entity.player.PlayerEntity player) {
                return new com.warehousemod.screen.WarehouseScreenHandler(syncId, inv, WarehouseManager.this.warehouseInventory);
            }

            @Override
            public Text getDisplayName() {
                return Text.literal("公共仓库");
            }
        };
        player.openHandledScreen(factory);
    }

    public void clearWarehouse(ServerPlayerEntity player) {
        for (int i = 0; i < warehouseInventory.size(); i++) {
            ItemStack stack = warehouseInventory.getStack(i);
            if (!stack.isEmpty()) {
                player.dropItem(stack, false, false);
                warehouseInventory.setStack(i, ItemStack.EMPTY);
            }
        }
        saveWarehouse();
    }

    public Map<String, Integer> getWarehouseItems() {
        Map<String, Integer> items = new HashMap<>();
        int nonEmptySlots = 0;

        for (int i = 0; i < warehouseInventory.size(); i++) {
            ItemStack stack = warehouseInventory.getStack(i);
            if (!stack.isEmpty()) {
                nonEmptySlots++;
                // 检查是否为潜影盒
                if (isShulkerBox(stack)) {
                    // 递归读取潜影盒内的物品
                    Map<String, Integer> shulkerItems = getShulkerBoxItems(stack);
                    shulkerItems.forEach((item, count) -> {
                        items.put(item, items.getOrDefault(item, 0) + count);
                    });
                } else {
                    // 普通物品
                    String itemId = stack.getItem().getTranslationKey();
                    items.put(itemId, items.getOrDefault(itemId, 0) + stack.getCount());
                }
            }
        }

        com.warehousemod.WarehouseMod.LOGGER.info("仓库统计: {} 个非空槽位, {} 种不同物品", nonEmptySlots, items.size());
        return items;
    }

    /**
     * 检查物品是否为潜影盒
     */
    private boolean isShulkerBox(ItemStack stack) {
        String itemId = stack.getItem().getTranslationKey();
        return itemId.contains("shulker_box");
    }

    /**
     * 读取潜影盒内的所有物品
     */
    private Map<String, Integer> getShulkerBoxItems(ItemStack shulkerBox) {
        Map<String, Integer> items = new HashMap<>();

        try {
            // 方法1: 使用组件系统获取潜影盒内容
            var containerComponent = shulkerBox.get(net.minecraft.component.DataComponentTypes.CONTAINER);
            if (containerComponent != null) {
                // 遍历容器内的所有物品
                for (ItemStack nestedStack : containerComponent.iterateNonEmpty()) {
                    if (!nestedStack.isEmpty()) {
                        // 递归检查嵌套的潜影盒
                        if (isShulkerBox(nestedStack)) {
                            Map<String, Integer> nestedItems = getShulkerBoxItems(nestedStack);
                            nestedItems.forEach((nestedItem, nestedCount) -> {
                                items.put(nestedItem, items.getOrDefault(nestedItem, 0) + nestedCount);
                            });
                        } else {
                            String itemId = nestedStack.getItem().getTranslationKey();
                            items.put(itemId, items.getOrDefault(itemId, 0) + nestedStack.getCount());
                        }
                    }
                }
                com.warehousemod.WarehouseMod.LOGGER.info("通过组件系统读取潜影盒内容: {} 种物品", items.size());
                return items;
            } else {
                // 如果容器组件为空，使用简化方法
                com.warehousemod.WarehouseMod.LOGGER.warn("潜影盒容器组件为空");
                trySimpleShulkerBoxReading(shulkerBox, items);
            }
        } catch (Exception e) {
            com.warehousemod.WarehouseMod.LOGGER.warn("读取潜影盒物品失败: {}", e.getMessage());
            // 如果组件方法失败，使用简化方法
            trySimpleShulkerBoxReading(shulkerBox, items);
        }

        return items;
    }

    /**
     * 简化方法：作为最后的备选方案
     */
    private void trySimpleShulkerBoxReading(ItemStack shulkerBox, Map<String, Integer> items) {
        try {
            // 只是简单地添加潜影盒本身作为物品
            String shulkerBoxId = shulkerBox.getItem().getTranslationKey();
            items.put(shulkerBoxId, items.getOrDefault(shulkerBoxId, 0) + 1);

            com.warehousemod.WarehouseMod.LOGGER.info("使用简化方法，只统计潜影盒本身");
        } catch (Exception e3) {
            com.warehousemod.WarehouseMod.LOGGER.warn("简化方法也失败: {}", e3.getMessage());
        }
    }

    /**
     * 将物品ID转换为翻译键格式
     * 例如: "minecraft:stone" -> "block.minecraft.stone"
     *        "minecraft:stick" -> "item.minecraft.stick"
     */
    private String convertItemIdToTranslationKey(String itemId) {
        if (itemId.startsWith("minecraft:")) {
            String itemName = itemId.substring(10); // 移除 "minecraft:"

            // 判断是方块还是物品
            if (isBlock(itemName)) {
                return "block.minecraft." + itemName;
            } else {
                return "item.minecraft." + itemName;
            }
        }

        // 对于其他命名空间的物品，保持原样
        return itemId.replace(":", ".");
    }

    /**
     * 判断物品ID是否为方块
     * 这是一个简化的判断，实际可能需要更复杂的逻辑
     */
    private boolean isBlock(String itemName) {
        // 常见的方块类型
        String[] blockIndicators = {
                "stone", "dirt", "grass", "wood", "log", "planks", "bricks", "glass",
                "wool", "concrete", "terracotta", "ore", "sand", "gravel", "clay",
                "bedrock", "obsidian", "prismarine", "purpur", "end_stone", "netherrack",
                "andesite", "diorite", "granite", "cobblestone", "mossy_cobblestone"
        };

        for (String indicator : blockIndicators) {
            if (itemName.contains(indicator)) {
                return true;
            }
        }

        // 默认情况下，如果名称包含这些词缀，则认为是方块
        return itemName.endsWith("_block") ||
                itemName.endsWith("_slab") ||
                itemName.endsWith("_stairs") ||
                itemName.endsWith("_wall") ||
                itemName.endsWith("_fence") ||
                itemName.endsWith("_leaves") ||
                itemName.endsWith("_sapling");
    }

    public SimpleInventory getInventory() {
        return warehouseInventory;
    }

    private void loadWarehouse() {
        try {
            warehouseFile = server.getSavePath(WorldSavePath.ROOT).resolve(WAREHOUSE_FILE);

            if (Files.exists(warehouseFile)) {
                NbtCompound nbt = NbtIo.read(warehouseFile);
                if (nbt != null && nbt.contains("Items")) {
                    NbtList list = nbt.getList("Items", 10);
                    // 使用新的 API
                    RegistryWrapper.WrapperLookup wrapperLookup = server.getRegistryManager();
                    warehouseInventory.readNbtList(list, wrapperLookup);
                }
            }
        } catch (IOException e) {
            com.warehousemod.WarehouseMod.LOGGER.error("加载仓库数据失败", e);
        }
    }

    public void saveWarehouse() {
        try {
            NbtCompound nbt = new NbtCompound();
            // 使用新的 API
            RegistryWrapper.WrapperLookup wrapperLookup = server.getRegistryManager();
            nbt.put("Items", warehouseInventory.toNbtList(wrapperLookup));
            NbtIo.write(nbt, warehouseFile);
        } catch (IOException e) {
            com.warehousemod.WarehouseMod.LOGGER.error("保存仓库数据失败", e);
        }
    }
}