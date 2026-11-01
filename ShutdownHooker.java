package net.heartbeat;

// AND FOR THIS WE DEPEND ON THE FABRIC API
// WHY DO YOU HAVE TO DO THIS TO ME
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.heartbeat.util.EmbeddedExtractor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class ShutdownHooker {
    // We install a shutdown hook for this reason:
    // Windows holds a lock on files that are used, so when the JVM loads the mod JARs it locks them, so that they cannot be deleted
    // Therefore we have to wait until the lock is released (which happens when the JVM terminates)
    // But then we can't execute code anymore, so we spawn a detached Process that handles the deletion of files.
    // There is a Path.toFile().deleteOnExit(), but ChatGPT (the almighty) told that the Lock may still not be released when this executes,
    // so we can't use that (and I had the shutdown hook already set up when I learnt about the method ¯\_(ツ)_/¯)
    public static void install() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                String pid = String.valueOf(ProcessHandle.current().pid());

                // This may not show up in logs
                Heartbeat.LOGGER.info("Starting Replacer... ");

                // Get os specifics
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

                // Args passed
                Path absoluteJarPath = Path.of(".\\.heartbeat\\" + EmbeddedExtractor.REPLACER_JAR_FILE_NAME).toAbsolutePath();
                String gameDir = FabricLoader.getInstance().getGameDir().toString();
                String removableJars = HeartbeatClient.getRemovableJarsString();

                List<String> cmd;

                if (os.contains("win")){
                    String javaHome = System.getProperty("java.home");
                    Path javaBin = Paths.get(javaHome, "bin", "javaw.exe");

                    cmd = List.of(
                            "cmd.exe", "/c", "start", "", "/b", // no new console window
                            "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-Command",
                            // hide the spawned process window as well
                            "Start-Process -WindowStyle Hidden -FilePath '" + javaBin +
                                    // 1st Arg: JVM process ID
                                    // 2nd Arg: Running dir of Game
                                    // 3rd Arg: String of jars that should be deleted ("mod1.jar,mod2.jar")
                                    "' -ArgumentList @('-jar','" +
                                    absoluteJarPath + "','" + pid + "','" + gameDir + "','" + removableJars + "')"
                    );

                } else if (os.contains("mac") || os.contains("darwin")) {
                    String javaHome = System.getProperty("java.home");
                    String javaCmd  = Paths.get(javaHome, "bin", "java").toString();
                    cmd = List.of(javaCmd, "-jar", absoluteJarPath.toString(), pid, gameDir, removableJars);

                } else if (os.contains("nux") || os.contains("nix")) {
                    String javaHome = System.getProperty("java.home");
                    String javaCmd  = Paths.get(javaHome, "bin", "java").toString();
                    cmd = List.of(javaCmd, "-jar", absoluteJarPath.toString(), pid, gameDir, removableJars);

                } else {
                    String javaHome = System.getProperty("java.home");
                    String javaCmd  = Paths.get(javaHome, "bin", "java").toString();
                    cmd = List.of(javaCmd, "-jar", absoluteJarPath.toString(), pid, gameDir, removableJars);
                }

                // Launch DETACHED so it survives after JVM exits
                new ProcessBuilder(cmd)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

            } catch (Exception e) {
                // This may not show up in logs
                Heartbeat.LOGGER.error("Failed to start Replacer: ", e);
            }
        });
    }
}