package org.prefix_manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PrefixManager {
    private static final PrefixManager INSTANCE = new PrefixManager();
    private final Map<String, Prefix> prefixes = new HashMap<>();
    private final Map<UUID, String> playerPrefixes = new HashMap<>();
    private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("prefix_manager");
    private final Path prefixesFile = configDir.resolve("prefixes.json");
    private final Path playerDataFile = configDir.resolve("player_prefixes.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Set<String>> unlockedPrefixes = new HashMap<>();
    private final Path unlockedPrefixesFile = configDir.resolve("unlocked_prefixes.json");

    private PrefixManager() {
        loadData();
    }

    public static PrefixManager getInstance() {
        return INSTANCE;
    }

    public void loadData() {
        try {
            Files.createDirectories(configDir);

            // Загрузка префиксов
            if (Files.exists(prefixesFile)) {
                Type prefixMapType = new TypeToken<Map<String, Prefix>>(){}.getType();
                try (Reader reader = Files.newBufferedReader(prefixesFile)) {
                    Map<String, Prefix> loadedPrefixes = gson.fromJson(reader, prefixMapType);
                    if (loadedPrefixes != null) {
                        prefixes.putAll(loadedPrefixes);
                    }
                }
            } else {
                // Создаем дефолтные префиксы
                createDefaultPrefixes();
                saveData();
            }

            // Загрузка данных игроков
            if (Files.exists(playerDataFile)) {
                Type playerDataType = new TypeToken<Map<UUID, String>>(){}.getType();
                try (Reader reader = Files.newBufferedReader(playerDataFile)) {
                    Map<UUID, String> loadedPlayerData = gson.fromJson(reader, playerDataType);
                    if (loadedPlayerData != null) {
                        playerPrefixes.putAll(loadedPlayerData);
                    }
                }
            }

            // Загрузка разблокированных префиксов
            if (Files.exists(unlockedPrefixesFile)) {
                Type unlockedType = new TypeToken<Map<UUID, Set<String>>>(){}.getType();
                try (Reader reader = Files.newBufferedReader(unlockedPrefixesFile)) {
                    Map<UUID, Set<String>> loaded = gson.fromJson(reader, unlockedType);
                    if (loaded != null) {
                        unlockedPrefixes.putAll(loaded);
                    }
                }
            }
        } catch (IOException e) {
            Prefix_manager.LOGGER.error("Ошибка при загрузке данных префиксов", e);
        }
    }

    private void createDefaultPrefixes() {
        // Стандартные префиксы
        addPrefix(new Prefix("default", "Обычный", "•", "§7", 
            "Доступен всем", Prefix.UnlockType.DEFAULT, null,false));
        
        // Префиксы за достижения
        addPrefix(new Prefix("adventurer", "Искатель", "⚔", "§6", 
            "Исследуйте все биомы", Prefix.UnlockType.ACHIEVEMENT, 
            "minecraft:adventure/adventuring_time", false));
        
        addPrefix(new Prefix("warrior", "Воин", "☠", "§c", 
            "Убейте всех типов мобов", Prefix.UnlockType.ACHIEVEMENT, 
            "minecraft:adventure/kill_all_mobs", false));
        
        addPrefix(new Prefix("nether", "Адский", "☗", "§c", 
            "Посетите все биомы Нижнего мира", Prefix.UnlockType.ACHIEVEMENT, 
            "minecraft:nether/explore_nether", false));
        
        addPrefix(new Prefix("dragon", "Драконий", "☬", "§5", 
            "Победите дракона Края", Prefix.UnlockType.ACHIEVEMENT, 
            "minecraft:end/kill_dragon", false));
        
        addPrefix(new Prefix("hero", "Герой", "♔", "§e", 
            "Получите все достижения", Prefix.UnlockType.ACHIEVEMENT, 
            "minecraft:story/complete_all_achievements", false));
        
        // Админские префиксы
        addPrefix(new Prefix("star", "Звездный", "★", "§6", 
            "Выдается администрацией", Prefix.UnlockType.ADMIN, null, false));
        
        // Особые префиксы
        addPrefix(new Prefix("event", "Праздничный", "❆", "§b", 
            "Участвуйте в особых событиях", Prefix.UnlockType.SPECIAL,
                "winter_event", false));
        
        // Скрытые префиксы
        addPrefix(new Prefix("ancient", "Древний", "⚛", "§5", 
            "Найдите древний город", Prefix.UnlockType.SECRET,
            "minecraft:adventure/ancient_city", true));
            
        addPrefix(new Prefix("warden", "Страж", "☠", "§3", 
            "Победите Вардена", Prefix.UnlockType.SECRET,
            "minecraft:adventure/kill_warden", true));
            
        addPrefix(new Prefix("master", "Мастер", "✵", "§6", 
            "Соберите все неритовые инструменты", Prefix.UnlockType.SECRET,
            "minecraft:nether/netherite_armor", true));
    }

    public void saveData() {
        try {
            // Сохранение префиксов
            try (Writer writer = Files.newBufferedWriter(prefixesFile)) {
                gson.toJson(prefixes, writer);
            }

            // Сохранение данных игроков
            try (Writer writer = Files.newBufferedWriter(playerDataFile)) {
                gson.toJson(playerPrefixes, writer);
            }

            // Сохранение разблокированных префиксов
            try (Writer writer = Files.newBufferedWriter(unlockedPrefixesFile)) {
                gson.toJson(unlockedPrefixes, writer);
            } catch (IOException e) {
                Prefix_manager.LOGGER.error("Ошибка при сохранении разблокированных префиксов", e);
            }
        } catch (IOException e) {
            Prefix_manager.LOGGER.error("Ошибка при сохранении данных префиксов", e);
        }
    }

    public void addPrefix(Prefix prefix) {
        prefixes.put(prefix.getId(), prefix);
        saveData();
    }

    public void setPlayerPrefix(UUID playerUuid, String prefixId) {
        if (prefixes.containsKey(prefixId)) {
            playerPrefixes.put(playerUuid, prefixId);
            saveData();
        }
    }

    public Prefix getPlayerPrefix(UUID playerUuid) {
        String prefixId = playerPrefixes.get(playerUuid);
        return prefixId != null ? prefixes.get(prefixId) : prefixes.get("default");
    }

    public Collection<Prefix> getAllPrefixes() {
        return prefixes.values();
    }

    public boolean hasPrefix(String prefixId) {
        return prefixes.containsKey(prefixId);
    }

    public boolean isUnlocked(UUID playerUuid, String prefixId) {
        Prefix prefix = prefixes.get(prefixId);
        if (prefix == null) return false;

        // Дефолтные префиксы доступны всем
        if (prefix.getUnlockType() == Prefix.UnlockType.DEFAULT) return true;

        // Проверяем разблокированные префиксы
        Set<String> playerUnlocked = unlockedPrefixes.getOrDefault(playerUuid, new HashSet<>());
        return playerUnlocked.contains(prefixId);
    }

    public void unlockPrefix(UUID playerUuid, String prefixId) {
        unlockedPrefixes.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(prefixId);
        saveData();
    }

    // Получить все доступные игроку префиксы
    public Collection<Prefix> getAvailablePrefixes(UUID playerUuid) {
        return prefixes.values().stream()
                .filter(prefix -> isUnlocked(playerUuid, prefix.getId()))
                .collect(Collectors.toList());
    }

    public Prefix getDefaultPrefix() {
        return prefixes.get("default");
    }

    public Map<String, Prefix> getAllPrefixesMap() {
        return prefixes;
    }

    public Set<String> getUnlockedPrefixes(UUID playerUuid) {
        return unlockedPrefixes.getOrDefault(playerUuid, new HashSet<>());
    }

    public void setUnlockedPrefixes(UUID playerUuid, Set<String> prefixes) {
        unlockedPrefixes.put(playerUuid, prefixes);
    }

    public void updatePrefixes(Map<String, Prefix> newPrefixes) {
        prefixes.clear();
        prefixes.putAll(newPrefixes);
    }
} 