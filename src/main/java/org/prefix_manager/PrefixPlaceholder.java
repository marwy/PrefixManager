package org.prefix_manager;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public class PrefixPlaceholder {
    private static final Identifier PLAYER_PREFIX = new Identifier(Prefix_manager.MOD_ID, "prefix");
    
    public static void register() {
        // Регистрируем плейсхолдер %prefix_manager:prefix%
        Placeholders.register(PLAYER_PREFIX, (ctx, arg) -> {
            if (ctx.hasPlayer()) {
                ServerPlayerEntity player = ctx.player();
                Prefix prefix = PrefixManager.getInstance().getPlayerPrefix(player.getUuid());
                // Проверяем на null и возвращаем дефолтный префикс если нужно
                if (prefix == null) {
                    prefix = PrefixManager.getInstance().getDefaultPrefix();
                }
                return PlaceholderResult.value(prefix.getFormattedSymbol());
            }
            return PlaceholderResult.invalid("Нет игрока");
        });
    }
}