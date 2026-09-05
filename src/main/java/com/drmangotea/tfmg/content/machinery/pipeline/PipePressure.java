package com.drmangotea.tfmg.content.machinery.pipeline;

import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.registry.TFMGDataMaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What one pipe block is rated for and how much pressure a block of it eats.
 * <p>
 * Values come from the {@code tfmg:pipe_pressure} data map, so a pack can retune them or rate a pipe from
 * another mod without touching code; anything with no entry falls back to the config defaults. Only pressure
 * put on a line by a Booster Station is ever measured against {@link #rating()} - Create's own pumps are not
 * subject to any of this.
 */
public record PipePressure(int rating, float decay) {

    public static final Codec<PipePressure> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(Codec.INT.fieldOf("rating").forGetter(PipePressure::rating),
                    Codec.FLOAT.optionalFieldOf("decay", 0f).forGetter(PipePressure::decay))
            .apply(instance, PipePressure::new));

    /** The rating and decay of the given pipe, or the configured default if it has no entry. */
    public static PipePressure of(BlockState state) {
        PipePressure entry = entry(state);
        if (entry != null)
            return entry;
        return new PipePressure(TFMGConfigs.common().pipeline.defaultPipeRating.get(),
                TFMGConfigs.common().pipeline.defaultPipeDecay.getF());
    }

    /**
     * The data map's own entry for this block, or null if it has none. Worth distinguishing from
     * {@link #of(BlockState)} when the question is "is this a rated pipe at all" rather than "what would this
     * hold" - a pump has no entry, and reading a rating off one would be meaningless.
     */
    @Nullable
    public static PipePressure entry(BlockState state) {
        return state.getBlock().builtInRegistryHolder().getData(TFMGDataMaps.PIPE_PRESSURE);
    }
}
