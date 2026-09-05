package com.drmangotea.tfmg.content.machinery.pipeline;

import com.drmangotea.tfmg.config.PipelineConfig;
import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.registry.TFMGSoundEvents;
import com.drmangotea.tfmg.registry.TFMGTriggers;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What happens to a pipe carrying more station pressure than it is rated for.
 * <p>
 * The excess is {@code pressure / rating - 1}. At or below zero the pipe is fine. At one - double the rating -
 * it goes the moment it is pressurised, which is what makes a plastic pipe under steel pressure pop on
 * placement. In between it is a per-tick roll whose mean time to failure is
 * {@code reference seconds x (reference excess / excess) ^ exponent}, floored, so a line 2% over holds for
 * minutes and might survive while a line 25% over is on borrowed time.
 * <p>
 * Only pressure a Booster Station put on the line counts. A Create pump pushing 256 into a plastic pipe is
 * still just a pump.
 */
public class PipeBurst {

    public static final ParticleOptions STRAIN_PARTICLE = ParticleTypes.SMOKE;
    public static final ParticleOptions VENT_PARTICLE = ParticleTypes.CLOUD;

    /** How far from a burst on a looped line the advancement is handed out. */
    private static final double LOOP_ADVANCEMENT_RADIUS = 32;

    private static final int STRAIN_PARTICLE_INTERVAL = 10;
    /** A straining pipe is heard at most this often. A long run of them would otherwise be a wall of noise. */
    private static final int STRAIN_SOUND_INTERVAL = 60;

    /** How far over its rating this pipe is; zero or less is safe. */
    public static float excess(float pressure, int rating) {
        if (rating <= 0)
            return 0;
        return pressure / rating - 1;
    }

    /**
     * Runs one tick of strain on an over-pressured pipe.
     *
     * @param looped whether the station driving this pipe is fed, however far round, by its own outlet
     * @return true if the pipe burst and should be dropped from the station's line
     */
    public static boolean tick(ServerLevel level, BlockPos pos, float pressure, float excess, boolean looped) {
        if (excess <= 0)
            return false;

        if (onInterval(level, pos, STRAIN_PARTICLE_INTERVAL))
            strainParticles(level, pos, Mth.clamp(excess, 0.05F, 1));
        if (onInterval(level, pos, STRAIN_SOUND_INTERVAL))
            strainSound(level, pos, excess);

        if (!TFMGConfigs.common().pipeline.pipesBurst.get())
            return false;
        if (!shouldBurst(level, excess))
            return false;

        burst(level, pos, looped);
        return true;
    }

    private static boolean shouldBurst(ServerLevel level, float excess) {
        if (excess >= 1)
            return true;
        PipelineConfig config = TFMGConfigs.common().pipeline;
        double meanSeconds = config.burstReferenceSeconds.getF()
                * Math.pow(config.burstReferenceExcess.getF() / excess, config.burstExponent.getF());
        meanSeconds = Math.max(config.burstMinimumSeconds.getF(), meanSeconds);
        return level.random.nextDouble() < 1.0 / (meanSeconds * 20.0);
    }

    /** Staggers the pipes on a line by position, so a whole straining run does not fire on the same tick. */
    private static boolean onInterval(ServerLevel level, BlockPos pos, int interval) {
        return level.getGameTime() % interval == Math.floorMod(pos.hashCode(), interval);
    }

    private static void strainParticles(ServerLevel level, BlockPos pos, float intensity) {
        level.sendParticles(STRAIN_PARTICLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1 + (int) (intensity * 3), 0.2, 0.2, 0.2, 0.01);
    }

    /**
     * The tick a pipe first goes over its rating. Waiting for the next beat of the throttle would leave a pipe
     * silently straining for up to three seconds before the player heard anything about it.
     */
    public static void beginStrain(ServerLevel level, BlockPos pos, float excess) {
        if (excess <= 0)
            return;
        strainParticles(level, pos, Mth.clamp(excess, 0.05F, 1));
        strainSound(level, pos, excess);
    }

    /** The strain clip on its own, pitched up a little with how far over the rating the pipe is. */
    private static void strainSound(ServerLevel level, BlockPos pos, float excess) {
        float intensity = Mth.clamp(excess, 0.05F, 1);
        TFMGSoundEvents.PIPELINE_PIPE_STRAIN.playOnServer(level, pos,
                TFMGConfigs.common().pipeline.pipeStrainVolume.getF(), 0.9F + 0.2F * intensity);
    }

    /**
     * Destroys the pipe the way Create handles a rotation clash - break particles, break sound and the item on
     * the floor - then lets whatever was in it out.
     */
    private static void burst(ServerLevel level, BlockPos pos, boolean looped) {
        FluidStack carried = carriedFluid(level, pos);
        level.destroyBlock(pos, true);
        burstSound(level, pos);
        spill(level, pos, carried);
        if (looped)
            awardLoopBurst(level, pos);
    }

    /**
     * Two clips at once: the burst, and the sharp explosion the first iteration used as a placeholder, kept
     * underneath it at its own volume and its own high pitch. The clip carries the pipe tearing; the boom
     * carries the size of it, and together they make a burst worth flinching at.
     */
    private static void burstSound(ServerLevel level, BlockPos pos) {
        PipelineConfig config = TFMGConfigs.common().pipeline;
        TFMGSoundEvents.PIPELINE_PIPE_BURST.playOnServer(level, pos, config.pipeBurstVolume.getF(), 1);

        float boom = config.pipeBurstBoomVolume.getF();
        if (boom > 0)
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, boom, 1.6F);
    }

    /**
     * A pipe that let go on a line whose pressure had come round to itself. Everyone near enough to have
     * watched it happen gets the advancement, not only whoever placed the pipe - the loop is usually several
     * people's work and nobody is holding anything at the moment it goes.
     */
    private static void awardLoopBurst(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double radiusSquared = LOOP_ADVANCEMENT_RADIUS * LOOP_ADVANCEMENT_RADIUS;
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(center) <= radiusSquared))
            TFMGTriggers.PIPELINE_LOOP_BURST.get().trigger(player);
    }

    /** Whatever the pipe was moving when it let go. */
    private static FluidStack carriedFluid(ServerLevel level, BlockPos pos) {
        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
        if (pipe == null)
            return FluidStack.EMPTY;
        for (Direction side : Iterate.directions) {
            PipeConnection.Flow flow = pipe.getFlow(side);
            if (flow != null && !flow.fluid.isEmpty())
                return flow.fluid;
        }
        return FluidStack.EMPTY;
    }

    /**
     * Placeable liquids land as a source block, the same way an open-ended pipe pours; anything with no block
     * form - the gases - vents instead.
     */
    private static void spill(ServerLevel level, BlockPos pos, FluidStack fluid) {
        if (fluid.isEmpty())
            return;

        Fluid type = fluid.getFluid();
        if (type instanceof FlowingFluid && FluidHelper.hasBlockState(type)) {
            BlockState state = level.getBlockState(pos);
            if (state.canBeReplaced()) {
                level.setBlock(pos, type.defaultFluidState().createLegacyBlock(), 3);
                return;
            }
        }

        level.sendParticles(VENT_PARTICLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.25, 0.25,
                0.25, 0.05);
    }
}
