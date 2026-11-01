package net.heartbeat.replacer;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    public static final int REPLACER_VERSION = 1;

    // 1st Arg: pid of jvm
    // 2nd Arg: Running dir of Game
    // 3rd Arg: String of jars that should be deleted ("mod1.jar,mod2.jar")
    public static void main(String[] args) {
        long pid = Long.parseLong(args[0]);

        // 10min until we time out
        // During testing we didn't even need to wait for JVM pid to not exist anymore, but on slow PC's this could be needed.
        if(!waitForPidExit(pid, Duration.ofMinutes(10)))
            return;

        Path gameDir = Path.of(args[1]);
        Path dotHeartbeatDir = gameDir.resolve(".heartbeat");
        Path updatesDir = dotHeartbeatDir.resolve("updates");
        Path modsDir = gameDir.resolve("mods");

        // If "" is passed as an arg it does not count as a string, so we have to account for that
        String[] toBeDeletedJarNames;
        if(args.length == 3)
            toBeDeletedJarNames = args[2].split(",");
        else
            toBeDeletedJarNames = new String[0];

        // Copy every new version jar from updatesDir to modsDir
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(updatesDir)) {
            for (Path file : stream) {
                // Partial Download, just delete
                if(file.getFileName().toString().endsWith(".part")) {
                    // Ignore Result
                    boolean _delete = file.toFile().delete();
                } else if (file.getFileName().toString().endsWith(".jar")) {
                    Path dest = modsDir.resolve(file.getFileName());
                    Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Moved: " + file + " -> " + dest);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to move file: " + e);
        }

        // Delete every jar that should be deleted:
        for(String jarName : toBeDeletedJarNames){
            Path file = modsDir.resolve(jarName);
            boolean success = file.toFile().delete();

            if(success)
                System.out.println("Deleted old JAR '" + jarName + "'");
            else
                System.out.println("Couldn't delete old JAR '" + jarName + "'");
        }
    }

    public static boolean waitForPidExit(long pid, Duration timeout){
        Optional<ProcessHandle> opt = ProcessHandle.of(pid);

        // If the PID doesn't exist or is already dead, we're done.
        if (opt.isEmpty() || !opt.get().isAlive()) return true;

        ProcessHandle ph = opt.get();

        try {
            if (timeout == null || timeout.isZero() || timeout.isNegative())
                ph.onExit().join();
            else
                ph.onExit().get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            return true;
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            return false;
        }
    }
}
