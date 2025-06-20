package nexo.beta.managers;

import java.util.Random;

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

    public CorruptionManager(NexoAndCorruption plugin, NexoManager nexoManager, ConfigManager config) {
        this.plugin = plugin;
        this.nexoManager = nexoManager;
        this.config = config;
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        if (!config.isCorruptionEnabled()) return;

        long interval = config.getCorruptionSpreadInterval();
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
        start();
    }

    private void ejecutarCiclo() {
        int bloques = config.getCorruptionBlocksPerCycle();
        for (int i = 0; i < bloques; i++) {
            World world = elegirMundo();
            if (world == null) return;
            Chunk[] loaded = world.getLoadedChunks();
            if (loaded.length == 0) continue;
            Chunk chunk = loaded[random.nextInt(loaded.length)];
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = random.nextInt(world.getMaxHeight() - world.getMinHeight()) + world.getMinHeight();
            Block block = chunk.getBlock(x, y, z);

            Location loc = block.getLocation();
            if (nexoManager.estaEnZonaProtegida(loc)) continue;

            if (block.getType() == Material.DIRT || block.getType() == Material.GRASS_BLOCK) {
                block.setType(Material.NETHERRACK);
                if (config.isDebugHabilitado()) {
                    plugin.getLogger().info("[DEBUG] Bloque corrompido en " + loc.toVector());
                }
            }
        }
    }

    private World elegirMundo() {
        if (Bukkit.getWorlds().isEmpty()) return null;
        return Bukkit.getWorlds().get(random.nextInt(Bukkit.getWorlds().size()));
    }
}
