package net.cobaltmc.cobaltage.mixin.client.gui.screens.inventory;

import net.cobaltmc.cobaltage.item.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {

    @Unique
    private static final Identifier BEACON_CUSTOM_GUI_BACKGROUND_LIGHT = Identifier.fromNamespaceAndPath("cobaltage", "textures/gui/beacon.png");

    public BeaconScreenMixin(BeaconMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Redirect(
            method = "extractBackground",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/BeaconScreen;BEACON_LOCATION:Lnet/minecraft/resources/Identifier;", opcode = Opcodes.GETSTATIC)
    )
    private Identifier useMyCustomTexture() {
        return BEACON_CUSTOM_GUI_BACKGROUND_LIGHT;
    }

    @Inject(
            method = "extractBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;II)V", ordinal = 0),
            cancellable = true
    )
    protected void drawCustomBackgroundItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        // Custom Item Display including Cobalt Ingot
        graphics.item(new ItemStack(Items.NETHERITE_INGOT), i + 7, j + 109);
        graphics.item(new ItemStack(Items.EMERALD),         i + 28, j + 109);
        graphics.item(new ItemStack(Items.DIAMOND),         i + 49, j + 109);
        graphics.item(new ItemStack(ModItems.COBALT_INGOT), i + 70, j + 109);
        graphics.item(new ItemStack(Items.GOLD_INGOT),      i + 91, j + 109);
        graphics.item(new ItemStack(Items.IRON_INGOT),      i + 112, j + 109);

        // Cancel vanilla item rendering execution to prevent overlap
        ci.cancel();
    }
}