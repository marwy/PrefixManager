package org.prefix_manager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.util.Identifier;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Map;

public class PrefixCommand {
    private static final Identifier OPEN_SCREEN = new Identifier(Prefix_manager.MOD_ID, "open_screen");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("prefix")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if (player != null) {
                    ServerPlayNetworking.send(player, OPEN_SCREEN, PacketByteBufs.create());
                    return 1;
                }
                return 0;
            }));

        // Команды для админов
        dispatcher.register(CommandManager.literal("prefixadmin")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("unlock")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                .then(CommandManager.argument("prefixId", StringArgumentType.word())
                .executes(context -> {
                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                    String prefixId = StringArgumentType.getString(context, "prefixId");
                    
                    if (PrefixManager.getInstance().hasPrefix(prefixId)) {
                        PrefixManager.getInstance().unlockPrefix(target.getUuid(), prefixId);
                        
                        // Отправляем обновленный список разблокированных префиксов
                        var buf = PacketByteBufs.create();
                        Set<String> unlockedPrefixes = PrefixManager.getInstance().getUnlockedPrefixes(target.getUuid());
                        buf.writeInt(unlockedPrefixes.size());
                        for (String id : unlockedPrefixes) {
                            buf.writeString(id);
                        }
                        ServerPlayNetworking.send(target, Prefix_manager.SYNC_UNLOCKED, buf);
                        
                        context.getSource().sendFeedback(() -> 
                            Text.literal("§aПрефикс " + prefixId + " разблокирован для " + target.getName().getString()), true);
                        return 1;
                    } else {
                        context.getSource().sendError(Text.literal("§cПрефикс не найден!"));
                        return 0;
                    }
                }))))
            .then(CommandManager.literal("list")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> 
                        Text.literal("§6Доступные префиксы:§r\n" + 
                            PrefixManager.getInstance().getAllPrefixes().stream()
                                .map(p -> "- " + p.getId() + " (" + p.getDisplayName() + ")")
                                .collect(Collectors.joining("\n"))), false);
                    return 1;
                }))
            .then(CommandManager.literal("reload")
                .executes(context -> {
                    // Перезагружаем префиксы
                    PrefixManager.getInstance().loadData();
                    
                    // Отправляем обновленные данные всем игрокам
                    for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                        // Отправляем все префиксы
                        var buf = PacketByteBufs.create();
                        Map<String, Prefix> prefixMap = PrefixManager.getInstance().getAllPrefixesMap();
                        
                        buf.writeInt(prefixMap.size());
                        prefixMap.forEach((id, prefix) -> {
                            buf.writeString(id);
                            buf.writeString(prefix.getId());
                            buf.writeString(prefix.getDisplayName());
                            buf.writeString(prefix.getSymbol());
                            buf.writeString(prefix.getColor());
                            buf.writeString(prefix.getDescription());
                            buf.writeEnumConstant(prefix.getUnlockType());
                            buf.writeString(prefix.getUnlockCondition() != null ? prefix.getUnlockCondition() : "");
                            buf.writeBoolean(prefix.isHidden());
                        });
                        ServerPlayNetworking.send(player, Prefix_manager.SYNC_PREFIXES, buf);

                        // Отправляем разблокированные префиксы
                        var unlockedBuf = PacketByteBufs.create();
                        Set<String> unlockedPrefixes = PrefixManager.getInstance().getUnlockedPrefixes(player.getUuid());
                        unlockedBuf.writeInt(unlockedPrefixes.size());
                        for (String prefixId : unlockedPrefixes) {
                            unlockedBuf.writeString(prefixId);
                        }
                        ServerPlayNetworking.send(player, Prefix_manager.SYNC_UNLOCKED, unlockedBuf);
                    }
                    
                    context.getSource().sendFeedback(() -> 
                        Text.literal("§aПрефиксы успешно перезагружены"), true);
                    return 1;
                })));
    }
} 