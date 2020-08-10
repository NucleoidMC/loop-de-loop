package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopGenerator;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.gegy1000.plasmid.game.GameWorld;
import net.gegy1000.plasmid.game.GameWorldState;
import net.gegy1000.plasmid.game.StartResult;
import net.gegy1000.plasmid.game.event.OfferPlayerListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.RequestStartListener;
import net.gegy1000.plasmid.game.player.JoinResult;
import net.gegy1000.plasmid.game.rule.GameRule;
import net.gegy1000.plasmid.game.rule.RuleResult;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

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
    }

    public static CompletableFuture<Void> open(GameWorldState worldState, LoopDeLoopConfig config) {
        LoopDeLoopGenerator generator = new LoopDeLoopGenerator(config);

        return generator.create().thenAccept(map -> {
            GameWorld gameWorld = worldState.openWorld(map.asGenerator());

            LoopDeLoopWaiting waiting = new LoopDeLoopWaiting(gameWorld, map, config);

            gameWorld.newGame(game -> {
                game.setRule(GameRule.ALLOW_CRAFTING, RuleResult.DENY);
                game.setRule(GameRule.ALLOW_PORTALS, RuleResult.DENY);
                game.setRule(GameRule.ALLOW_PVP, RuleResult.DENY);
                game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
                game.setRule(GameRule.ENABLE_HUNGER, RuleResult.DENY);

                game.on(RequestStartListener.EVENT, waiting::requestStart);
                game.on(OfferPlayerListener.EVENT, waiting::offerPlayer);

                game.on(PlayerAddListener.EVENT, waiting::addPlayer);
                game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
            });
        });
    }

    private JoinResult offerPlayer(ServerPlayerEntity player) {
        if (this.gameWorld.getPlayerCount() >= this.config.players.getMaxPlayers()) {
            return JoinResult.gameFull();
        }

        return JoinResult.ok();
    }

    private StartResult requestStart() {
        if (this.gameWorld.getPlayerCount() < this.config.players.getMinPlayers()) {
            return StartResult.notEnoughPlayers();
        }

        LoopDeLoopActive.open(this.gameWorld, this.map, this.config);

        return StartResult.ok();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnPlayer(player);
        return true;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
