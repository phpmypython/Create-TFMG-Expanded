package com.drmangotea.tfmg.compat.propulsion;

/**
 * The heat contract this mod answers Create Propulsion: Simulated's burners with, in one place so the
 * machines that take their heat cannot drift apart.
 *
 * <p>Used by the mixins under {@code com.drmangotea.tfmg.mixin.compat.propulsion}, which are the only
 * classes in this mod that name a type from theirs. This class names none, so a pack without the
 * burner mod can load it happily. It sits outside the mixin package on purpose: Mixin refuses to
 * class load anything under a declared mixin package that is not itself an applied mixin
 * ({@code MixinProcessor.applyMixins} throws {@code IllegalClassLoadError} on a package match), and
 * these members are called from methods merged into the block entities the mixins target.
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
 * Every machine here reads heat back through Create's {@code BoilerHeater} registry, which their
 * burners register into, and wants the 2 that only SEETHING scores: the vat needs it for heat level 2
 * ({@code HeatCondition.HEATED}), and the distillation tower's throughput is proportional to what it
 * reads. So the burner has to hold <em>strictly</em> above 0.9 of its capacity - HeatMapper's test is
 * {@code > 0.9f}, not {@code >=}.
 */
public final class PropulsionHeatCompat {

    private PropulsionHeatCompat() {}

    /**
     * Fraction of the burner's buffer to hold while the machine above it is working.
     *
     * <p>Their thermostat refuels only while
     * {@code stored - consumeHeat(stored, rated, true) - 0.05 < capacity * getOperatingThreshold()},
     * so the buffer settles just above {@code capacity * threshold}. 0.95 puts that floor at 570 of
     * 600 HU on the liquid burner and 380 of 400 on the solid one: 0.05 of capacity clear of the
     * SEETHING line, so the one tick sag at the end of a burn cycle never drops the block state to
     * KINDLED, which the machine above would read as heat 1. 0.9 would sit exactly on the boundary
     * and flicker.
     */
    public static final float OPERATING_THRESHOLD = 0.95F;

    /**
     * How much heat to draw from a burner offering {@code maxAvailable} and rated at
     * {@code expectedHeatOutput} per tick: half its rated output, so 1.0 HU/tick on the liquid burner
     * and 0.5 on the solid one.
     *
     * <p>Generation while burning is the full rated output, so one machine holds either burner at
     * roughly a 50% duty cycle - the liquid burner oscillates between about 569 and 589 HU (0.948 to
     * 0.982 of capacity, SEETHING throughout) and eats about 1 mB of fuel a second at TFMG fuel
     * multipliers. Taking half rather than all of it leaves room for the 0.05 HU/tick passive loss and
     * for fuels whose thrust multiplier is below 1.
     *
     * <p>Callers answer 0 while they are inactive, and none of them keeps a heat store of its own -
     * they re-read the burner's block state through Create's {@code BoilerHeater} on their own lazy
     * tick - so this is pure and a simulated draw is the executed one by construction.
     */
    public static float consume(float maxAvailable, float expectedHeatOutput) {
        return Math.min(expectedHeatOutput * 0.5F, maxAvailable);
    }
}
