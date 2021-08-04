package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopGenerator;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public final class LoopDeLoopWaiting {
    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;

    private final LoopDeLoopSpawnLogic spawnLogic;

    private LoopDeLoopWaiting(ServerWorld world, GameSpace gameSpace, LoopDeLoopMap map, LoopDeLoopConfig config) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;

        this.spawnLogic = new LoopDeLoopSpawnLogic(world, map);
    }

    public static GameOpenProcedure open(GameOpenContext<LoopDeLoopConfig> context) {
        var config = context.config();
        var generator = new LoopDeLoopGenerator(config);

        var map = generator.build();
        var worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asGenerator(context.server()));

        return context.openWithWorld(worldConfig, (activity, world) -> {
            LoopDeLoopWaiting waiting = new LoopDeLoopWaiting(world, activity.getGameSpace(), map, config);

            GameWaitingLobby.addTo(activity, config.players());
            activity.allow(GameRuleType.FALL_DAMAGE);

            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);

            activity.listen(GamePlayerEvents.OFFER, waiting::offerPlayer);
            activity.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
        });
    }

    private GameResult requestStart() {
        LoopDeLoopActive.open(this.world, this.gameSpace, this.map, this.config);

        return GameResult.ok();
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return this.spawnLogic.acceptPlayer(offer, GameMode.ADVENTURE);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
