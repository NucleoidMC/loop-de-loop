package io.github.restioson.loopdeloop;

import io.github.restioson.loopdeloop.game.LoopDeLoopConfig;
import io.github.restioson.loopdeloop.game.LoopDeLoopWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;

public final class LoopDeLoop implements ModInitializer {
    public static final String ID = "loopdeloop";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<LoopDeLoopConfig> TYPE = GameTypes.register(
            Identifier.fromNamespaceAndPath(LoopDeLoop.ID, "loopdeloop"),
            LoopDeLoopConfig.CODEC,
            LoopDeLoopWaiting::open
    );

    public static final StatisticKey<Integer> TOTAL_HOOPS = StatisticKey.intKey(Identifier.fromNamespaceAndPath(ID, "total_hoops"));
    public static final StatisticKey<Integer> MISSED_HOOPS = StatisticKey.intKey(Identifier.fromNamespaceAndPath(ID, "missed_hoops"));
    public static final StatisticKey<Integer> BOOSTS_USED = StatisticKey.intKey(Identifier.fromNamespaceAndPath(ID, "boosts_used"));

    @Override
    public void onInitialize() {
    }
}
