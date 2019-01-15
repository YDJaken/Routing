package com.Speed3D.GraphHopper;

import java.io.UnsupportedEncodingException;

import com.dy.Util.HttpRequestUtil;
import com.graphhopper.routing.util.DataFlagEncoder;
import com.graphhopper.routing.util.DataFlagEncoder.WeightingConfig;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.AbstractWeighting;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters.Routing;

public class roadStatusWeighting extends AbstractWeighting {

	private final double maxSpeed;
	private final double headingPenalty;
	private final long headingPenaltyMillis;
	private final PMap map;
	private WeightingConfig dataConfig;

	public roadStatusWeighting(FlagEncoder encoder, PMap map) {
		super(encoder);
		this.headingPenalty = map.getDouble(Routing.HEADING_PENALTY, Routing.DEFAULT_HEADING_PENALTY);
		this.headingPenaltyMillis = Math.round(this.headingPenalty * 1000);
		this.map = map;
		if (encoder instanceof DataFlagEncoder) {
			this.dataConfig = ((DataFlagEncoder) encoder).createWeightingConfig(map);
			this.maxSpeed = dataConfig.getMaxSpecifiedSpeed() / highWayWeighting.SPEED_CONV;
		} else {
			this.maxSpeed = encoder.getMaxSpeed() / highWayWeighting.SPEED_CONV;
		}
	}

	public roadStatusWeighting(FlagEncoder encoder) {
		this(encoder, new HintsMap(0));
	}

	@Override
	public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
		double test = 1.0;
		try {
			byte[] ret = HttpRequestUtil.getRoadStatus(GraphHopperInstance.roadStatusURL,
					WebHelper.toGeojson(edgeState.fetchWayGeometry(3), false));
			if (ret == null) {
				test = 1.0;
			} else {
				test = Double.parseDouble(new String(ret, "UTF-8"));
			}
		} catch (NumberFormatException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		double speed = 0.0;
		if (flagEncoder instanceof DataFlagEncoder) {
			speed = dataConfig.getSpeed(edgeState);
		} else {
			speed = reverse ? flagEncoder.getReverseSpeed(edgeState.getFlags())
					: flagEncoder.getSpeed(edgeState.getFlags());
		}
		speed *= test;
		if (speed == 0)
			return Double.POSITIVE_INFINITY;
		double time = edgeState.getDistance() / speed * highWayWeighting.SPEED_CONV;
		// add direction penalties at start/stop/via points
		boolean unfavoredEdge = edgeState.getBool(EdgeIteratorState.K_UNFAVORED_EDGE, false);
		if (unfavoredEdge)
			time += headingPenalty;

		return time;
	}

	@Override
	public long calcMillis(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
		if (flagEncoder instanceof DataFlagEncoder) {
			double speed = dataConfig.getSpeed(edgeState);
			if (speed == 0)
				return Long.MAX_VALUE;

			double maxspeed = ((DataFlagEncoder) flagEncoder).getMaxspeed(edgeState,
					((DataFlagEncoder) flagEncoder).getAccessType("motor_vehicle"), reverse);
			if (maxspeed > 0 && speed > maxspeed)
				speed = maxspeed;

			long timeInMillis = (long) (edgeState.getDistance() / speed * highWayWeighting.SPEED_CONV);

			boolean unfavoredEdge = edgeState.getBool(EdgeIteratorState.K_UNFAVORED_EDGE, false);
			if (unfavoredEdge)
				timeInMillis += Math
						.round(this.map.getDouble(Routing.HEADING_PENALTY, Routing.DEFAULT_HEADING_PENALTY) * 1000);

			if (timeInMillis < 0)
				throw new IllegalStateException(
						"Some problem with weight calculation: time:" + timeInMillis + ", speed:" + speed);

			return timeInMillis;
		} else {
			long time = 0;
			boolean unfavoredEdge = edgeState.getBool(EdgeIteratorState.K_UNFAVORED_EDGE, false);
			if (unfavoredEdge)
				time += headingPenaltyMillis;
			return time + super.calcMillis(edgeState, reverse, prevOrNextEdgeId);
		}
	}

	@Override
	public String getName() {
		return "roadstatus";
	}

	@Override
	public double getMinWeight(double arg0) {
		return arg0 / maxSpeed;
	}

}
