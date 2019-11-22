package com.dy.GraphHopper;

import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.util.DataFlagEncoder;
import com.graphhopper.routing.util.DataFlagEncoder.WeightingConfig;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.AbstractWeighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Parameters.Routing;

public class highWayWeighting extends AbstractWeighting {

	private final HintsMap hintsMap;
	private final DataFlagEncoder dataflge;
	private final WeightingConfig dataConfig;
	protected final static double SPEED_CONV = 3600;

	public highWayWeighting(HintsMap hintsMap, DataFlagEncoder encoder) {
		super(encoder);
		this.dataflge = encoder;
		this.hintsMap = hintsMap;
		this.dataConfig = this.dataflge.createWeightingConfig(this.hintsMap);
	}

	@Override
	public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
		if (reverse) {
			BooleanEncodedValue type = this.dataflge.getAccessEnc();
			if (!edgeState.getReverse(type)) {
				return Double.POSITIVE_INFINITY;
			} else if (!edgeState.get(type)) {
				return Double.POSITIVE_INFINITY;
			}
		}
		long time = calcMillis(edgeState, reverse, prevOrNextEdgeId);
		if (time == Long.MAX_VALUE)
			return Double.POSITIVE_INFINITY;
		if (dataConfig.getSpeed(edgeState) > GraphHopperInstance.highWaySpeed) {
			return edgeState.getDistance() / 1000.0;
		}
		return edgeState.getDistance();
	}

	@Override
	public long calcMillis(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
		double speed = dataConfig.getSpeed(edgeState);
		if (speed == 0)
			return Long.MAX_VALUE;

		double maxspeed = this.dataflge.getMaxPossibleSpeed();
		if (maxspeed > 0 && speed > maxspeed)
			speed = maxspeed;

		long timeInMillis = (long) (edgeState.getDistance() / speed * SPEED_CONV);

		boolean unfavoredEdge = edgeState.get(EdgeIteratorState.UNFAVORED_EDGE);
		if (unfavoredEdge)
			timeInMillis += Math
					.round(this.hintsMap.getDouble(Routing.HEADING_PENALTY, Routing.DEFAULT_HEADING_PENALTY) * 1000);

		if (timeInMillis < 0)
			throw new IllegalStateException(
					"Some problem with weight calculation: time:" + timeInMillis + ", speed:" + speed);

		return timeInMillis;
	}

	@Override
	public String getName() {
		return "highway";
	}

	@Override
	public double getMinWeight(double arg0) {
		return 0;
	}

}
