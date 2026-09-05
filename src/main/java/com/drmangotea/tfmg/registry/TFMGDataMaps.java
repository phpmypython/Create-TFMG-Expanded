package com.drmangotea.tfmg.registry;

import com.drmangotea.tfmg.TFMG;
import com.drmangotea.tfmg.content.machinery.pipeline.PipePressure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * TFMG's own data maps. A data map is a datapack file that attaches values to registry entries, so these are
 * retunable by packs and can name blocks from other mods without a hard dependency.
 */
public class TFMGDataMaps {

    /**
     * Pressure rating and per-block pressure decay of a pipe, read from
     * {@code data/<namespace>/data_maps/block/pipe_pressure.json}. Synced, because the goggle readout on a
     * pipe shows its rating.
     */
    public static final DataMapType<Block, PipePressure> PIPE_PRESSURE =
            DataMapType.builder(TFMG.asResource("pipe_pressure"), Registries.BLOCK, PipePressure.CODEC)
                    .synced(PipePressure.CODEC, false)
                    .build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(PIPE_PRESSURE);
    }
}
