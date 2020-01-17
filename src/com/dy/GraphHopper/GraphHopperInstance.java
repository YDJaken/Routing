package com.dy.GraphHopper;

import java.io.File;
import java.io.Serializable;

import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.DefaultFlagEncoderFactory;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Parameters.Routing;

public class GraphHopperInstance implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3244935579291336567L;
	private static GraphHopper hopper;
	static final double highWaySpeed = 80.0;
	static final String roadStatusURL = "http://localhost:8080/Routing/roudStatusServlet";

	private static void init() {
		GraphHopperInstance.setGraphHopperLocation(
				File.separator.equals("/") ? "/data2/DESP_DATA/GraphHopper" : "D:\\Data\\PBF\\GraphHopper");
		GraphHopperInstance.setCHEnabled(false);
		GraphHopperInstance.setMaxVisitedNodes(1000000);
		GraphHopperInstance.setEncodingManager("generic,car,foot,bike");
		GraphHopperInstance.setWayPointMaxDistance(1000000);
		GraphHopperInstance.setCHThreads(3);
		GraphHopperInstance.setDataReaderFile(
				File.separator.equals("/") ? "/data2/DESP_DATA/GraphHopper/china.osm.pbf" : "D:\\Data\\PBF\\china\\china.osm.pbf");
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
	public static void setEncodingManager(String manager) {
		GraphHopperInstance.setEncodingManager(false, "cn", manager);
	}

	private static void setEncodingManager(boolean enableInstructions, String language, String manager) {
		EncodingManager.Builder builder = GHUtility.addDefaultEncodedValues(new EncodingManager.Builder(8));
		builder.addAll(new DefaultFlagEncoderFactory(), manager);
		builder.setEnableInstructions(enableInstructions);
		builder.setPreferredLanguage(language);
		hopper.setEncodingManager(builder.build());
	}
}
