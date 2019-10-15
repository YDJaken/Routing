package com.dy.GraphHopper;

import java.io.Serializable;

import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters.Routing;

public class GraphHopperInstance implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3244935579291336567L;
	private static GraphHopper hopper;
	static final double highWaySpeed = 80.0;
	static final String roadStatusURL = "http://192.168.1.15:8080/graphhopperRouting/roudStatusServlet";

	private static void init() {
		GraphHopperInstance.setGraphHopperLocation("/home/dy/Desktop/Data");
		GraphHopperInstance.setCHEnabled(false);
//		GraphHopperInstance.setEnableInstructions(false);
		GraphHopperInstance.setMaxVisitedNodes(1000000);
//		GraphHopperInstance.setPreferredLanguage("cn");
//		GraphHopperInstance.setEncodingManager("generic,car");
		GraphHopperInstance.setWayPointMaxDistance(1000000);
		GraphHopperInstance.setCHThreads(3);
		GraphHopperInstance.setDataReaderFile("/home/dy/Desktop/Data/china.osm.pbf");
		GraphHopperInstance.load();
	}

	// 使用双重校验锁方式实现单例
	public static GraphHopper getInstance() {
		if (hopper == null) {
			synchronized (GraphHopperInstance.class) {
				if (hopper == null) {
					hopper = new myGraphHopper();
					init();
				}
			}
		}
		return hopper;
	}

	// 防止序列化破坏单例模式
	private Object readResolve() {
		return hopper;
	}

	public static void setMaxPaths(GHRequest req, String max) {
		req.getHints().put("alternative_route.max_paths", max);
	}

	public static void setBlock_area(GHRequest req, String block_area) {
		req.getHints().put(Routing.BLOCK_AREA, block_area);
	}

	public static void load() {
		hopper.importOrLoad();
	}

//	public static void setEnableInstructions(boolean enable) {
//		hopper.setEnableInstructions(enable);
//	}

	public static void setElevationProvider(ElevationProvider elevationProvider) {
		hopper.setElevationProvider(elevationProvider);
	}

	public static void setElevation(boolean elevation) {
		hopper.setElevation(elevation);
	}

	public static void setCHThreads(int count) {
		hopper.getCHFactoryDecorator().setPreparationThreads(Math.min(count, 1));
	}

	public static void setWayPointMaxDistance(int distance) {
		hopper.setWayPointMaxDistance(distance);
	}

//	public static void setPreferredLanguage(String language) {
//		hopper.setPreferredLanguage(language);
//	}

	public static void setMaxVisitedNodes(int count) {
		hopper.setMaxVisitedNodes(count);
	}

	@SuppressWarnings("deprecation")
	public static void setCHEnabled(boolean enabled) {
		hopper.setCHEnable(enabled);
		hopper.setCHEnabled(enabled);
	}

	public static void setDataReaderFile(String file) {
		hopper.setDataReaderFile(file);
	}

	public static void setGraphHopperLocation(String folder) {
		hopper.setGraphHopperLocation(folder);
	}

	// 可接受的参数 car,foot,bike,bike2,mtb,racingbike,motorcycle
//	public static void setEncodingManager(String manager) {
//		hopper.setEncodingManager(new EncodingManager(manager, 8));
//	}
}
