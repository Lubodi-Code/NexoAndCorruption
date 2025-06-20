package nexo.beta.managers;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import nexo.beta.NexoAndCorruption;

/**
 * Sistema avanzado de corrupción con construcción de estructuras orgánicas
 */
public class EnhancedCorruptionManager {

    private final NexoAndCorruption plugin;
    private final NexoManager nexoManager;
    private ConfigManager config;
    private BukkitTask task;
    private final Random random = new Random();

    // Estructuras de datos para el crecimiento orgánico
    private final Set<CorruptionZone> activeZones = new HashSet<>();
    private final Map<Location, CorruptionNode> corruptionNodes = new HashMap<>();

    // Configuración de patrones
    private final List<StructurePattern> patterns = new ArrayList<>();

    // Materiales de construcción corrupta
    private static final Map<StructureType, List<Material>> STRUCTURE_MATERIALS = new HashMap<>();
    
    static {
        // Torres/Pilares
        STRUCTURE_MATERIALS.put(StructureType.PILLAR, Arrays.asList(
            Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE
        ));
        
        // Ramificaciones orgánicas
        STRUCTURE_MATERIALS.put(StructureType.BRANCH, Arrays.asList(
            Material.NETHERRACK, Material.WARPED_STEM, Material.CRIMSON_STEM
        ));
        
        // Nodos de poder
        STRUCTURE_MATERIALS.put(StructureType.POWER_NODE, Arrays.asList(
            Material.MAGMA_BLOCK, Material.SOUL_FIRE, Material.SHROOMLIGHT
        ));
        
        // Conexiones
        STRUCTURE_MATERIALS.put(StructureType.CONNECTION, Arrays.asList(
            Material.SOUL_SAND, Material.SOUL_SOIL, Material.BASALT
        ));
    }

    public EnhancedCorruptionManager(NexoAndCorruption plugin, NexoManager nexoManager, ConfigManager config) {
        this.plugin = plugin;
        this.nexoManager = nexoManager;
        this.config = config;
        initializePatterns();
    }

    private void initializePatterns() {
        // Patrón de Torre Corrupta
        patterns.add(new StructurePattern(StructureType.PILLAR, 0.3, this::buildCorruptTower));
        
        // Patrón de Ramificación Orgánica
        patterns.add(new StructurePattern(StructureType.BRANCH, 0.4, this::buildOrganicBranch));
        
        // Patrón de Nodo de Poder
        patterns.add(new StructurePattern(StructureType.POWER_NODE, 0.2, this::buildPowerNode));
        
        // Patrón de Conexión
        patterns.add(new StructurePattern(StructureType.CONNECTION, 0.1, this::buildConnection));
    }

    public void start() {
        if (task != null) task.cancel();
        if (!config.isCorruptionEnabled()) return;

        long interval = config.getCorruptionSpreadInterval();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                executeCycle();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void executeCycle() {
        int maxOperations = config.getCorruptionBlocksPerCycle();
        
        // Crear nuevas zonas si es necesario
        if (activeZones.size() < 3 && random.nextDouble() < 0.3) {
            createNewCorruptionZone();
        }
        
        // Expandir zonas existentes
        Iterator<CorruptionZone> iterator = activeZones.iterator();
        while (iterator.hasNext() && maxOperations > 0) {
            CorruptionZone zone = iterator.next();
            int used = expandZone(zone, Math.min(maxOperations, 3));
            maxOperations -= used;
            
            if (zone.isCompleted()) {
                iterator.remove();
            }
        }
    }

    private void createNewCorruptionZone() {
        Location center = findValidLocation();
        if (center == null) return;
        
        CorruptionZone zone = new CorruptionZone(center);
        zone.targetSize = random.nextInt(config.getCorruptionAreaMax() - config.getCorruptionAreaMin()) 
                         + config.getCorruptionAreaMin();
        
        activeZones.add(zone);
        
        // Crear nodo inicial
        CorruptionNode rootNode = new CorruptionNode(center, StructureType.POWER_NODE);
        corruptionNodes.put(center, rootNode);
        zone.addNode(rootNode);
        
        if (config.isDebugHabilitado()) {
            plugin.getLogger().info("[DEBUG] Nueva zona de corrupción orgánica en " + center.toVector());
        }
    }

    private int expandZone(CorruptionZone zone, int maxOperations) {
        int operationsUsed = 0;
        
        for (int i = 0; i < maxOperations && !zone.isCompleted(); i++) {
            if (expandZoneStep(zone)) {
                operationsUsed++;
            }
        }
        
        return operationsUsed;
    }

    private boolean expandZoneStep(CorruptionZone zone) {
        // Seleccionar un nodo activo para expandir
        CorruptionNode node = zone.getRandomActiveNode();
        if (node == null) return false;
        
        // Decidir qué tipo de estructura crear
        StructurePattern pattern = selectPattern();
        if (pattern == null) return false;
        
        // Encontrar dirección de crecimiento
        Vector direction = findGrowthDirection(node.location);
        if (direction == null) return false;
        
        // Calcular nueva posición
        Location newPos = node.location.clone().add(direction.multiply(random.nextInt(3) + 2));
        
        // Verificar validez
        if (nexoManager.estaEnZonaProtegida(newPos) || corruptionNodes.containsKey(newPos)) {
            return false;
        }
        
        // Construir estructura
        boolean success = pattern.builder.build(node, newPos, zone);
        if (success) {
            zone.currentSize++;
            return true;
        }
        
        return false;
    }

    // Constructores de estructuras específicas
    private boolean buildCorruptTower(CorruptionNode fromNode, Location location, CorruptionZone zone) {
        World world = location.getWorld();
        if (world == null) return false;
        
        int height = random.nextInt(8) + 4; // Torres de 4-12 bloques
        List<Material> materials = STRUCTURE_MATERIALS.get(StructureType.PILLAR);
        
        // Construir pilar
        for (int y = 0; y < height; y++) {
            Block block = world.getBlockAt(location.getBlockX(), location.getBlockY() + y, location.getBlockZ());
            Material material = materials.get(random.nextInt(materials.size()));
            block.setType(material);
        }
        
        // Añadir decoraciones
        if (random.nextDouble() < 0.4) {
            Block top = world.getBlockAt(location.getBlockX(), location.getBlockY() + height, location.getBlockZ());
            top.setType(Material.SOUL_FIRE);
        }
        
        // Crear nodo
        CorruptionNode newNode = new CorruptionNode(location, StructureType.PILLAR);
        corruptionNodes.put(location, newNode);
        zone.addNode(newNode);
        
        return true;
    }

    private boolean buildOrganicBranch(CorruptionNode fromNode, Location location, CorruptionZone zone) {
        World world = location.getWorld();
        if (world == null) return false;
        
        List<Material> materials = STRUCTURE_MATERIALS.get(StructureType.BRANCH);
        
        // Crear ramificación curva
        Vector direction = location.toVector().subtract(fromNode.location.toVector()).normalize();
        Location current = fromNode.location.clone();
        
        int length = random.nextInt(6) + 3;
        for (int i = 0; i < length; i++) {
            // Añadir curvatura aleatoria
            direction.add(new Vector(
                (random.nextDouble() - 0.5) * 0.3,
                (random.nextDouble() - 0.5) * 0.2,
                (random.nextDouble() - 0.5) * 0.3
            )).normalize();
            
            current.add(direction);
            Block block = world.getBlockAt(current);
            Material material = materials.get(random.nextInt(materials.size()));
            block.setType(material);
            
            // Posibilidad de ramificación secundaria
            if (i > 2 && random.nextDouble() < 0.2) {
                buildSecondaryBranch(current, materials);
            }
        }
        
        // Crear nodo al final
        CorruptionNode newNode = new CorruptionNode(location, StructureType.BRANCH);
        corruptionNodes.put(location, newNode);
        zone.addNode(newNode);
        
        return true;
    }

    private void buildSecondaryBranch(Location from, List<Material> materials) {
        World world = from.getWorld();
        Vector direction = new Vector(
            random.nextDouble() - 0.5,
            random.nextDouble() - 0.5,
            random.nextDouble() - 0.5
        ).normalize();
        
        Location current = from.clone();
        int length = random.nextInt(3) + 2;
        
        for (int i = 0; i < length; i++) {
            current.add(direction);
            Block block = world.getBlockAt(current);
            Material material = materials.get(random.nextInt(materials.size()));
            block.setType(material);
        }
    }

    private boolean buildPowerNode(CorruptionNode fromNode, Location location, CorruptionZone zone) {
        World world = location.getWorld();
        if (world == null) return false;
        
        // Crear estructura de nodo de poder (esfera pequeña)
        int radius = 2;
        List<Material> materials = STRUCTURE_MATERIALS.get(StructureType.POWER_NODE);
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    if (distance <= radius) {
                        Block block = world.getBlockAt(
                            location.getBlockX() + x,
                            location.getBlockY() + y,
                            location.getBlockZ() + z
                        );
                        
                        if (distance <= 1) {
                            block.setType(Material.MAGMA_BLOCK);
                        } else {
                            Material material = materials.get(random.nextInt(materials.size()));
                            block.setType(material);
                        }
                    }
                }
            }
        }
        
        // Efectos especiales
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 20);
        
        CorruptionNode newNode = new CorruptionNode(location, StructureType.POWER_NODE);
        corruptionNodes.put(location, newNode);
        zone.addNode(newNode);
        
        return true;
    }

    private boolean buildConnection(CorruptionNode fromNode, Location location, CorruptionZone zone) {
        // Construir conexión directa entre nodos
        World world = location.getWorld();
        if (world == null) return false;
        
        List<Material> materials = STRUCTURE_MATERIALS.get(StructureType.CONNECTION);
        
        // Línea directa con variaciones
        Vector direction = location.toVector().subtract(fromNode.location.toVector());
        int steps = (int) direction.length();
        direction.normalize();
        
        Location current = fromNode.location.clone();
        for (int i = 0; i < steps; i++) {
            current.add(direction);
            Block block = world.getBlockAt(current);
            Material material = materials.get(random.nextInt(materials.size()));
            block.setType(material);
        }
        
        return true;
    }

    // Métodos auxiliares
    private StructurePattern selectPattern() {
        double totalWeight = patterns.stream().mapToDouble(p -> p.probability).sum();
        double random = this.random.nextDouble() * totalWeight;
        
        double current = 0;
        for (StructurePattern pattern : patterns) {
            current += pattern.probability;
            if (random <= current) {
                return pattern;
            }
        }
        return patterns.get(0);
    }

    private Vector findGrowthDirection(Location from) {
        // Buscar dirección preferente hacia superficie o alejándose del centro
        Vector[] directions = {
            new Vector(1, 0, 0), new Vector(-1, 0, 0),
            new Vector(0, 1, 0), new Vector(0, -1, 0),
            new Vector(0, 0, 1), new Vector(0, 0, -1),
            new Vector(1, 1, 0), new Vector(-1, 1, 0),
            new Vector(1, 0, 1), new Vector(-1, 0, -1)
        };
        
        return directions[random.nextInt(directions.length)];
    }

    private Location findValidLocation() {
        for (int attempt = 0; attempt < 10; attempt++) {
            World world = Bukkit.getWorlds().get(random.nextInt(Bukkit.getWorlds().size()));
            Chunk[] chunks = world.getLoadedChunks();
            if (chunks.length == 0) continue;
            
            Chunk chunk = chunks[random.nextInt(chunks.length)];
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            int y = world.getHighestBlockYAt(x, z);
            
            Location loc = new Location(world, x, y, z);
            if (!nexoManager.estaEnZonaProtegida(loc)) {
                return loc;
            }
        }
        return null;
    }

    // Clases auxiliares
    private static class CorruptionZone {
        Location center;
        int currentSize = 0;
        int targetSize;
        List<CorruptionNode> nodes = new ArrayList<>();
        
        CorruptionZone(Location center) {
            this.center = center;
        }
        
        void addNode(CorruptionNode node) {
            nodes.add(node);
        }
        
        CorruptionNode getRandomActiveNode() {
            if (nodes.isEmpty()) return null;
            return nodes.get(new Random().nextInt(nodes.size()));
        }
        
        boolean isCompleted() {
            return currentSize >= targetSize;
        }
    }

    private static class CorruptionNode {
        Location location;
        StructureType type;
        boolean isActive = true;
        
        CorruptionNode(Location location, StructureType type) {
            this.location = location;
            this.type = type;
        }
    }

    private static class StructurePattern {
        StructureType type;
        double probability;
        StructureBuilder builder;
        
        StructurePattern(StructureType type, double probability, StructureBuilder builder) {
            this.type = type;
            this.probability = probability;
            this.builder = builder;
        }
    }

    @FunctionalInterface
    private interface StructureBuilder {
        boolean build(CorruptionNode fromNode, Location location, CorruptionZone zone);
    }

    private enum StructureType {
        PILLAR, BRANCH, POWER_NODE, CONNECTION
    }

    // Métodos de control públicos
    public void shutdown() {
        if (task != null) task.cancel();
    }

    public void reload(ConfigManager newConfig) {
        this.config = newConfig;
        start();
    }
}
