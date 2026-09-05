package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.config.PipelineConfig;
import com.drmangotea.tfmg.content.machinery.pipeline.PipeBurst;
import com.drmangotea.tfmg.content.machinery.pipeline.PipePressure;
import com.drmangotea.tfmg.content.machinery.pipeline.PressuredFace;
import com.drmangotea.tfmg.content.machinery.pipeline.StationPressure;
import com.drmangotea.tfmg.content.machinery.pipeline.StationPressureWalk;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A pump casing, and - when it is the one next to the outlet stub - the controller that runs the whole
 * station.
 * <p>
 * The controller is what turns rotation into pipeline pressure. Every casing in the row is a kinetic block in
 * its own right: drive one on either of its side faces and it contributes its RPM, capped, on top of whatever
 * station pressure is arriving at the inlet, and the sum is put on the line beyond the outlet by
 * {@link StationPressureWalk}. Lubrication Oil changes none of that - a bucket poured into a built station
 * takes a permanent cut off the stress each driven casing demands, which is why a given engine can drive a
 * longer station once it has been oiled.
 */
public class PumpCasingBlockEntity extends BoosterStationPartBlockEntity {

    private static final int RECALCULATION_INTERVAL = 20;
    /** How many stations back the loop check walks before deciding the chain is not a ring. */
    private static final int MAX_UPSTREAM_HOPS = 32;

    /** Number of casings in the row. Only the controller has this. */
    private int casingCount;

    /** Faces beyond the outlet this station is holding pressure on, and how much of each is ours. */
    private final Map<BlockFace, PressuredFace> appliedOutlet = new HashMap<>();
    /** The same, for the modest pull the inlet puts on the line behind it. */
    private final Map<BlockFace, PressuredFace> appliedInlet = new HashMap<>();

    /** Station pressure standing on each pipe past the outlet - what ratings are measured against. */
    private Map<BlockPos, Float> linePressure = new HashMap<>();
    /** Only the pipes currently over their rating, so the per-tick roll stays cheap. */
    private final List<BlockPos> overRated = new ArrayList<>();

    private float incomingPressure;
    private float outputPressure;
    private float pullPressure;
    /** Whether the ceiling is what is holding the output down, which the goggles say out loud. */
    private boolean pressureCapped;

    /** The station whose pressure is standing on the pipe at our inlet, if it is not our own. */
    @Nullable
    private BlockPos incomingFrom;
    /** Set when the pressure at our inlet is the pressure we put on our own outlet, however far round. */
    private boolean pressureLooped;
    /** The one-station case of that: our own discharge has come all the way back to our own inlet. */
    private boolean selfFed;

    /** |RPM| of each casing in the row, from the outlet end back. Only the controller has this. */
    private int[] casingSpeeds = new int[0];
    private int drivenCasings;
    private int weakestRating;

    /** Server-authoritative and sent the moment it changes, so the running loop starts without a delay. */
    private boolean pumping;

    private boolean lubricated;
    private boolean recalculateNextTick = true;
    /** The kinetic network is told this station's load once after every load, then only when it changes. */
    private boolean stressAnnounced;

    public PumpCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(RECALCULATION_INTERVAL);
    }

    // ---------------------------------------------------------------- row shape

    public Direction getFlow() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /**
     * A casing only carries fluid once its row is assembled <i>and</i> closed with steel. An open casing is a
     * shell with nothing in it to pump - it is not a length of pipe, and a line must not run through it.
     */
    @Override
    public boolean carriesFluid() {
        return super.carriesFluid() && PumpCasingBlock.isClosed(getBlockState());
    }

    @Override
    public boolean carriesFlowToward(Direction direction) {
        return carriesFluid() && direction.getAxis() == getFlow().getAxis();
    }

    @Override
    @Nullable
    public Direction getFlowDirection() {
        return carriesFluid() ? getFlow() : null;
    }

    public boolean isController() {
        return casingCount > 0 && worldPosition.equals(getControllerPos());
    }

    public void setCasingCount(int casingCount) {
        this.casingCount = casingCount;
    }

    /** Outlet stub, casings from the outlet end back, then the inlet stub. */
    public List<BlockPos> getParts() {
        List<BlockPos> parts = new ArrayList<>(casingCount + 2);
        Direction flow = getFlow();
        parts.add(worldPosition.relative(flow));
        for (int i = 0; i < casingCount; i++)
            parts.add(worldPosition.relative(flow.getOpposite(), i));
        parts.add(worldPosition.relative(flow.getOpposite(), casingCount));
        return parts;
    }

    public BlockPos getOutletStub() {
        return worldPosition.relative(getFlow());
    }

    public BlockPos getInletStub() {
        return worldPosition.relative(getFlow().getOpposite(), casingCount);
    }

    // ---------------------------------------------------------------- readouts other parts ask for

    public float getOutputPressure() {
        return outputPressure;
    }

    public float getPullPressure() {
        return pullPressure;
    }

    public boolean isLubricated() {
        return lubricated;
    }

    /**
     * Pours a bucket of Lubrication Oil into the station. There is no tank and nothing to run dry: an oiled
     * station stays oiled, and the discount on its casings' stress applies from then on.
     *
     * @return whether the station took the oil, which is false if it had already been done
     */
    public boolean lubricate() {
        if (lubricated)
            return false;
        setLubricated(true);
        return true;
    }

    /** Carries the flag across when a row is rebuilt in place - see {@link BoosterStationAssembly#reverse}. */
    public void setLubricated(boolean lubricated) {
        this.lubricated = lubricated;
        stressAnnounced = false;
        refreshRowStress();
        notifyUpdate();
    }

    /**
     * Stress this casing demands, per RPM. A loose casing may spin freely - there is nothing inside it doing
     * any work until the row is built - so the impact only applies once it belongs to a station.
     */
    public float getStressPerRpm() {
        PumpCasingBlockEntity station = getStation();
        if (station == null)
            return 0;
        PipelineConfig config = TFMGConfigs.common().pipeline;
        float impact = config.boosterStationStressPerRpm.getF();
        if (station.lubricated)
            impact *= Math.max(0, 1 - config.boosterStationOilStressDiscount.getF() / 100f);
        return impact;
    }

    @Override
    public float calculateStressApplied() {
        float impact = getStressPerRpm();
        this.lastStressApplied = impact;
        return impact;
    }

    /** Tells the kinetic network this casing's load changed - the row was built, broken or oiled. */
    public void updateStationStress() {
        if (level == null || level.isClientSide || !hasNetwork())
            return;
        getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }

    /** The same for every casing in the row: oil and assembly change all of their loads at once. */
    void refreshRowStress() {
        forEachCasing(PumpCasingBlockEntity::updateStationStress);
    }

    /** Recalculate on the next tick - the row changed, or a station upstream did. */
    public void markPressureDirty() {
        recalculateNextTick = true;
    }

    /**
     * A casing's own speed is a term in its station's output pressure, so a shaft speeding up or stopping puts
     * the whole row back in the queue instead of waiting for the next scheduled walk.
     */
    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide)
            return;
        PumpCasingBlockEntity station = getStation();
        if (station != null)
            station.markPressureDirty();
    }

    // ---------------------------------------------------------------- ticking

    /** Whether the station is doing its job: something is driving it and fluid is on the move. */
    public boolean isPumping() {
        return pumping;
    }

    /** Whether anything is turning the station at all, which is what the motor loop runs on. */
    public boolean hasRotation() {
        return drivenCasings > 0;
    }

    /**
     * Mean speed of the casings that are actually turning. The mean rather than the sum, because this is how
     * fast the machine is running, not how much of it there is - a long row driven slowly should not sound
     * like a short one driven hard.
     */
    public float getDriveSpeed() {
        if (drivenCasings <= 0)
            return 0;
        long total = 0;
        for (int rpm : casingSpeeds)
            total += rpm;
        return (float) total / drivenCasings;
    }

    /**
     * How fast the station is turning, 0 to 1, which is what the motor loop's pitch and volume follow. It is
     * the drive speed against the speed the config calls full tilt.
     */
    public float getSpeedLoad() {
        float full = TFMGConfigs.common().pipeline.stationSoundFullSpeed.getF();
        if (full <= 0)
            return 1;
        return Mth.clamp(getDriveSpeed() / full, 0, 1);
    }

    /**
     * How hard the station is pushing, 0 to 1, which is what the fluid loop's volume follows. It is the outlet
     * pressure against the pressure the config calls full tilt, so a pump barely moving water sounds nothing
     * like one at the top of its range.
     */
    public float getPressureLoad() {
        float full = TFMGConfigs.common().pipeline.stationSoundFullPressure.getF();
        if (full <= 0)
            return 1;
        return Mth.clamp(outputPressure / full, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || !isController())
            return;

        if (level.isClientSide) {
            // Client only; a dedicated server never gets here, so StationSound is never loaded there
            StationSound.tick(this);
            return;
        }

        if (recalculateNextTick) {
            recalculateNextTick = false;
            recalculate();
        }

        updatePumping();
        tickBursting();
    }

    /**
     * Whether the station is pumping is worked out every tick and sent the moment it changes, rather than
     * riding along with the once-a-second pressure sync. That is what lets the running loop start on the same
     * client tick the station starts moving fluid instead of at the next sync.
     */
    private void updatePumping() {
        boolean nowPumping = drivenCasings > 0 && outputPressure > 0 && hasFluidMoving();
        if (nowPumping == pumping)
            return;
        pumping = nowPumping;
        sendData();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide || !isController())
            return;
        // Create wipes a pipe's pressure whenever the network around it changes, and a station is not one of
        // the pumps it knows to notify; walking again once a second puts the line back without a mixin
        recalculate();
    }

    private void tickBursting() {
        if (overRated.isEmpty() || !(level instanceof ServerLevel serverLevel))
            return;

        boolean lineChanged = false;
        for (int i = overRated.size() - 1; i >= 0; i--) {
            BlockPos pos = overRated.get(i);
            Float pressure = linePressure.get(pos);
            if (pressure == null) {
                overRated.remove(i);
                continue;
            }
            float excess = PipeBurst.excess(pressure, PipePressure.of(serverLevel.getBlockState(pos)).rating());
            if (excess <= 0) {
                overRated.remove(i);
                continue;
            }
            if (PipeBurst.tick(serverLevel, pos, pressure, excess, pressureLooped)) {
                overRated.remove(i);
                lineChanged = true;
            }
        }

        if (lineChanged)
            markPressureDirty();
    }

    // ---------------------------------------------------------------- the pressure model

    private void recalculate() {
        if (level == null || level.isClientSide)
            return;

        if (!carriesFluid() || !isController()) {
            clearPressure();
            return;
        }

        int[] previousSpeeds = casingSpeeds;
        readCasingSpeeds();
        boolean speedsChanged = !Arrays.equals(previousSpeeds, casingSpeeds);

        PipelineConfig config = TFMGConfigs.common().pipeline;
        int rpmCap = config.boosterStationInputRpmCap.get();
        float added = 0;
        for (int rpm : casingSpeeds)
            added += Math.min(rpm, rpmCap);

        incomingPressure = readIncomingPressure();
        // The pressure arriving at the inlet is a term in the pressure leaving the outlet, so stations wired
        // into a ring feed each other and climb every pass. That is the point - the pipes bursting is how a
        // loop is meant to end - but with bursting turned off in the config nothing else stops the climb, and
        // a float that runs away would end up an int on Create's side. The ceiling is that backstop
        float ceiling = config.boosterStationMaxPressure.get();
        float wanted = incomingPressure + added;
        float newOutput = Math.min(ceiling, wanted);
        boolean nowCapped = wanted > ceiling;
        float newPull = Math.min(rpmCap, added);

        boolean pressureChanged =
                newOutput != outputPressure || newPull != pullPressure || nowCapped != pressureCapped;
        outputPressure = newOutput;
        pullPressure = newPull;
        pressureCapped = nowCapped;
        pressureLooped = selfFed || isOnPressureLoop();

        Set<BlockPos> parts = new HashSet<>(getParts());
        StationPressureWalk outlet = new StationPressureWalk(level,
                new BlockFace(getOutletStub(), stubOuterFace(getOutletStub(), getFlow())), false, outputPressure,
                config.boosterStationRange.get(), true, parts).run();
        StationPressureWalk inlet = new StationPressureWalk(level,
                new BlockFace(getInletStub(), stubOuterFace(getInletStub(), getFlow().getOpposite())), true,
                pullPressure, FluidPropagator.getPumpRange(), false, parts).run();

        boolean facesChanged = applyPressure(appliedOutlet, outlet.pressuredFaces());
        facesChanged |= applyPressure(appliedInlet, inlet.pressuredFaces());

        // Only reset the fluid networks when something actually moved; resetting pauses transfer for two
        // ticks, and this runs once a second
        if (facesChanged || pressureChanged)
            FluidPropagator.resetAffectedFluidNetworks(level, getOutletStub(),
                    stubOuterFace(getOutletStub(), getFlow()).getOpposite());

        boolean lineChanged = !outlet.pipes.equals(linePressure);
        linePressure = outlet.pipes;
        StationPressure.set(level, worldPosition, linePressure);
        refreshRatings();

        outlet.downstreamStations.forEach((controller, pressure) -> {
            if (!controller.equals(worldPosition)
                    && level.getBlockEntity(controller) instanceof PumpCasingBlockEntity station)
                station.markPressureDirty();
        });

        if (!stressAnnounced) {
            stressAnnounced = true;
            refreshRowStress();
        }
        if (lineChanged || pressureChanged || speedsChanged)
            sendData();
    }

    /** Runs on removal and on chunk unload alike, which is exactly when a station stops driving its line. */
    @Override
    public void invalidate() {
        super.invalidate();
        if (level != null && isController())
            StationPressure.clear(level, worldPosition);
    }

    /**
     * The pressure another station is delivering to our inlet, if any, and which station that is.
     * <p>
     * Our own pressure arriving back at our own inlet is not an input - a station may not feed itself - but it
     * is the tightest possible loop, and worth remembering as one.
     */
    private float readIncomingPressure() {
        incomingFrom = null;
        selfFed = false;

        BlockPos inlet = getInletStub();
        Direction outer = stubOuterFace(inlet, getFlow().getOpposite());
        StationPressure.Entry entry = StationPressure.entry(level, inlet.relative(outer));
        if (entry == null)
            return 0;
        if (entry.controller().equals(worldPosition)) {
            selfFed = true;
            return 0;
        }
        incomingFrom = entry.controller();
        return entry.pressure();
    }

    /**
     * Whether the pressure feeding this station started at its own outlet. Each station knows which station's
     * pressure is standing on its inlet, so following that link upstream either comes back here - a ring - or
     * runs out of stations. Bounded, because a mistake in the chain must not become an endless walk.
     */
    private boolean isOnPressureLoop() {
        if (level == null)
            return false;
        BlockPos cursor = incomingFrom;
        for (int hops = 0; cursor != null && hops < MAX_UPSTREAM_HOPS; hops++) {
            if (cursor.equals(worldPosition))
                return true;
            if (!(level.getBlockEntity(cursor) instanceof PumpCasingBlockEntity upstream))
                return false;
            cursor = upstream.incomingFrom;
        }
        return false;
    }

    /** Where a stub faces, falling back to the row direction if its state has drifted. */
    private Direction stubOuterFace(BlockPos stubPos, Direction expected) {
        if (level != null && level.getBlockEntity(stubPos) instanceof StationStubBlockEntity stub)
            return stub.getOuterFace();
        return expected;
    }

    /**
     * How fast each casing in the row is turning, from the outlet end back. A casing is a kinetic block, so
     * this is simply its shaft speed - however that shaft is being driven, and from whichever of its two side
     * faces. Both faces are the same shaft, so a casing has one speed however many things are bolted to it.
     */
    private void readCasingSpeeds() {
        int[] speeds = new int[casingCount];
        int driven = 0;
        for (int i = 0; i < casingCount; i++) {
            PumpCasingBlockEntity casing = casingAt(i);
            if (casing == null)
                continue;
            speeds[i] = (int) Math.abs(casing.getSpeed());
            if (speeds[i] > 0)
                driven++;
        }
        casingSpeeds = speeds;
        drivenCasings = driven;
    }

    @Nullable
    private PumpCasingBlockEntity casingAt(int indexFromOutlet) {
        if (level == null)
            return null;
        BlockPos pos = worldPosition.relative(getFlow().getOpposite(), indexFromOutlet);
        return level.getBlockEntity(pos) instanceof PumpCasingBlockEntity casing ? casing : null;
    }

    private void forEachCasing(Consumer<PumpCasingBlockEntity> action) {
        for (int i = 0; i < casingCount; i++) {
            PumpCasingBlockEntity casing = casingAt(i);
            if (casing != null)
                action.accept(casing);
        }
    }

    /**
     * Puts the difference between what we are holding on each face and what we want onto the line. Create only
     * offers a relative change, so this is the only way to raise, lower and take back pressure without
     * trampling a Create pump sharing the same pipe.
     *
     * @return whether the set of pressurised faces changed
     */
    private boolean applyPressure(Map<BlockFace, PressuredFace> applied, List<PressuredFace> wanted) {
        Set<BlockFace> stillWanted = new HashSet<>(wanted.size());
        boolean setChanged = false;

        for (PressuredFace face : wanted) {
            BlockFace key = new BlockFace(face.pos(), face.side());
            stillWanted.add(key);
            PressuredFace previous = applied.get(key);
            float standing = previous == null ? 0 : previous.standingShare(level);
            face.addPressure(level, face.pressure() - standing);
            if (previous == null)
                setChanged = true;
            applied.put(key, face);
        }

        setChanged |= applied.entrySet().removeIf(entry -> {
            if (stillWanted.contains(entry.getKey()))
                return false;
            PressuredFace face = entry.getValue();
            // A pipe out of the loaded area cannot be read or written, and reading it would force its chunk
            // in. Keep the record instead: the walk stops at the loaded edge too, so the face comes back the
            // moment the chunk does, and the difference is then measured against what we really left there.
            if (!level.isLoaded(face.pos()))
                return false;
            face.addPressure(level, -face.standingShare(level));
            return true;
        });

        return setChanged;
    }

    /** Takes every bit of pressure this station put on the line back off. */
    private void clearPressure() {
        applyPressure(appliedOutlet, List.of());
        applyPressure(appliedInlet, List.of());
        linePressure = new HashMap<>();
        overRated.clear();
        outputPressure = 0;
        pullPressure = 0;
        incomingPressure = 0;
        pressureCapped = false;
        pressureLooped = false;
        incomingFrom = null;
        selfFed = false;
        weakestRating = 0;
        if (level != null)
            StationPressure.set(level, worldPosition, Map.of());
    }

    /**
     * Works out which pipes on the line are over their rating, and lets the ones that have just gone over be
     * heard at once. The throttle in {@link PipeBurst} keeps a long straining run from becoming a wall of
     * noise, but it would also swallow the first three seconds of a pipe's distress, which is exactly the part
     * the player needs.
     */
    private void refreshRatings() {
        Set<BlockPos> wereOverRated = new HashSet<>(overRated);
        overRated.clear();
        weakestRating = 0;
        if (level == null)
            return;
        ServerLevel serverLevel = level instanceof ServerLevel server ? server : null;
        linePressure.forEach((pos, pressure) -> {
            int rating = PipePressure.of(level.getBlockState(pos)).rating();
            if (weakestRating == 0 || rating < weakestRating)
                weakestRating = rating;
            float excess = PipeBurst.excess(pressure, rating);
            if (excess <= 0)
                return;
            overRated.add(pos);
            if (serverLevel != null && !wereOverRated.contains(pos))
                PipeBurst.beginStrain(serverLevel, pos, excess);
        });
    }

    // ---------------------------------------------------------------- assembly bookkeeping

    /**
     * Breaks the row back into loose blocks and gives back the block of steel assembly consumed. The impeller
     * does not come back: it is part of the casing item now, and the casing drops as the block it is.
     */
    public void disassemble(@Nullable BlockPos removedPart) {
        if (release(removedPart))
            Block.popResource(level, worldPosition, TFMGBlocks.STEEL_BLOCK.asStack());
    }

    /**
     * Turns the row back into loose blocks without giving anything back, which is what reversing a station
     * needs: the same blocks are about to be assembled again the other way round. The part that is being
     * removed, if any, is skipped - it is already on its way out and drops itself.
     * <p>
     * Zeroing the casing count first is what makes this happen exactly once, even though every part in the
     * row changes state during the same call.
     *
     * @return whether there was a row to take apart
     */
    public boolean release(@Nullable BlockPos removedPart) {
        if (level == null || casingCount <= 0)
            return false;

        List<BlockPos> parts = getParts();
        clearPressure();
        casingCount = 0;

        for (BlockPos part : parts) {
            if (part.equals(removedPart))
                continue;
            BlockState state = level.getBlockState(part);
            if (PumpCasingBlock.isClosed(state))
                level.setBlock(part, TFMGBlocks.PUMP_CASING.getDefaultState().setValue(
                        HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING)), 3);
            if (level.getBlockEntity(part) instanceof BoosterStationPartBlockEntity be) {
                if (be instanceof PumpCasingBlockEntity casing)
                    casing.casingCount = 0;
                // Clearing the station is what drops a casing's load: no station, no work being done
                be.clearStation();
            }
        }
        return true;
    }

    @Override
    protected void onStationChanged() {
        super.onStationChanged();
        markPressureDirty();
        if (level != null && !level.isClientSide)
            updateStationStress();
    }

    // ---------------------------------------------------------------- goggles

    /** The station's figures. The header belongs to whichever part the player is looking at. */
    public boolean addStationTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        TFMGTexts.BoosterStation.flow(getFlow()).forGoggles(tooltip, 1);

        if (drivenCasings == 0) {
            TFMGTexts.BoosterStation.notDriven().forGoggles(tooltip, 1);
        } else {
            TFMGTexts.BoosterStation.casings(drivenCasings, casingSpeeds.length).forGoggles(tooltip, 1);
            for (int rpm : casingSpeeds)
                if (rpm > 0)
                    TFMGTexts.BoosterStation.casingSpeed(rpm).forGoggles(tooltip, 2);
        }

        if (incomingPressure > 0)
            TFMGTexts.BoosterStation.incoming(incomingPressure).forGoggles(tooltip, 1);
        (pressureCapped ? TFMGTexts.BoosterStation.outputCapped(outputPressure)
                : TFMGTexts.BoosterStation.output(outputPressure)).forGoggles(tooltip, 1);

        if (weakestRating > 0)
            (outputPressure > weakestRating ? TFMGTexts.BoosterStation.overRated(weakestRating)
                    : TFMGTexts.BoosterStation.weakestRating(weakestRating)).forGoggles(tooltip, 1);

        (lubricated ? TFMGTexts.BoosterStation.lubricated(
                TFMGConfigs.common().pipeline.boosterStationOilStressDiscount.getF())
                : TFMGTexts.BoosterStation.dry()).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    protected boolean addPartTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        addPartHeader(tooltip);
        PumpCasingBlockEntity station = getStation();
        if (station != null)
            return station.addStationTooltip(tooltip, isPlayerSneaking);

        TFMGTexts.BoosterStation.notAssembled().forGoggles(tooltip, 1);
        // A loose casing is symmetrical, so the only way to see which way it will pump is to say so
        TFMGTexts.BoosterStation.flow(getFlow()).forGoggles(tooltip, 1);
        return true;
    }

    // ---------------------------------------------------------------- serialisation

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        casingCount = tag.getInt("Casings");
        lubricated = tag.getBoolean("Lubricated");
        outputPressure = tag.getFloat("Output");
        pullPressure = tag.getFloat("Pull");
        incomingPressure = tag.getFloat("Incoming");
        pressureCapped = tag.getBoolean("Capped");
        drivenCasings = tag.getInt("DrivenCasings");
        casingSpeeds = tag.getIntArray("CasingSpeeds");
        weakestRating = tag.getInt("WeakestRating");
        pumping = tag.getBoolean("Pumping");

        if (clientPacket) {
            linePressure = readLinePressure(tag);
            if (level != null)
                StationPressure.set(level, worldPosition, linePressure);
            return;
        }

        readFaces(tag.getList("AppliedOutlet", Tag.TAG_COMPOUND), appliedOutlet);
        readFaces(tag.getList("AppliedInlet", Tag.TAG_COMPOUND), appliedInlet);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Casings", casingCount);
        tag.putBoolean("Lubricated", lubricated);
        tag.putFloat("Output", outputPressure);
        tag.putFloat("Pull", pullPressure);
        tag.putFloat("Incoming", incomingPressure);
        tag.putBoolean("Capped", pressureCapped);
        tag.putInt("DrivenCasings", drivenCasings);
        tag.putIntArray("CasingSpeeds", casingSpeeds);
        tag.putInt("WeakestRating", weakestRating);
        tag.putBoolean("Pumping", pumping);

        if (clientPacket) {
            writeLinePressure(tag);
            return;
        }

        tag.put("AppliedOutlet", writeFaces(appliedOutlet));
        tag.put("AppliedInlet", writeFaces(appliedInlet));
    }

    /**
     * Schematics carry the shape of the row and whether it has been oiled, not the pressure - a printed
     * station starts cold and works out its own line the first time it ticks.
     */
    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putInt("Casings", casingCount);
        tag.putBoolean("Lubricated", lubricated);
    }

    private static ListTag writeFaces(Map<BlockFace, PressuredFace> faces) {
        ListTag list = new ListTag();
        faces.values().forEach(face -> list.add(face.save()));
        return list;
    }

    private static void readFaces(ListTag list, Map<BlockFace, PressuredFace> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            PressuredFace face = PressuredFace.load(list.getCompound(i));
            into.put(new BlockFace(face.pos(), face.side()), face);
        }
    }

    private void writeLinePressure(CompoundTag tag) {
        long[] positions = new long[linePressure.size()];
        int[] pressures = new int[linePressure.size()];
        int index = 0;
        for (Map.Entry<BlockPos, Float> entry : linePressure.entrySet()) {
            positions[index] = entry.getKey().asLong();
            pressures[index] = Math.round(entry.getValue());
            index++;
        }
        tag.putLongArray("LinePipes", positions);
        tag.putIntArray("LinePressures", pressures);
    }

    private static Map<BlockPos, Float> readLinePressure(CompoundTag tag) {
        long[] positions = tag.getLongArray("LinePipes");
        int[] pressures = tag.getIntArray("LinePressures");
        Map<BlockPos, Float> map = new HashMap<>(positions.length);
        for (int i = 0; i < positions.length && i < pressures.length; i++)
            map.put(BlockPos.of(positions[i]), (float) pressures[i]);
        return map;
    }
}
