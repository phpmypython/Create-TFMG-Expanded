package com.drmangotea.tfmg.mixin.client;

import com.drmangotea.tfmg.content.decoration.pipes.PipeGoggleInfo;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * Gives Create's plain and encased fluid pipes the goggle readout. TFMG's own pipes extend this class,
 * so they are covered by the same mixin rather than by a subclass of their own.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = FluidPipeBlockEntity.class, remap = false)
public abstract class FluidPipeBlockEntityGoggleMixin extends BlockEntity implements IHaveGoggleInformation {

    public FluidPipeBlockEntityGoggleMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return PipeGoggleInfo.addToTooltip(this, tooltip, isPlayerSneaking);
    }
}
