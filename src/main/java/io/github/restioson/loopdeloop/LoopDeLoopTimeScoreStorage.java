package io.github.restioson.loopdeloop;

import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.storage.ServerStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoopDeLoopTimeScoreStorage implements ServerStorage {
	public final Object2ObjectMap<Identifier, Object2LongArrayMap<UUID>> scoreMap = new Object2ObjectOpenHashMap<>();

	public void putPlayerTime(Identifier config, ServerPlayerEntity player, long score) {
		if (getPlayerTime(config, player) > score) {
			this.scoreMap.get(config).put(player.getUuid(), score);
		}
	}

	public long getPlayerTime(Identifier identifier, ServerPlayerEntity player) {
		return this.scoreMap.computeIfAbsent(identifier, identifier1 -> new Object2LongArrayMap<>()).getOrDefault(player.getUuid(), Long.MAX_VALUE);
	}

	public LinkedHashMap<UUID, Long> getSortedScores(Identifier identifier) {
		return scoreMap.get(identifier).object2LongEntrySet().stream().sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	@Override
	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		CompoundTag configTag = new CompoundTag();
		this.scoreMap.forEach(((config, scoreMap) -> {
			ListTag playersTag = new ListTag();
			this.scoreMap.get(config).forEach((uuid, score) -> playersTag.add(this.createPlayerScoreTag(uuid, score)));

			configTag.put(config.toString(), playersTag);
		}));

		tag.put("Configs", configTag);
		return tag;
	}

	@Override
	public void fromTag(CompoundTag tag) {
		CompoundTag configTag = (CompoundTag) tag.get("Configs");
		configTag.getKeys().forEach(config -> {
			Identifier configId = Identifier.tryParse(config);
			scoreMap.put(configId, new Object2LongArrayMap<>());

			ListTag playersTag = configTag.getList(config, NbtType.COMPOUND);
			playersTag.forEach(tag1 -> {
				CompoundTag scoreTag = (CompoundTag) tag1;
				scoreMap.get(configId).put(scoreTag.getUuid("UUID"), scoreTag.getLong("Score"));
			});
		});
	}

	private CompoundTag createPlayerScoreTag(UUID uuid, long score) {
		CompoundTag tag = new CompoundTag();
		tag.putUuid("UUID", uuid);
		tag.putLong("Score", score);
		return tag;
	}
}
