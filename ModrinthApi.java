package net.heartbeat.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.heartbeat.Heartbeat;
import net.heartbeat.HeartbeatClient;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModrinthApi {
    public CompletableFuture<Optional<HeartbeatUpdateInfo>> getUpdateLink(String hash){
        // Construct body
        JsonObject body = constructUpdateLinkBody(hash);

        // send request
        CompletableFuture<String> res = sendHttpQuery(body.toString());

        // handle result
        return res.thenApply(resBody -> {
            // See https://docs.modrinth.com/api/operations/getlatestversionsfromhashes/
            JsonObject root = JsonParser.parseString(resBody).getAsJsonObject();

            JsonObject version = root.getAsJsonObject(hash);
            JsonObject file = version.get("files").getAsJsonArray().get(0).getAsJsonObject();

            String newestHash = file.get("hashes").getAsJsonObject().get("sha1").getAsString();
            String downloadUrl = file.get("url").getAsString();
            String fileName = file.get("filename").getAsString();

            var info = new HeartbeatUpdateInfo(downloadUrl, fileName);

            if(!java.util.Objects.equals(hash, newestHash))
                return Optional.of(info);
            else
                return Optional.empty();
        });
    }

    public void download(String downloadUri, String fileName, ModInfo info){
        Heartbeat.LOGGER.info("Downloading update for '{}' from '{}'({})", info.modName(), downloadUri, fileName);

        HttpClient client = HttpClient.newHttpClient();

        Path targetPath;
        Path tmpPath;

        try{
            targetPath = FileUtil.getUpdatePath().resolve(fileName);
            tmpPath = Files.createTempFile(FileUtil.getUpdatePath(), fileName, ".part");
        }
        catch(Exception ex){
            Heartbeat.LOGGER.error("Failed reserving temp file for '{}' from '{}'({})", info.modName(), downloadUri, fileName);
            return;
        }


        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(downloadUri))
                .build();

        // async download
        CompletableFuture<HttpResponse<Path>> future =
                client.sendAsync(req, HttpResponse.BodyHandlers.ofFile(tmpPath));

        future.thenAccept(resp -> {
            if (resp.statusCode() == 200) {
                // Rename .part
                try {
                    Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Heartbeat.LOGGER.error("Failed renaming .part ({}): {}", fileName, ex);
                }

                // Mark that this mod's old jar should be removed on restart
                HeartbeatClient.markJarAsRemovable(info.jarName());

                // Display Toast
                // This is the reason for the ModInfo class
                // We don't get the name of the mod from request as that only includes version info,
                // so we just store the name as well (too bad we don't get tuples)
                MinecraftClient.getInstance().execute(() -> ToastUtil.showUpdatedToast(info.modName()));
            }
            else{
                // Display Error Toast
                MinecraftClient.getInstance().execute(() -> ToastUtil.showErrorToast(info.modName()));
            }
        });
    }

    private JsonObject constructUpdateLinkBody(String hash){
        // Construct the HTTP Request Body
        JsonObject body = new JsonObject();

        // add Hashes to body
        JsonArray hashes = new JsonArray();
        hashes.add(hash);

        body.add("hashes", hashes);

        // add algorithm
        body.addProperty("algorithm", "sha1");

        // add loaders
        JsonArray loaders = new JsonArray();
        loaders.add("fabric");
        body.add("loaders", loaders);

        // add game version
        JsonArray versions = new JsonArray();
        versions.add(FabricLoader.getInstance().getRawGameVersion());
        body.add("game_versions", versions);

        return body;
    }

    private CompletableFuture<String> sendHttpQuery(String jsonBody){
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/version_files/update"))
                .header("User-Agent", "MyMod/1.0 (+github.com/me/my-mod)")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
}
