package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopGenerator;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.game.world.bubble.BubbleWorldConfig;

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

    public static CompletableFuture<Void> open(GameOpenContext<LoopDeLoopConfig> context) {

        LoopDeLoopGenerator generator = new LoopDeLoopGenerator(context.getConfig());

        return generator.create().thenAccept(map -> {
            BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                    .setGenerator(map.asGenerator(context.getServer()))
                    .setDefaultGameMode(GameMode.SPECTATOR);

            GameWorld gameWorld = context.openWorld(worldConfig);

            LoopDeLoopWaiting waiting = new LoopDeLoopWaiting(gameWorld, map, context.getConfig());

            gameWorld.openGame(game -> {
                game.setRule(GameRule.CRAFTING, RuleResult.DENY);
                game.setRule(GameRule.PORTALS, RuleResult.DENY);
                game.setRule(GameRule.PVP, RuleResult.DENY);
                game.setRule(GameRule.HUNGER, RuleResult.DENY);
                game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);

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
        String[] lines = new String[] {
                "Loop-de-loop - fly through all the hoops with your elytra. Whoever does it first wins!",
                "You start with some rockets and can get more by flying through hoops, or when you fail a hoop."
        };

        for (String line : lines) {
            Text text = new LiteralText(line).formatted(Formatting.GOLD);
            player.sendMessage(text, false);
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

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
