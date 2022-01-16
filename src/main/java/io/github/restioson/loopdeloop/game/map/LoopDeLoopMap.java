package io.github.restioson.loopdeloop.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.util.ArrayList;
import java.util.List;

public final class LoopDeLoopMap {
    private final MapTemplate template;

    public final List<LoopDeLoopHoop> hoops = new ArrayList<>();

    private BlockBounds spawnPlatform;
    private BlockPos spawn;

    public LoopDeLoopMap(MapTemplate template) {
        this.template = template;
    }

    public void addHoop(LoopDeLoopHoop hoop) {
        this.hoops.add(hoop);
    }

    public void setSpawn(BlockBounds platform, BlockPos pos) {
        this.spawnPlatform = platform;
        this.spawn = pos;
    }

    public BlockBounds getSpawnPlatform() {
        return this.spawnPlatform;
    }

    public BlockPos getSpawn() {
        return this.spawn;
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
