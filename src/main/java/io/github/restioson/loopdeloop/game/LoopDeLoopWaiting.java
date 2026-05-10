package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopGenerator;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public final class LoopDeLoopWaiting {
    private final ServerLevel world;
    private final GameSpace gameSpace;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;

    private final LoopDeLoopSpawnLogic spawnLogic;

    private LoopDeLoopWaiting(ServerLevel world, GameSpace gameSpace, LoopDeLoopMap map, LoopDeLoopConfig config) {
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
        var worldConfig = new RuntimeLevelConfig()
                .setGenerator(map.asGenerator(context.server()));

        return context.openWithLevel(worldConfig, (activity, world) -> {
            LoopDeLoopWaiting waiting = new LoopDeLoopWaiting(world, activity.getGameSpace(), map, config);

            GameWaitingLobby.addTo(activity, config.players());
            activity.allow(GameRuleType.FALL_DAMAGE);

            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);

            activity.listen(GamePlayerEvents.ACCEPT, waiting::acceptPlayer);
            activity.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
        });
    }

    private GameResult requestStart() {
        LoopDeLoopActive.open(this.world, this.gameSpace, this.map, this.config);

        return GameResult.ok();
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return this.spawnLogic.acceptPlayer(offer, GameType.ADVENTURE);
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayer player) {
        this.spawnLogic.resetPlayer(player, GameType.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
