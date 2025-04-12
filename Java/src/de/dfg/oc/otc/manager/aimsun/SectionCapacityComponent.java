package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.aid.disturbance.Disturbance;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import de.dfg.oc.otc.aid.disturbance.DisturbanceType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * This component is used for the dynamic calculation of the current capacity of
 * a section. The formulas follow the ones from the U.S. Department of Transportation.
 */
public class SectionCapacityComponent {
    /**
     * Factor for converting km/h to miles/h.
     */
    private final float KMH_TO_MPH = 0.621371192f;
    /**
     * Assigned section of the capacity component.
     */
    private final Section section;
    /**
     * Width of each lane in feet (10 feet equal 3 meters, 6 feet = 2 m).
     * minimum of 8 feet, maximum of 16.
     */
    private final float laneWidth = 10;
    /**
     * Percentage of heavy vehicles (0-100).
     */
    // TODO woher wert nehmen?
    private final int heavyVehiclePercentage = 3;

    public SectionCapacityComponent(final Section section) {
        this.section = section;
    }

    /**
     * Calculates the current capacity of the section.
     */
    public float getCapacity() {
        float capacity;

        // Convert speed limit to miles per hour
        float speedLimit = this.section.getSpeedlimit() * this.KMH_TO_MPH;

        // Choose calculation method
        if (speedLimit > 40) {
            // Highway method
            capacity = calculateHighwayCapacity(speedLimit);
        } else {
            // Urban method
            capacity = calculateUrbanCapacity();
        }

        // Adjust capacity to current disturbances
        // capacity *= getDisturbanceFactor(EnumSet.of(DisturbanceType.INCIDENT, DisturbanceType.LANE_CLOSURE));

        return capacity;
    }

    /**
     * Calculates the capacity for highway sections.
     *
     * @param speedLimit of the section
     * @return Capacity of the section
     * @see <a href="www.fhwa.dot.gov/ohim/hpmsmanl/appn2.cfm">Multilane Highway Capacity</a>
     */
    private float calculateHighwayCapacity(float speedLimit) {
        final float BFFS = getHighwayBaseFreeFlowSpeed(speedLimit);
        final float laneWidthFactor = getHighwayLaneWidthFactor();
        final float heavyVehicleFactor = getHeavyVehicleFactor();

        final float FFS = BFFS - laneWidthFactor;

        final float laneCapacity;
        if (FFS <= 60) {
            laneCapacity = 1000 + 20 * FFS;
        } else {
            laneCapacity = 2200;
        }

        return laneCapacity * section.getNumberOfLanes() * heavyVehicleFactor;
    }

    /**
     * Calculates the base free flow speed for highways.
     *
     * @param speedLimit of the section
     * @return BFFS Base free flow speed for highways
     */
    private float getHighwayBaseFreeFlowSpeed(float speedLimit) {
        float BFFS = speedLimit * getDisturbanceFactor(EnumSet.of(DisturbanceType.SPEED_CHANGE));

        if (speedLimit >= 40 && speedLimit < 50) {
            BFFS += 7;
        } else if (speedLimit >= 50) {
            BFFS += 5;
        }

        return BFFS;
    }

    /**
     * Calculates the lane width adjustment factor for highway capacity.
     */
    private float getHighwayLaneWidthFactor() {
        if (this.laneWidth == 11) {
            return 1.9f;
        } else if (this.laneWidth <= 10) {
            return 6.6f;
        }
        return 0;
    }

    /**
     * Calculates the heavy vehicle adjustment factor.
     */
    private float getHeavyVehicleFactor() {
        return 100f / (100 + this.heavyVehiclePercentage);
    }

    /**
     * Calculates the capacity for urban sections.
     *
     * @return Capacity of the section
     * @see <a href="www.fhwa.dot.gov/ohim/hpmsmanl/appn7.cfm">Urban One/Two/Three Lane Highway Capacity</a>
     */
    private float calculateUrbanCapacity() {
        final float laneWidthFactor = getUrbanLaneWidthFactor();
        final float heavyVehicleFactor = getHeavyVehicleFactor();
        // Default Peak hour factor for urban sections
        final float PHF = 0.92f;
        final float parkingActivityFactor = calculateParkingActivityFactor();
        final float areaTypeFactor = .991f;

        return section.getBaseCapacity() * laneWidthFactor * heavyVehicleFactor * parkingActivityFactor * areaTypeFactor * PHF;
    }

    private float calculateParkingActivityFactor() {
        int parkingLanes = 0;

        if (parkingLanes == 0) {
            return 1;
        }

        // 12 for two-way streets with parking on both sides or one-way streets with parking on one side
        int parkingManeuverFactor = 12;
        return (parkingLanes - 0.1f - 18 * parkingManeuverFactor / 3600) / parkingLanes;
    }

    /**
     * Calculates the lane width factor for urban capacity.
     */
    private float getUrbanLaneWidthFactor() {
        return 1 + (this.laneWidth - 12) / 30;
    }

    /**
     * Calculates the total degree of all section disturbances for that
     * particular section.
     */
    private float getDisturbanceFactor(Collection<DisturbanceType> sectionDisturbanceTypes) {
        List<Disturbance> disturbances = DisturbanceManager.getInstance().getDisturbancesForLink(this.section.getId());
        if (disturbances.isEmpty()) {
            return 1;
        }

        float totalDisturbanceDegree = 0;
        int disturbanceCount = 0;

        // Summarize degrees of each disturbance
        for (Disturbance disturbance : disturbances) {
            if (sectionDisturbanceTypes.contains(disturbance.getType())) {
                totalDisturbanceDegree += disturbance.getDegree();
                disturbanceCount++;
            }
        }

        if (!disturbances.isEmpty()) {
            return 1 - totalDisturbanceDegree / disturbanceCount;
        }

        return 1;
    }
}
