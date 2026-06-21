package io.github.restioson.loopdeloop.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.restioson.loopdeloop.LoopDeLoop;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record LoopDeLoopConfig(
        WaitingLobbyConfig players,
        int timeLimit,
        int loops,
        int loopRadius,
        int startRockets,
        int yVarMax,
        ZVariation zVarMax,
        ZVariation zVarMin,
        boolean flappyMode,
        HolderSet<Block> loopBlocks,
        String statisticsBundle,
        int rocketPower,
        boolean infiniteMode,
        boolean debugMode
) {
    public static final MapCodec<LoopDeLoopConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WaitingLobbyConfig.CODEC.fieldOf("players").orElse(new WaitingLobbyConfig(1, 100)).forGetter(LoopDeLoopConfig::players),
            Codec.INT.fieldOf("time_limit_secs").forGetter(LoopDeLoopConfig::timeLimit),
            Codec.INT.fieldOf("loops").forGetter(LoopDeLoopConfig::loops),
            Codec.INT.fieldOf("loop_radius").orElse(5).forGetter(LoopDeLoopConfig::loopRadius),
            Codec.INT.fieldOf("start_rockets").forGetter(LoopDeLoopConfig::startRockets),
            Codec.INT.fieldOf("y_var_max").forGetter(LoopDeLoopConfig::yVarMax),
            ZVariation.CODEC.fieldOf("z_var_max").forGetter(LoopDeLoopConfig::zVarMax),
            ZVariation.CODEC.fieldOf("z_var_min").forGetter(LoopDeLoopConfig::zVarMin),
            Codec.BOOL.fieldOf("flappy_mode").orElse(false).forGetter(LoopDeLoopConfig::flappyMode),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("loop_blocks", HolderSet.direct(Block::builtInRegistryHolder, Blocks.DYED_TERRACOTTA.blue())).forGetter(LoopDeLoopConfig::loopBlocks),
            Codec.STRING.optionalFieldOf("statistics_bundle", LoopDeLoop.ID).forGetter(LoopDeLoopConfig::statisticsBundle),
            Codec.INT.optionalFieldOf("rocketPower", 1).forGetter(LoopDeLoopConfig::rocketPower),
            Codec.BOOL.optionalFieldOf("infinite_mode", false).forGetter(LoopDeLoopConfig::infiniteMode),
            Codec.BOOL.optionalFieldOf("debug_mode", false).forGetter(LoopDeLoopConfig::debugMode)
    ).apply(instance, LoopDeLoopConfig::new));

    public record ZVariation(int start, int end) {
        public static final Codec<ZVariation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("start").forGetter(config -> config.start),
                Codec.INT.fieldOf("end").forGetter(config -> config.end)
        ).apply(instance, ZVariation::new));
    }
}
