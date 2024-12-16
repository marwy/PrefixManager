package org.prefix_manager.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.prefix_manager.Prefix;
import org.prefix_manager.Prefix_manager;
import org.prefix_manager.PrefixManager;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class Prefix_managerClient implements ClientModInitializer {
    private static final Identifier OPEN_SCREEN = new Identifier(Prefix_manager.MOD_ID, "open_screen");

    @Override
    public void onInitializeClient() {
        // Регистрируем обработчик открытия экрана
        ClientPlayNetworking.registerGlobalReceiver(OPEN_SCREEN, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                MinecraftClient.getInstance().setScreen(new PrefixScreen());
            });
        });

        // Обработчик синхронизации префиксов
        ClientPlayNetworking.registerGlobalReceiver(Prefix_manager.SYNC_PREFIXES, (client, handler, buf, responseSender) -> {
            Map<String, Prefix> prefixes = new HashMap<>();
            int count = buf.readInt();
            
            for (int i = 0; i < count; i++) {
                String mapKey = buf.readString();
                String id = buf.readString();
                String displayName = buf.readString();
                String symbol = buf.readString();
                String color = buf.readString();
                String description = buf.readString();
                Prefix.UnlockType unlockType = buf.readEnumConstant(Prefix.UnlockType.class);
                String unlockCondition = buf.readString();
                boolean hidden = buf.readBoolean();
                
                prefixes.put(mapKey, new Prefix(id, displayName, symbol, color, description, 
                    unlockType, unlockCondition.isEmpty() ? null : unlockCondition, hidden));
            }
            
            client.execute(() -> {
                PrefixManager.getInstance().updatePrefixes(prefixes);
            });
        });

        // Обработчик синхронизации разблокированных префиксов
        ClientPlayNetworking.registerGlobalReceiver(Prefix_manager.SYNC_UNLOCKED, (client, handler, buf, responseSender) -> {
            Set<String> unlockedPrefixes = new HashSet<>();
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                unlockedPrefixes.add(buf.readString());
            }
            
            client.execute(() -> {
                PrefixManager.getInstance().setUnlockedPrefixes(client.player.getUuid(), unlockedPrefixes);
            });
        });
    }
}
