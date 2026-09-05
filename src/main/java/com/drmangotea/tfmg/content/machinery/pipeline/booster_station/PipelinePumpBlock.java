package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * A pump casing that has been built into a station: closed, running, and called a Pipeline Pump.
 * <p>
 * It is a block of its own rather than a state of {@link PumpCasingBlock} so that the name changes with it.
 * Goggles, Jade, WTHIT, the debug screen and anything else that names a block all read the block's own
 * translation key, and there is no override on a single block that satisfies all of them - the coil that
 * becomes a Large Transformer is the same trick, and the same reason.
 * <p>
 * Everything else is inherited: the same block entity type, the same kinetics, the same wrench and the same
 * right-click behaviour. Assembly swaps one block for the other and back, which
 * {@link PumpCasingBlock#onRemove} knows not to read as a part being taken out of the row.
 * <p>
 * It has no item. It is placed only by assembly, drops a Pump Casing when broken, and hands a Pump Casing to
 * a creative player who picks it.
 */
public class PipelinePumpBlock extends PumpCasingBlock implements SpecialBlockItemRequirement {

    public static final MapCodec<PipelinePumpBlock> CODEC = simpleCodec(PipelinePumpBlock::new);

    public PipelinePumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PipelinePumpBlock> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
                                       Player player) {
        return TFMGBlocks.PUMP_CASING.asStack();
    }

    /**
     * Without this a schematicannon could not print a station: it refuses anything whose requirement is
     * invalid, and this block has no item of its own. It costs a Pump Casing, the same way an encased shaft
     * costs a shaft. The Block of Steel that assembly consumes is not charged - it goes once per row, not
     * once per pump, and the printer only ever sees one block at a time.
     */
    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        return ItemRequirement.of(TFMGBlocks.PUMP_CASING.getDefaultState(), be);
    }
}
