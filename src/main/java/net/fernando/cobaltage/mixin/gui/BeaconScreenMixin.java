package net.fernando.cobaltage.mixin.gui;

import net.fernando.cobaltage.item.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends HandledScreen<BeaconScreenHandler> {

    @Unique
    private static final Identifier BEACON_CUSTOM_GUI_BACKGROUND_LIGHT = Identifier.of("cobaltage", "textures/gui/beacon.png");

    public BeaconScreenMixin(BeaconScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Redirect(
            method = "drawBackground",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/BeaconScreen;TEXTURE:Lnet/minecraft/util/Identifier;")
    )
    private Identifier useMyCustomTexture() {
        return BEACON_CUSTOM_GUI_BACKGROUND_LIGHT;
    }

    // Intercepting the exact moment before vanilla drawItem is called
    @Inject(
            method = "drawBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;II)V", ordinal = 0),
            cancellable = true
    )
    protected void drawCustomBackgroundItems(DrawContext context, float deltaTicks, int mouseX, int mouseY, CallbackInfo ci) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        // Custom Item Display made by KanieOutis
        context.drawItem(new ItemStack(Items.NETHERITE_INGOT), i + 7, j + 109);
        context.drawItem(new ItemStack(Items.EMERALD),         i + 28, j + 109);
        context.drawItem(new ItemStack(Items.DIAMOND),         i + 49, j + 109);
        context.drawItem(new ItemStack(ModItems.COBALT_INGOT),      i + 70, j + 109);
        context.drawItem(new ItemStack(Items.GOLD_INGOT),      i + 91, j + 109);
        context.drawItem(new ItemStack(Items.IRON_INGOT), i + 112, j + 109);

        // Blocking drawBackground execution to prevent the drawing of original ingots over custom ones
        ci.cancel();
    }
}