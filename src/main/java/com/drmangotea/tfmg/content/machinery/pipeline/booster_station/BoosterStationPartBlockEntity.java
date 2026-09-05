package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A block that is part of an assembled Booster Station - a casing or one of the two stubs.
 * <p>
 * Every part carries a {@link FluidTransportBehaviour}, so an assembled row is a straight run of pipe as far
 * as Create is concerned and fluid flows through it with no special handling. What the parts do not do is
 * decide anything: the controller casing owns the pressure model, and each part just reports which of its
 * faces carry flow and asks the controller what pressure to put on them.
 * <p>
 * The controller is remembered as an offset rather than an absolute position so a station survives being
 * printed from a schematic somewhere else.
 * <p>
 * The base is a {@link KineticBlockEntity} because the casing is the part that takes rotation, and both parts
 * share everything else. A stub's block is deliberately not an {@code IRotate}, and Create's propagator skips
 * kinetic block entities whose block is not one, so a stub is a node that never joins a network and stays at
 * zero speed and zero stress.
 */
public abstract class BoosterStationPartBlockEntity extends KineticBlockEntity {

    @Nullable
    private BlockPos controllerOffset;

    protected BoosterStationPipeBehaviour pipe;

    public BoosterStationPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        pipe = new BoosterStationPipeBehaviour(this);
        behaviours.add(pipe);
    }

    public boolean isAssembled() {
        return controllerOffset != null;
    }

    /**
     * Whether this part is part of a row that is actually built, and so may move fluid at all.
     * <p>
     * A row of loose casings and stubs is just blocks in a line: it must not connect to a pipe, must not
     * carry flow and must not drive any pressure. Everything that lets fluid past a part goes through here,
     * so there is one place that decides it.
     */
    public boolean carriesFluid() {
        return isAssembled();
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerOffset == null ? null : worldPosition.offset(controllerOffset);
    }

    @Nullable
    public PumpCasingBlockEntity getStation() {
        BlockPos controllerPos = getControllerPos();
        if (controllerPos == null || level == null)
            return null;
        if (level.getBlockEntity(controllerPos) instanceof PumpCasingBlockEntity station && station.isAssembled())
            return station;
        return null;
    }

    public void setStation(BlockPos controllerPos) {
        controllerOffset = controllerPos.subtract(worldPosition);
        onStationChanged();
    }

    public void clearStation() {
        controllerOffset = null;
        onStationChanged();
    }

    /**
     * Assembling or breaking a row changes which faces carry fluid, so the pipe data has to be rebuilt and the
     * neighbours told - otherwise a pipe already sitting against a stub never notices it became a connection.
     * <p>
     * The shape update is not redundant with the block change assembly makes: a stub that already happened to
     * face the right way is written back unchanged, and an unchanged write updates nothing.
     */
    protected void onStationChanged() {
        refreshFluidConnections();
    }

    /**
     * Rebuilds this part's pipe data and tells the neighbours, for whenever {@link #carriesFluid()} changes
     * its answer - assembling a row, closing it, or breaking it.
     */
    public void refreshFluidConnections() {
        if (level == null)
            return;
        if (pipe != null)
            pipe.wipePressure();
        notifyUpdate();
        if (!level.isClientSide) {
            getBlockState().updateNeighbourShapes(level, worldPosition, Block.UPDATE_ALL);
            FluidPropagator.propagateChangedPipe(level, worldPosition, getBlockState());
        }
    }

    /** Called when this block is being removed; takes the rest of the row apart with it. */
    public void onPartRemoved() {
        PumpCasingBlockEntity station = getStation();
        if (station != null)
            station.disassemble(worldPosition);
    }

    /**
     * Whether one of this part's faces is carrying fluid right now. Create writes the flow on a connection
     * whether or not the packet is a client one, so this reads the same on both sides.
     */
    public boolean hasFluidMoving() {
        if (pipe == null || !carriesFluid())
            return false;
        for (Direction side : Iterate.directions) {
            PipeConnection.Flow flow = pipe.getFlow(side);
            if (flow != null && !flow.fluid.isEmpty())
                return true;
        }
        return false;
    }

    /** Which faces of this part fluid can move through. Nothing until the row is assembled. */
    public abstract boolean carriesFlowToward(Direction direction);

    /** The direction fluid travels through the row, or null while the row is not assembled. */
    @Nullable
    public abstract Direction getFlowDirection();

    /** Pressure this part holds on its upstream face - the station's pull. */
    public float getInboundPressure() {
        PumpCasingBlockEntity station = getStation();
        return station == null ? 0 : station.getPullPressure();
    }

    /** Pressure this part holds on its downstream face - the station's discharge. */
    public float getOutboundPressure() {
        PumpCasingBlockEntity station = getStation();
        return station == null ? 0 : station.getOutputPressure();
    }

    /**
     * Whether pressure walking down the line from another station should stop here and be handed to this
     * station as its input. Only an inlet stub, and only on the face the pipe actually meets.
     */
    public boolean acceptsLineInputFrom(Direction outerFace) {
        return false;
    }

    /**
     * The station panel, then whatever Create wants to say about this part as a kinetic block. A driven casing
     * therefore reads its own stress under the station's figures; a stub adds nothing, because it is never
     * driven.
     */
    @Override
    public final boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean described = addPartTooltip(tooltip, isPlayerSneaking);
        boolean kinetics = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return described || kinetics;
    }

    /** What this part has to say for itself; the casing also describes a row that is not assembled yet. */
    protected boolean addPartTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        PumpCasingBlockEntity station = getStation();
        if (station == null)
            return false;
        addPartHeader(tooltip);
        return station.addStationTooltip(tooltip, isPlayerSneaking);
    }

    /**
     * The panel is headed with the name of the block the player is actually looking at, which is what makes an
     * assembled casing read as a Pipeline Pump and a loose one as a Pump Casing.
     */
    protected void addPartHeader(List<Component> tooltip) {
        TFMGLang.builder()
                .add(getBlockState().getBlock().getName())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        controllerOffset = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeStation(tag);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeStation(tag);
    }

    private void writeStation(CompoundTag tag) {
        if (controllerOffset != null)
            tag.putLong("Controller", controllerOffset.asLong());
    }
}
