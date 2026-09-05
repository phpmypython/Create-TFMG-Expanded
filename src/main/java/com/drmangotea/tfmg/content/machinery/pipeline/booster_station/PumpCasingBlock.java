package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.drmangotea.tfmg.registry.TFMGFluids;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The body of a Booster Station: a squat pump casing that carries fluid straight through and takes rotation on
 * either of its two side faces.
 * <p>
 * The casing is an ordinary Create kinetic block. Its shaft runs across the row - the axis perpendicular to
 * {@link #HORIZONTAL_FACING} - so anything that drives a shaft drives it: a shaft off an engine, an electric
 * motor, a cogwheel, a windmill. Both side faces are ends of the <i>same</i> shaft, so two sources on one
 * casing have to turn the same way, exactly as they would on any other Create shaft.
 * <p>
 * Assembly is one step: lay the row out and right-click any casing with a block of steel, which closes every
 * casing and starts the station. The impeller is not a separate step - it is a crafting ingredient of the
 * casing, so a casing already has one inside. {@link #HORIZONTAL_FACING} is the direction fluid travels, and
 * is what tells a symmetrical row which end is the inlet.
 * <p>
 * A casing that has been built into a station is a {@link PipelinePumpBlock} instead, which is how it comes to
 * be called a Pipeline Pump; the two share this class and their block entity.
 */
public class PumpCasingBlock extends HorizontalKineticBlock implements IBE<PumpCasingBlockEntity> {

    public static final MapCodec<PumpCasingBlock> CODEC = simpleCodec(PumpCasingBlock::new);

    public PumpCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends PumpCasingBlock> codec() {
        return CODEC;
    }

    /** Whether this state is a casing that has been built and closed, rather than a loose one. */
    public static boolean isClosed(BlockState state) {
        return state.getBlock() instanceof PipelinePumpBlock;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Fluid runs the way the player is looking, so a row is laid down by walking along it
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    /**
     * The shaft runs across the row, so it meets the two faces a pipe never uses. Those are the faces the
     * casing's coupling bosses stick out of, which is where a drive is meant to be bolted on.
     */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    /**
     * A loose casing turns like any other directional machine. A casing that is part of a station reverses the
     * whole row instead, the way a wrench reverses a Create mechanical pump: one part cannot face a different
     * way from the rest, but the station as a whole can be turned round.
     */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof PumpCasingBlockEntity casing) || !casing.isAssembled())
            return super.onWrenched(state, context);

        PumpCasingBlockEntity station = casing.getStation();
        if (station == null)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (BoosterStationAssembly.reverse(level, station))
            IWrenchable.playRotateSound(level, pos);
        else
            BoosterStationAssembly.deny(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PumpCasingBlockEntity casing))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.is(CommonMetal.STEEL.storageBlocks.items()))
            return assemble(stack, state, level, pos, player, casing);

        if (stack.is(TFMGFluids.LUBRICATION_OIL.getBucket().get()))
            return lubricate(level, pos, player, hand, casing);

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * A bucket of Lubrication Oil poured into a built station, the way the large transformer takes its oil.
     * There is no tank and nothing to pipe in: the station is oiled from then on, and its casings demand less
     * stress for good.
     */
    private ItemInteractionResult lubricate(Level level, BlockPos pos, Player player, InteractionHand hand,
                                            PumpCasingBlockEntity casing) {
        PumpCasingBlockEntity station = casing.getStation();
        if (station == null || station.isLubricated())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        if (!station.lubricate())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.isCreative())
            player.setItemInHand(hand, Items.BUCKET.getDefaultInstance());
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
        return ItemInteractionResult.SUCCESS;
    }

    /** One click builds the station: the row is scanned, closed with the steel and started. */
    private ItemInteractionResult assemble(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, PumpCasingBlockEntity casing) {
        if (casing.isAssembled())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        BoosterStationAssembly.Row row = BoosterStationAssembly.scan(level, pos, state.getValue(HORIZONTAL_FACING));
        if (row == null) {
            BoosterStationAssembly.deny(level, pos);
            return ItemInteractionResult.CONSUME;
        }

        BoosterStationAssembly.assemble(level, row);
        BoosterStationAssembly.consume(player, stack);
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Building a row and releasing it swap the loose casing for the pump and back. That is one part changing
     * state, not a part being taken out of the row, so it must not set the whole station off disassembling
     * itself - which is what makes the two blocks safe to swap in place.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!(newState.getBlock() instanceof PumpCasingBlock)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BoosterStationPartBlockEntity part)
                part.onPartRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<PumpCasingBlockEntity> getBlockEntityClass() {
        return PumpCasingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PumpCasingBlockEntity> getBlockEntityType() {
        return TFMGBlockEntities.PUMP_CASING.get();
    }
}
