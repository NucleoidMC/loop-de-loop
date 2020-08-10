package io.github.restioson.loopdeloop.game.map;

import net.gegy1000.plasmid.game.map.template.MapTemplate;
import net.gegy1000.plasmid.game.map.template.TemplateChunkGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;

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
