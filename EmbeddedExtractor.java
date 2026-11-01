package net.heartbeat.util;

import net.heartbeat.Heartbeat;
import net.heartbeat.replacer.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.util.Comparator.reverseOrder;

public class EmbeddedExtractor {
    private static final String EMBEDDED_EXACT = "heartbeat-replacer-embedded.jar";
    private static final String META_INF_PATH = "META-INF/jars/" + EMBEDDED_EXACT;
    public static final String REPLACER_JAR_FILE_NAME = "heartbeat-replacer-embedded" + "-v" + Main.REPLACER_VERSION + ".jar";

    private EmbeddedExtractor() {}

    // Extracts the replacer JAR resource and places it into the target Path
    public static void extractReplacerJar(Path target){
        try (InputStream in = EmbeddedExtractor.class.getClassLoader().getResourceAsStream(META_INF_PATH)){
            if (in == null) {
                throw new IOException("Embedded lib jar not found at " + META_INF_PATH);
            }

            Path dst = target.resolve(REPLACER_JAR_FILE_NAME);

            // Current version of replacer is already present
            if(dst.toFile().exists())
                return;

            // Clear dir so only newest version is retained
            clearDir(target);

            Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
        }
        catch(Exception ex){
            Heartbeat.LOGGER.error("Unable to extract replacer JAR from resources: ", ex);
        }
    }

    public static void clearDir(Path dir){
        try{
            Files.walk(dir).skip(1).sorted(reverseOrder()).forEach(p -> {
                try {
                    if(p.getFileName().endsWith(".jar"))
                        Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch(Exception ex){
            Heartbeat.LOGGER.error("Unable to clear dir: {0}", ex);
        }
    }
}
