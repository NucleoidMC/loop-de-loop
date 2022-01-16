package io.github.restioson.loopdeloop.game;

import com.google.common.collect.Sets;
import io.github.restioson.loopdeloop.LoopDeLoop;
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
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class LoopDeLoopActive {
    private static final GameTeam TEAM = new GameTeam(
            new GameTeamKey(LoopDeLoop.ID),
            GameTeamConfig.builder()
                    .setCollision(AbstractTeam.CollisionRule.NEVER)
                    .setFriendlyFire(false)
                    .build()
    );

    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final LoopDeLoopMap map;
    private final LoopDeLoopConfig config;
    private final List<LoopDeLoopWinner> finished;
    private final LoopDeLoopSpawnLogic spawnLogic;
    private final LoopDeLoopTimerBar timerBar;
    private final LoopDeLoopSideBar sidebar;

    private final TeamManager teamManager;

    // Only stores flying players, i.e non-completed players
    private final Object2ObjectMap<ServerPlayerEntity, LoopDeLoopPlayer> playerStates;

    private StartingCountdown startingCountdown;

    @Nullable
    private ServerPlayerEntity lastCompleter;
    private long closeTime = -1;
    private long finishTime = -1;
    private long startTime = -1;
    private static final int LEAP_INTERVAL_TICKS = 5;
    private static final double LEAP_VELOCITY = 3.0;

    private LoopDeLoopActive(
            ServerWorld world, GameSpace gameSpace,
            LoopDeLoopMap map, LoopDeLoopConfig config,
            Set<ServerPlayerEntity> participants,
            GlobalWidgets widgets, TeamManager teamManager
    ) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.finished = new ArrayList<>();
        this.spawnLogic = new LoopDeLoopSpawnLogic(world, map);
        this.timerBar = new LoopDeLoopTimerBar(widgets);
        this.playerStates = new Object2ObjectOpenHashMap<>();
        this.teamManager = teamManager;

        for (ServerPlayerEntity player : participants) {
            this.playerStates.put(player, new LoopDeLoopPlayer(player));
        }

        this.sidebar = new LoopDeLoopSideBar(widgets, this.config.loops());
    }

    public static void open(ServerWorld world, GameSpace gameSpace, LoopDeLoopMap map, LoopDeLoopConfig config) {
        gameSpace.setActivity(activity -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(activity);
            TeamManager teamManager = TeamManager.addTo(activity);
            teamManager.addTeam(TEAM);

            Set<ServerPlayerEntity> participants = Sets.newHashSet(gameSpace.getPlayers());
            LoopDeLoopActive active = new LoopDeLoopActive(world, gameSpace, map, config, participants, widgets, teamManager);

            activity.deny(GameRuleType.CRAFTING);
            activity.deny(GameRuleType.PORTALS);
            activity.deny(GameRuleType.PVP);
            activity.deny(GameRuleType.BLOCK_DROPS);
            activity.deny(GameRuleType.FALL_DAMAGE);
            activity.deny(GameRuleType.HUNGER);
            activity.deny(GameRuleType.THROW_ITEMS);

            activity.listen(GameActivityEvents.ENABLE, active::onOpen);

            activity.listen(GamePlayerEvents.OFFER, active::offerPlayer);
            activity.listen(GamePlayerEvents.REMOVE, active::removePlayer);

            activity.listen(GameActivityEvents.TICK, active::tick);

            activity.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
            activity.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);

            activity.listen(ItemUseEvent.EVENT, active::onUseItem);
        });
    }

    private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        ItemStack heldStack = player.getStackInHand(hand);

        LoopDeLoopPlayer state = this.playerStates.get(player);
        if (state != null) {
            if (heldStack.getItem() == Items.FEATHER) {
                ItemCooldownManager cooldown = player.getItemCooldownManager();
                if (!cooldown.isCoolingDown(heldStack.getItem())) {
                    Vec3d rotationVec = player.getRotationVec(1.0F);
                    player.setVelocity(rotationVec.multiply(LEAP_VELOCITY));
                    Vec3d oldVel = player.getVelocity();
                    player.setVelocity(oldVel.x, oldVel.y + 0.5f, oldVel.z);
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                    player.playSound(SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    cooldown.set(heldStack.getItem(), LEAP_INTERVAL_TICKS);

                    state.leapsUsed++;
                }
            } else if (heldStack.getItem() == Items.FIREWORK_ROCKET) {
                state.fireworksUsed++;
            }
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    private void onOpen() {
        for (ServerPlayerEntity player : this.playerStates.keySet()) {
            this.teamManager.addPlayerTo(player, TEAM.key());

            String[] lines;
            if (this.config.flappyMode()) {
                lines = new String[] {
                        "Loop-de-loop - fly through all the hoops with your feather. Whoever does it first wins!",
                        "Right-click with your feather to fly forwards."
                };
            } else {
                lines = new String[] {
                        "Loop-de-loop - fly through all the hoops with your elytra. Whoever does it first wins!",
                        "You start with some rockets and can get more by flying through hoops, or when you fail a hoop."
                };
            }

            for (String line : lines) {
                Text text = new LiteralText(line).formatted(Formatting.GOLD);
                player.sendMessage(text, false);
            }

            this.spawnParticipant(player);
        }

        long time = this.world.getTime();
        this.startTime = time - (time % 20) + (4 * 20) + 19;
        this.finishTime = this.startTime + (this.config.timeLimit() * 20);
        this.startingCountdown = new StartingCountdown(this.startTime);

        this.sidebar.render(this.buildLeaderboard());
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return this.spawnLogic.acceptPlayer(offer, GameMode.SPECTATOR);
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.playerStates.remove(player);
    }

    // thx https://stackoverflow.com/a/6810409/4871468
    private static String ordinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + suffixes[i % 10];
        };
    }

    private void tick() {
        long time = this.world.getTime();

        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                this.gameSpace.close(GameCloseReason.FINISHED);
            }
            return;
        }

        if (this.startingCountdown != null) {
            if (this.startingCountdown.tick(this.playerStates.keySet()::iterator, time)) {
                this.startingCountdown = null;

                if (this.map.getSpawnPlatform() != null) {
                    for (BlockPos pos : this.map.getSpawnPlatform()) {
                        this.world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }

            return;
        }

        if (time > this.finishTime || this.playerStates.isEmpty()) {
            this.tickEndWaiting(time);
            return;
        }

        this.timerBar.update(this.finishTime - time, this.config.timeLimit() * 20);
        this.tickPlayers(time);
    }

    private void tickEndWaiting(long time) {
        for (ServerPlayerEntity player : this.playerStates.keySet()) {
            player.changeGameMode(GameMode.SPECTATOR);
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
            this.onPlayerFinish(player, state, time);
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

            if (!this.config.flappyMode()) {
                giveRocket(player, 1);
            }

            state.totalHoops++;
            state.lastHoop = nextHoopIdx;
            state.previousFails = 0;
            state.lastFailOrSuccess = time;

            this.sidebar.render(this.buildLeaderboard());

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
        double yMax = (this.config.yVarMax() / 2.0) + 30;
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
                .sorted(Comparator.comparingInt(player -> this.config.loops() - player.lastHoop))
                .limit(5)
                .collect(Collectors.toList());
    }

    private void onPlayerFinish(ServerPlayerEntity player, LoopDeLoopPlayer state, long time) {
        this.finished.add(new LoopDeLoopWinner(player.getEntityName(), time));
        this.lastCompleter = player;

        String ordinal = ordinal(this.finished.size());

        var message = new LiteralText("You finished in ")
                .append(new LiteralText(ordinal).formatted(Formatting.AQUA))
                .append(" place!");

        player.sendMessage(message, true);
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.changeGameMode(GameMode.SPECTATOR);

        this.publishPlayerStatistics(player, state, time);
    }

    private void publishPlayerStatistics(ServerPlayerEntity player, LoopDeLoopPlayer state, long finishTime) {
        int playerTime = (int) (finishTime - this.startTime);

        var statistics = this.gameSpace.getStatistics().bundle(this.config.statisticsBundle());

        var playerStatistics = statistics.forPlayer(player);
        playerStatistics.set(StatisticKeys.QUICKEST_TIME, playerTime);
        state.applyTo(playerStatistics);
    }

    private void failHoop(ServerPlayerEntity player, LoopDeLoopPlayer state, long time) {
        if (time - state.lastFailOrSuccess < 10) {
            return;
        }

        state.previousFails += 1;
        state.lastFailOrSuccess = time;
        state.missedHoops++;

        if (!this.config.flappyMode()) {
            giveRocket(player, Math.min(state.previousFails, 3));

            List<FireworkRocketEntity> rockets = this.world.getEntitiesByType(
                    EntityType.FIREWORK_ROCKET,
                    player.getBoundingBox(),
                    firework -> firework.getOwner() == player
            );
            for (FireworkRocketEntity rocket : rockets) {
                rocket.remove(Entity.RemovalReason.DISCARDED);
            }
        }

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
        MutableText message;

        if (this.finished.isEmpty()) {
            message = new LiteralText("The game ended, but nobody won!").formatted(Formatting.GOLD);
        } else {
            message = new LiteralText("The game has ended!\n").formatted(Formatting.GOLD);

            for (int i = 0; i < 5 && i < this.finished.size(); i++) {
                LoopDeLoopWinner player = this.finished.get(i);

                Text ordinal = new LiteralText(ordinal(i + 1)).formatted(Formatting.AQUA);
                Text playerName = new LiteralText(player.name()).formatted(Formatting.AQUA);
                Text time = new LiteralText(String.format("%.2fs", (player.time() - this.startTime) / 20.0f)).formatted(Formatting.GREEN);

                MutableText line = new LiteralText("   ").append(ordinal)
                        .append(" place - ").append(playerName)
                        .append(" in ").append(time);
                message.append(line.append("\n"));
            }
        }

        this.gameSpace.getPlayers().sendMessage(message);
        this.broadcastSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        long time = this.world.getTime();
        this.failHoop(player, this.playerStates.get(player), time);
        return ActionResult.FAIL;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        long time = this.world.getTime();
        this.failHoop(player, this.playerStates.get(player), time);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);

        if (this.config.flappyMode()) {
            ItemStack feather = ItemStackBuilder.of(Items.FEATHER)
                    .addLore(new LiteralText("Flap flap"))
                    .build();
            player.equipStack(EquipmentSlot.OFFHAND, feather);
        } else {
            ItemStack elytra = ItemStackBuilder.of(Items.ELYTRA)
                    .setUnbreakable()
                    .build();
            player.equipStack(EquipmentSlot.CHEST, elytra);

            giveRocket(player, this.config.startRockets());
        }
    }

    private static void giveRocket(ServerPlayerEntity player, int n) {
        ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, n);
        player.getInventory().insertStack(rockets);
    }

    private void broadcastSound(SoundEvent sound, float pitch) {
        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
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
