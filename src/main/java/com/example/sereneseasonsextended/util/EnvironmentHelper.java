package com.example.sereneseasonsextended.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnvironmentHelper {
    private static final Logger LOGGER = LogManager.getLogger("EnvironmentHelper");
    private static boolean isServerEnvironment;
    private static boolean isSinglePlayer;

    public static void initialize() {
        // Определяем, находимся ли мы на сервере или в клиентском окружении
        isServerEnvironment = !FMLEnvironment.dist.isClient();

        if (!isServerEnvironment) { // Если это клиент, проверяем одиночную игру
            isSinglePlayer = detectSinglePlayer();
        } else {
            isSinglePlayer = false; // На сервере одиночная игра невозможна
        }

        // Логируем результаты
        LOGGER.info("EnvironmentHelper initialized: Server Environment = {}, Single Player Mode = {}", isServerEnvironment, isSinglePlayer);
    }

    public static boolean shouldRunMod() {
        boolean result = isServerEnvironment || isSinglePlayer; // Мод работает, если это сервер или одиночная игра
        LOGGER.debug("EnvironmentHelper: Should Run Mod = {}", result);
        return result;
    }

    private static boolean detectSinglePlayer() {
        try {
            Minecraft mcInstance = Minecraft.getInstance();
            // Проверяем, существует ли сервер. Если сервер null, значит это одиночная игра
            boolean isSinglePlayerMode = mcInstance.getCurrentServer() == null;
            return isSinglePlayerMode;
        } catch (Exception e) {
            LOGGER.warn("Failed to determine single-player mode: {}", e.getMessage());
        }
        return false;
    }
}
