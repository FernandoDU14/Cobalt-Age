package net.cobaltmc.cobaltage.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.cobaltmc.cobaltage.block.signal.engine.modern.UpdateOrder;
import net.cobaltmc.cobaltage.block.signal.engine.modern.WireHandler;
import net.cobaltmc.cobaltage.util.interfaces.mixin.IServerLevel;
import net.cobaltmc.cobaltage.util.signal.profiler.ProfilerResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;

public class CobaltAgeCommand {
    private static final DynamicCommandExceptionType NO_SUCH_UPDATE_ORDER = new DynamicCommandExceptionType((value) -> Component.literal("no such update order: " + String.valueOf(value)));
    private static final String[] UPDATE_ORDERS;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder builder = (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("cobaltage").requires((source) -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))).executes((context) -> queryEnabled((CommandSourceStack)context.getSource()))).then(Commands.literal("on").executes((context) -> setEnabled((CommandSourceStack)context.getSource(), true)))).then(Commands.literal("off").executes((context) -> setEnabled((CommandSourceStack)context.getSource(), false)))).then(((LiteralArgumentBuilder)Commands.literal("updateOrder").executes((context) -> queryUpdateOrder((CommandSourceStack)context.getSource()))).then(Commands.argument("updateOrder", StringArgumentType.word()).suggests((context, suggestionBuilder) -> SharedSuggestionProvider.suggest(UPDATE_ORDERS, suggestionBuilder)).executes((context) -> setUpdateOrder((CommandSourceStack)context.getSource(), parseUpdateOrder(context, "updateOrder")))))).then(((LiteralArgumentBuilder)Commands.literal("resetProfiler").requires((source) -> false)).executes((context) -> resetProfiler((CommandSourceStack)context.getSource())));
        dispatcher.register(builder);
    }

    private static UpdateOrder parseUpdateOrder(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String value = StringArgumentType.getString(context, name);

        try {
            return UpdateOrder.byId(value);
        } catch (Exception var4) {
            throw NO_SUCH_UPDATE_ORDER.create(name);
        }
    }

    private static int queryEnabled(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WireHandler wireHandler = ((IServerLevel)level).cobaltage$getWireHandler();
        String state = wireHandler.getConfig().getEnabled() ? "enabled" : "disabled";
        source.sendSuccess(() -> Component.literal(String.format("Cobalt Age's Modern Engine is currently %s", state)), false);
        return 1;
    }

    private static int setEnabled(CommandSourceStack source, boolean on) {
        ServerLevel level = source.getLevel();
        WireHandler wireHandler = ((IServerLevel)level).cobaltage$getWireHandler();
        wireHandler.getConfig().setEnabled(on);
        String state = wireHandler.getConfig().getEnabled() ? "enabled" : "disabled";
        source.sendSuccess(() -> Component.literal(String.format("Cobalt Age's Modern Engine has been %s!", state)), true);
        return 1;
    }

    private static int queryUpdateOrder(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WireHandler wireHandler = ((IServerLevel)level).cobaltage$getWireHandler();
        String value = wireHandler.getConfig().getUpdateOrder().id();
        source.sendSuccess(() -> Component.literal(String.format("Update order for signal engine is currently %s", value)), false);
        return 1;
    }

    private static int setUpdateOrder(CommandSourceStack source, UpdateOrder updateOrder) {
        ServerLevel level = source.getLevel();
        WireHandler wireHandler = ((IServerLevel)level).cobaltage$getWireHandler();
        wireHandler.getConfig().setUpdateOrder(updateOrder);
        String value = wireHandler.getConfig().getUpdateOrder().id();
        source.sendSuccess(() -> Component.literal(String.format("update order for signal engine has been set to %s!", value)), true);
        return 1;
    }

    private static int resetProfiler(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("profiler results have been cleared!"), true);
        ProfilerResults.log();
        ProfilerResults.clear();
        return 1;
    }

    static {
        UpdateOrder[] updateOrders = UpdateOrder.values();
        UPDATE_ORDERS = new String[updateOrders.length];

        for(int i = 0; i < updateOrders.length; ++i) {
            UPDATE_ORDERS[i] = updateOrders[i].id();
        }

    }
}