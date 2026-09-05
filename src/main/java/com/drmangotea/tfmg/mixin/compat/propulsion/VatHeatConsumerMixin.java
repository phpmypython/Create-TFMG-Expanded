package com.drmangotea.tfmg.mixin.compat.propulsion;

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
 * <h2>Where the numbers come from</h2>
 *
 * Read out of createpropulsion 1.1.5 (MIT), {@code AbstractBurnerBlockEntity},
 * {@code LiquidBurnerBlockEntity}, {@code SolidBurnerBlockEntity}, {@code HeatMapper} and
 * {@code events.ModSetupEvents}:
 *
 * <pre>
 *   liquid burner   capacity 600 HU, rated output 2 HU/tick
 *   solid burner    capacity 400 HU, rated output 1 HU/tick
 *   passive loss    0.05 HU/tick, while the buffer holds heat and nothing is drawing
 *   HeatMapper      stored/capacity &gt; 0.9 SEETHING, &gt; 0.6 KINDLED, &gt; 0.3 FADING,
 *                   &gt; 0.01 SMOULDERING, else NONE
 *   their heater    SEETHING 2, FADING or KINDLED 1, SMOULDERING 0, NONE not a heater
 * </pre>
 *
 * The vat needs 2 from the block below to reach heat level 2 ({@link HeatCondition#HEATED}), so the
 * burner has to sit at SEETHING, i.e. <em>strictly</em> above 0.9 of its capacity - HeatMapper's
 * test is {@code > 0.9f}, not {@code >=}.
 *
 * <p>Their thermostat refuels only while
 * {@code stored - consumeHeat(stored, rated, true) - 0.05 < capacity * getOperatingThreshold()},
 * so the buffer settles just above {@code capacity * threshold}. A threshold of 0.95 puts that floor
 * at 570 of 600 HU on the liquid burner and 380 of 400 on the solid one: 0.05 of capacity clear of
 * the SEETHING line, so the one tick sag at the end of a burn cycle never drops the block state to
 * KINDLED, which the vat would read as heat 1 and stall a heated recipe on. A threshold of 0.9 would
 * sit exactly on the boundary and flicker.
 *
 * <p>{@code consumeHeat} draws half the burner's own rated output - the {@code expectedHeatOutput}
 * argument is that rated figure, 2 HU/tick liquid and 1 HU/tick solid - so 1.0 and 0.5 HU/tick.
 * Generation while burning is the full rated output, so one vat holds either burner at roughly a
 * 50% duty cycle: the liquid burner oscillates between about 569 and 589 HU (0.948 to 0.982 of
 * capacity, SEETHING throughout) and eats about 1 mB of fuel a second at TFMG fuel multipliers.
 * Taking half rather than all of it also leaves room for the 0.05 HU/tick passive loss and for fuels
 * whose thrust multiplier is below 1.
 *
 * <p>Their heater entry tops out at 2 per block, so a single burner cannot drive a heat level 3
 * recipe: Claus sulfur recovery still wants the firebox, which TFMG registers at 3 when seething.
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

    /**
     * Fraction of the burner's buffer to hold while working. 0.95 keeps it above HeatMapper's
     * SEETHING line (0.9) with margin, which is what the vat reads as heat level 2.
     */
    @Override
    public float getOperatingThreshold() {
        return 0.95F;
    }

    /**
     * Half the burner's rated output per tick, so a working vat costs fuel but one burner can still
     * keep up. The vat keeps no heat store of its own - it re-reads the burner's block state through
     * Create's {@code BoilerHeater} on its own lazy tick - so there is nothing to mutate here and the
     * simulated answer is the executed one by construction.
     */
    @Override
    public float consumeHeat(float maxAvailable, float expectedHeatOutput, boolean simulate) {
        if (!isActive())
            return 0;
        return Math.min(expectedHeatOutput * 0.5F, maxAvailable);
    }
}
