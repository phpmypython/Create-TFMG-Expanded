package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.registry.TFMGPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns the casing's shaft, the way every other shaft-driven machine does.
 * <p>
 * Everything static about the casing stays in the block model; only the parts that turn are in a partial - the
 * impeller and its shaft while the casing is open, the two coupling bosses once it is closed. This is the
 * no-Flywheel path; {@link PumpCasingVisual} is the instanced one, and the two have to agree.
 */
public class PumpCasingRenderer extends KineticBlockEntityRenderer<PumpCasingBlockEntity> {

    public PumpCasingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PumpCasingBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(shaftModel(state), state, shaftFacing(state));
    }

    /**
     * Which way the shaft points in the world. The models are drawn with the row running west to east, so the
     * shaft lies along the model's south face, and the blockstate's own rotation lands that on the direction
     * clockwise of the flow - the same axis {@link PumpCasingBlock#getRotationAxis} spins about.
     */
    public static Direction shaftFacing(BlockState state) {
        return state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getClockWise();
    }

    /** An open casing shows the impeller on the shaft; a closed one shows only the coupling bosses. */
    public static PartialModel shaftModel(BlockState state) {
        return PumpCasingBlock.isClosed(state) ? TFMGPartialModels.PUMP_CASING_SHAFT
                : TFMGPartialModels.PUMP_CASING_IMPELLER;
    }
}
