package dev.coldpenguins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.util.RandomSource;

/** Stable names are used in entity and nest saves; ordinals are only used for synchronization. */
public enum PenguinSpecies {
    ORIGINAL("original", 1.0), EMPEROR("emperor", 1.2), GENTOO("gentoo", 1.0), AFRICAN("african", 0.9), BLUE("blue", 0.65),
    MACARONI("macaroni", 0.88), ROCKHOPPER("rockhopper", 0.70);

    final String id;
    final double scale;
    PenguinSpecies(String id, double scale) { this.id = id; this.scale = scale; }

    static PenguinSpecies fromName(String id) {
        for (var species : values()) if (species.id.equals(id)) return species;
        return ORIGINAL;
    }

    static PenguinSpecies forLocation(LevelAccessor level, BlockPos pos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(Biomes.WINDSWEPT_HILLS) && hasNearbyOcean(level, pos, false)) return ROCKHOPPER;
        if (biome.is(Biomes.STONY_SHORE)) {
            // Give the crested species half of shore flocks. The existing species share the rest.
            int roll = random.nextInt(4);
            if (roll == 0 && hasNearbyOcean(level, pos, true)) return MACARONI;
            if (roll == 1) return ROCKHOPPER;
        }
        return forBiome(biome, random);
    }

    boolean hasCrest() {
        return this == MACARONI || this == ROCKHOPPER;
    }

    static boolean hasNearbyOcean(LevelAccessor level, BlockPos pos, boolean coldOnly) {
        int waterY = level.getSeaLevel() - 1;
        if (Math.abs(pos.getY() - waterY) > 32) return false;
        for (int distance = 8; distance <= 32; distance += 8) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Holder<Biome> sample = level.getUncachedNoiseBiome(
                            QuartPos.fromBlock(pos.getX() + dx * distance), QuartPos.fromBlock(waterY),
                            QuartPos.fromBlock(pos.getZ() + dz * distance));
                    if (coldOnly ? isColdOcean(sample) : sample.is(BiomeTags.IS_OCEAN)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isColdOcean(Holder<Biome> biome) {
        return biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN)
                || biome.is(Biomes.FROZEN_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN);
    }

    static PenguinSpecies forBiome(Holder<Biome> biome, RandomSource random) {
        if (biome.is(Biomes.BEACH)) return random.nextBoolean() ? AFRICAN : BLUE;
        if (biome.is(Biomes.STONY_SHORE)) return random.nextBoolean() ? GENTOO : BLUE;
        if (biome.is(Biomes.SNOWY_BEACH) || biome.is(Biomes.FROZEN_RIVER)) return GENTOO;
        if (isEmperorHabitat(biome)) return EMPEROR;
        return ORIGINAL;
    }
    static boolean isEmperorHabitat(Holder<Biome> biome) {
        return biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES)
                || biome.is(Biomes.FROZEN_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                || biome.is(Biomes.FROZEN_PEAKS);
    }
}
