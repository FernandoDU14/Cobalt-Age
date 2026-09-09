package net.cobaltmc.cobaltage.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.cobaltmc.cobaltage.command.CobaltAgeCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Commands.class})
public class CommandMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(
            method = {"<init>"},
            at = {@At(
                    value = "INVOKE",
                    shift = Shift.BEFORE,
                    target = "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"
            )}
    )
    private void alternate_current$registerCommands(Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo ci) {
        CobaltAgeCommand.register(this.dispatcher);
    }
}