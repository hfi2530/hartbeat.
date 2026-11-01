package net.heartbeat.util;

import net.heartbeat.Heartbeat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class HashUtil {
    // Vibe coded, it's fine though
    public static String sha1(Path jar){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            try (var in = Files.newInputStream(jar)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) md.update(buf, 0, r);
            }

            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder(digest.length * 2);

            for (byte b : digest)
                sb.append(String.format("%02x", b));

            return sb.toString();
        }
        catch(Exception ex){
            Heartbeat.LOGGER.error("Error while computing sha1 for '{}': {}", jar, ex);
            return "";
        }
    }
}
