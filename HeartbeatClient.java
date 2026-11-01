package net.heartbeat;

import net.fabricmc.api.ClientModInitializer;
import net.heartbeat.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class HeartbeatClient implements ClientModInitializer {
	private static String removableJarsString = "";
	public static List<String> updatedMods = new ArrayList<>();

	@Override
	public void onInitializeClient() {
		// Initialize dirs & jar
		FileUtil.initialize();

		// Install Shutdown Hook
		ShutdownHooker.install();
	}

	public static void markJarAsRemovable(String jar){
		removableJarsString += "," + jar;
	}

	public static String getRemovableJarsString(){
		return removableJarsString;
	}

}