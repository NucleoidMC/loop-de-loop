package io.github.restioson.loopdeloop;

import io.github.restioson.loopdeloop.game.LoopDeLoopConfig;
import io.github.restioson.loopdeloop.game.LoopDeLoopWaiting;
import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LoopDeLoop implements ModInitializer {
    public static final String ID = "loopdeloop";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<LoopDeLoopConfig> TYPE = GameType.register(
            new Identifier(LoopDeLoop.ID, "loopdeloop"),
            LoopDeLoopWaiting::open,
            LoopDeLoopConfig.CODEC
    );

    @Override
    public void onInitialize() {
    }
}
