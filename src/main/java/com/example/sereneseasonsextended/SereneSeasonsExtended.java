package com.example.sereneseasonsextended;

import com.example.sereneseasonsextended.features.SnowBlockReplacer;
import com.example.sereneseasonsextended.util.EnvironmentHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.lavabucket.hourglass.config.HourglassConfig;

@Mod(SereneSeasonsExtended.MODID)
public class SereneSeasonsExtended {

    public static final String MODID = "sereneseasonsextended";
    private static final Logger LOGGER = LogManager.getLogger(SereneSeasonsExtended.class);

    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    public SereneSeasonsExtended() {
        EnvironmentHelper.initialize();
        if (EnvironmentHelper.shouldRunMod()) {
            LOGGER.info("Serene Seasons Extended: Registering event handlers.");
            MinecraftForge.EVENT_BUS.register(SnowBlockReplacer.class);
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            LOGGER.warn("Serene Seasons Extended: Mod will not run in this environment.");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Serene Seasons Extended is loading!");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = event.getServer();
            if (server != null) {
                Level level = server.getLevel(Level.OVERWORLD);
                if (level != null) {
                    onTick(level);
                } else {
                    LOGGER.warn("Overworld level is not available.");
                }
            }
        }
    }

    private void onTick(Level level) {
        ticker++;

        // Проверка каждые 20 секунд (400 тиков)
        if (ticker >= 400) {
            ticker = 0;

            // Проверяем, должно ли работать изменение времени
            if (EnvironmentHelper.shouldRunMod()) {

                // Получаем текущий подсезон
                Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                if (currentSubSeason != lastSubSeason) {
                    lastSubSeason = currentSubSeason;

                    // Устанавливаем множители скорости дня и ночи
                    double daySpeed = getDaySpeedForSeason(currentSubSeason);
                    double nightSpeed = getNightSpeedForSeason(currentSubSeason);

                    HourglassConfig.SERVER_CONFIG.daySpeed.set(daySpeed);
                    HourglassConfig.SERVER_CONFIG.nightSpeed.set(nightSpeed);

                    LOGGER.info("Season changed to {}. Day speed: {}, Night speed: {}", currentSubSeason, daySpeed, nightSpeed);
                }
            }
        }
    }

    private double getDaySpeedForSeason(Season.SubSeason season) {
        return switch (season) {
            case EARLY_SPRING -> 1.0909090909090908;
            case MID_SPRING -> 0.8665151515151515;
            case LATE_SPRING -> 0.6661868686868686;
            case EARLY_SUMMER -> 0.5857575757575757;
            case MID_SUMMER -> 0.6657828282828281;
            case LATE_SUMMER -> 0.8568939393939394;
            case EARLY_AUTUMN -> 1.0909090909090908;
            case MID_AUTUMN -> 1.2750252525252526;
            case LATE_AUTUMN -> 1.4735353535353535;
            case EARLY_WINTER -> 1.5450252525252526;
            case MID_WINTER -> 1.4517171717171717;
            case LATE_WINTER -> 1.2567171717171717;
        };
    }

    private double getNightSpeedForSeason(Season.SubSeason season) {
        return switch (season) {
            case EARLY_SPRING -> 0.9230769230769231;
            case MID_SPRING -> 1.112948717948718;
            case LATE_SPRING -> 1.282457264957265;
            case EARLY_SUMMER -> 1.3505128205128205;
            case MID_SUMMER -> 1.2827991452991454;
            case LATE_SUMMER -> 1.1210897435897436;
            case EARLY_AUTUMN -> 0.9230769230769231;
            case MID_AUTUMN -> 0.7672863247863249;
            case LATE_AUTUMN -> 0.5993162393162393;
            case EARLY_WINTER -> 0.5388247863247864;
            case MID_WINTER -> 0.6177777777777778;
            case LATE_WINTER -> 0.7827777777777778;
        };
    }
}
