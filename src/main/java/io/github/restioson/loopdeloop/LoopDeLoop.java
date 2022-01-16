package io.github.restioson.loopdeloop;

import io.github.restioson.loopdeloop.game.LoopDeLoopConfig;
import io.github.restioson.loopdeloop.game.LoopDeLoopWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.game.GameType;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;

public final class LoopDeLoop implements ModInitializer {
    public static final String ID = "loopdeloop";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<LoopDeLoopConfig> TYPE = GameType.register(
            new Identifier(LoopDeLoop.ID, "loopdeloop"),
            LoopDeLoopConfig.CODEC,
            LoopDeLoopWaiting::open
    );

    public static final StatisticKey<Integer> TOTAL_HOOPS = StatisticKey.intKey(new Identifier(ID, "total_hoops"), StatisticKey.StorageType.TOTAL);
    public static final StatisticKey<Integer> MISSED_HOOPS = StatisticKey.intKey(new Identifier(ID, "missed_hoops"), StatisticKey.StorageType.TOTAL);
    public static final StatisticKey<Integer> FIREWORKS_USED = StatisticKey.intKey(new Identifier(ID, "fireworks_used"), StatisticKey.StorageType.TOTAL);
    public static final StatisticKey<Integer> LEAPS_USED = StatisticKey.intKey(new Identifier(ID, "leaps_used"), StatisticKey.StorageType.TOTAL);

    @Override
    public void onInitialize() {
    }
}
