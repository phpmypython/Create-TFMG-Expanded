package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Points both station blocks at the hand-written models under {@code block/booster_station}.
 * <p>
 * The geometry is drawn once, running west to east with a stub's flange at the west face, and the blockstate
 * turns it - so a casing facing east and a stub facing west both come out unrotated.
 */
public class BoosterStationGenerator extends SpecialBlockStateGen {

    private static final Direction CASING_MODEL_FACING = Direction.EAST;
    private static final Direction STUB_MODEL_FACING = Direction.WEST;

    @Override
    protected int getXRotation(BlockState state) {
        return 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction modelFacing = isCasing(state) ? CASING_MODEL_FACING : STUB_MODEL_FACING;
        return (int) (facing.toYRot() - modelFacing.toYRot());
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
                                                BlockState state) {
        return prov.models().getExistingFile(prov.modLoc("block/booster_station/" + modelName(state)));
    }

    /**
     * The model an item of this block should show. A casing is always advertised closed, and gets its own
     * model rather than the block's: the block's shaft lives in a partial so it can turn, and an item has no
     * block entity to turn it.
     */
    public static String itemModelName(boolean casing) {
        return casing ? "pump_casing_item" : "station_stub";
    }

    private static String modelName(BlockState state) {
        if (!isCasing(state))
            return "station_stub";
        return PumpCasingBlock.isClosed(state) ? "pump_casing" : "pump_casing_unfinished";
    }

    private static boolean isCasing(BlockState state) {
        return state.getBlock() instanceof PumpCasingBlock;
    }
}
