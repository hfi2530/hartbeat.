package net.heartbeat.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

public class ToastUtil {
    public static void showUpdatedToast(String name){
        // Get the singleton client instance
        MinecraftClient client = MinecraftClient.getInstance();

        // Fire a "tutorial hint" toast with a title and description
        client.getToastManager().add(
                SystemToast.create(
                        client,
                        SystemToast.Type.PACK_COPY_FAILURE,
                        Text.translatable("gui.heartbeat.title.updated"),
                        Text.translatable("gui.heartbeat.description.updated", name)
                )
        );
    }

    public static void showErrorToast(String name){
        // Get the singleton client instance
        MinecraftClient client = MinecraftClient.getInstance();

        // Fire a "tutorial hint" toast with a title and description
        client.getToastManager().add(
                SystemToast.create(
                        client,
                        SystemToast.Type.PACK_COPY_FAILURE,
                        Text.translatable("gui.heartbeat.title.failure"),
                        Text.translatable("gui.heartbeat.description.failure", name)
                )
        );
    }
}
