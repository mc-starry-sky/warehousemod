package com.warehousemod.command;

import com.warehousemod.WarehouseMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.HashMap;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarehouseCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("o")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        if (WarehouseMod.warehouseManager != null) {
                            WarehouseMod.warehouseManager.openWarehouse(player);
                        } else {
                            player.sendMessage(Text.literal("§c仓库管理器未初始化，请稍后重试"), false);
                        }
                    }
                    return 1;
                })
        );

        dispatcher.register(literal("cr")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        if (WarehouseMod.warehouseManager != null) {
                            WarehouseMod.warehouseManager.clearWarehouse(player);
                            player.sendMessage(Text.literal("§a已清空仓库并将物品扔出"), false);
                        } else {
                            player.sendMessage(Text.literal("§c仓库管理器未初始化，请稍后重试"), false);
                        }
                    }
                    return 1;
                })
        );

        dispatcher.register(literal("c")
                .then(argument("schematic", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            String schematicName = StringArgumentType.getString(context, "schematic");

                            if (player != null) {
                                calculateMaterials(player, schematicName);
                            }
                            return 1;
                        })
                )
        );
    }

    private static void calculateMaterials(ServerPlayerEntity player, String schematicName) {
        try {
            if (WarehouseMod.warehouseManager == null || WarehouseMod.schematicManager == null) {
                player.sendMessage(Text.literal("§c仓库管理器或原理图管理器未初始化，请稍后重试"), false);
                return;
            }

            // 获取仓库物品并显示调试信息
            Map<String, Integer> warehouseItems = WarehouseMod.warehouseManager.getWarehouseItems();
            player.sendMessage(Text.literal("§e仓库中现有物品数量: " + warehouseItems.size()), false);
            if (!warehouseItems.isEmpty()) {
                warehouseItems.forEach((item, count) -> {
                    // 使用物品ID创建可翻译的文本
                    Text displayText = Text.translatable(item).copy().formatted(Formatting.GRAY)
                            .append(Text.literal(": " + count).formatted(Formatting.WHITE));
                    player.sendMessage(Text.literal("§7- ").append(displayText), false);
                });
            }

            // 获取原理图材料
            player.sendMessage(Text.literal("§e正在解析原理图: " + schematicName), false);
            Map<String, Integer> schematicMaterials = WarehouseMod.schematicManager.getSchematicMaterials(schematicName);

            player.sendMessage(Text.literal("§e原理图解析完成，找到材料种类: " + schematicMaterials.size()), false);
            schematicMaterials.forEach((item, count) -> {
                // 使用物品ID创建可翻译的文本
                Text displayText = Text.translatable(item).copy().formatted(Formatting.GRAY)
                        .append(Text.literal(": " + count).formatted(Formatting.WHITE));
                player.sendMessage(Text.literal("§7- ").append(displayText), false);
            });

            // 构建材料计算报告
            Text resultHeader = Text.literal("=== 材料计算报告 ===").formatted(Formatting.GOLD, Formatting.BOLD);
            Text schematicInfo = Text.literal("原理图: " + schematicName).formatted(Formatting.GREEN);

            player.sendMessage(resultHeader, false);
            player.sendMessage(schematicInfo, false);
            player.sendMessage(Text.literal(""), false); // 空行

            boolean hasMissing = false;
            for (Map.Entry<String, Integer> entry : schematicMaterials.entrySet()) {
                String item = entry.getKey();
                int needed = entry.getValue();
                int has = warehouseItems.getOrDefault(item, 0);
                int missing = Math.max(0, needed - has);

                // 创建物品名称的翻译文本
                Text itemName = Text.translatable(item);

                if (missing > 0) {
                    hasMissing = true;
                    Text missingText = Text.literal("缺少: ")
                            .formatted(Formatting.RED)
                            .append(itemName.copy().formatted(Formatting.RED))
                            .append(Text.literal(" - 需要: " + needed + ", 现有: " + has + ", 缺少: " + missing).formatted(Formatting.RED));
                    player.sendMessage(missingText, false);
                } else {
                    Text sufficientText = Text.literal("充足: ")
                            .formatted(Formatting.GREEN)
                            .append(itemName.copy().formatted(Formatting.GREEN))
                            .append(Text.literal(" - 需要: " + needed + ", 现有: " + has).formatted(Formatting.GREEN));
                    player.sendMessage(sufficientText, false);
                }
            }

            player.sendMessage(Text.literal(""), false); // 空行
            if (!hasMissing) {
                player.sendMessage(Text.literal("✓ 所有材料都充足！").formatted(Formatting.DARK_GREEN, Formatting.BOLD), false);
            } else {
                player.sendMessage(Text.literal("⚠ 部分材料不足，请补充仓库").formatted(Formatting.YELLOW), false);
            }

        } catch (Exception e) {
            WarehouseMod.LOGGER.error("计算材料时出错", e);
            player.sendMessage(Text.literal("§c错误: " + e.getMessage()), false);
            player.sendMessage(Text.literal("§e提示: 请检查原理图文件是否存在且格式正确"), false);
            player.sendMessage(Text.literal("§e支持的格式: .litematic, .schematic, .schem, .nbt"), false);
        }
    }
}