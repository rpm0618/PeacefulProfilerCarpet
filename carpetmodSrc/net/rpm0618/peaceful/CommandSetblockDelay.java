package net.rpm0618.peaceful;

import carpet.commands.CommandCarpetBase;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class CommandSetblockDelay extends CommandCarpetBase {

    private static class DelayedBlock implements Comparable<DelayedBlock> {
        int targetTick;
        BlockPos pos;
        Block block;

        public DelayedBlock(int targetTick, BlockPos pos, Block block) {
            this.targetTick = targetTick;
            this.pos = pos;
            this.block = block;
        }

        @Override
        public int compareTo(DelayedBlock o) {
            return Integer.compare(targetTick, o.targetTick);
        }
    }

    private static final HashMap<World, PriorityQueue<DelayedBlock>> QUEUE = new HashMap<>();
    private static @Nullable MinecraftServer SERVER;

    public static void tick(World world) {
        if (SERVER == null) return;

        PriorityQueue<DelayedBlock> queue = QUEUE.get(world);
        if (queue == null) return;

        while (!queue.isEmpty() && queue.peek().targetTick <= SERVER.getTickCounter()) {
            DelayedBlock delayedBlock = queue.poll();
            world.setBlockState(delayedBlock.pos, delayedBlock.block.getDefaultState());
        }
    }

    @Override
    public String getName() {
        return "setBlockDelay";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/setBlockDelay <delay> <x> <y> <z> <block>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (SERVER == null) {
            SERVER = server;
        }

        if (args.length < 5) {
            throw new WrongUsageException(getUsage(sender));
        }
        int delay = CommandBase.parseInt(args[0]);
        BlockPos pos = CommandBase.parseBlockPos(sender, args, 1, false);
        Block block = CommandBase.getBlockByText(sender, args[4]);

        int targetTick = server.getTickCounter() + delay;
        PriorityQueue<DelayedBlock> queue = QUEUE.computeIfAbsent(sender.getEntityWorld(), k -> new PriorityQueue<>());
        queue.add(new DelayedBlock(targetTick, pos, block));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length > 1 && args.length <= 4) {
            return getTabCompletionCoordinate(args, 1, targetPos);
        } else if (args.length == 5) {
            return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
        } else {
            return Collections.emptyList();
        }
    }
}
