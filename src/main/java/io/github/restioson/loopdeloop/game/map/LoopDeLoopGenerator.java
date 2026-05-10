package io.github.restioson.loopdeloop.game.map;

import io.github.restioson.loopdeloop.game.LoopDeLoopConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public final class LoopDeLoopGenerator {
    public final LoopDeLoopConfig config;

    public LoopDeLoopGenerator(LoopDeLoopConfig config) {
        this.config = config;
    }

    public LoopDeLoopMap build() {
        MapTemplate template = MapTemplate.createEmpty();
        LoopDeLoopMap map = new LoopDeLoopMap(template);
        LoopDeLoopConfig cfg = this.config;

        BlockBounds spawnPlatform = this.spawnPlatform(template);

        BlockPos.MutableBlockPos circlePos = new BlockPos.MutableBlockPos();
        circlePos.set(0, 128, 32);
        RandomSource random = RandomSource.createThreadLocalInstance();

        // y = mx + c  -- these are gradient values
        double mZVarMax = (cfg.zVarMax().end() - cfg.zVarMax().start()) / (double) cfg.loops();
        double mZVarMin = (cfg.zVarMin().end() - cfg.zVarMin().start()) / (double) cfg.loops();

        var loopBlocks = cfg.loopBlocks();
        for (int i = 0; i < cfg.loops(); i++) {
            Block outline = loopBlocks.get(i % loopBlocks.size()).value();
            this.addCircle(template, cfg.loopRadius(), circlePos.immutable(), map, outline.defaultBlockState());

            // New circle
            int zVarMax = Mth.ceil(mZVarMax * i + cfg.zVarMax().start());
            int zVarMin = Mth.ceil(mZVarMin * i + cfg.zVarMin().start());
            int zMove = Mth.nextInt(random, zVarMax, zVarMin);
            int yVar = cfg.yVarMax() / 2;
            int y = Mth.nextInt(random, 128 - yVar, 128 + yVar);
            int xMove = Mth.nextInt(random, -16, 16);
            circlePos.move(Direction.SOUTH, zMove);
            circlePos.move(Direction.EAST, xMove);
            circlePos.setY(y);
        }

        map.setSpawn(spawnPlatform, BlockPos.containing(spawnPlatform.centerTop()));

        return map;
    }

    private BlockBounds spawnPlatform(MapTemplate template) {
        BlockBounds platform = BlockBounds.of(new BlockPos(-5, 122, -5), new BlockPos(5, 122, 5));
        for (BlockPos pos : platform) {
            template.setBlockState(pos, Blocks.RED_TERRACOTTA.defaultBlockState());
        }
        return platform;
    }

    private void addCircle(MapTemplate template, int radius, BlockPos centre, LoopDeLoopMap map, BlockState outline) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        map.addHoop(new LoopDeLoopHoop(centre, radius));

        int radius2 = radius * radius;
        int outlineRadius2 = (radius - 1) * (radius - 1);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                int distance2 = x * x + y * y;
                if (distance2 >= radius2) {
                    continue;
                }

                if (distance2 >= outlineRadius2) {
                    mutablePos.set(centre.getX() + x, centre.getY() + y, centre.getZ());
                    template.setBlockState(mutablePos, outline);
                }
            }
        }
    }
}
