package com.drmangotea.tfmg.content.machinery.pipeline;

import com.drmangotea.tfmg.content.machinery.pipeline.booster_station.BoosterStationPartBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

/**
 * The walk a Booster Station runs down the line to decide what pressure stands on every pipe it reaches.
 * <p>
 * The shape is Create's - {@link PumpBlockEntity#distributePressureTo(Direction)} does a breadth-first search
 * out to a range, then a depth-first pass that keeps only the faces on a path to a real endpoint, so pressure
 * never lands on a dead-end branch. Three things differ:
 * <ul>
 * <li>pressure falls off with distance, by the decay of each pipe it passes through
 *     ({@link PipePressure}), instead of staying flat;</li>
 * <li>another station terminates the walk rather than being walked through, which is what makes stations
 *     chain: the pressure arriving at its inlet becomes that station's own input;</li>
 * <li>pressure is <i>not</i> divided among parallel branches. Create divides because it uses pressure as a
 *     stand-in for flow. Here pressure is pressure: both arms of a fork stand at the same head, which is what
 *     a real line does, and each is judged against its own rating. Throughput is unaffected either way -
 *     {@link com.simibubi.create.content.fluids.FluidNetwork} draws its whole allowance from the source face
 *     and splits that between targets, so a fork never multiplies what a station can move.</li>
 * </ul>
 * The same walk covers the pull on the inlet side by being run with {@code pull} set and decay off, which is
 * Create's plain pump behaviour and is deliberately not recorded as station pressure - a station is never the
 * reason the pipe behind it bursts.
 */
public class StationPressureWalk {

    private final Level level;
    private final BlockFace start;
    private final boolean pull;
    private final float basePressure;
    private final int maxDistance;
    private final boolean applyDecay;
    private final Set<BlockPos> ownParts;

    private final Map<BlockPos, Node> graph = new HashMap<>();
    private final Set<BlockFace> targets = new HashSet<>();
    private final Set<BlockFace> validFaces = new HashSet<>();

    /** Faces to push pressure onto, and how much stands on each. */
    public final Map<BlockFace, Float> faces = new HashMap<>();
    /** Whether each of those faces is being driven inward or outward. */
    public final Map<BlockFace, Boolean> inbound = new HashMap<>();
    /** Highest pressure this walk put on each pipe - what a rating is measured against. */
    public final Map<BlockPos, Float> pipes = new HashMap<>();
    /** Controllers of the stations this walk ran into, which now have a new input pressure. */
    public final Map<BlockPos, Float> downstreamStations = new HashMap<>();

    private static class Node {
        int distance;
        float pressure;
        final Map<Direction, Boolean> faces = new IdentityHashMap<>();
    }

    public StationPressureWalk(Level level, BlockFace start, boolean pull, float basePressure, int maxDistance,
                               boolean applyDecay, Set<BlockPos> ownParts) {
        this.level = level;
        this.start = start;
        this.pull = pull;
        this.basePressure = basePressure;
        this.maxDistance = maxDistance;
        this.applyDecay = applyDecay;
        this.ownParts = ownParts;
    }

    public StationPressureWalk run() {
        if (basePressure <= 0)
            return this;

        if (!hasReachedValidEndpoint(start, basePressure))
            search();

        collectValidFaces(new BlockFace(start.getPos(), start.getOppositeFace()));
        applyResults();
        return this;
    }

    private void search() {
        Node origin = node(start.getPos(), 0, basePressure);
        origin.faces.put(start.getFace(), pull);
        Node first = node(start.getConnectedPos(), 1, basePressure);
        first.faces.put(start.getFace().getOpposite(), !pull);

        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(start.getConnectedPos());

        while (!frontier.isEmpty()) {
            BlockPos currentPos = frontier.removeFirst();
            if (!level.isLoaded(currentPos) || !visited.add(currentPos))
                continue;

            BlockState currentState = level.getBlockState(currentPos);
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, currentPos);
            if (pipe == null)
                continue;

            Node current = graph.get(currentPos);
            float onward = applyDecay ? Math.max(0, current.pressure - PipePressure.of(currentState).decay())
                    : current.pressure;

            for (Direction face : FluidPropagator.getPipeConnections(currentState, pipe)) {
                BlockFace blockFace = new BlockFace(currentPos, face);
                BlockPos connectedPos = blockFace.getConnectedPos();

                if (!level.isLoaded(connectedPos) || blockFace.isEquivalent(start))
                    continue;

                if (hasReachedValidEndpoint(blockFace, current.pressure)) {
                    current.faces.put(face, pull);
                    targets.add(blockFace);
                    continue;
                }

                // As in Create's own walk, a pump is never walked through - it drives its own pressure - and
                // this is also what keeps a pump from ever being counted as a pipe that can burst
                if (FluidPropagator.getPipe(level, connectedPos) == null || visited.contains(connectedPos)
                        || ownParts.contains(connectedPos)
                        || level.getBlockEntity(connectedPos) instanceof PumpBlockEntity)
                    continue;

                // Out of range: stop here, but keep the face so the last pipe still carries the flow
                if (current.distance + 1 >= maxDistance || onward <= 0) {
                    current.faces.put(face, pull);
                    targets.add(blockFace);
                    continue;
                }

                current.faces.put(face, pull);
                node(connectedPos, current.distance + 1, onward).faces.put(face.getOpposite(), !pull);
                frontier.addLast(connectedPos);
            }
        }
    }

    private Node node(BlockPos pos, int distance, float pressure) {
        Node existing = graph.get(pos);
        if (existing != null)
            return existing;
        Node node = new Node();
        node.distance = distance;
        node.pressure = pressure;
        graph.put(pos, node);
        return node;
    }

    /**
     * Create's depth-first pass: a face is kept only if some branch beyond it ends at a real endpoint, so a
     * capped stub of pipe going nowhere is never pressurised and never bursts.
     */
    private boolean collectValidFaces(BlockFace currentFace) {
        Node node = graph.get(currentFace.getPos());
        if (node == null)
            return false;

        boolean anyBranchSuccessful = false;
        for (Direction nextFacing : Iterate.directions) {
            if (nextFacing == currentFace.getFace())
                continue;
            Boolean recorded = node.faces.get(nextFacing);
            if (recorded == null)
                continue;

            BlockFace localTarget = new BlockFace(currentFace.getPos(), nextFacing);
            if (targets.contains(localTarget)) {
                validFaces.add(localTarget);
                anyBranchSuccessful = true;
            } else if (recorded == pull && collectValidFaces(new BlockFace(
                    currentFace.getPos().relative(nextFacing), nextFacing.getOpposite()))) {
                validFaces.add(localTarget);
                anyBranchSuccessful = true;
            }
        }

        if (anyBranchSuccessful)
            validFaces.add(currentFace);
        return anyBranchSuccessful;
    }

    private void applyResults() {
        for (BlockFace face : validFaces) {
            BlockPos pos = face.getPos();
            if (ownParts.contains(pos))
                continue;
            Node node = graph.get(pos);
            if (node == null)
                continue;
            Boolean isInbound = node.faces.get(face.getFace());
            if (isInbound == null || node.pressure <= 0)
                continue;

            faces.put(face, node.pressure);
            inbound.put(face, isInbound);
            pipes.merge(pos, node.pressure, Math::max);
        }
    }

    /**
     * Copy of Create's endpoint test with one addition: any other Booster Station is an endpoint. Its row is a
     * pipe as far as Create is concerned, so without this a station upstream would push its pressure straight
     * through the next station's casings and out the far side, and nothing would ever be boosted.
     */
    private boolean hasReachedValidEndpoint(BlockFace blockFace, float pressureHere) {
        BlockPos connectedPos = blockFace.getConnectedPos();
        BlockState connectedState = level.getBlockState(connectedPos);
        BlockEntity blockEntity = level.getBlockEntity(connectedPos);
        Direction face = blockFace.getFace();

        if (blockEntity instanceof BoosterStationPartBlockEntity part && !ownParts.contains(connectedPos)
                && part.isAssembled()) {
            BlockPos controller = part.getControllerPos();
            // Only an inlet stub met on its outer face takes what arrives as its own input pressure; running
            // into the side of a casing, or head-on into another outlet, just ends the line
            if (controller != null && !pull && part.acceptsLineInputFrom(blockFace.getOppositeFace()))
                downstreamStations.merge(controller, pressureHere, Math::max);
            return true;
        }

        if (PumpBlock.isPump(connectedState) && connectedState.getValue(FACING).getAxis() == face.getAxis()
                && blockEntity instanceof PumpBlockEntity pumpBE) {
            boolean connectedFront = blockFace.getOppositeFace() == connectedState.getValue(FACING);
            return pumpBE.isPullingOnSide(connectedFront) != pull;
        }

        FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, connectedPos);
        if (pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace()))
            return false;

        if (blockEntity != null) {
            IFluidHandler capability =
                    level.getCapability(Capabilities.FluidHandler.BLOCK, connectedPos, face.getOpposite());
            if (capability != null)
                return true;
        }

        return FluidPropagator.isOpenEnd(level, blockFace.getPos(), face);
    }

    /** The faces this walk wants pressurised, as a flat list for the station to record. */
    public List<PressuredFace> pressuredFaces() {
        List<PressuredFace> list = new ArrayList<>(faces.size());
        faces.forEach((face, pressure) ->
                list.add(new PressuredFace(face.getPos(), face.getFace(), inbound.get(face), pressure)));
        return list;
    }
}
