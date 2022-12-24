package io.github.restioson.loopdeloop.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.restioson.loopdeloop.LoopDeLoop;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record LoopDeLoopConfig(
        PlayerConfig players,
        int timeLimit,
        int loops,
        int loopRadius,
        int startRockets,
        int yVarMax,
        ZVariation zVarMax,
        ZVariation zVarMin,
        boolean flappyMode,
        RegistryEntryList<Block> loopBlocks,
        String statisticsBundle,
        int rocketPower,
        boolean infiniteMode
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
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("loop_blocks", RegistryEntryList.of(Block::getRegistryEntry, Blocks.BLUE_TERRACOTTA)).forGetter(LoopDeLoopConfig::loopBlocks),
            Codec.STRING.optionalFieldOf("statistics_bundle", LoopDeLoop.ID).forGetter(LoopDeLoopConfig::statisticsBundle),
            Codec.INT.optionalFieldOf("rocketPower", 1).forGetter(LoopDeLoopConfig::rocketPower),
            Codec.BOOL.optionalFieldOf("infinite_mode", false).forGetter(LoopDeLoopConfig::infiniteMode)
    ).apply(instance, LoopDeLoopConfig::new));

    public record ZVariation(int start, int end) {
        public static final Codec<ZVariation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("start").forGetter(config -> config.start),
                Codec.INT.fieldOf("end").forGetter(config -> config.end)
        ).apply(instance, ZVariation::new));
    }
}
