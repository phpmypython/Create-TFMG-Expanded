package com.drmangotea.tfmg.content.machinery.pipeline;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * One pipe face a station is holding pressure on, and how much of that face's pressure is the station's.
 * <p>
 * Create only offers {@link FluidTransportBehaviour#addPressure} - a relative change - and wipes a pipe's
 * pressure outright whenever the network around it changes. A station therefore has to remember its own
 * contribution so it can take exactly that much back off, and notice when a wipe has already taken it.
 */
public record PressuredFace(BlockPos pos, Direction side, boolean inbound, float pressure) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putInt("Side", side.get3DDataValue());
        tag.putBoolean("In", inbound);
        tag.putFloat("P", pressure);
        return tag;
    }

    public static PressuredFace load(CompoundTag tag) {
        return new PressuredFace(new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                Direction.from3DDataValue(tag.getInt("Side")), tag.getBoolean("In"), tag.getFloat("P"));
    }

    /**
     * How much of this face's current pressure is still ours. Less than {@link #pressure()} means Create
     * wiped the pipe since we last applied it, and the difference has to go back on.
     */
    public float standingShare(Level level) {
        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
        if (pipe == null)
            return 0;
        PipeConnection connection = pipe.getConnection(side);
        if (connection == null)
            return 0;
        return Math.min(pressure, connection.getPressure().get(inbound));
    }

    /** Adds {@code delta} to this face's pressure, in the direction this face is driven. */
    public void addPressure(Level level, float delta) {
        if (Math.abs(delta) < 1.0E-4F)
            return;
        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
        if (pipe != null)
            pipe.addPressure(side, inbound, delta);
    }
}
