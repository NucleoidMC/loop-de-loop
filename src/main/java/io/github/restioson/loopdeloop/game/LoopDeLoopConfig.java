package io.github.restioson.loopdeloop.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gegy1000.plasmid.game.config.GameConfig;
import net.gegy1000.plasmid.game.config.PlayerConfig;

public final class LoopDeLoopConfig implements GameConfig {
    public static final Codec<LoopDeLoopConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.players),
                Codec.INT.fieldOf("time_limit_secs").forGetter(config -> config.timeLimit),
                Codec.INT.fieldOf("loops").forGetter(config -> config.loops),
                Codec.INT.fieldOf("start_rockets").forGetter(config -> config.startRockets),
                Codec.INT.fieldOf("max_y_variation").forGetter(config -> config.maxYVariation)
        ).apply(instance, LoopDeLoopConfig::new);
    });

    public final PlayerConfig players;
    public final int timeLimit;
    public final int loops;
    public final int startRockets;
    public final int maxYVariation;

    public LoopDeLoopConfig(PlayerConfig players, int timeLimit, int loops, int startRockets, int maxYVariation) {
        this.players = players;
        this.timeLimit = timeLimit;
        this.loops = loops;
        this.startRockets = startRockets;
        this.maxYVariation = maxYVariation;
    }
}
