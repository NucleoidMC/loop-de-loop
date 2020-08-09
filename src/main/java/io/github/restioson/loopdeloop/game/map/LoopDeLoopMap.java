package io.github.restioson.loopdeloop.game.map;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.gegy1000.plasmid.game.map.template.MapTemplate;
import net.gegy1000.plasmid.game.map.template.TemplateChunkGenerator;
import net.gegy1000.plasmid.util.BlockBounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public final class LoopDeLoopMap {
    private final MapTemplate template;

    public final List<LoopDeLoopHoop> hoops = new ArrayList<>();

    private BlockPos spawn;

    public LoopDeLoopMap(MapTemplate template) {
        this.template = template;
    }

    public void addHoop(LoopDeLoopHoop hoop) {
        this.hoops.add(hoop);
    }

    public void setSpawn(BlockPos pos) {
        this.spawn = pos;
    }

    public BlockPos getSpawn() {
        return this.spawn;
    }

    public ChunkGenerator asGenerator() {
        return new TemplateChunkGenerator(this.template, BlockPos.ORIGIN);
    }
}
