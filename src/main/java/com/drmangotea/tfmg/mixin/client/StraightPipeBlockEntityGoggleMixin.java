package com.drmangotea.tfmg.mixin.client;

import com.drmangotea.tfmg.content.decoration.pipes.PipeGoggleInfo;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * Gives the glass pipes - Create's and TFMG's - the goggle readout.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = StraightPipeBlockEntity.class, remap = false)
public abstract class StraightPipeBlockEntityGoggleMixin extends BlockEntity implements IHaveGoggleInformation {

    public StraightPipeBlockEntityGoggleMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return PipeGoggleInfo.addToTooltip(this, tooltip, isPlayerSneaking);
    }
}
