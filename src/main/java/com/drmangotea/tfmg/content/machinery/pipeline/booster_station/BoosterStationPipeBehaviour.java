package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Map;

/**
 * Makes an assembled station row behave as one long pipe.
 * <p>
 * Modelled on Create's own {@code PumpFluidTransferBehaviour}: each part holds pressure on its two flow faces
 * every tick - the station's pull on the upstream face, its discharge on the downstream one - so Create's
 * flow machinery moves fluid through the row without knowing anything about stations. What the station adds
 * to the pipes beyond the outlet is applied separately, by the pressure walk.
 */
public class BoosterStationPipeBehaviour extends FluidTransportBehaviour {

    private final BoosterStationPartBlockEntity part;

    public BoosterStationPipeBehaviour(BoosterStationPartBlockEntity part) {
        super(part);
        this.part = part;
    }

    @Override
    public boolean canHaveFlowToward(BlockState state, Direction direction) {
        return part.carriesFlowToward(direction);
    }

    /**
     * A part that is not carrying fluid never pulls any either. {@link #canHaveFlowToward} already keeps the
     * connections from being made, but Create restores a face from saved NBT without asking, so the pull is
     * refused here as well rather than relying on the connection set alone.
     */
    @Override
    public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
        return part.carriesFlowToward(direction);
    }

    @Override
    public void tick() {
        super.tick();
        if (interfaces == null)
            return;

        Direction flow = part.getFlowDirection();
        if (flow == null)
            return;

        float inbound = part.getInboundPressure();
        float outbound = part.getOutboundPressure();

        for (Map.Entry<Direction, PipeConnection> entry : interfaces.entrySet()) {
            boolean downstream = entry.getKey() == flow;
            Couple<Float> pressure = entry.getValue().getPressure();
            pressure.set(true, downstream ? 0 : inbound);
            pressure.set(false, downstream ? outbound : 0);
        }
    }

    /**
     * The stub's own model carries the flange a pipe mates with, so Create should not draw a rim on top of it.
     */
    @Override
    public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state,
                                                    Direction direction) {
        AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
        return attachment == AttachmentTypes.RIM ? AttachmentTypes.NONE : attachment;
    }
}
