package com.Speed3D.Routing;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.Speed3D.GraphHopper.GraphHopperInstance;
import com.Speed3D.GraphHopper.WebHelper;
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
		String position = request.getParameter("position");
		if (position == null)
			return;
		String highway = request.getParameter("weighting");
		List<GHPoint> points = WebHelper.decodePoints(position);
		GHRequest req = new GHRequest(points).setVehicle("generic").setWeighting("generic")
				.setLocale(Locale.SIMPLIFIED_CHINESE);
		if (points.size() == 2) {
			req.setAlgorithm("alternative_route");
			String maxPath = request.getParameter("maxPath");
			if (maxPath == null) {
				maxPath = "3";
			}
			GraphHopperInstance.setMaxPaths(req, maxPath);
		}
		if (highway != null && !highway.equals("")) {
			req.setWeighting(highway);
		}
		String blockArea = request.getParameter("blockArea");
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
		ObjectNode node = WebHelper.jsonObject(rsp, hopper.isEnableInstructions(), true, true, false, 121.0f);
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
