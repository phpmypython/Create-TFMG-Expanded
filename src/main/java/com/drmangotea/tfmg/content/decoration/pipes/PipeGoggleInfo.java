package com.drmangotea.tfmg.content.decoration.pipes;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.drmangotea.tfmg.content.machinery.pipeline.PipePressure;
import com.drmangotea.tfmg.content.machinery.pipeline.StationPressure;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;

/**
 * Goggle readout for anything carrying fluid through Create's transport system: what it carries, which
 * way it moves on every connected face, and the pressure driving it.
 * <p>
 * The only thing this asks of a block is that it has a {@link FluidTransportBehaviour} - that is what
 * "is a pipe" means in Create's system, so pipes, pumps and valves all land here, TFMG's and Create's
 * alike, as does anything another mod builds on the same classes. The mixins under
 * {@code mixin.client} are what attach it.
 * <p>
 * Create syncs everything shown already: PipeConnection writes the pressure pair and the current flow
 * regardless of clientPacket, and FluidTransportBehaviour sends the block entity's data whenever either
 * of them changes. This is presentation only - no ticking, NBT or packet change.
 */
public class PipeGoggleInfo {

    private PipeGoggleInfo() {
    }

    public static boolean addToTooltip(BlockEntity blockEntity, List<Component> tooltip, boolean isPlayerSneaking) {
        FluidTransportBehaviour pipe = BlockEntityBehaviour.get(blockEntity, FluidTransportBehaviour.TYPE);
        // No connection data means this pipe's faces have not reached the client yet
        if (pipe == null || pipe.interfaces == null || pipe.interfaces.isEmpty())
            return false;

        // Let the block name itself, so a pump and a valve do not both announce themselves as "Pipe"
        TFMGLang.blockName(blockEntity.getBlockState()).style(ChatFormatting.GRAY).forGoggles(tooltip);

        FluidStack carried = FluidStack.EMPTY;
        int inbound = 0;
        int outbound = 0;
        for (Direction side : Iterate.directions) {
            PipeConnection.Flow flow = pipe.getFlow(side);
            if (flow == null)
                continue;
            if (carried.isEmpty())
                carried = flow.fluid;
            if (flow.inbound)
                inbound++;
            else
                outbound++;
        }

        (carried.isEmpty() ? TFMGTexts.Pipe.empty() : TFMGTexts.Pipe.carrying(carried)).forGoggles(tooltip, 1);

        addRatingInfo(blockEntity, tooltip);

        if (isPlayerSneaking) {
            for (Direction side : Iterate.directions) {
                PipeConnection connection = pipe.interfaces.get(side);
                if (connection != null)
                    faceLine(connection, side, pipe.getFlow(side)).forGoggles(tooltip, 1);
            }
        } else if (inbound + outbound == 0) {
            TFMGTexts.Pipe.noFlow().forGoggles(tooltip, 1);
        } else {
            TFMGTexts.Pipe.flowSummary(inbound, outbound).forGoggles(tooltip, 1);
        }

        addFilterInfo(blockEntity, tooltip);
        return true;
    }

    /**
     * One line per connected face: which way it moves, and the pressure driving that direction.
     * A face that is connected but carries no flow reads as idle, and still shows the pressure
     * standing on it - that pair is what a stalled line looks like.
     */
    private static LangBuilder faceLine(PipeConnection connection, Direction side, PipeConnection.Flow flow) {
        LangBuilder line = TFMGTexts.Pipe.face(side, flow == null ? TFMGTexts.Pipe.idle()
                : flow.inbound ? TFMGTexts.Pipe.inbound() : TFMGTexts.Pipe.outbound());

        Couple<Float> pressure = connection.getPressure();
        float driving = flow != null ? pressure.get(flow.inbound)
                : Math.max(pressure.getFirst(), pressure.getSecond());
        if (driving > 0)
            line.space().add(TFMGTexts.Pipe.pressure(driving));
        return line;
    }

    /**
     * What this pipe is rated for, what a Booster Station is currently standing on it, and whether that is
     * past the rating.
     * <p>
     * Only station pressure is measured against a rating - a mechanical pump's 256 into a plastic pipe is
     * still just a pump - so both the reading and the warning come from {@link StationPressure}, not from the
     * pressure Create shows on the faces above. Reading the two together as {@code 240 / 384} is what tells a
     * player at a glance how much headroom a run has left, without having to crouch.
     */
    private static void addRatingInfo(BlockEntity blockEntity, List<Component> tooltip) {
        Level level = blockEntity.getLevel();
        if (level == null)
            return;

        BlockState state = blockEntity.getBlockState();
        float stationPressure = StationPressure.get(level, blockEntity.getBlockPos());
        boolean isRated = PipePressure.entry(state) != null;
        // A pump lands here too - it carries a FluidTransportBehaviour - and has no rating to speak of.
        // Nothing to say about it unless a station is actually pushing pressure through it.
        if (!isRated && stationPressure <= 0)
            return;

        int rating = PipePressure.of(state).rating();
        if (stationPressure > rating)
            TFMGTexts.Pipe.overRated(stationPressure, rating).forGoggles(tooltip, 1);
        else if (isRated && stationPressure > 0)
            TFMGTexts.Pipe.pressureRated(stationPressure, rating).forGoggles(tooltip, 1);
        else
            TFMGTexts.Pipe.rated(rating).forGoggles(tooltip, 1);
    }

    /**
     * Smart pipes are the pipes carrying a {@link FilteringBehaviour}; a wrong filter is the usual
     * reason a multi-fluid output stops.
     */
    private static void addFilterInfo(BlockEntity blockEntity, List<Component> tooltip) {
        FilteringBehaviour filtering = BlockEntityBehaviour.get(blockEntity, FilteringBehaviour.TYPE);
        if (filtering == null)
            return;

        ItemStack filter = filtering.getFilter();
        if (filter.isEmpty()) {
            TFMGTexts.Pipe.noFilter().forGoggles(tooltip, 1);
            return;
        }

        // Fluid filters are usually set with a filled bucket; name the fluid rather than the item
        TFMGTexts.Pipe.filter(FluidUtil.getFluidContained(filter)
                        .map(FluidStack::getHoverName)
                        .orElseGet(filter::getHoverName))
                .forGoggles(tooltip, 1);
    }
}
