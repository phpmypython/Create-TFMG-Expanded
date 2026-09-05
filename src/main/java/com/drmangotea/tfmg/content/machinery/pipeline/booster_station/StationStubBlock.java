package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * The flanged stub that caps either end of a Booster Station. A pipe connects to its outer face only; the
 * other side mates with the first casing.
 * <p>
 * One block does both ends. Assembly turns it to face out of the row, so the player never has to decide
 * whether they are placing an inlet or an outlet - the row does.
 */
public class StationStubBlock extends HorizontalDirectionalBlock
        implements IBE<StationStubBlockEntity>, IWrenchable {

    public static final MapCodec<StationStubBlock> CODEC = simpleCodec(StationStubBlock::new);

    public StationStubBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Flange towards the player, which is the face they are about to run pipe to
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Loose stubs turn; a stub that is part of a row does not - assembly owns which way it faces. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (BoosterStationAssembly.isAssembledPart(context.getLevel(), context.getClickedPos()))
            return InteractionResult.PASS;
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BoosterStationPartBlockEntity part)
                part.onPartRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<StationStubBlockEntity> getBlockEntityClass() {
        return StationStubBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StationStubBlockEntity> getBlockEntityType() {
        return TFMGBlockEntities.STATION_STUB.get();
    }
}
