package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The flanged stub at either end of a station. One block does both jobs: which end of the row it sits on
 * decides whether it is the inlet or the outlet, and assembly turns it to face out of the row so a pipe only
 * ever meets its outer face.
 */
public class StationStubBlockEntity extends BoosterStationPartBlockEntity {

    private boolean outlet;

    public StationStubBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setOutlet(boolean outlet) {
        this.outlet = outlet;
    }

    public boolean isOutlet() {
        return outlet;
    }

    /** The face a pipe connects to; the opposite one faces the first casing. */
    public Direction getOuterFace() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /**
     * A stub carries fluid on the same terms as the row it caps, which is the controller's answer. If the
     * controller is out of the loaded area the stub keeps carrying - the row is still built, and cutting the
     * line because a chunk went away would stall every network attached to it.
     */
    @Override
    public boolean carriesFluid() {
        if (!super.carriesFluid())
            return false;
        PumpCasingBlockEntity station = getStation();
        return station == null || station.carriesFluid();
    }

    @Override
    public boolean carriesFlowToward(Direction direction) {
        return carriesFluid() && direction.getAxis() == getOuterFace().getAxis();
    }

    @Override
    @Nullable
    public Direction getFlowDirection() {
        if (!carriesFluid())
            return null;
        return outlet ? getOuterFace() : getOuterFace().getOpposite();
    }

    @Override
    public boolean acceptsLineInputFrom(Direction outerFace) {
        return carriesFluid() && !outlet && getOuterFace() == outerFace;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        outlet = tag.getBoolean("Outlet");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Outlet", outlet);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putBoolean("Outlet", outlet);
    }
}
