package com.drmangotea.tfmg.worldgen.deposits;


import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.drmangotea.tfmg.registry.TFMGFluids;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public class OilDepositFeature extends Feature<NoneFeatureConfiguration> {

    /** Blocks of crude oil standing above a deposit, inclusive. */
    private static final int MINIMUM_COLUMN_HEIGHT = 10;
    private static final int MAXIMUM_COLUMN_HEIGHT = 25;

    public OilDepositFeature(Codec<NoneFeatureConfiguration> p_65786_) {
        super(p_65786_);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        BlockPos startingPos = context.origin();
        WorldGenLevel level = context.level();
        BlockPos pos = startingPos;
        RandomSource randomsource = context.random();

        if (randomsource.nextInt(20) != 0)
            return false;

        for (int i = 0; i < randomsource.nextInt(6) + 1; i++) {
            placeDeposit(pos, level, randomsource);
            pos = pos.north(randomsource.nextInt(40) - 20);
            pos = pos.west(randomsource.nextInt(40) - 20);
        }

        return true;
    }

    public void placeDeposit(BlockPos startingPos, WorldGenLevel level, RandomSource randomsource) {
        // A position the region refuses is outside the area this feature may write to. Bailing out
        // here leaves nothing behind; carrying on would build a column with no deposit under it.
        if (!setBlock(level, startingPos, TFMGBlocks.OIL_DEPOSIT.getDefaultState()))
            return;

        BlockPos pos = startingPos;
        // Rolled once. Re-rolling the bound on every iteration, as this did, ends the loop as soon as
        // the roll comes in at or below the current index, so most columns came out only a few blocks
        // tall regardless of the 25 written here.
        int columnHeight = MINIMUM_COLUMN_HEIGHT
                + randomsource.nextInt(MAXIMUM_COLUMN_HEIGHT - MINIMUM_COLUMN_HEIGHT + 1);

        for (int i = 0; i < columnHeight; i++) {
            pos = pos.above();

            setBlock(level, pos, crudeOil());


            Direction direction1 = Direction.getRandom(randomsource);
            if (direction1.getAxis().isHorizontal())
                setBlock(level, pos.relative(direction1), crudeOil());

            if (i < 4) {
                Direction direction2 = Direction.getRandom(randomsource);
                if (direction2.getAxis().isHorizontal())
                    setBlock(level, pos.relative(direction2), TFMGBlocks.FOSSILSTONE.getDefaultState());
            }

        }

        clearBedrockAbove(level, startingPos);
    }

    /**
     * A deposit is placed on the world floor, inside the bedrock band, and the oil column above it is
     * the only thing that carves through that band. Whatever the column's height, anything still
     * bedrock between the deposit and open ground seals the deposit off for good: a pumpjack finds a
     * deposit only by following an unbroken run of Industrial Pipe straight down to it, and no pipe can
     * be placed through bedrock. Replace what is left in the way with the oil that should have been
     * there.
     */
    private static void clearBedrockAbove(WorldGenLevel level, BlockPos depositPos) {
        int topOfBedrock = level.getMinBuildHeight() + 4;

        for (BlockPos pos = depositPos.above(); pos.getY() <= topOfBedrock; pos = pos.above())
            if (level.getBlockState(pos).is(Blocks.BEDROCK))
                setBlock(level, pos, crudeOil());
    }

    private static BlockState crudeOil() {
        return TFMGFluids.CRUDE_OIL.get().getSource().defaultFluidState().createLegacyBlock();
    }

    /** Returns whether the region accepted the write; it refuses anything outside its writable area. */
    public static boolean setBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, 2);
    }
}
