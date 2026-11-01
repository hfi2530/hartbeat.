package net.heartbeat.util;

import net.fabricmc.loader.api.FabricLoader;
import net.heartbeat.Heartbeat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
    public static Path getDefaultPath(){
        return FabricLoader.getInstance().getGameDir().resolve("." + Heartbeat.MOD_ID);
    }

    public static Path getUpdatePath(){
        return getDefaultPath().resolve("updates");
    }

    public static void initialize() {
        try{
            // Create default dir (if not exists)
            if(!Files.exists(getDefaultPath()))
                Files.createDirectory(getDefaultPath());

            // Create update dir (if not exists)
            if(!Files.exists(getUpdatePath()))
                Files.createDirectory(getUpdatePath());

            // Extract replacer JAR to defaultPath
            EmbeddedExtractor.extractReplacerJar(getDefaultPath());
        } catch (IOException e) {
            Heartbeat.LOGGER.error("Ran into error while creating default path: {0}", e);
        }
    }
}
