package org.prefix_manager.client;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.prefix_manager.Prefix;
import org.prefix_manager.PrefixManager;
import org.prefix_manager.Prefix_manager;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PrefixScreen extends BaseUIModelScreen<FlowLayout> {
    
    public PrefixScreen() {
        super(FlowLayout.class, DataSource.asset(new Identifier(Prefix_manager.MOD_ID, "prefix_screen")));
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        UUID playerUuid = MinecraftClient.getInstance().player.getUuid();
        GridLayout grid = rootComponent.childById(GridLayout.class, "prefix-grid");
        
        if (grid != null) {
            updatePrefixGrid(grid, playerUuid);
        }
    }

    private void updatePrefixGrid(GridLayout grid, UUID playerUuid) {
        grid.remove();
        
        // Создаем класс для хранения позиции
        class Position {
            int row = 0;
            int col = 0;
            
            void next() {
                col++;
                if (col >= 6) {
                    col = 0;
                    row++;
                }
            }
        }
        
        Position pos = new Position();
        
        // Сортируем и фильтруем префиксы
        PrefixManager.getInstance().getAllPrefixes().stream()
            .filter(prefix -> !prefix.isHidden() || PrefixManager.getInstance().isUnlocked(playerUuid, prefix.getId()))
            .sorted((p1, p2) -> {
                boolean u1 = PrefixManager.getInstance().isUnlocked(playerUuid, p1.getId());
                boolean u2 = PrefixManager.getInstance().isUnlocked(playerUuid, p2.getId());
                if (u1 == u2) return 0;
                return u1 ? -1 : 1;
            })
            .forEach(prefix -> {
                boolean isUnlocked = PrefixManager.getInstance().isUnlocked(playerUuid, prefix.getId());
                
                // Создаем контейнер для префикса
                FlowLayout container = Containers.verticalFlow(Sizing.fixed(45), Sizing.fixed(45));
                
                // Создаем кнопку префикса
                ButtonComponent button = createPrefixButton(prefix, isUnlocked);
                container.child(button);
                
                // Добавляем подсказку
                setupTooltip(container, prefix, isUnlocked);
                
                grid.child(container, pos.row, pos.col);
                pos.next();
            });
    }

    private ButtonComponent createPrefixButton(Prefix prefix, boolean isUnlocked) {
        ButtonComponent button = Components.button(
            Text.literal(isUnlocked ? prefix.getFormattedSymbol() : "§8•"),
            btn -> {
                if (isUnlocked) {
                    // Звук выбора
                    MinecraftClient.getInstance().world.playSound(
                        MinecraftClient.getInstance().player,
                        MinecraftClient.getInstance().player.getBlockPos(),
                        SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundCategory.MASTER,
                        0.5F, 1.2F);
                    
                    // Выбираем префикс
                    var buf = PacketByteBufs.create();
                    buf.writeString(prefix.getId());
                    ClientPlayNetworking.send(Prefix_manager.SELECT_PREFIX, buf);
                    MinecraftClient.getInstance().setScreen(null);
                } else {
                    // Звук блокировки
                    MinecraftClient.getInstance().world.playSound(
                        MinecraftClient.getInstance().player,
                        MinecraftClient.getInstance().player.getBlockPos(),
                        SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                        SoundCategory.MASTER,
                        0.5F, 0.8F);
                }
            });

        // Стилизация кнопки
        button.sizing(Sizing.fixed(35));
        
        return button;
    }

    private void setupTooltip(FlowLayout container, Prefix prefix, boolean isUnlocked) {
        if (isUnlocked) {
            String rarity = prefix.isHidden() ? "§d§oСекретный" : "§f";
            container.tooltip(Text.literal(
                rarity + prefix.getDisplayName() + "\n" +
                "§7Нажмите для выбора"
            ));
        } else {
            String description = switch (prefix.getUnlockType()) {
                case ACHIEVEMENT -> "§7" + prefix.getDescription();
                case SPECIAL -> "§7" + prefix.getDescription();
                case ADMIN -> "§7Выдается администрацией";
                case SECRET -> "§7???";
                default -> prefix.getDescription();
            };
            container.tooltip(Text.literal(
                "§f" + prefix.getDisplayName() + "\n" +
                description
            ));
        }
    }
} 