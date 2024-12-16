package org.prefix_manager;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import java.util.Set;

public class AchievementHandler {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkPlayerAdvancements(player);
            }
        });
    }

    private static void checkPlayerAdvancements(ServerPlayerEntity player) {
        for (Advancement advancement : player.getServer().getAdvancementLoader().getAdvancements()) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
            
            if (progress.isDone()) {
                String advancementId = advancement.getId().toString();
                
                PrefixManager.getInstance().getAllPrefixes().forEach(prefix -> {
                    if (prefix.getUnlockType() == Prefix.UnlockType.ACHIEVEMENT 
                            && prefix.getUnlockCondition() != null 
                            && prefix.getUnlockCondition().equals(advancementId)) {
                        
                        if (!PrefixManager.getInstance().isUnlocked(player.getUuid(), prefix.getId())) {
                            // Разблокируем префикс
                            PrefixManager.getInstance().unlockPrefix(player.getUuid(), prefix.getId());
                            
                            // Отправляем сообщение игроку
                            String message = prefix.isHidden() 
                                ? "§d✧ Обнаружен секретный префикс: " 
                                : "§a✦ Разблокирован новый префикс: ";
                            player.sendMessage(Text.literal(message + 
                                prefix.getFormattedSymbol() + " §7" + prefix.getDisplayName()));
                            
                            // Мгновенная синхронизация
                            var buf = PacketByteBufs.create();
                            Set<String> unlockedPrefixes = PrefixManager.getInstance()
                                .getUnlockedPrefixes(player.getUuid());
                            
                            // Записываем каждый префикс отдельно
                            buf.writeInt(unlockedPrefixes.size());
                            for (String prefixId : unlockedPrefixes) {
                                buf.writeString(prefixId);
                            }
                            
                            ServerPlayNetworking.send(player, Prefix_manager.SYNC_UNLOCKED, buf);
                        }
                    }
                });
            }
        }
    }
} 