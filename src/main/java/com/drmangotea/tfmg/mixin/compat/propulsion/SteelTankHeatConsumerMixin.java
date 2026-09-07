package com.drmangotea.tfmg.mixin.compat.propulsion;

import com.drmangotea.tfmg.compat.propulsion.PropulsionHeatCompat;
import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.controller.DistillationControllerBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.heat.IHeatConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the distillation tower's steel tank a heat consumer for Create Propulsion: Simulated's
 * burners, the same way {@link VatHeatConsumerMixin} does for the chemical vat.
 *
 * <p>The tower reads those burners correctly already - {@code SteelTankBlockEntity.updateTemperature}
 * sums Create's {@code BoilerHeater} over the footprint, and their burners register into it. What
 * stops them is {@code AbstractBurnerBlockEntity.shouldThermostatBurn}, which lights only for a
 * redstone signal, a Create {@code FluidTankBlock}, a Create basin, or a block entity implementing
 * this interface. {@link com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlock} extends
 * the plain {@code Block}, so the tank misses that third case and the burner stays cold.
 *
 * <p>Applied only when {@code createpropulsion} is installed and still declares the contract this was
 * compiled against - see {@code com.drmangotea.tfmg.mixin.TFMGMixinPlugin}. Neither
 * {@link SteelTankBlockEntity} nor {@link DistillationControllerBlockEntity} names a type from that
 * mod.
 *
 * <h2>Why the tower cannot use the vat's rule</h2>
 *
 * The vat mixin waits for a matched recipe on the controller, because the vat matches its recipe
 * whether or not it is hot. The distillation controller does not: {@code manageRecipe} returns early
 * on {@code activeHeat == 0}, before it ever calls {@code findRecipe}, so {@code controller.recipe}
 * stays null until the tower is already being heated. A rule that waited for it would never light the
 * burner that is supposed to produce that heat. So this asks the question one step earlier - would
 * this tower run if it were heated - which is
 * {@link DistillationControllerBlockEntity#hasWorkForHeat()}: outputs, a tank tall and wide enough to
 * hold them, and a matching recipe. That is everything {@code manageRecipe} tests apart from the heat
 * itself, so a tower that could never run - too short for its outputs, say - never lights a burner to
 * find out.
 *
 * <h2>The numbers</h2>
 *
 * Threshold and draw are the shared ones in {@link PropulsionHeatCompat}, which holds the arithmetic
 * behind them: 0.95 of the burner's buffer keeps it above {@code HeatMapper}'s SEETHING line, and the
 * draw is half the burner's rated output.
 *
 * <p>Unlike the vat, the tower has no heat level to clear - it distils at
 * {@code speedModifier = activeHeat / 2}, so any heat at all works and more of it is faster. A
 * seething burner scores 2, the same as a firebox below seething, so it drives the tower at the speed
 * that firebox would; a seething firebox scores 3 and is still half again as fast.
 */
@Mixin(SteelTankBlockEntity.class)
public class SteelTankHeatConsumerMixin implements IHeatConsumer {

    /**
     * True while this tower would distil if it had heat. Asked of whichever tank block sits directly
     * on the burner, so it resolves the tank multiblock's controller first: the burner under any
     * bottom-layer block is under the footprint {@code updateTemperature} sums, and only that
     * controller knows the tower is a tower and where its distillation controller is.
     */
    @Override
    public boolean isActive() {
        SteelTankBlockEntity tank = (SteelTankBlockEntity) (Object) this;
        SteelTankBlockEntity controller = tank.getControllerBE();
        if (controller == null || !controller.isDistillationTower)
            return false;
        BlockPos distillationPos = controller.distillationControllerPos;
        if (distillationPos == null)
            return false;
        BlockEntity be = tank.getLevel().getBlockEntity(distillationPos);
        return be instanceof DistillationControllerBlockEntity distillation
                && distillation.hasWorkForHeat();
    }

    /** {@link PropulsionHeatCompat#OPERATING_THRESHOLD}, which keeps the burner seething. */
    @Override
    public float getOperatingThreshold() {
        return PropulsionHeatCompat.OPERATING_THRESHOLD;
    }

    /**
     * Half the burner's rated output per tick while the tower has work, so the tower costs fuel only
     * while it is distilling. See {@link PropulsionHeatCompat#consume(float, float)}.
     */
    @Override
    public float consumeHeat(float maxAvailable, float expectedHeatOutput, boolean simulate) {
        if (!isActive())
            return 0;
        return PropulsionHeatCompat.consume(maxAvailable, expectedHeatOutput);
    }
}
