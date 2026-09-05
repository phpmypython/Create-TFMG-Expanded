package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Turning a line of loose blocks into a station, and telling whether such a line exists at all.
 * <p>
 * The row is {@code [stub] [casing] x N [stub]} in a straight horizontal line, N at least one. Both ends take
 * the same stub block: which one is the inlet follows from the direction the casings face, and assembly turns
 * each stub outwards so a pipe only ever meets its outer face. The casing that was clicked sets the flow
 * direction for the whole row, so a row whose casings were placed facing different ways along the same axis
 * still assembles rather than making the player re-place them.
 * <p>
 * It is one step. A block of steel closes every casing and starts the station at the same time, because the
 * impeller that used to be the first step is now a crafting ingredient of the casing itself.
 */
public class BoosterStationAssembly {

    /** A row longer than this is almost certainly not what the player meant, and bounds the scan. */
    public static final int MAX_CASINGS = 64;

    public record Row(List<BlockPos> casings, BlockPos inletStub, BlockPos outletStub, Direction flow) {

        /** The casing next to the outlet stub, which runs the station. */
        public BlockPos controller() {
            return casings.get(casings.size() - 1);
        }
    }

    /**
     * Finds the row the given casing belongs to, or null if it is not part of a complete one.
     */
    @Nullable
    public static Row scan(Level level, BlockPos casingPos, Direction flow) {
        Direction.Axis axis = flow.getAxis();
        LinkedList<BlockPos> casings = new LinkedList<>();
        casings.add(casingPos);

        BlockPos cursor = casingPos.relative(flow);
        while (isFreeCasing(level, cursor, axis)) {
            if (casings.size() >= MAX_CASINGS)
                return null;
            casings.addLast(cursor);
            cursor = cursor.relative(flow);
        }
        BlockPos outletStub = cursor;

        cursor = casingPos.relative(flow.getOpposite());
        while (isFreeCasing(level, cursor, axis)) {
            if (casings.size() >= MAX_CASINGS)
                return null;
            casings.addFirst(cursor);
            cursor = cursor.relative(flow.getOpposite());
        }
        BlockPos inletStub = cursor;

        if (!isFreeStub(level, inletStub) || !isFreeStub(level, outletStub))
            return null;
        return new Row(casings, inletStub, outletStub, flow);
    }

    /**
     * Builds a valid row: every casing becomes a closed Pipeline Pump facing the way the row flows, and the
     * stubs turn to face out of it. Closing is what makes the row a length of pipe, so it happens before any
     * part is told which station it belongs to and starts rebuilding its connections.
     */
    public static void assemble(Level level, Row row) {
        BlockPos controllerPos = row.controller();

        for (BlockPos casingPos : row.casings())
            level.setBlock(casingPos, TFMGBlocks.PIPELINE_PUMP.getDefaultState()
                    .setValue(HorizontalDirectionalBlock.FACING, row.flow()), 3);
        orientStub(level, row.inletStub(), row.flow().getOpposite());
        orientStub(level, row.outletStub(), row.flow());

        if (level.getBlockEntity(controllerPos) instanceof PumpCasingBlockEntity controller)
            controller.setCasingCount(row.casings().size());
        if (level.getBlockEntity(row.inletStub()) instanceof StationStubBlockEntity inlet)
            inlet.setOutlet(false);
        if (level.getBlockEntity(row.outletStub()) instanceof StationStubBlockEntity outlet)
            outlet.setOutlet(true);

        // Last, so every part already knows the shape of the row before any of them wakes up
        for (BlockPos casingPos : row.casings())
            setStation(level, casingPos, controllerPos);
        setStation(level, row.inletStub(), controllerPos);
        setStation(level, row.outletStub(), controllerPos);

        // The controller is the last casing to be told, so every casing before it worked out its stress while
        // there was still no station for it to find. Now that there is one, the whole row asks again
        if (level.getBlockEntity(controllerPos) instanceof PumpCasingBlockEntity controller)
            controller.refreshRowStress();

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, controllerPos);
    }

    /**
     * Turns a built station round: the same blocks, the opposite flow, so what was the outlet becomes the
     * inlet. This is the wrench on an assembled casing, and it matches what wrenching a Create mechanical
     * pump does to its own direction.
     * <p>
     * It is done by releasing the row and building it again the other way rather than by editing the parts in
     * place. Releasing is what takes the station's pressure back off the line and empties both of its applied
     * face maps, so the walk starts clean in the new direction; scanning again re-derives which stub is which
     * and moves the controller to the casing that is now next to the outlet; and both halves already tell the
     * neighbouring pipes to re-evaluate. Only the oil has to be carried across by hand, because the flag
     * lives on the controller and the controller has moved.
     *
     * @return whether the row came back together
     */
    public static boolean reverse(Level level, PumpCasingBlockEntity station) {
        Direction reversed = station.getFlow().getOpposite();
        BlockPos casingPos = station.getBlockPos();
        boolean lubricated = station.isLubricated();

        if (!station.release(null))
            return false;

        Row row = scan(level, casingPos, reversed);
        if (row == null)
            return false;

        assemble(level, row);
        if (lubricated && level.getBlockEntity(row.controller()) instanceof PumpCasingBlockEntity controller)
            controller.setLubricated(true);
        return true;
    }

    public static void deny(Level level, BlockPos pos) {
        AllSoundEvents.DENY.playOnServer(level, pos);
    }

    /**
     * Whether the part at this position belongs to an assembled station.
     * <p>
     * A loose casing or stub turns with the wrench like any other directional machine, but an assembled one
     * must not: its facing is the row's, and turning a single part would leave the row pointing two ways at
     * once and its stubs facing into the line.
     */
    public static boolean isAssembledPart(LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BoosterStationPartBlockEntity part && part.isAssembled();
    }

    public static void consume(Player player, ItemStack stack) {
        if (!player.isCreative())
            stack.shrink(1);
    }

    private static void setStation(Level level, BlockPos pos, BlockPos controllerPos) {
        if (level.getBlockEntity(pos) instanceof BoosterStationPartBlockEntity part)
            part.setStation(controllerPos);
    }

    private static void orientStub(Level level, BlockPos pos, Direction outward) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(HorizontalDirectionalBlock.FACING, outward), 3);
    }

    private static boolean isFreeCasing(Level level, BlockPos pos, Direction.Axis axis) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(TFMGBlocks.PUMP_CASING.get()))
            return false;
        if (state.getValue(HorizontalDirectionalBlock.FACING).getAxis() != axis)
            return false;
        return level.getBlockEntity(pos) instanceof PumpCasingBlockEntity be && !be.isAssembled();
    }

    private static boolean isFreeStub(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof StationStubBlock))
            return false;
        return level.getBlockEntity(pos) instanceof StationStubBlockEntity be && !be.isAssembled();
    }
}
