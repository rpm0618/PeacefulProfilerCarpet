package net.rpm0618.peaceful;

import carpet.commands.CommandCarpetBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandPeacefulConfig extends CommandCarpetBase {
    @Override
    public String getName() {
        return "peacefulConfig";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/peacefulConfig <subcommand>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

    }
}
