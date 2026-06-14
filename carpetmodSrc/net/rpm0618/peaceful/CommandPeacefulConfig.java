package net.rpm0618.peaceful;

import carpet.commands.CommandCarpetBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandPeacefulConfig extends CommandCarpetBase {
    @Override
    public String getName() {
        return "peacefulConfig";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "Usage: peacefulConfig <glass | rehash> [<X> <Z>]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "glass":
                    sender.sendMessage(new TextComponentString("GLASS_CHUNK is " + ProfileConfig.GLASS_CHUNK.x + ", " + ProfileConfig.GLASS_CHUNK.z));
                    return;
                case "rehash":
                    sender.sendMessage(new TextComponentString("REHASH_CHUNK is " + ProfileConfig.REHASH_CHUNK.x + ", " + ProfileConfig.REHASH_CHUNK.z));
                    return;
                default:
                    throw new WrongUsageException(getUsage(sender));
            }
        }

        if (args.length != 3) {
            throw new WrongUsageException(getUsage(sender));
        }

        int chunkX = parseChunkPosition(args[1], sender.getPosition().getX());
        int chunkZ = parseChunkPosition(args[2], sender.getPosition().getZ());

        switch (args[0].toLowerCase()) {
            case "glass":
                ProfileConfig.GLASS_CHUNK = new ChunkPos(chunkX, chunkZ);
                sender.sendMessage(new TextComponentString("GLASS_CHUNK set to " + chunkX + ", " + chunkZ));
                return;
            case "rehash":
                ProfileConfig.REHASH_CHUNK = new ChunkPos(chunkX, chunkZ);
                sender.sendMessage(new TextComponentString("REHASH_CHUNK set to " + chunkX + ", " + chunkZ));
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        int chunkX = sender.getPosition().getX() >> 4;
        int chunkZ = sender.getPosition().getZ() >> 4;

        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "glass", "rehash");
        } else if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, Integer.toString(chunkX), "~");
        } else if (args.length == 3) {
            return getListOfStringsMatchingLastWord(args, Integer.toString(chunkZ), "~");
        } else {
            return Collections.emptyList();
        }
    }
}
