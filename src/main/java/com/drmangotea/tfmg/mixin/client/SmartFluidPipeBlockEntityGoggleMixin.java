package com.drmangotea.tfmg.mixin.client;

import com.drmangotea.tfmg.content.decoration.pipes.PipeGoggleInfo;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * Gives the smart fluid pipes the goggle readout, including the filter line the readout picks up from
 * their FilteringBehaviour.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = SmartFluidPipeBlockEntity.class, remap = false)
public abstract class SmartFluidPipeBlockEntityGoggleMixin extends BlockEntity implements IHaveGoggleInformation {

    public SmartFluidPipeBlockEntityGoggleMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return PipeGoggleInfo.addToTooltip(this, tooltip, isPlayerSneaking);
    }
}
