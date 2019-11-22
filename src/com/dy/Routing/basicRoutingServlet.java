package com.dy.Routing;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dy.GraphHopper.GraphHopperInstance;
import com.dy.GraphHopper.WebHelper;
import com.dy.Util.ServletUtil;
import com.dy.Util.TypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.exceptions.ConnectionNotFoundException;
import com.graphhopper.util.exceptions.PointNotFoundException;
import com.graphhopper.util.shapes.GHPoint;

/**
 * Servlet implementation class basicRoutingServlet
 */
@WebServlet("/basicRoutingServlet")
public class basicRoutingServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public basicRoutingServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		GraphHopper hopper = GraphHopperInstance.getInstance();
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
		String highway = null;
		if (data != null) {
			JsonNode tmp = data.get("weighting");
			if (tmp != null) {
				highway = tmp.textValue();
			}
		} else {
			highway = (String) ServletUtil.getRequestParameter(request, "weighting");
		}

		List<GHPoint> points = WebHelper.decodePoints(position);
		String vehicle = (String) basicIsoChroneServlet.loadValue("vehicle", data, "generic", TypeEnum.String, request);
		GHRequest req = new GHRequest(points).setVehicle(vehicle).setWeighting("fastest")
				.setLocale(Locale.SIMPLIFIED_CHINESE);
		if (points.size() == 2) {
			req.setAlgorithm("alternative_route");
			String maxPath = (String) ServletUtil.getRequestParameter(request, "maxPath");
			if (maxPath == null) {
				maxPath = "3";
			}
			GraphHopperInstance.setMaxPaths(req, maxPath);
		}
		if (highway != null && !highway.equals("")) {
			req.setWeighting(highway);
		}
		String blockArea = null;
		if (data != null) {
			JsonNode tmp = data.get("blockArea");
			if (tmp != null) {
				blockArea = tmp.textValue();
			}
		} else {
			blockArea = (String) ServletUtil.getRequestParameter(request, "blockArea");
		}

		if (blockArea != null) {
			GraphHopperInstance.setBlock_area(req, blockArea);
		}
		GHResponse rsp = hopper.route(req);
		if (rsp.hasErrors()) {
			Object[] arr = rsp.getErrors().toArray();
			StringBuilder ret = new StringBuilder();
			ret.append("{\"errors\":[");
			for (int i = 0; i < arr.length; i++) {
				if (i != arr.length - 1) {
					if (arr[i] instanceof PointNotFoundException) {
						int index = ((PointNotFoundException) arr[i]).getPointIndex();
						ret.append("{\"pointIndex\":");
						ret.append(index);
						ret.append("}");
					} else if (arr[i] instanceof ConnectionNotFoundException) {
						ret.append("{\"NotFound\":true");
						ret.append("}");
					} else {
						ret.append("\"");
						ret.append(arr[i].toString());
						ret.append("\"");
					}
					ret.append(",");
				} else {
					if (arr[i] instanceof PointNotFoundException) {
						int index = ((PointNotFoundException) arr[i]).getPointIndex();
						ret.append("{\"pointIndex\":");
						ret.append(index);
						ret.append("}");
					} else if (arr[i] instanceof ConnectionNotFoundException) {
						ret.append("{\"NotFound\":true");
						ret.append("}");
					} else {
						ret.append("\"");
						ret.append(arr[i].toString());
						ret.append("\"");
					}
				}
			}
			ret.append("]}");
			response.getWriter().write(ret.toString());
			response.getWriter().flush();
			response.getWriter().close();
			return;
		}
		ObjectNode node = WebHelper.jsonObject(rsp, hopper.getEncodingManager().isEnableInstructions(), true, true,
				false, 121.0f);
		response.getWriter().write(node.toString());
		response.getWriter().flush();
		response.getWriter().close();
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
