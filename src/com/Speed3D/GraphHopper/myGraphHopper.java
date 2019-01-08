package com.Speed3D.GraphHopper;

import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.DataFlagEncoder;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;

public class myGraphHopper extends GraphHopperOSM {

	@Override
	public Weighting createWeighting(HintsMap hintsMap, FlagEncoder encoder, Graph graph) {
		String weightingStr = hintsMap.getWeighting();
		switch (weightingStr) {
		case "highway":
			return new highWayWeighting(hintsMap, (DataFlagEncoder) encoder);
		case "roadstatus":
			return new roadStatusWeighting(encoder,hintsMap);
		default:
			return super.createWeighting(hintsMap, encoder, graph);
		}
	}
}
