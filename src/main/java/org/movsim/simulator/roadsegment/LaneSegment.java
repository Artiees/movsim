/*
 * Copyright (C) 2010, 2011  Martin Budden, Ralph Germ, Arne Kesting, and Martin Treiber.
 *
 * This file is part of MovSim.
 *
 * MovSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MovSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MovSim.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.movsim.simulator.roadsegment;

import java.util.ArrayList;

import org.movsim.simulator.vehicles.Vehicle;

public class LaneSegment {
    private static final boolean DEBUG = false;
    // Lane linkage
    private final RoadSegment roadSegment;
    private LaneSegment sinkLaneSegment;
    private LaneSegment sourceLaneSegment;
    private final int lane;
    private Lane.Type type;
    private static final int VEHICLES_PER_LANE_INITIAL_SIZE = 50;
    final ArrayList<Vehicle> vehicles;
    private int removedVehicleCount; // used for calculating traffic flow
   
    LaneSegment(RoadSegment roadSegment, int lane) {
    	this.roadSegment = roadSegment;
    	this.lane = lane;
        vehicles = new ArrayList<Vehicle>(VEHICLES_PER_LANE_INITIAL_SIZE);
        type = Lane.Type.TRAFFIC;
    }

    /**
     * Returns the lane.
     * 
     * @return lane
     */
    public final int lane() {
        return lane;
    }

    /**
     * Sets the type of the lane.
     * 
     * @param type
     */
    public final void setType(Lane.Type type) {
        this.type = type;
        if (type == Lane.Type.ENTRANCE) {
            setSinkLaneSegment(null);
        }
    }

    /**
     * Returns the type of the lane.
     * 
     * @return type of lane
     */
    public final Lane.Type type() {
        return type;
    }

    /**
     * Returns the road segment for the lane.
     * 
     * @return road segment
     */
    public final RoadSegment roadSegment() {
        return roadSegment;
    }

    /**
     * Returns the length of the lane.
     * 
     * @return length of lane
     */
    public final double roadLength() {
        return roadSegment.roadLength();
    }

    public final void setSourceLaneSegment(LaneSegment sourceLaneSegment) {
        this.sourceLaneSegment = sourceLaneSegment;
    }

    public final LaneSegment sourceLaneSegment() {
        return sourceLaneSegment;
    }

    public final void setSinkLaneSegment(LaneSegment sinkLaneSegment) {
        this.sinkLaneSegment = sinkLaneSegment;
    }

    public final LaneSegment sinkLaneSegment() {
        return sinkLaneSegment;
    }
    /**
     * Clears this road segment of any vehicles.
     */
    public final void clearVehicles() {
        vehicles.clear();
    }

    /**
     * Returns the number of vehicles on this lane segment.
     * 
     * @param lane
     * 
     * @return the number of vehicles on this lane segment
     */
    public final int vehicleCount() {
        return vehicles.size();
    }

    /**
     * <p>
     * Returns the vehicle at the given index.
     * </p>
     * 
     * <p>
     * Vehicles are sorted in order of decreasing position:
     * </p>
     * 
     * <p>
     * V[n+1].pos < V[n].pos < V[n-1].pos ... < V[1].pos < V[0].pos
     * </p>
     * 
     * @param index
     * 
     * @return vehicle at given index
     */
    public Vehicle getVehicle(int index) {
        return vehicles.get(index);
    }

    /**
     * Removes the vehicle at the given index.
     * 
     * @param index
     *            index of vehicle to remove
     */
    @Deprecated
    public void removeVehicle(int index) {
        vehicles.remove(index);
    }

    /**
     * Removes the front vehicle on the given lane.
     * 
     * @param lane
     */
    public void removeFrontVehicleOnLane() {
        if (vehicles.size() > 0) {
            vehicles.remove(0);
        }
    }

    /**
     * Removes any vehicles that have moved past the end of this road segment.
     */
    public void removeVehiclesPastEnd() {
    	final double roadLength = roadSegment.roadLength();
        int vehicleCount = vehicles.size();
        // remove any vehicles that have gone past the end of this road segment
        while (vehicleCount > 0 && vehicles.get(0).posRearBumper() > roadLength) {
            vehicles.remove(0);
            removedVehicleCount++;
            vehicleCount--;
        }
    }

    /**
     * Adds a vehicle to this lane segment.
     * 
     * @param vehicle
     */
    public void addVehicle(Vehicle vehicle) {
        assert vehicle.posRearBumper() >= 0.0;
        assert vehicle.getSpeed() >= 0.0;
        assert vehicle.getLane() == lane;
        final int index = positionBinarySearch(vehicle.posRearBumper());
        if (index < 0) {
            vehicles.add(-index - 1, vehicle);
        } else if (index == 0) {
            vehicles.add(0, vehicle);
        } else {
            // vehicle is in the same position as an existing vehicle - this should not happen
            assert false;
        }
        vehicle.setRoadSegment(roadSegment.id(), roadSegment.roadLength());
        assert laneIsSorted();
    }

    public void appendVehicle(Vehicle vehicle) {
        assert vehicle.posRearBumper() >= 0.0;
        assert vehicle.getSpeed() >= 0.0;
        assert vehicle.getLane() == lane;
//        vehicle.setRoadSegment(id, roadLength);
        assert laneIsSorted();
        if (DEBUG) {
            if (vehicles.size() > 0) {
                final Vehicle lastVehicle = vehicles.get(vehicles.size() - 1);
                if (lastVehicle.posRearBumper() < vehicle.posRearBumper()) {
                    assert false;
                }
            }
        }
        vehicles.add(vehicle);
        assert laneIsSorted();
    }

    /**
     * Returns the rear vehicle.
     * 
     * @return the rear vehicle
     */
    public Vehicle rearVehicle() {
        final int count = vehicles.size();
        if (count > 0) {
            return vehicles.get(count - 1);
        }
        return null;
    }

    /**
     * Finds the vehicle immediately at or behind the given position.
     * 
     * @return reference to the rear vehicle
     */
    public Vehicle rearVehicle(double vehiclePos) {

        final int index = positionBinarySearch(vehiclePos);
        final int insertionPoint = -index - 1;
        if (index >= 0) {
            // exact match found, so return the matched vehicle
            if (index < vehicles.size()) {
                return vehicles.get(index);
            }
        } else {
            // get next vehicle if not past end
            if (insertionPoint < vehicles.size()) {
                return vehicles.get(insertionPoint);
            }
        }
        // index == laneVehicles[lane].size() - 1 || insertionPoint == laneVehicles[lane].size()
        // subject vehicle is rear vehicle on this road segment, so check source road segment
        if (sourceLaneSegment != null) {
            // didn't find a rear vehicle in the current road segment, so
            // check the previous (source) road segment
                final Vehicle sourceFrontVehicle = sourceLaneSegment.frontVehicle();
                if (sourceFrontVehicle != null) {
                    // return a copy of the front vehicle on the source road segment, with its
                    // position set relative to the current road segment
                    final Vehicle rearVehicle = new Vehicle(sourceFrontVehicle);
                    rearVehicle.setPosition(rearVehicle.getPosition() - sourceLaneSegment.roadLength());
                    return rearVehicle;
                }
        }
        return null;
    }

    public Vehicle rearVehicleOnSinkLanePosAdjusted() {

        // subject vehicle is front vehicle on this road segment, so check sink road segment
        if (sinkLaneSegment == null) {
            return null;
        }
        // find the rear vehicle in the sink lane on the sink lane segment
        final Vehicle sinkRearVehicle = sinkLaneSegment.rearVehicle();
        if (sinkRearVehicle == null) {
            return null;
        }
        // return a copy of the rear vehicle on the sink road segment, with its position
        // set relative to the current road segment
        final Vehicle ret = new Vehicle(sinkRearVehicle);
        ret.setPosition(ret.getPosition() + roadSegment.roadLength());
        return ret;
    }

    Vehicle secondLastVehicleOnSinkLanePosAdjusted() {

        // subject vehicle is front vehicle on this lane segment, so check sink lane segment
        if (sinkLaneSegment == null) {
            return null;
        }
        // find the rear vehicle in the sink lane segment
        final int sinkLaneVehicleCount = sinkLaneSegment.vehicleCount();
        if (sinkLaneVehicleCount < 2) {
            // should actually check sinkLane of sinkLane, but as long as sinkLane not
            // outrageously short, the assumption that there is no vehicle is reasonable
            return null;
        }
        final Vehicle vehicle = sinkLaneSegment.getVehicle(sinkLaneVehicleCount - 2);
        // return a copy of the rear vehicle on the sink lane segment, with its position
        // set relative to the current road segment
        final Vehicle ret = new Vehicle(vehicle);
        ret.setPosition(ret.getPosition() + roadSegment.roadLength());
        return ret;
    }

    /**
     * Returns the front vehicle.
     * 
     * @param lane
     * @return the front vehicle
     */
    public Vehicle frontVehicle() {
        if (vehicles.size() > 0) {
            return vehicles.get(0);
        }
        return null;
    }

    /**
     * Finds the vehicle immediately in front of the given position.
     * That is a vehicle such that vehicle.positon() > vehicePos (strictly greater than).
     * The vehicle whose position equals vehiclePos is deemed to be in the rear.
     * 
     * @return reference to the front vehicle
     */
    public Vehicle frontVehicle(double vehiclePos) {

        // index = Collections.binarySearch(vehicles, subjectVehicle, vehiclePositionComparator);
        final int index = positionBinarySearch(vehiclePos);
        final int insertionPoint = -index - 1;
        if (index > 0) {
            // exact match found
            return vehicles.get(index - 1);
        } else if (insertionPoint > 0) {
            return vehicles.get(insertionPoint - 1);
        }
        // index == 0 or insertionPoint == 0
        // subject vehicle is front vehicle on this road segment, so check for vehicles
        // on sink lane segment
        if (sinkLaneSegment != null) {
        // didn't find a front vehicle in the current road segment, so
        // check the next (sink) road segment
            // find the rear vehicle in the sink lane on the sink road segment
            final Vehicle sinkRearVehicle = sinkLaneSegment.rearVehicle();
            if (sinkRearVehicle != null) {
                // return a copy of the rear vehicle on the sink road segment, with its position
                // set relative to the current road segment
                final Vehicle frontVehicle = new Vehicle(sinkRearVehicle);
                frontVehicle.setPosition(frontVehicle.getPosition() + roadSegment.roadLength());
                return frontVehicle;
            }
        }
        return null;
    }

    public final Vehicle frontVehicle(Vehicle vehicle) {
    	return frontVehicle(vehicle.posRearBumper());
    }

    private int positionBinarySearch(double vehiclePos) {
        int low = 0;
        int high = vehicles.size() - 1;

        while (low <= high) {
            final int mid = (low + high) >> 1;
            final double midPos = vehicles.get(mid).posRearBumper();
            // final int compare = Double.compare(midPos, vehiclePos);
            // note vehicles are sorted in reverse order of position
            final int compare = Double.compare(vehiclePos, midPos);
            if (compare < 0) {
                low = mid + 1;
            } else if (compare > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1); // key not found
    }

    /**
     * <p>
     * Update the vehicle positions and velocities by calling vehicle.updatePositionAndVelocity for
     * each vehicle.
     * </p>
     * 
     * <p>
     * If there is a test car, then record its position, velocity etc.
     * </p>
     * 
     * <p>
     * If there is a traffic inhomogeneity, then apply it to each vehicle.
     * </p>
     * 
     * @param dt
     *            simulation time interval
     * @param simulationTime
     * @param iterationCount 
     */
    // TODO ake method in AbstractRoadSection is leaner and some functionality has been moved to the vehicle 
    public void updateVehiclePositionsAndVelocities(double dt, double simulationTime, long iterationCount) {
       assert laneIsSorted();
        // this function may change vehicle ordering in this or another road segment
        // remember V[n+1].pos < V[n].pos < V[n-1].pos ... < V[1].pos < V[0].pos
        // Vehicle iteration loop goes backwards, that is it starts with vehicles nearest
        // the start of this road segment. This is so a vehicle's new speed and position is
        // calculated before the vehicle in front of it has been moved.
        Vehicle frontFrontVehicle = null;
        Vehicle frontVehicle;
        final int count = vehicles.size();
        // TODO refactor this loop so frontVehicle is reused as vehicle and end two cases are
        // unrolled
        for (int i = count - 1; i >= 0; --i) {
            final Vehicle vehicle = vehicles.get(i);
            if (i > 0) {
                frontVehicle = vehicles.get(i - 1);
            } else {
                if (sinkLaneSegment == null) {
                    // no sink lane for this lane, so there are no vehicles ahead of vehicle(0)
                    frontVehicle = null;
                } else {
                    // the front vehicle is the rear vehicle on the sink lane
                    frontVehicle = rearVehicleOnSinkLanePosAdjusted();
                    if (frontVehicle == null) {
                        // no vehicle in the sink lane, so must recursively follow the lanes
                        // to the end of the road
                        frontVehicle = frontVehicle(vehicle.getPosition());
                    }
                }
            }
        }
        // occasionally updatePositionAndVelocity could cause the vehicles array
        // to become unsorted if LongitudinalDriverModel.MAX_DECELERATION >
        // LaneChangeModel.maxSafeBraking, since a new entry into a lane might cause
        // excessive breaking
        // sortVehicles();
        assert laneIsSorted();
    }


    /**
     * If there is a traffic sink, use it to perform any traffic outflow.
     * 
     * @param dt
     *            simulation time interval
     * @param simulationTime
     */
    public void outFlow(double dt, double simulationTime, long iterationCount) {
    	
    }

    /**
     * Returns true if the vehicle array is sorted.
     * 
     * @return true if the vehicle array is sorted
     */
    public boolean laneIsSorted() {
        final int count = vehicles.size();
        if (count > 1) { // if zero or one vehicles in lane then it is necessarily sorted
            Vehicle frontVehicle = vehicles.get(0);
            for (int i = 1; i < count; ++i) {
                final Vehicle vehicle = vehicles.get(i);
                if (frontVehicle.posRearBumper() < vehicle.posRearBumper()) {
                    return false;
                }
                // current vehicle is front vehicle next time around
                frontVehicle = vehicle;
            }
        }
        return true;
    }

    /**
     * Simple bubble sort of the vehicles.
     * Useful for debugging.
     */
    @SuppressWarnings("unused")
    private void sortVehicles() {
        // Collections.sort(vehicles, vehiclePositionComparator);
        final int count = vehicles.size();
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 1; i < count; ++i) {
                final Vehicle front = vehicles.get(i - 1);
                final Vehicle rear = vehicles.get(i);
                if (rear.posRearBumper() > front.posRearBumper()) {
                    sorted = false;
                    // swap the two vehicles
                    vehicles.set(i - 1, rear);
                    vehicles.set(i, front);
                }
            }
        }
    }
}
