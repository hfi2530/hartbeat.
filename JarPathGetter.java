package net.heartbeat.util;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Optional;

public class JarPathGetter {
    public static Optional<Path> get(String modId){
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getOrigin().getPaths().getFirst());
    }
}
