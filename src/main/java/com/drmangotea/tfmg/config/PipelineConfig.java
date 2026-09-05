package com.drmangotea.tfmg.config;

import net.createmod.catnip.config.ConfigBase;

/**
 * Everything that governs pipeline pressure - the pressure a Booster Station puts on a line, how far it
 * carries, what the pipes are rated for and when they burst.
 * <p>
 * None of this touches Create's own pumps: they keep their 16-block, no-rules behaviour, and only pressure
 * that a station put on a line is measured against a rating.
 */
public class PipelineConfig extends ConfigBase {

    public final ConfigGroup boosterStation = group(1, "booster_station", "Booster Station");
    public final ConfigInt boosterStationRange =
            i(128, 1, "boosterStationRange", Comments.boosterStationRange);
    public final ConfigInt boosterStationInputRpmCap =
            i(256, 1, "boosterStationInputRpmCap", Comments.boosterStationInputRpmCap);
    public final ConfigInt boosterStationMaxPressure =
            i(8192, 1, "boosterStationMaxPressure", Comments.boosterStationMaxPressure);
    public final ConfigFloat boosterStationStressPerRpm =
            f(8, 0, "boosterStationStressPerRpm", Comments.boosterStationStressPerRpm);

    public final ConfigGroup lubrication = group(1, "lubrication", "Lubrication Oil");
    public final ConfigFloat boosterStationOilStressDiscount =
            f(25, 0, "boosterStationOilStressDiscount", Comments.boosterStationOilStressDiscount);

    public final ConfigGroup pipes = group(1, "pipes", "Pipe Ratings");
    public final ConfigInt defaultPipeRating =
            i(384, 1, "defaultPipeRating", Comments.defaultPipeRating);
    public final ConfigFloat defaultPipeDecay =
            f(6, 0, "defaultPipeDecay", Comments.defaultPipeDecay);

    public final ConfigGroup bursting = group(1, "bursting", "Bursting");
    public final ConfigBool pipesBurst = b(true, "pipesBurst", Comments.pipesBurst);
    public final ConfigFloat burstReferenceExcess =
            f(0.25f, 0.001f, "burstReferenceExcess", Comments.burstReferenceExcess);
    public final ConfigFloat burstReferenceSeconds =
            f(5, 0.05f, "burstReferenceSeconds", Comments.burstReferenceSeconds);
    public final ConfigFloat burstExponent =
            f(1.5f, 0, "burstExponent", Comments.burstExponent);
    public final ConfigFloat burstMinimumSeconds =
            f(3, 0.05f, "burstMinimumSeconds", Comments.burstMinimumSeconds);

    public final ConfigGroup sounds = group(1, "sounds", "Sounds");
    public final ConfigFloat stationMotorVolumeMin =
            f(0.25f, 0, 1, "stationMotorVolumeMin", Comments.stationMotorVolumeMin);
    public final ConfigFloat stationMotorVolumeMax =
            f(0.45f, 0, 1, "stationMotorVolumeMax", Comments.stationMotorVolumeMax);
    public final ConfigFloat stationFluidVolumeMin =
            f(0.25f, 0, 1, "stationFluidVolumeMin", Comments.stationFluidVolumeMin);
    public final ConfigFloat stationFluidVolumeMax =
            f(0.45f, 0, 1, "stationFluidVolumeMax", Comments.stationFluidVolumeMax);
    public final ConfigFloat stationSoundPitchMin =
            f(0.7f, 0.1f, 2, "stationSoundPitchMin", Comments.stationSoundPitchMin);
    public final ConfigFloat stationSoundPitchMax =
            f(1.3f, 0.1f, 2, "stationSoundPitchMax", Comments.stationSoundPitchMax);
    public final ConfigFloat stationSoundFullSpeed =
            f(256, 1, "stationSoundFullSpeed", Comments.stationSoundFullSpeed);
    public final ConfigFloat stationSoundFullPressure =
            f(512, 1, "stationSoundFullPressure", Comments.stationSoundFullPressure);
    public final ConfigFloat pipeStrainVolume =
            f(0.4f, 0, 1, "pipeStrainVolume", Comments.pipeStrainVolume);
    public final ConfigFloat pipeBurstVolume =
            f(0.8f, 0, 1, "pipeBurstVolume", Comments.pipeBurstVolume);
    public final ConfigFloat pipeBurstBoomVolume =
            f(0.6f, 0, 1, "pipeBurstBoomVolume", Comments.pipeBurstBoomVolume);

    @Override
    public String getName() {
        return "pipeline";
    }

    private static class Comments {
        static String boosterStationRange =
                "How many blocks of pipe a Booster Station's pressure reaches. Independent of Create's "
                        + "mechanicalPumpRange, which only governs Create's own pumps.";
        static String boosterStationInputRpmCap =
                "The most RPM one pump casing can contribute to a station's output pressure.";
        static String boosterStationMaxPressure =
                "Hard ceiling on the pressure a station puts on its outlet. Stations wired into a ring feed "
                        + "each other's inlets and compound every pass, which is intended - the pipes bursting "
                        + "is how a loop ends - but with pipesBurst off nothing else stops the climb, so this "
                        + "is where it stops. Well above steel's 1536 rating, so every real pipe still goes "
                        + "long before the ceiling is reached.";
        static String boosterStationStressPerRpm =
                "Stress impact of one driven pump casing, in SU per RPM. A loose casing may spin freely and "
                        + "costs nothing; only a casing in a built station does any work.";
        static String boosterStationOilStressDiscount =
                "Percentage taken off the stress each driven casing demands once a station has been given a "
                        + "bucket of Lubrication Oil. Oil never changes pressure and is never required.";
        static String defaultPipeRating =
                "Pressure rating used for pipes with no entry in the tfmg:pipe_pressure data map.";
        static String defaultPipeDecay =
                "Pressure lost per block for pipes with no entry in the tfmg:pipe_pressure data map.";
        static String pipesBurst =
                "Whether pipes carrying more station pressure than they are rated for can burst.";
        static String burstReferenceExcess =
                "The excess (pressure / rating - 1) at which a pipe takes burstReferenceSeconds to burst.";
        static String burstReferenceSeconds =
                "Mean seconds to burst at burstReferenceExcess. Other excesses scale by "
                        + "(burstReferenceExcess / excess) ^ burstExponent.";
        static String burstExponent =
                "How sharply time-to-burst falls off as the excess grows.";
        static String stationMotorVolumeMin =
                "Volume of the motor loop while a Booster Station is barely turning. The motor runs on "
                        + "rotation alone, whether or not there is anything to pump.";
        static String stationMotorVolumeMax =
                "Volume of the motor loop at stationSoundFullSpeed. Setting both motor volumes to 0 silences "
                        + "that layer.";
        static String stationFluidVolumeMin =
                "Volume of the fluid loop while a Booster Station is barely pushing anything. The fluid layer "
                        + "only plays on top of the motor, never on its own.";
        static String stationFluidVolumeMax =
                "Volume of the fluid loop at stationSoundFullPressure. Setting both fluid volumes to 0 "
                        + "silences that layer. The clip is already mixed below the motor, so this range "
                        + "matching the motor's is the intended balance.";
        static String stationSoundPitchMin =
                "Pitch of the motor loop while a station is barely turning. The fluid layer is always played "
                        + "at its own pitch.";
        static String stationSoundPitchMax =
                "Pitch of the motor loop at stationSoundFullSpeed.";
        static String stationSoundFullSpeed =
                "Drive speed in RPM that the motor loop treats as full tilt. Its pitch and volume slide "
                        + "between their minimum and maximum as the mean speed of the station's driven "
                        + "casings goes from nothing to this.";
        static String stationSoundFullPressure =
                "Outlet pressure the fluid loop treats as full tilt. Its volume slides between its minimum "
                        + "and maximum as the station's outlet pressure goes from nothing to this.";
        static String pipeStrainVolume =
                "Volume of the sound a pipe over its rating makes. 0 silences it.";
        static String pipeBurstVolume =
                "Volume of the sound a pipe makes when it bursts. 0 silences it.";
        static String pipeBurstBoomVolume =
                "Volume of the explosion layered underneath the burst, which is what gives it its size. 0 "
                        + "leaves the burst clip on its own.";
        static String burstMinimumSeconds =
                "Floor on the mean time to burst. An excess of 1 or more (double the rating) always bursts "
                        + "immediately and ignores this.";
    }
}
