package org.prefix_manager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.Map;

public class Prefix_manager implements ModInitializer {
    public static final String MOD_ID = "prefix_manager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public static final Identifier SYNC_PREFIXES = new Identifier(MOD_ID, "sync_prefixes");
    public static final Identifier SELECT_PREFIX = new Identifier(MOD_ID, "select_prefix");
    public static final Identifier SYNC_UNLOCKED = new Identifier(MOD_ID, "sync_unlocked");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Prefix Manager");
        
        // Инициализация менеджера префиксов
        PrefixManager prefixManager = PrefixManager.getInstance();
        
        // Регистрация плейсхолдера
        PrefixPlaceholder.register();
        
        // Регистрация команды
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
            PrefixCommand.register(dispatcher));
        
        // Регистрация обработчиков пакетов
        ServerPlayNetworking.registerGlobalReceiver(SELECT_PREFIX, (server, player, handler, buf, responseSender) -> {
            String selectedPrefix = buf.readString();
            if (prefixManager.hasPrefix(selectedPrefix)) {
                prefixManager.setPlayerPrefix(player.getUuid(), selectedPrefix);
            }
        });
        
        // Регистрируем обработчик достижений
        AchievementHandler.register();
        
        // Регистрируем обработчик подключения игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Отправляем все префиксы
            var buf = PacketByteBufs.create();
            Map<String, Prefix> prefixMap = PrefixManager.getInstance().getAllPrefixesMap();
            
            // Записываем количество префиксов
            buf.writeInt(prefixMap.size());
            
            // Записываем каждый префикс
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
            
            sender.sendPacket(SYNC_PREFIXES, buf);

            // Отправляем разблокированные префиксы
            var unlockedBuf = PacketByteBufs.create();
            Set<String> unlockedPrefixes = PrefixManager.getInstance().getUnlockedPrefixes(handler.player.getUuid());
            
            // Записываем каждый префикс отдельно
            unlockedBuf.writeInt(unlockedPrefixes.size());
            for (String prefixId : unlockedPrefixes) {
                unlockedBuf.writeString(prefixId);
            }
            
            sender.sendPacket(SYNC_UNLOCKED, unlockedBuf);
        });
    }
}
