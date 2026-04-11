package carpet.commands;


import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import net.rpm0618.peaceful.CustomHashMap;

public class CommandLoadedChunks extends CommandCarpetBase
{
    /**
     * Gets the name of the command
     */
    protected World world;
    public String getUsage(ICommandSender sender)
    {
        return "Usage: loadedChunks <size | key | value>";
    }

    public String getName()
    {
        return "loadedChunks";
    }

    /**
     * Callback for when the command is executed
     */
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (!command_enabled("commandLoadedChunks", sender)) return;
        if (args.length == 0) throw new WrongUsageException(getUsage(sender));

        world = sender.getEntityWorld();

        try {
            switch (args[0]){
                case "size":
                    size(server, sender, args);
                    break;
                case "poke":
                    poke(server, sender, args);
                    break;
                case "search":
                    if (args.length != 3) throw new WrongUsageException(getUsage(sender));
                    search(sender, parseChunkPosition(args[1], sender.getPosition().getX()), parseChunkPosition(args[2], sender.getPosition().getZ()));
                    break;
                case "remove":
                    if (args.length != 3) throw new WrongUsageException(getUsage(sender));
                    remove(sender, parseChunkPosition(args[1], sender.getPosition().getX()), parseChunkPosition(args[2], sender.getPosition().getZ()));
                    break;
                case "add":
                    if (args.length != 3) throw new WrongUsageException(getUsage(sender));
                    add(sender, parseChunkPosition(args[1], sender.getPosition().getX()), parseChunkPosition(args[2], sender.getPosition().getZ()));
                    break;
                case "inspect":
                    inspect(server, sender, args);
                    break;
                case "dump":
                    String fileName = "loadedchunks-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS").format(new Date()) + ".csv";
                    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
                        pw.println("index,key,x,z,hash");
                        long[] keys = (long[]) getPrivateMethods(world, "key");
                        Object[] values = (Object[]) getPrivateMethods(world, "value");
                        int getHashSize = (int) getPrivateMethods(world, "n");
                        for (int i = 0, n = getHashSize; i <= n; i++) {
                            long key = keys[i];
                            Chunk val = (Chunk) values[i];
                            if (val == null) {
                                pw.println(i + ",,,,");
                            } else {
                                pw.printf("%d,%d,%d,%d,%d\n", i, key, val.x, val.z, HashCommon.mix(key) & (n - 1));
                            }
                        }
                        pw.flush();
                    }
                    notifyCommandListener(sender, this, "Written to %s", fileName);
                    break;
                default:
                    throw new WrongUsageException(getUsage(sender));
            }
        }catch (Exception exception){
            exception.printStackTrace();
            throw new CommandException(exception.getMessage());
        }

    }

    private Object getPrivateMethods(World world, String name){
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        CustomHashMap<Chunk> loadedChunks = (CustomHashMap<Chunk>) provider.loadedChunks;
        try {
            Field f = loadedChunks.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(loadedChunks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected CustomHashMap<Chunk> getLoadedChunks (ICommandSender sender){
        world = sender.getEntityWorld();
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        return (CustomHashMap<Chunk>) provider.loadedChunks;
    }

    protected void size(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException, NoSuchFieldException, IllegalAccessException {
        CustomHashMap<Chunk> loadedChunks = this.getLoadedChunks(sender);
        sender.sendMessage(new TextComponentString(String.format("Hashmap size is %d, %.2f (%d|%d)", loadedChunks.size(), getFillLevel(loadedChunks), getMaxField(loadedChunks), getMask(loadedChunks))));
    }

    protected void poke(MinecraftServer server, ICommandSender sender, String[] args) throws IOException {
        String defaultPath = "C:\\Ryan\\Personal\\minecraft\\PeacefulProfilerCarpet\\server\\cluster.csv";

        if (args.length == 1) {
            ArrayList<ChunkPos> chunks = loadCsv(defaultPath);
            for (ChunkPos chunk : chunks) {
                sender.getEntityWorld().getBlockState(new BlockPos(chunk.x << 4, 0, chunk.z << 4), "Poked by command");
            }
            sender.sendMessage(new TextComponentString("Poked " + chunks.size() + " chunks"));
        }
    }

    private ArrayList<ChunkPos> loadCsv(String filePath) throws IOException {
        ArrayList<ChunkPos> loadedChunks = new ArrayList<>();
        try (Scanner scanner = new Scanner(Files.newInputStream(Paths.get(filePath)))) {
            while (scanner.hasNextLine()) {
                String[] split = scanner.nextLine().split(",");
                loadedChunks.add(new ChunkPos(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
            }
        }
        return loadedChunks;
    }

    protected void inspect(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException, NoSuchFieldException, IllegalAccessException {
        CustomHashMap<Chunk> loadedChunks = this.getLoadedChunks(sender);
        Object[] chunks = getValues(loadedChunks);
        int mask = getMask(loadedChunks);
        Integer start = 0, end = chunks.length;
        Optional<Long> keyClass = Optional.empty();
        for (int i = 1; i < args.length; i++){
            switch (args[i]){
                case "from":
                    start = Integer.valueOf(args[++i]);
                    break;
                case "to":
                    end = Integer.valueOf(args[++i]);;
                    break;
                case "class":
                    keyClass = Optional.of(Long.valueOf(args[++i]));
                    break;
                default:
                    throw new WrongUsageException(getUsage(sender));
            }
        }
        ArrayList<String> inspections = new ArrayList<>();
        String last = "";
        int lastN = 0;
        for (int i = start; (i & mask) != (end & mask); i++) {
            Chunk chunk = (Chunk) chunks[i & mask];
            if(keyClass.isPresent()){
                if(chunk == null){
                    if(!last.equals("null")){
                        if(lastN > 0)
                            inspections.add(String.format("... %d %s", lastN, last));
                        last = "null";
                        lastN = 0;
                    }
                    lastN++;
                    continue;
                }
                if(getKeyClass(chunk, mask) != keyClass.get()){
                    if(last != "chunks"){
                        if(lastN > 0)
                            inspections.add(String.format("... %d %s", lastN, last));
                        last = "chunks";
                        lastN = 0;
                    }
                    lastN++;
                    continue;
                }
            }
            if(last != ""){
                if(lastN > 0)
                    inspections.add(String.format("... %d %s", lastN, last));
                last = "";
                lastN = 0;
            }
            String formatted = formatChunk(chunk, i & mask, mask);
            inspections.add(formatted);

        }
        String result = inspections.stream().collect(Collectors.joining(", ", "[", "]"));
        sender.sendMessage(new TextComponentString(result));
    }

    protected void search(ICommandSender sender, int chunkX, int chunkZ) throws NoSuchFieldException, IllegalAccessException {
        CustomHashMap<Chunk> loadedChunks = (CustomHashMap<Chunk>) ((ChunkProviderServer) world.getChunkProvider()).loadedChunks;
        Object[] chunks = getValues(loadedChunks);
        int mask = getMask(loadedChunks);
        for (int i = 0; i < chunks.length; i++) {
            Chunk chunk = (Chunk) chunks[i];
            if(chunk == null)
                continue;
            if (chunk.x != chunkX || chunk.z != chunkZ)
                continue;
            sender.sendMessage(new TextComponentString(formatChunk(chunk,i, mask)));
            break;
        }
    }

    protected static HashMap<Long, Chunk> tempChunks = new HashMap<>();

    protected void add(ICommandSender sender, int x, int z) {
        long hash = ChunkPos.asLong(x, z);
        if(!tempChunks.containsKey(hash)){
            sender.sendMessage(new TextComponentString(String.format("Chunk (%d, %d) couldn't been found", x, z)));
            return;
        }
        Chunk chunk = tempChunks.get(hash);
        CustomHashMap<Chunk> loadedChunks = getLoadedChunks();
        loadedChunks.put(hash, chunk);
        sender.sendMessage(new TextComponentString(String.format("Chunk (%d, %d) has been added back", x, z)));
    }

    protected void remove(ICommandSender sender, int x, int z) {
        long hash = ChunkPos.asLong(x, z);

        CustomHashMap<Chunk> loadedChunks = getLoadedChunks();
        if(!loadedChunks.containsKey(hash)){
            sender.sendMessage(new TextComponentString(String.format("Chunk (%d, %d) is not in loaded list", x, z)));
        }
        Chunk chunk = loadedChunks.remove(hash);
        tempChunks.put(hash, chunk);
        sender.sendMessage(new TextComponentString(String.format("Chunk (%d, %d) has been removed", x, z)));
    }

    protected CustomHashMap<Chunk> getLoadedChunks(){
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        return (CustomHashMap<Chunk>) provider.loadedChunks;
    }

    public String formatChunk(Chunk chunk, int pos, int mask){
        if (chunk == null) {
            return String.format("%d: null", pos);

        }

        return String.format("%d: %s(%d, %d) %d",
                pos, getChunkDescriber(chunk), chunk.x, chunk.z,
                getKeyClass(chunk, mask));
    }

    public String getChunkDescriber(Chunk chunk){
        int x = chunk.x, z = chunk.z;
        long hash = ChunkPos.asLong(x, z);
        String describer = "";
        if(world.isSpawnChunk(x, z)){
            describer +="S ";
        }
        if(((hash ^ (hash >>> 16)) & 0xFFFF) == 0){
            describer +="0 ";
        }
        return describer;
    }

    public static long getKeyClass(Chunk chunk, int mask){
        return HashCommon.mix(ChunkPos.asLong(chunk.x, chunk.z)) & mask;
    }

    public static int getMaxField(CustomHashMap hashMap) throws NoSuchFieldException, IllegalAccessException {
        Field maxFill = CustomHashMap.class.getDeclaredField("maxFill");
        maxFill.setAccessible(true);
        return (int) maxFill.get(hashMap);
    }

    public static int getMask(CustomHashMap hashMap) throws NoSuchFieldException, IllegalAccessException {
        Field mask = CustomHashMap.class.getDeclaredField("mask");
        mask.setAccessible(true);
        return (int) mask.get(hashMap);
    }

    public static float getFillLevel(CustomHashMap hashMap) throws NoSuchFieldException, IllegalAccessException {
        return (float) hashMap.size() / getMaxField(hashMap);
    }

    public static Object[] getValues(CustomHashMap hashMap) throws NoSuchFieldException, IllegalAccessException {
        Field value = CustomHashMap.class.getDeclaredField("value");
        value.setAccessible(true);
        return (Object[]) value.get(hashMap);
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {

        if (!CarpetSettings.commandLoadedChunks)
        {
            return Collections.emptyList();
        }

        if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args,
                    "size", "inspect", "search", "remove", "add", "dump");
        }

        switch (args[0]){
            case "inspect":
                switch (args[args.length - 1]){
                    case "class":
                    case "from":
                    case "to":
                        return Collections.emptyList();
                }
                return getListOfStringsMatchingLastWord(args,
                        "class", "from", "to");
            case "search":
            case "remove":
            case "add":
                if (args.length > 3)
                    return Collections.emptyList();
                return getChunkCompletitions(sender, args, 2);
        }

        return Collections.emptyList();
    }


    public List<String> getChunkCompletitions(ICommandSender sender, String[] args, int index) {
        int chunkX = sender.getPosition().getX() >> 4;
        int chunkZ = sender.getPosition().getZ() >> 4;

        if (args.length == index) {
            return getListOfStringsMatchingLastWord(args, Integer.toString(chunkX), "~");
        } else if (args.length == index + 1) {
            return getListOfStringsMatchingLastWord(args, Integer.toString(chunkZ), "~");
        } else {
            return Collections.emptyList();
        }
    }
}
