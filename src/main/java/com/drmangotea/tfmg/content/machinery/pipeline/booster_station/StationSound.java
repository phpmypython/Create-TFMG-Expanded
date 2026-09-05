package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.config.PipelineConfig;
import com.drmangotea.tfmg.registry.TFMGSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * The two loops a Booster Station plays: the machine turning, and the fluid it is moving.
 * <p>
 * They are independent instances rather than one clip, because they answer different questions. A station
 * with rotation and nothing to pump is still running, and should be heard doing it; the water only joins in
 * once something is actually flowing. Layering them also means the fluid can rise and fall with the pressure
 * while the motor rises and falls with the RPM, which one clip could never do.
 * <p>
 * Client only, and reached from {@link PumpCasingBlockEntity#tick()} inside the {@code isClientSide} branch,
 * which is how Create wires its own {@code SoundScapes}: a dedicated server never takes that branch, so this
 * class is never loaded there.
 * <p>
 * Looping {@link AbstractTickableSoundInstance}s rather than sounds replayed on a timer, so a layer stops the
 * moment its condition does instead of running to the end of the clip. Which controllers are already looping
 * is kept here rather than on the block entity, so the block entity holds no client-only state and a loop the
 * sound engine dropped for its own reasons is noticed and started again.
 * <p>
 * Pitch and volume are rewritten every tick. The sound engine reads both off a ticking instance each tick, so
 * a loop slides with the station rather than being restarted.
 */
public class StationSound extends AbstractTickableSoundInstance {

    /** One of the two layers, and everything that differs between them. */
    public enum Layer {

        /** The machine turning. Runs on rotation alone - a station with nothing to pump still hums. */
        MOTOR(TFMGSoundEvents.PIPELINE_STATION_MOTOR) {
            @Override
            boolean playing(PumpCasingBlockEntity station) {
                return station.hasRotation();
            }

            @Override
            float volume(PumpCasingBlockEntity station) {
                return Mth.lerp(station.getSpeedLoad(), config().stationMotorVolumeMin.getF(),
                        config().stationMotorVolumeMax.getF());
            }

            @Override
            float pitch(PumpCasingBlockEntity station) {
                return Mth.lerp(station.getSpeedLoad(), config().stationSoundPitchMin.getF(),
                        config().stationSoundPitchMax.getF());
            }

            @Override
            boolean silenced() {
                return silent(config().stationMotorVolumeMin.getF(), config().stationMotorVolumeMax.getF());
            }
        },

        /** What the station is moving. Only while fluid actually flows, and never without the motor under it. */
        FLUID(TFMGSoundEvents.PIPELINE_STATION_FLUID) {
            @Override
            boolean playing(PumpCasingBlockEntity station) {
                // Rotation as well as flow: the fluid layer sits on top of the motor and is never heard alone
                return station.hasRotation() && station.isPumping();
            }

            @Override
            float volume(PumpCasingBlockEntity station) {
                return Mth.lerp(station.getPressureLoad(), config().stationFluidVolumeMin.getF(),
                        config().stationFluidVolumeMax.getF());
            }

            @Override
            float pitch(PumpCasingBlockEntity station) {
                // Water does not change note with the pressure the way the motor changes note with the RPM
                return 1;
            }

            @Override
            boolean silenced() {
                return silent(config().stationFluidVolumeMin.getF(), config().stationFluidVolumeMax.getF());
            }
        };

        private final TFMGSoundEvents.SoundEntry entry;
        private final Map<BlockPos, StationSound> active = new HashMap<>();

        Layer(TFMGSoundEvents.SoundEntry entry) {
            this.entry = entry;
        }

        abstract boolean playing(PumpCasingBlockEntity station);

        abstract float volume(PumpCasingBlockEntity station);

        abstract float pitch(PumpCasingBlockEntity station);

        /** Whether the config has turned this layer off entirely, in which case it is never started. */
        abstract boolean silenced();

        static PipelineConfig config() {
            return TFMGConfigs.common().pipeline;
        }

        static boolean silent(float min, float max) {
            return Math.max(min, max) <= 0;
        }

        /** Starts this layer for the given controller if it should be running and is not already. */
        private void tick(PumpCasingBlockEntity station) {
            BlockPos pos = station.getBlockPos();
            StationSound current = active.get(pos);
            // A different block entity at the same position means the chunk was reloaded; the old instance
            // stops itself on its next tick, and holding on to it would keep the new station silent
            if (current != null && (current.isStopped() || current.station != station)) {
                active.remove(pos, current);
                current = null;
            }
            if (current != null || !playing(station) || silenced())
                return;
            StationSound sound = new StationSound(this, station);
            active.put(pos, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    private final Layer layer;
    private final PumpCasingBlockEntity station;
    private final BlockPos pos;

    private StationSound(Layer layer, PumpCasingBlockEntity station) {
        super(layer.entry.getMainEvent(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.layer = layer;
        this.station = station;
        this.pos = station.getBlockPos();
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        looping = true;
        delay = 0;
        follow();
    }

    /**
     * Starts whichever layers should be running. Both conditions are server-authoritative and sent as soon as
     * they change - rotation with the casing speeds, flow with the pumping flag - so a layer starts on the
     * tick the station earns it rather than at the next periodic sync.
     */
    public static void tick(PumpCasingBlockEntity station) {
        for (Layer layer : Layer.values())
            layer.tick(station);
    }

    @Override
    public void tick() {
        if (!station.isRemoved() && layer.playing(station)) {
            follow();
            return;
        }
        // stop() is final, so the bookkeeping has to happen here rather than in an override
        layer.active.remove(pos, this);
        stop();
    }

    private void follow() {
        volume = layer.volume(station);
        pitch = layer.pitch(station);
    }
}
