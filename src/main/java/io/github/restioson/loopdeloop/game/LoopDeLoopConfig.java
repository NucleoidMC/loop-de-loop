package io.github.restioson.loopdeloop.game;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;

import java.util.Arrays;
import java.util.List;

public final class LoopDeLoopConfig {
    public static final Codec<LoopDeLoopConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").orElse(new PlayerConfig(1, 100)).forGetter(config -> config.players),
            Codec.INT.fieldOf("time_limit_secs").forGetter(config -> config.timeLimit),
            Codec.INT.fieldOf("loops").forGetter(config -> config.loops),
            Codec.INT.fieldOf("loop_radius").orElse(5).forGetter(config -> config.loopRadius),
            Codec.INT.fieldOf("start_rockets").forGetter(config -> config.startRockets),
            Codec.INT.fieldOf("y_var_max").forGetter(config -> config.yVarMax),
            ZVariation.CODEC.fieldOf("z_var_max").forGetter(config -> config.zVarMax),
            ZVariation.CODEC.fieldOf("z_var_min").forGetter(config -> config.zVarMin),
            Codec.BOOL.fieldOf("flappy_mode").orElse(false).forGetter(config -> config.flappyMode),
            Registry.BLOCK.listOf().optionalFieldOf("loop_blocks", ImmutableList.of(Blocks.BLUE_TERRACOTTA)).forGetter(config -> Arrays.asList(config.loopBlocks))
    ).apply(instance, LoopDeLoopConfig::new));

    public final PlayerConfig players;
    public final int timeLimit;
    public final int loops;
    public final int loopRadius;
    public final int startRockets;
    public final int yVarMax;
    public final ZVariation zVarMax;
    public final ZVariation zVarMin;
    public final boolean flappyMode;
    public final Block[] loopBlocks;

    public LoopDeLoopConfig(
            PlayerConfig players,
            int timeLimit,
            int loops,
            int loopRadius,
            int startRockets,
            int yVarMax,
            ZVariation zVarMax,
            ZVariation zVarMin,
            boolean flappyMode,
            List<Block> loopBlocks
    ) {
        this.players = players;
        this.timeLimit = timeLimit;
        this.loops = loops;
        this.loopRadius = loopRadius;
        this.startRockets = startRockets;
        this.yVarMax = yVarMax;
        this.zVarMax = zVarMax;
        this.zVarMin = zVarMin;
        this.flappyMode = flappyMode;
        this.loopBlocks = loopBlocks.toArray(new Block[0]);
    }

    public static class ZVariation {
        public static final Codec<ZVariation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("start").forGetter(config -> config.start),
                Codec.INT.fieldOf("end").forGetter(config -> config.end)
        ).apply(instance, ZVariation::new));

        public final int start;
        public final int end;

        public ZVariation(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
