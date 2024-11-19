package com.example.sereneseasonsextended.features;

import com.example.sereneseasonsextended.util.EnvironmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber
public class SnowBlockReplacer {
    private static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> playerPositions = new HashMap<>();
    private static final int UPDATE_INTERVAL = 100; // каждые 5 секунд
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !EnvironmentHelper.shouldRunMod()) {
            return;
        }

        LOGGER.debug("SnowBlockReplacer: Processing server tick. Tick count = {}", tickCounter);

        tickCounter++;
        MinecraftServer server = event.getServer();
        Level level = server.getLevel(Level.OVERWORLD);

        if (level == null) {
            LOGGER.warn("SnowBlockReplacer: Overworld level is null. Skipping this tick.");
            return; // Убедимся, что мир доступен
        }

        // Обновляем позиции игроков каждые 5 секунд
        if (tickCounter % UPDATE_INTERVAL == 0) {
            LOGGER.info("SnowBlockReplacer: Updating player positions.");
            updatePlayerPositions(server.getPlayerList().getPlayers());
        }

        // Заменяем снежные блоки на воздух случайно
        if (tickCounter % getRandomInterval() == 0) {
            LOGGER.info("SnowBlockReplacer: Replacing snow blocks.");
            replaceSnowBlocks(level);
        }
    }

    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }
    }

    private static void replaceSnowBlocks(Level level) {
        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();

        // Не выполняем замену блоков в зимний период
        if (isWinterSubSeason(currentSubSeason)) {
            return;
        }

        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            // Получаем радиус симуляции
            int simulationDistance = getSimulationDistance(player);
            int radius = simulationDistance * 16;
            LOGGER.debug("SnowBlockReplacer: Checking blocks within radius {} around player {}", radius, playerPos);

            BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
            if (targetPos != null) {
                LOGGER.info("Found snow block at {}", targetPos);
                boolean blockReplaced = level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                if (blockReplaced) {
                    LOGGER.info("Successfully replaced snow block at {}", targetPos);
                } else {
                    LOGGER.warn("Failed to replace block at {}", targetPos);
                }
            } else {
                LOGGER.warn("No snow block found within the radius.");
            }
        }
    }

    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            int simulationDistance = server.getPlayerList().getViewDistance(); // Получаем настройку серверного расстояния симуляции
            LOGGER.debug("Simulation distance for player {} is {}", player.getName().getString(), simulationDistance);
            return simulationDistance;
        }
        LOGGER.warn("Server is null or unable to get view distance, using default value of 10.");
        return 10; // Значение по умолчанию
    }

    private static BlockPos findSnowBlockInRadius(Level level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.SNOW_BLOCK)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isWinterSubSeason(Season.SubSeason subSeason) {
        return subSeason == Season.SubSeason.EARLY_WINTER ||
                subSeason == Season.SubSeason.MID_WINTER ||
                subSeason == Season.SubSeason.LATE_WINTER;
    }

    private static int getRandomInterval() {
        return 60 + RANDOM.nextInt(120); // случайное число от 3 до 8 секунд в тиках
    }
}
