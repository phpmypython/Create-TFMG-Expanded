package com.drmangotea.tfmg.content.machinery.pipeline;

import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which pipes are carrying pressure that a Booster Station put there, and how much.
 * <p>
 * This is the one thing that separates station pressure from Create's: a pipe's own
 * {@link com.simibubi.create.content.fluids.PipeConnection} only knows a total, and a Create pump's share of
 * that total is not subject to ratings or bursting. Stations register what they added here, and only these
 * numbers are measured against a pipe's rating.
 * <p>
 * Kept on both sides. The server fills it from the station's pressure walk; the client fills it when a
 * station's block entity data arrives, which is what lets the goggle readout on a pipe warn about a rating.
 * It is a cache of live world state, never saved - a station rebuilds its entries when it next recalculates.
 */
public class StationPressure {

    public record Entry(float pressure, BlockPos controller) {
    }

    private static final WorldAttached<Map<BlockPos, Entry>> BY_PIPE =
            new WorldAttached<>($ -> new HashMap<>());
    private static final WorldAttached<Map<BlockPos, Set<BlockPos>>> BY_STATION =
            new WorldAttached<>($ -> new HashMap<>());

    /** Replaces everything the given station had registered with the pipes it is now driving. */
    public static void set(LevelAccessor level, BlockPos controller, Map<BlockPos, Float> pressures) {
        Map<BlockPos, Entry> byPipe = BY_PIPE.get(level);
        Map<BlockPos, Set<BlockPos>> byStation = BY_STATION.get(level);

        Set<BlockPos> previous = byStation.remove(controller);
        if (previous != null)
            for (BlockPos pipe : previous) {
                Entry entry = byPipe.get(pipe);
                if (entry != null && entry.controller().equals(controller))
                    byPipe.remove(pipe);
            }

        if (pressures.isEmpty())
            return;

        Set<BlockPos> owned = new HashSet<>(pressures.size());
        pressures.forEach((pipe, pressure) -> {
            Entry existing = byPipe.get(pipe);
            // Two stations reaching the same pipe: the higher pressure is the one that can burst it
            if (existing == null || existing.pressure() < pressure)
                byPipe.put(pipe, new Entry(pressure, controller));
            owned.add(pipe);
        });
        byStation.put(controller, owned);
    }

    public static void clear(LevelAccessor level, BlockPos controller) {
        set(level, controller, Map.of());
    }

    /** Station pressure standing on this pipe, or 0 if no station is driving it. */
    public static float get(LevelAccessor level, BlockPos pipe) {
        Entry entry = entry(level, pipe);
        return entry == null ? 0 : entry.pressure();
    }

    /** Station pressure standing on this pipe together with the station that put it there. */
    @Nullable
    public static Entry entry(LevelAccessor level, BlockPos pipe) {
        return BY_PIPE.get(level).get(pipe);
    }
}
