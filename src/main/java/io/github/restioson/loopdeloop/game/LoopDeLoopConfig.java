package io.github.restioson.loopdeloop.game;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

import java.util.List;

public final record LoopDeLoopConfig(
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
    public static final Codec<LoopDeLoopConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").orElse(new PlayerConfig(1, 100)).forGetter(LoopDeLoopConfig::players),
            Codec.INT.fieldOf("time_limit_secs").forGetter(LoopDeLoopConfig::timeLimit),
            Codec.INT.fieldOf("loops").forGetter(LoopDeLoopConfig::loops),
            Codec.INT.fieldOf("loop_radius").orElse(5).forGetter(LoopDeLoopConfig::loopRadius),
            Codec.INT.fieldOf("start_rockets").forGetter(LoopDeLoopConfig::startRockets),
            Codec.INT.fieldOf("y_var_max").forGetter(LoopDeLoopConfig::yVarMax),
            ZVariation.CODEC.fieldOf("z_var_max").forGetter(LoopDeLoopConfig::zVarMax),
            ZVariation.CODEC.fieldOf("z_var_min").forGetter(LoopDeLoopConfig::zVarMin),
            Codec.BOOL.fieldOf("flappy_mode").orElse(false).forGetter(LoopDeLoopConfig::flappyMode),
            Registry.BLOCK.getCodec().listOf().optionalFieldOf("loop_blocks", ImmutableList.of(Blocks.BLUE_TERRACOTTA)).forGetter(LoopDeLoopConfig::loopBlocks)
    ).apply(instance, LoopDeLoopConfig::new));

    public record ZVariation(int start, int end) {
        public static final Codec<ZVariation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("start").forGetter(config -> config.start),
                Codec.INT.fieldOf("end").forGetter(config -> config.end)
        ).apply(instance, ZVariation::new));
    }
}
