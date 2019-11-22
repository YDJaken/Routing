package com.dy.Routing;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import com.dy.GraphHopper.GraphHopperInstance;
import com.dy.GraphHopper.WebHelper;
import com.dy.Util.ServletUtil;
import com.dy.Util.TypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GraphHopper;
import com.graphhopper.isochrone.algorithm.Isochrone;
import com.graphhopper.json.geo.JsonFeature;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.isochrone.algorithm.DelaunayTriangulationIsolineBuilder;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Servlet implementation class basicRoutingServlet
 */
@WebServlet("/basicIsoChroneServlet")
public class basicIsoChroneServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final DelaunayTriangulationIsolineBuilder delaunayTriangulationIsolineBuilder = new DelaunayTriangulationIsolineBuilder();
	private static final GeometryFactory geometryFactory = new GeometryFactory();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public basicIsoChroneServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String position = (String) ServletUtil.getRequestParameter(request, "position");
		ObjectNode data = null;
		if (position == null)
			return;
		if (ServletUtil.isJsonString(position)) {
			ObjectMapper mapper = new ObjectMapper();
			data = mapper.readValue(position, ObjectNode.class);
			position = data.get("position").textValue();
			if (position == null) {
				return;
			}
		}

		List<GHPoint> points = WebHelper.decodePoints(position);
		if (points == null || points.size() > 1) {
			return;
		}
		GHPoint point = points.get(0);
		String vehicle = (String) loadValue("vehicle", data, "car", TypeEnum.String, request);
		int Buckets = (int) loadValue("buckets", data, 1, TypeEnum.Int, request);
		if (Buckets > 20 || Buckets < 1) {
			return;
		}
		boolean flow = (boolean) loadValue("flow", data, false, TypeEnum.Boolean, request);
		String result = (String) loadValue("result", data, "polygon", TypeEnum.String, request);
		int timeLimit = (int) loadValue("timeLimit", data, 600, TypeEnum.Int, request);
		int distanceLimit = (int) loadValue("distanceLimit", data, -1, TypeEnum.Int, request);
		GraphHopper graphHopper = GraphHopperInstance.getInstance();
		EncodingManager encodingManager = graphHopper.getEncodingManager();

		if (!encodingManager.hasEncoder(vehicle)) {
			return;
		}

		FlagEncoder encoder = encodingManager.getEncoder(vehicle);
		EdgeFilter edgeFilter = DefaultEdgeFilter.allEdges(encoder);
		LocationIndex locationIndex = graphHopper.getLocationIndex();
		QueryResult qr = locationIndex.findClosest(point.lat, point.lon, edgeFilter);
		if (!qr.isValid()) {
			return;
		}
		Graph graph = graphHopper.getGraphHopperStorage();
		QueryGraph queryGraph = new QueryGraph(graph);
		queryGraph.lookup(Collections.singletonList(qr));

		HintsMap hintsMap = new HintsMap();

		Weighting weighting = graphHopper.createWeighting(hintsMap, encoder, graph);
		Isochrone isochrone = new Isochrone(queryGraph, weighting, flow);

		if (distanceLimit > 0) {
			isochrone.setDistanceLimit(distanceLimit);
		} else {
			isochrone.setTimeLimit(timeLimit);
		}

		List<List<Coordinate>> buckets = isochrone.searchGPS(qr.getClosestNode(), Buckets);
		if (isochrone.getVisitedNodes() > graphHopper.getMaxVisitedNodes() / 5) {
			return;
		}
		StringBuilder builder = new StringBuilder();

		if ("polygon".equalsIgnoreCase(result)) {
			for (List<Coordinate> bucket : buckets) {
				if (bucket.size() < 2) {
					return;
				}
			}
			ArrayList<JsonFeature> features = new ArrayList<>();
			List<Coordinate[]> polygonShells = delaunayTriangulationIsolineBuilder.calcList(buckets,
					buckets.size() - 1);
			for (Coordinate[] polygonShell : polygonShells) {
				JsonFeature feature = new JsonFeature();
				HashMap<String, Object> properties = new HashMap<>();
				properties.put("bucket", features.size());
				properties.put("copyrights", WebHelper.COPYRIGHTS);
				feature.setProperties(properties);
				feature.setGeometry(geometryFactory.createPolygon(polygonShell));
				features.add(feature);
			}
			builder.append("{ \"type\":\"FeatureCollection\",\"features\":[");
			for (int i = 0; i < features.size(); i++) {
				JsonFeature target = features.get(i);
				Geometry tmp = target.getGeometry();
				Coordinate[] pts = tmp.getCoordinates();
				builder.append("{\"type\": \"Polygon\",\"coordinates\":[[");
				for (int j = 0; j < pts.length; j++) {
					Coordinate pt = pts[j];
					builder.append("[" + pt.x + "," + pt.y + "]");
					if (j != pts.length - 1) {
						builder.append(",");
					}
				}
				builder.append("]]}");
				if (i != features.size() - 1)
					builder.append(",");
			}
			builder.append("]}");

		} else if ("pointlist".equalsIgnoreCase(result)) {

			builder.append("{ \"type\":\"FeatureCollection\",\"features\":[");
			for (int j = 0; j < buckets.size(); j++) {
				List<Coordinate> bucket = buckets.get(j);
				if (bucket.size() < 2) {
					return;
				}
				builder.append("{\"type\": \"LineString\",\"coordinates\":[");
				for (int i = 0; i < bucket.size(); i++) {
					Coordinate target = bucket.get(i);
					builder.append("[" + target.x + "," + target.y + "]");
					if (i != bucket.size() - 1) {
						builder.append(",");
					}
				}
				builder.append("]}");
				if (j != buckets.size() - 1)
					builder.append(",");
			}
			builder.append("]}");
		}

		PrintWriter writer = response.getWriter();
		writer.write(builder.toString());
		writer.flush();
		writer.close();
	}

	public static Object loadValue(String ID, ObjectNode data, Object defaultValue, short valueType,
			HttpServletRequest request) {
		Object vehicle = null;
		if (data == null) {
			vehicle = ServletUtil.getRequestParameter(request, ID);
		} else {
			JsonNode tmp = data.get(ID);
			if (tmp != null) {
				switch (valueType) {
				case 0:
					vehicle = tmp.textValue();
					break;
				case 1:
					vehicle = tmp.booleanValue();
					break;
				case 2:
					vehicle = tmp.doubleValue();
					break;
				case 3:
					vehicle = tmp.longValue();
					break;
				case 4:
					vehicle = tmp.intValue();
					break;
				case 5:
					vehicle = tmp.shortValue();
					break;
				case 6:
					vehicle = tmp.floatValue();
					break;
				case 7:
					try {
						vehicle = tmp.binaryValue();
					} catch (IOException e) {
						vehicle = null;
					}
					break;
				}
			}
		}
		return vehicle == null ? defaultValue : vehicle;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
