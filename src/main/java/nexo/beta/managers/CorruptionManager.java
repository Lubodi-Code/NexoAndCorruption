package nexo.beta.managers;

import java.util.Random;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nexo.beta.NexoAndCorruption;

/**
 * Gestiona la expansión básica de la corrupción en el mundo.
 */
public class CorruptionManager {

    private final NexoAndCorruption plugin;
    private final NexoManager nexoManager;
    private ConfigManager config;

    private BukkitTask task;
    private final Random random = new Random();
    private List<Material> blockTypes;
    private double surfaceProbability;

    private Location currentCenter;
    private int currentSize;
    private int targetSize;

    public CorruptionManager(NexoAndCorruption plugin, NexoManager nexoManager, ConfigManager config) {
        this.plugin = plugin;
        this.nexoManager = nexoManager;
        this.config = config;
        this.blockTypes = config.getCorruptionBlocks();
        this.surfaceProbability = config.getCorruptionSurfaceProbability();
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        if (!config.isCorruptionEnabled()) return;

        long interval = config.getCorruptionSpreadInterval();
        blockTypes = config.getCorruptionBlocks();
        surfaceProbability = config.getCorruptionSurfaceProbability();
        currentCenter = null;
        currentSize = 0;
        targetSize = 0;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                ejecutarCiclo();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
    }

    public void reload(ConfigManager newConfig) {
        this.config = newConfig;
        this.blockTypes = newConfig.getCorruptionBlocks();
        this.surfaceProbability = newConfig.getCorruptionSurfaceProbability();
        start();
    }

    private void ejecutarCiclo() {
        int pasos = config.getCorruptionBlocksPerCycle();

        if (currentCenter == null || currentSize > targetSize) {
            seleccionarNuevaZona();
        }

        for (int i = 0; i < pasos; i++) {
            if (currentCenter == null) break;
            if (currentSize > targetSize) {
                currentCenter = null;
                break;
            }
            expandirZona();
        }
    }

    private void seleccionarNuevaZona() {
        for (int intento = 0; intento < 5; intento++) {
            World world = elegirMundo();
            if (world == null) return;
            Chunk[] loaded = world.getLoadedChunks();
            if (loaded.length == 0) continue;
            Chunk chunk = loaded[random.nextInt(loaded.length)];
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int worldX = (chunk.getX() << 4) + x;
            int worldZ = (chunk.getZ() << 4) + z;
            int y = world.getHighestBlockYAt(worldX, worldZ);
            Block block = world.getBlockAt(worldX, y, worldZ);

            Location loc = block.getLocation();
            if (nexoManager.estaEnZonaProtegida(loc)) continue;

            currentCenter = loc;
            currentSize = 1;
            int min = config.getCorruptionAreaMin();
            int max = config.getCorruptionAreaMax();
            targetSize = random.nextInt(max - min + 1) + min;
            if (config.isDebugHabilitado()) {
                plugin.getLogger().info("[DEBUG] Nueva zona de corrupción en " + loc.toVector() + " tamaño objetivo " + targetSize);
            }
            break;
        }
    }

    private void expandirZona() {
        if (currentCenter == null) return;
        World world = currentCenter.getWorld();
        if (world == null) return;

        int size = currentSize;
        int startX = currentCenter.getBlockX() - size / 2;
        int startZ = currentCenter.getBlockZ() - size / 2;

        for (int x = startX; x < startX + size; x++) {
            for (int z = startZ; z < startZ + size; z++) {
                int blockY;
                if (random.nextDouble() < surfaceProbability) {
                    blockY = world.getHighestBlockYAt(x, z);
                } else {
                    int maxY = world.getHighestBlockYAt(x, z);
                    blockY = random.nextInt(maxY - world.getMinHeight() + 1) + world.getMinHeight();
                }
                Block block = world.getBlockAt(x, blockY, z);
                Location loc = block.getLocation();
                if (nexoManager.estaEnZonaProtegida(loc)) continue;
                if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK) {
                    Material newType = blockTypes.get(random.nextInt(blockTypes.size()));
                    block.setType(newType);
                    if (config.isDebugHabilitado()) {
                        plugin.getLogger().info("[DEBUG] Bloque corrompido en " + loc.toVector());
                    }
                }
            }
        }
        currentSize++;
    }

    private World elegirMundo() {
        if (Bukkit.getWorlds().isEmpty()) return null;
        return Bukkit.getWorlds().get(random.nextInt(Bukkit.getWorlds().size()));
    }
}
