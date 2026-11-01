package net.heartbeat.mixin.client.modmenu;

import com.terraformersmc.modmenu.api.UpdateInfo;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.LegacyTexturedButtonWidget;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.heartbeat.Heartbeat;
import net.heartbeat.HeartbeatClient;
import net.heartbeat.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Objects;

@Mixin(ModsScreen.class)
public class GuiMixin extends Screen {
    protected GuiMixin(Text title) { super(title); }

    @Shadow(remap = false) private ModListEntry selected;
    @Shadow(remap = false) @Final private static int RIGHT_PANE_Y;

    @Unique private ClickableWidget updateButton;
    @Unique private static final Identifier UPDATE_BUTTON_LOCATION = Identifier.of(Heartbeat.MOD_ID, "textures/gui/update_button.png");
    @Unique private static final Text UPDATE_TOOLTIP = Text.translatable("gui.heartbeat.tooltip.update");
    @Unique private static final Text NO_UPDATE_AVAILABLE_TOOLTIP = Text.translatable("gui.heartbeat.tooltip.no_update_available");


    @Inject(method="init", at=@At("HEAD"))
    void onInit(CallbackInfo ci){
        this.updateButton = LegacyTexturedButtonWidget.legacyTexturedBuilder(ScreenTexts.EMPTY, button -> {
                    // Update this mod
                    // I mean it shouldn't be rocket science, right????
                    final Mod mod = Objects.requireNonNull(selected).getMod();
                    final UpdateInfo info = Objects.requireNonNull(mod.getUpdateInfo());
                    final Path path = JarPathGetter.get(mod.getId()).orElseThrow();
                    final String hash = HashUtil.sha1(path);
                    final String fileName = path.getFileName().toString();

                    var api = new ModrinthApi();

                    api.getUpdateLink(hash).thenApply(hUpdateInfo -> {
                        // Already up to date, shouldn't happen
                        if(hUpdateInfo.isEmpty())
                            return 0;

                        var gHUpdateInfo = hUpdateInfo.get();

                        api.download(gHUpdateInfo.downloadURI(), gHUpdateInfo.fileName(), new ModInfo(fileName, mod.getName()));

                        return 1;
                    });

                    // Deactivate Button
                    button.active = false;
                    HeartbeatClient.updatedMods.add(mod.getId());
                })
                .position(width - 24 * 2, RIGHT_PANE_Y)
                .size(20, 20)
                .uv(0, 0, 20)
                .texture(UPDATE_BUTTON_LOCATION, 32, 64)
                .build();

        this.addDrawableChild(this.updateButton);
    }

    // Refresh the update button when selected entry refreshes
    @Inject(method="updateSelectedEntry", at=@At("HEAD"), remap = false)
    void onRefreshEntry(ModListEntry entry, CallbackInfo ci){
        if(entry == null){
            this.updateButton.visible = false;
            return;
        }

        Mod mod = entry.getMod();
        UpdateInfo info = null;

        if(mod.getUpdateInfo() != null)
            info = mod.getUpdateInfo();

        this.updateButton.visible = mod.allowsUpdateChecks();
        this.updateButton.active = info != null && info.isUpdateAvailable() && !HeartbeatClient.updatedMods.contains(entry.mod.getId());
    }

    // Draw Tooltip when we hover update button
    @Inject(method="render", at=@At("TAIL"))
    void onRender(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci){
        if(!updateButton.visible)
            return;

        Text ttr = updateButton.active ? UPDATE_TOOLTIP : NO_UPDATE_AVAILABLE_TOOLTIP;

        // Outside, so don't render
        if(mouseX < width - 24 * 2 ||
                mouseX >= (width - 24 * 2) + 20 ||
                mouseY < RIGHT_PANE_Y ||
                mouseY >= RIGHT_PANE_Y + 20)
            return;

        drawContext.drawTooltip(
                textRenderer,
                ttr,
                mouseX,
                mouseY
        );
    }
}
