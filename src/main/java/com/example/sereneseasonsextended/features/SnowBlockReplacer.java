package com.example.sereneseasonsextended.features;

import com.example.sereneseasonsextended.util.EnvironmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.Holder;
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
    private static final Map<String, Float> biomeTemperatures = new HashMap<>();
    private static final int UPDATE_INTERVAL = 100;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !EnvironmentHelper.shouldRunMod()) {
            return;
        }

        tickCounter++;
        MinecraftServer server = event.getServer();
        Level level = server.getLevel(Level.OVERWORLD);

        if (level == null) {
            return;
        }

        if (tickCounter % UPDATE_INTERVAL == 0) {
            updatePlayerPositions(server.getPlayerList().getPlayers());
        }

        if (tickCounter % getRandomInterval() == 0) {
            replaceSnowBlocks(level);
        }
    }

    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }
    }

    private static void replaceSnowBlocks(Level level) {
        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            int simulationDistance = getSimulationDistance(player);
            int radius = simulationDistance * 16;

            Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
            float temperature = getCachedBiomeTemperature(level, playerPos, currentSubSeason);
            if (temperature < 0.15f) { // Пропускаем обработку в холодных биомах
                continue;
            }

            int blocksToReplace = calculateBlocksToReplace(temperature);

            for (int i = 0; i < blocksToReplace; i++) {
                BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                if (targetPos != null) {
                    boolean blockReplaced = level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                    if (blockReplaced) {
                        LOGGER.debug("Replaced snow block at {}", targetPos);
                    }
                } else {
                    break;
                }
            }
        }
    }

    private static float getCachedBiomeTemperature(Level level, BlockPos pos, Season.SubSeason currentSubSeason) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        String biomeName = biomeHolder.unwrapKey().map(Object::toString).orElse("unknown");

        // Если температура для этого биома ещё не закэширована
        if (!biomeTemperatures.containsKey(biomeName)) {
            // Получаем базовую температуру для биома
            float temperature = getBiomeTemperature(level, biomeHolder, pos);

            // Проверка на зимний субсезон
            if (isWinterSubSeason(currentSubSeason)) {
                // Понижаем температуру до 0.14F, если она выше
                if (temperature > 0.14f) {
                    temperature = 0.14f; // Понижаем температуру до 0.14F
                }
            }

            // Сохраняем температуру в кэш
            biomeTemperatures.put(biomeName, temperature);
            LOGGER.info("Biome: {}, Temperature: {}", biomeName, temperature); // Логирование температуры
            return temperature;
        }

        // Если температура уже закэширована, проверяем, нужно ли обновить кэш
        float cachedTemperature = biomeTemperatures.get(biomeName);

        // Если субсезон не зима, обновляем кэш
        if (!isWinterSubSeason(currentSubSeason)) {
            // Получаем актуальную температуру для не зимнего сезона
            float newTemperature = getBiomeTemperature(level, biomeHolder, pos);

            // Если новая температура отличается от закэшированной или если была установлена зимняя температура
            if (newTemperature != cachedTemperature || cachedTemperature <= 0.14f) {
                // Сброс кэша и обновление температуры для не зимнего сезона
                biomeTemperatures.put(biomeName, newTemperature);
                LOGGER.info("Biome: {}, Updated Temperature: {}", biomeName, newTemperature); // Логирование обновленной температуры
                return newTemperature;
            }
        }

        // Если субсезон снова зима, возвращаем закэшированную температуру
        if (isWinterSubSeason(currentSubSeason) && cachedTemperature > 0.14f) {
            // Если субсезон снова зимний и температура была выше 0.14F, сбрасываем её на зимнюю
            cachedTemperature = 0.14f;
            biomeTemperatures.put(biomeName, cachedTemperature);
            LOGGER.info("Biome: {}, Reset Temperature to Winter: {}", biomeName, cachedTemperature);
        }

        return cachedTemperature;
    }

    /**
     * Получение температуры биома.
     */
    public static float getBiomeTemperature(LevelReader level, Holder<Biome> biomeHolder, BlockPos pos) {
        Biome biome = biomeHolder.value(); // Извлекаем объект Biome из Holder
        return biome.getBaseTemperature(); // Используем базовую температуру
    }

    private static int calculateBlocksToReplace(float temperature) {
        if (temperature < 0.2f) {
            return 1;
        } else if (temperature < 0.5f) {
            return 3;
        } else {
            return 5;
        }
    }

    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            return server.getPlayerList().getViewDistance();
        }
        return 10;
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
        return 60 + RANDOM.nextInt(120);
    }
}
