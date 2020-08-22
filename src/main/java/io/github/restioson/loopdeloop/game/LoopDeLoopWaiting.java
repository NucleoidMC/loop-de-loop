package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopGenerator;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

import java.util.concurrent.CompletableFuture;

public final class LoopDeLoopWaiting {
    private final GameWorld gameWorld;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;

    private final LoopDeLoopSpawnLogic spawnLogic;

    private LoopDeLoopWaiting(GameWorld gameWorld, LoopDeLoopMap map, LoopDeLoopConfig config) {
        this.gameWorld = gameWorld;
        this.map = map;
        this.config = config;

        this.spawnLogic = new LoopDeLoopSpawnLogic(gameWorld, map);

        gameWorld.addResource(map.acquireTickets(gameWorld));
    }

    public static CompletableFuture<GameWorld> open(GameOpenContext<LoopDeLoopConfig> context) {

        LoopDeLoopGenerator generator = new LoopDeLoopGenerator(context.getConfig());

        return generator.create().thenCompose(map -> {
            BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                    .setGenerator(map.asGenerator(context.getServer()))
                    .setDefaultGameMode(GameMode.SPECTATOR);

            return context.openWorld(worldConfig).thenApply(gameWorld -> {
                LoopDeLoopWaiting waiting = new LoopDeLoopWaiting(gameWorld, map, context.getConfig());

                return GameWaitingLobby.open(gameWorld, context.getConfig().players, game -> {
                    game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);

                    game.on(RequestStartListener.EVENT, waiting::requestStart);

                    game.on(PlayerAddListener.EVENT, waiting::addPlayer);
                    game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
                });
            });
        });
    }

    private StartResult requestStart() {
        LoopDeLoopActive.open(this.gameWorld, this.map, this.config);

        return StartResult.OK;
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
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
