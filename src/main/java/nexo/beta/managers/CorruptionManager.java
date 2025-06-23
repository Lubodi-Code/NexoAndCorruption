package nexo.beta.managers;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    // Materiales que se consideran transparentes/no sólidos
    private static final Set<Material> TRANSPARENT_MATERIALS = new HashSet<>();
    
    static {
        TRANSPARENT_MATERIALS.add(Material.AIR);
        TRANSPARENT_MATERIALS.add(Material.WATER);
        TRANSPARENT_MATERIALS.add(Material.LAVA);
        TRANSPARENT_MATERIALS.add(Material.TALL_GRASS);
        TRANSPARENT_MATERIALS.add(Material.FERN);
        TRANSPARENT_MATERIALS.add(Material.LARGE_FERN);
        TRANSPARENT_MATERIALS.add(Material.DEAD_BUSH);
        TRANSPARENT_MATERIALS.add(Material.SNOW);
        TRANSPARENT_MATERIALS.add(Material.VINE);
        TRANSPARENT_MATERIALS.add(Material.KELP);
        TRANSPARENT_MATERIALS.add(Material.KELP_PLANT);
        TRANSPARENT_MATERIALS.add(Material.SEAGRASS);
        TRANSPARENT_MATERIALS.add(Material.TALL_SEAGRASS);
    }

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
            
            // Buscar el bloque de superficie real
            Block surfaceBlock = encontrarBloqueDSuperficie(world, worldX, worldZ);
            if (surfaceBlock == null) continue;

            Location loc = surfaceBlock.getLocation();
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

    /**
     * Encuentra el bloque sólido de superficie en las coordenadas X,Z dadas
     * @param world El mundo
     * @param x Coordenada X
     * @param z Coordenada Z
     * @return El bloque de superficie o null si no se encuentra
     */
    private Block encontrarBloqueDSuperficie(World world, int x, int z) {
        int maxY = world.getHighestBlockYAt(x, z);
        
        // Buscar desde arriba hacia abajo el primer bloque sólido
        for (int y = maxY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            // Si encontramos un bloque sólido (no transparente)
            if (!TRANSPARENT_MATERIALS.contains(type) && type != Material.BEDROCK) {
                return block;
            }
        }
        
        return null; // No se encontró superficie válida
    }

    /**
     * Encuentra múltiples bloques de superficie en una ubicación (para diferentes alturas)
     * @param world El mundo
     * @param x Coordenada X
     * @param z Coordenada Z
     * @return Lista de bloques de superficie válidos
     */
    private List<Block> encontrarTodosLosBloquesDeSupericie(World world, int x, int z) {
        List<Block> surfaceBlocks = new java.util.ArrayList<>();
        int maxY = world.getHighestBlockYAt(x, z);
        boolean foundAir = false;
        
        // Buscar desde arriba hacia abajo
        for (int y = maxY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            if (TRANSPARENT_MATERIALS.contains(type)) {
                foundAir = true;
            } else if (foundAir && type != Material.BEDROCK) {
                // Encontramos un bloque sólido después de aire = superficie
                surfaceBlocks.add(block);
                foundAir = false;
            }
        }
        
        return surfaceBlocks;
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
                Block targetBlock = null;
                
                if (random.nextDouble() < surfaceProbability) {
                    // Buscar bloque de superficie
                    targetBlock = encontrarBloqueDSuperficie(world, x, z);
                } else {
                    // Buscar bloque subterráneo aleatorio
                    int maxY = world.getHighestBlockYAt(x, z);
                    int randomY = random.nextInt(maxY - world.getMinHeight() + 1) + world.getMinHeight();
                    Block block = world.getBlockAt(x, randomY, z);
                    
                    // Verificar que sea un bloque sólido válido
                    if (!TRANSPARENT_MATERIALS.contains(block.getType()) && 
                        block.getType() != Material.BEDROCK) {
                        targetBlock = block;
                    }
                }
                
                if (targetBlock != null) {
                    Location loc = targetBlock.getLocation();
                    if (nexoManager.estaEnZonaProtegida(loc)) continue;
                    
                    Material newType = blockTypes.get(random.nextInt(blockTypes.size()));
                    targetBlock.setType(newType);
                    
                    if (config.isDebugHabilitado()) {
                        plugin.getLogger().info("[DEBUG] Bloque corrompido en " + loc.toVector() + 
                                              " (era " + targetBlock.getType() + ", ahora " + newType + ")");
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
