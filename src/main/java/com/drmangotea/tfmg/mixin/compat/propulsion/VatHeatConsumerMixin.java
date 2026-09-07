package com.drmangotea.tfmg.mixin.compat.propulsion;

import com.drmangotea.tfmg.compat.propulsion.PropulsionHeatCompat;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.drmangotea.tfmg.recipes.VatMachineRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import dev.propulsionteam.propulsionsimulated.content.heat.IHeatConsumer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the chemical vat a heat consumer for Create Propulsion: Simulated's burners.
 *
 * <p>The vat already reads those burners correctly - they register into Create's
 * {@code BoilerHeater} registry, which is what {@code VatBlockEntity.updateTemperature} asks. What
 * stops them is on the burner's side: {@code AbstractBurnerBlockEntity.shouldThermostatBurn} only
 * lights for a redstone signal, a Create fluid tank, a Create basin, or a block entity implementing
 * this interface. Implementing it here is the only way to satisfy that gate from outside their mod.
 *
 * <p>This lives in a compat package and is applied only when {@code createpropulsion} is installed
 * and still declares the contract this was compiled against - see
 * {@code com.drmangotea.tfmg.mixin.TFMGMixinPlugin}. {@link VatBlockEntity} itself never names a
 * type from that mod, so a pack without it is unaffected.
 *
 * <p>The threshold and the draw are the shared ones in {@link PropulsionHeatCompat}, which holds the
 * arithmetic behind them. Their heater entry tops out at 2 per block, so a single burner cannot drive
 * a heat level 3 recipe: Claus sulfur recovery still wants the firebox, which TFMG registers at 3
 * when seething.
 */
@Mixin(VatBlockEntity.class)
public class VatHeatConsumerMixin implements IHeatConsumer {

    /**
     * True while the vat has a recipe waiting on heat, which is the only time the burner should
     * spend fuel. Asked of whichever vat block sits directly on the burner, so it delegates to the
     * controller: for a wider vat the burner under a corner block is still under the footprint that
     * {@code updateTemperature} sums, and only the controller keeps the matched recipe.
     */
    @Override
    public boolean isActive() {
        VatBlockEntity controller = ((VatBlockEntity) (Object) this).getControllerBE();
        if (controller == null)
            return false;
        VatMachineRecipe recipe = controller.recipe;
        return recipe != null
                && (recipe.heatLevel > 0 || recipe.getRequiredHeat() != HeatCondition.NONE);
    }

    /** {@link PropulsionHeatCompat#OPERATING_THRESHOLD}, which keeps the burner seething. */
    @Override
    public float getOperatingThreshold() {
        return PropulsionHeatCompat.OPERATING_THRESHOLD;
    }

    /**
     * Half the burner's rated output per tick while the vat is working, so a working vat costs fuel
     * but one burner can still keep up. See {@link PropulsionHeatCompat#consume(float, float)}.
     */
    @Override
    public float consumeHeat(float maxAvailable, float expectedHeatOutput, boolean simulate) {
        if (!isActive())
            return 0;
        return PropulsionHeatCompat.consume(maxAvailable, expectedHeatOutput);
    }
}
