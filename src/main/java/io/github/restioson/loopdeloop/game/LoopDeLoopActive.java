package io.github.restioson.loopdeloop.game;

import io.github.restioson.loopdeloop.game.map.LoopDeLoopHoop;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopMap;
import io.github.restioson.loopdeloop.game.map.LoopDeLoopWinner;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public final class LoopDeLoopActive {
    private final GameWorld gameWorld;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;
    private final List<LoopDeLoopWinner> finished;
    private final LoopDeLoopSpawnLogic spawnLogic;
    private final LoopDeLoopTimerBar timerBar = new LoopDeLoopTimerBar();
    private LoopDeLoopScoreboard scoreboard;
    // Only stores flying players, i.e non-completed players
    private final Object2ObjectMap<ServerPlayerEntity, LoopDeLoopPlayer> playerStates;
    @Nullable
    private ServerPlayerEntity lastCompleter;
    @Nullable
    Team team;
    private long closeTime = -1;
    private long finishTime = -1;
    private long startTime = -1;

    private LoopDeLoopActive(GameWorld gameWorld, LoopDeLoopMap map, LoopDeLoopConfig config, Set<ServerPlayerEntity> participants) {
        this.gameWorld = gameWorld;
        this.map = map;
        this.config = config;
        this.finished = new ArrayList<>();
        this.spawnLogic = new LoopDeLoopSpawnLogic(gameWorld, map);
        this.playerStates = new Object2ObjectOpenHashMap<>();

        for (ServerPlayerEntity player : participants) {
            this.playerStates.put(player, new LoopDeLoopPlayer(player));
        }
    }

    public static void open(GameWorld gameWorld, LoopDeLoopMap map, LoopDeLoopConfig config) {
        Set<ServerPlayerEntity> participants = gameWorld.getPlayers();
        LoopDeLoopActive active = new LoopDeLoopActive(gameWorld, map, config, participants);

        gameWorld.openGame(game -> {
            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.DENY);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
            game.setRule(GameRule.HUNGER, RuleResult.DENY);
            game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
        });
    }

    private void onOpen() {
        ServerWorld world = this.gameWorld.getWorld();
        ServerScoreboard scoreboard = world.getScoreboard();

        Team oldTeam = scoreboard.getTeam("loopdeloop");
        if (oldTeam != null) {
            scoreboard.removeTeam(oldTeam);
        }

        this.team = scoreboard.addTeam("loopdeloop");
        this.team.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        for (ServerPlayerEntity player : this.playerStates.keySet()) {
            scoreboard.addPlayerToTeam(player.getEntityName(), this.team);
            this.spawnParticipant(player);
        }

        long time = world.getTime();
        this.startTime = time - (time % 20) + (4 * 20) + 19;
        this.finishTime = this.startTime + (this.config.timeLimit * 20);
        this.scoreboard = new LoopDeLoopScoreboard(
                this.gameWorld.getWorld().getScoreboard(),
                this.config.loops
        );
        this.scoreboard.render(this.buildLeaderboard());
    }

    private void onClose() {
        this.gameWorld.getWorld().getScoreboard().removeTeam(this.team);
        this.timerBar.close();
        this.scoreboard.close();
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.playerStates.containsKey(player)) {
            this.spawnSpectator(player);
        }
        this.timerBar.addPlayer(player);
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.playerStates.remove(player);
    }

    // thx https://stackoverflow.com/a/6810409/4871468
    private static String ordinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    private void tick() {
        ServerWorld world = this.gameWorld.getWorld();
        long time = world.getTime();

        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                this.gameWorld.close();
            }
            return;
        }

        if (this.startTime > time) {
            this.tickStartWaiting(time);
            return;
        }

        if (time > this.finishTime || this.playerStates.isEmpty()) {
            this.tickEndWaiting(time);
            return;
        }

        this.timerBar.update(this.finishTime - time, config.timeLimit * 20);
        this.tickPlayers(time);
    }

    private void tickStartWaiting(long time) {
        float sec_f = (this.startTime - time) / 20.0f;

        if (sec_f > 1) {
            for (Map.Entry<ServerPlayerEntity, LoopDeLoopPlayer> entry : this.playerStates.entrySet()) {
                LoopDeLoopPlayer state = entry.getValue();
                ServerPlayerEntity player = entry.getKey();

                if (state.lastPos == null) {
                    state.lastPos = player.getPos();
                    continue;
                }

                player.teleport(state.lastPos.x, state.lastPos.y, state.lastPos.z);
            }
        }

        int sec = (int) Math.floor(sec_f) - 1;

        if ((this.startTime - time) % 20 == 0) {
            if (sec > 0) {
                this.broadcastTitle(new LiteralText(Integer.toString(sec)).formatted(Formatting.BOLD));
                this.broadcastSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 1.0F);
            } else {
                this.broadcastTitle(new LiteralText("Go!").formatted(Formatting.BOLD));
                this.broadcastSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 2.0F);

                // Delete spawn platform
                BlockPos min = new BlockPos(-5, 122, -5);
                BlockPos max = new BlockPos(5, 122, 5);

                for (BlockPos pos : BlockPos.iterate(min, max)) {
                    this.gameWorld.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private void tickEndWaiting(long time) {
        for (ServerPlayerEntity player : this.playerStates.keySet()) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        this.closeTime = time + (5 * 20);
        this.broadcastWin();
    }

    private void tickPlayers(long time) {
        Iterator<Map.Entry<ServerPlayerEntity, LoopDeLoopPlayer>> iterator = this.playerStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayerEntity, LoopDeLoopPlayer> entry = iterator.next();
            LoopDeLoopPlayer state = entry.getValue();
            ServerPlayerEntity player = entry.getKey();

            if (this.tickPlayer(player, state, time)) {
                iterator.remove();
            }
        }
    }

    private boolean tickPlayer(ServerPlayerEntity player, LoopDeLoopPlayer state, long time) {
        int nextHoopIdx = state.lastHoop + 1;
        if (nextHoopIdx >= this.map.hoops.size()) {
            this.onPlayerFinish(player, time);
            return true;
        }

        if (state.lastHoop != -1 && player.isOnGround()) {
            this.failHoop(player, state, time);
            return false;
        }

        LoopDeLoopHoop nextHoop = this.map.hoops.get(nextHoopIdx);

        Vec3d lastPos = state.lastPos;
        Vec3d currentPos = player.getPos();

        state.lastPos = currentPos;

        if (nextHoop.intersectsSegment(lastPos, currentPos)) {
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            giveRocket(player, 1);

            state.lastHoop = nextHoopIdx;
            state.previousFails = 0;
            state.lastFailOrSuccess = time;

            this.scoreboard.render(this.buildLeaderboard());

            return false;
        }

        if (this.testFailure(player, state, nextHoop)) {
            this.failHoop(player, state, time);
            return false;
        }

        return false;
    }

    private boolean testFailure(ServerPlayerEntity player, LoopDeLoopPlayer state, LoopDeLoopHoop nextHoop) {
        // player has traveled past the next hoop
        if (Math.floor(player.getZ()) > nextHoop.centre.getZ() + 1) {
            return true;
        }

        // player has travelled outside of the map
        double yMax = (this.config.yVarMax / 2.0) + 30;
        if (player.getZ() < -5 || player.getY() < 128 - yMax || player.getY() > 128 + yMax) {
            return true;
        }

        int minX = nextHoop.centre.getX() - nextHoop.radius;
        int maxX = nextHoop.centre.getX() + nextHoop.radius;

        if (state.lastHoop >= 0) {
            LoopDeLoopHoop last = this.map.hoops.get(state.lastHoop);

            // we've gone behind the last hoop
            if (player.getZ() < last.centre.getZ() - 10) {
                return true;
            }

            minX = Math.min(minX, last.centre.getX() - last.radius);
            maxX = Math.max(maxX, last.centre.getX() + last.radius);
        }

        return player.getX() > maxX + 30 || player.getX() < minX - 30;
    }

    private List<LoopDeLoopPlayer> buildLeaderboard() {
        return this.playerStates.values().stream()
                .sorted(Comparator.comparingInt(player -> this.config.loops - player.lastHoop))
                .limit(5)
                .collect(Collectors.toList());
    }

    private void onPlayerFinish(ServerPlayerEntity player, long time) {
        this.finished.add(new LoopDeLoopWinner(player.getEntityName(), time));
        this.lastCompleter = player;

        String ordinal = ordinal(this.finished.size());
        player.sendMessage(new LiteralText("You finished in " + ordinal + " place!"), true);
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void failHoop(ServerPlayerEntity player, LoopDeLoopPlayer state, long time) {
        if (time - state.lastFailOrSuccess < 10) {
            return;
        }

        state.previousFails += 1;
        state.lastFailOrSuccess = time;
        giveRocket(player, Math.min(state.previousFails, 3));

        ServerWorld world = this.gameWorld.getWorld();
        List<FireworkRocketEntity> rockets = world.getEntitiesByType(
                EntityType.FIREWORK_ROCKET,
                player.getBoundingBox(),
                firework -> firework.getOwner() == player
        );
        rockets.forEach(Entity::remove);

        if (state.lastHoop == -1) {
            this.spawnLogic.spawnPlayer(player);
        } else {
            LoopDeLoopHoop lastHoop = this.map.hoops.get(state.lastHoop);
            Vec3d centre = Vec3d.ofCenter(lastHoop.centre);

            Random random = player.getRandom();
            float radius = 2;

            state.teleport(
                    centre.x + MathHelper.nextDouble(random, -radius, radius),
                    centre.y + MathHelper.nextDouble(random, -radius, radius),
                    centre.z + 2
            );
        }

        player.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private void broadcastWin() {
        Text message;

        if (this.finished.isEmpty()) {
            message = new LiteralText("The game ended, but nobody won!").formatted(Formatting.GOLD);
        } else {
            StringBuilder message_builder = new StringBuilder();
            message_builder.append("The game has ended!\n");

            for (int i = 0; i < 5 && i < this.finished.size(); i++) {
                LoopDeLoopWinner player = this.finished.get(i);
                message_builder.append(String.format(
                        "    %s place - %s in %.2fs\n",
                        ordinal(i + 1),
                        player.name,
                        (player.time - this.startTime) / 20.0f
                ));
            }

            String message_string = message_builder.toString();
            message = new LiteralText(message_string).formatted(Formatting.GOLD);
        }

        this.broadcastMessage(message);
        this.broadcastSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    private boolean onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        long time = this.gameWorld.getWorld().getTime();
        this.failHoop(player, this.playerStates.get(player), time);
        return true;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        long time = this.gameWorld.getWorld().getTime();
        this.failHoop(player, this.playerStates.get(player), time);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);

        ItemStack elytra = ItemStackBuilder.of(Items.ELYTRA)
                .setUnbreakable()
                .build();
        player.equipStack(EquipmentSlot.CHEST, elytra);

        giveRocket(player, this.config.startRockets);
    }

    private static void giveRocket(ServerPlayerEntity player, int n) {
        ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, n);
        player.inventory.insertStack(rockets);
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    // TODO: extract common broadcast utils into plasmid
    private void broadcastMessage(Text message) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            player.sendMessage(message, false);
        }
    }

    private void broadcastTitle(Text message) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            TitleS2CPacket packet = new TitleS2CPacket(TitleS2CPacket.Action.TITLE, message, 1, 5, 3);
            player.networkHandler.sendPacket(packet);
        }
    }

    private void broadcastSound(SoundEvent sound, float pitch) {
        for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
            if (player.equals(this.lastCompleter)) {
                continue;
            }
            player.playSound(sound, SoundCategory.PLAYERS, 1.0F, pitch);
        }
    }

    private void broadcastSound(SoundEvent sound) {
        this.broadcastSound(sound, 1.0F);
    }
}
