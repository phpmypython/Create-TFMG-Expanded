package com.drmangotea.tfmg.config;



public class TFMGCommonConfig extends net.createmod.catnip.config.ConfigBase {

    public final MachineConfig machines = nested(0, MachineConfig::new, "Config options for TFMG's machinery");
    public final DepositConfig worldgen = nested(1, DepositConfig::new, "Worldgen Settings");
    public final PipelineConfig pipeline = nested(2, PipelineConfig::new, "Pipeline pressure, pipe ratings and bursting");

    @Override
    public String getName() {
        return "common";
    }


}
