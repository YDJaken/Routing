package com.Speed3D.Routing;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class basicRoutingServlet
 */
@WebServlet("/roudStatusServlet")
public class roudStatusServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public roudStatusServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String wayPoint = request.getParameter("wayPoint");
		if(wayPoint == null) {
			wayPoint = (String)request.getAttribute("wayPoint");
			if(wayPoint == null) {
				wayPoint = request.getHeader("wayPoint");
			}
		}
		response.getWriter().append("1.0");
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
