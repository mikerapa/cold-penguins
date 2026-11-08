package dev.coldpenguins;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.util.RandomSource;

/** Stable names are used in entity and nest saves; ordinals are only used for synchronization. */
public enum PenguinSpecies {
    ORIGINAL("original", 1.0), EMPEROR("emperor", 1.2), GENTOO("gentoo", 1.0), AFRICAN("african", 0.9), BLUE("blue", 0.65);

    final String id;
    final double scale;
    PenguinSpecies(String id, double scale) { this.id = id; this.scale = scale; }

    static PenguinSpecies fromName(String id) {
        for (var species : values()) if (species.id.equals(id)) return species;
        return ORIGINAL;
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
