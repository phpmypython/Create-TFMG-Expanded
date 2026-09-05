package com.drmangotea.tfmg.datagen;

import com.drmangotea.tfmg.TFMG;
import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipes;
import com.drmangotea.tfmg.content.machinery.pipeline.PipePressure;
import com.drmangotea.tfmg.registry.TFMGDataMaps;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

public class TFMGDataMapProvider extends DataMapProvider {

    /**
     * Starting pressure ratings and per-block decay, one per pipe material. Higher grades hold more pressure
     * and lose less of it along the run, which is what makes steel near a station and cheaper pipe further out
     * the natural way to build a line.
     */
    private static final PipePressure STEEL = new PipePressure(1536, 2);
    private static final PipePressure CAST_IRON = new PipePressure(768, 4);
    /** Not one of the grades the feature was specified around; sits between cast iron and steel. */
    private static final PipePressure ALUMINUM = new PipePressure(640, 3);
    private static final PipePressure BRASS = new PipePressure(512, 6);
    /** Create's own pipes are copper. */
    private static final PipePressure COPPER = new PipePressure(384, 6);
    private static final PipePressure PLASTIC = new PipePressure(192, 8);

    public TFMGDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        Builder<PipePressure, Block> pipes = this.builder(TFMGDataMaps.PIPE_PRESSURE);
        addPipeFamily(pipes, TFMGPipes.PipeMaterial.STEEL, STEEL);
        addPipeFamily(pipes, TFMGPipes.PipeMaterial.CAST_IRON, CAST_IRON);
        addPipeFamily(pipes, TFMGPipes.PipeMaterial.ALUMINUM, ALUMINUM);
        addPipeFamily(pipes, TFMGPipes.PipeMaterial.BRASS, BRASS);
        addPipeFamily(pipes, TFMGPipes.PipeMaterial.PLASTIC, PLASTIC);

        // Create's copper pipes, valves and smart pipes, which a station's line will run through as readily
        for (String name : new String[] { "fluid_pipe", "glass_fluid_pipe", "encased_fluid_pipe",
                "smart_fluid_pipe", "fluid_valve" })
            pipes.add(ResourceLocation.fromNamespaceAndPath("create", name), COPPER, false);
    }

    /**
     * Every shape a pipe material comes in carries the same rating: encasing or glazing a pipe changes how it
     * looks and connects, not what it is made of. Pumps are left out - a station's walk never runs through
     * one, so a rating would never be read.
     */
    private void addPipeFamily(Builder<PipePressure, Block> builder,
                               TFMGPipes.PipeMaterial material, PipePressure pressure) {
        for (String pattern : new String[] { "%s_pipe", "glass_%s_pipe", "encased_%s_pipe",
                "%s_smart_fluid_pipe", "%s_fluid_valve" })
            builder.add(TFMG.asResource(pattern.formatted(material.name)), pressure, false);
    }
}
