package nexo.beta.managers;

import nexo.beta.NexoAndCorruption;
import nexo.beta.entities.Nexo;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class NexoManager {
    
    private static NexoManager instance;
    private final NexoAndCorruption plugin;
    private final ConfigManager configManager;
    private final Logger logger;
    
    // Mapa para almacenar un Nexo por mundo (UUID del mundo -> Nexo)
    private final Map<UUID, Nexo> nexosPorMundo;
    
    // Tasks para el sistema de regeneración y consumo
    private BukkitTask taskRegeneracion;
    private BukkitTask taskConsumoEnergia;
    private BukkitTask taskGuardadoAutomatico;
    
    private NexoManager(NexoAndCorruption plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = plugin.getLogger();
        this.nexosPorMundo = new HashMap<>();
    }
    
    /**
     * Inicializa el NexoManager (llamado desde PluginManager)
     */
    public static void initialize(NexoAndCorruption plugin, ConfigManager configManager) {
        if (instance == null) {
            instance = new NexoManager(plugin, configManager);
            instance.startSystems();
        }
    }
    
    /**
     * Obtiene la instancia del NexoManager
     */
    public static NexoManager getInstance() {
        return instance;
    }
    
    /**
     * Inicia todos los sistemas del Nexo
     */
    private void startSystems() {
        logger.info("§6⚡ Iniciando sistemas del Nexo...");
        
        // Cargar Nexos existentes
        loadExistingNexos();
        
        // Iniciar tareas automáticas
        startAutomaticTasks();
        
        logger.info("§a✅ Sistemas del Nexo iniciados correctamente");
    }
    
    /**
     * Carga los Nexos existentes desde la configuración
     */
    private void loadExistingNexos() {
        try {
            Location ubicacion = configManager.getUbicacionNexo();
            World mundo = ubicacion.getWorld();
            
            if (mundo != null) {
                // Crear o cargar el Nexo para este mundo
                Nexo nexo = new Nexo(ubicacion, configManager);
                nexosPorMundo.put(mundo.getUID(), nexo);
                
                if (configManager.isDebugHabilitado()) {
                    logger.info("§e[DEBUG] Nexo cargado en mundo: " + mundo.getName() + 
                               " en " + ubicacion.getBlockX() + ", " + ubicacion.getBlockY() + ", " + ubicacion.getBlockZ());
                }
            } else {
                logger.warning("§c⚠️ No se pudo cargar el mundo del Nexo. Verifica la configuración.");
            }
        } catch (Exception e) {
            logger.severe("§c❌ Error al cargar Nexos existentes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicia las tareas automáticas del sistema
     */
    private void startAutomaticTasks() {
        // Task de regeneración (cada segundo)
        if (configManager.isRegeneracionHabilitada()) {
            taskRegeneracion = new BukkitRunnable() {
                @Override
                public void run() {
                    regenerarTodosLosNexos();
                }
            }.runTaskTimer(plugin, 20L, 20L); // Cada segundo (20 ticks)
        }
        
        // Task de consumo de energía (cada minuto)
        taskConsumoEnergia = new BukkitRunnable() {
            @Override
            public void run() {
                consumirEnergiaTodosLosNexos();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Cada minuto (1200 ticks)
        
        // Task de guardado automático
        if (configManager.getIntervaloGuardadoAuto() > 0) {
            long intervalo = configManager.getIntervaloGuardadoAuto() * 20L; // Convertir segundos a ticks
            taskGuardadoAutomatico = new BukkitRunnable() {
                @Override
                public void run() {
                    guardarTodosLosNexos();
                }
            }.runTaskTimer(plugin, intervalo, intervalo);
        }
        
        logger.info("§a✅ Tareas automáticas del Nexo iniciadas");
    }
    
    /**
     * Detiene todos los sistemas del Nexo
     */
    public void shutdown() {
        logger.info("§6⚡ Deteniendo sistemas del Nexo...");
        
        // Cancelar tareas
        if (taskRegeneracion != null && !taskRegeneracion.isCancelled()) {
            taskRegeneracion.cancel();
        }
        if (taskConsumoEnergia != null && !taskConsumoEnergia.isCancelled()) {
            taskConsumoEnergia.cancel();
        }
        if (taskGuardadoAutomatico != null && !taskGuardadoAutomatico.isCancelled()) {
            taskGuardadoAutomatico.cancel();
        }
        
        // Guardar todos los Nexos antes de cerrar
        if (configManager.isGuardarAlApagar()) {
            guardarTodosLosNexos();
        }
        
        // Limpiar mapa
        nexosPorMundo.clear();
        
        logger.info("§a✅ Sistemas del Nexo detenidos correctamente");
    }
    
    // ==========================================
    // MÉTODOS DE GESTIÓN DE NEXOS
    // ==========================================
    
    /**
     * Obtiene el Nexo de un mundo específico
     */
    public Nexo getNexoEnMundo(World mundo) {
        return nexosPorMundo.get(mundo.getUID());
    }
    
    /**
     * Obtiene el Nexo más cercano a una ubicación
     */
    public Nexo getNexoCercano(Location ubicacion) {
        World mundo = ubicacion.getWorld();
        if (mundo == null) return null;
        
        return getNexoEnMundo(mundo);
    }
    
    /**
     * Verifica si una ubicación está dentro del radio de protección de algún Nexo
     */
    public boolean estaEnZonaProtegida(Location ubicacion) {
        Nexo nexo = getNexoCercano(ubicacion);
        if (nexo == null || !nexo.estaActivo()) return false;
        
        double distancia = nexo.getUbicacion().distance(ubicacion);
        return distancia <= configManager.getRadioProteccion();
    }
    
    /**
     * Crea un nuevo Nexo en la ubicación especificada
     */
    public Nexo crearNexo(Location ubicacion) {
        World mundo = ubicacion.getWorld();
        if (mundo == null) {
            logger.warning("§c⚠️ No se puede crear un Nexo en un mundo nulo");
            return null;
        }
        
        // Verificar si ya existe un Nexo en este mundo
        if (nexosPorMundo.containsKey(mundo.getUID())) {
            logger.warning("§c⚠️ Ya existe un Nexo en el mundo: " + mundo.getName());
            return nexosPorMundo.get(mundo.getUID());
        }
        
        // Crear nuevo Nexo
        Nexo nuevoNexo = new Nexo(ubicacion, configManager);
        nexosPorMundo.put(mundo.getUID(), nuevoNexo);
        
        logger.info("§a✅ Nuevo Nexo creado en " + mundo.getName() + 
                   " (" + ubicacion.getBlockX() + ", " + ubicacion.getBlockY() + ", " + ubicacion.getBlockZ() + ")");
        
        return nuevoNexo;
    }
    
    /**
     * Elimina el Nexo de un mundo
     */
    public boolean eliminarNexo(World mundo) {
        Nexo nexo = nexosPorMundo.remove(mundo.getUID());
        if (nexo != null) {
            nexo.destruir();
            logger.info("§c❌ Nexo eliminado del mundo: " + mundo.getName());
            return true;
        }
        return false;
    }
    
    // ==========================================
    // MÉTODOS DE REGENERACIÓN Y CONSUMO
    // ==========================================
    
    /**
     * Regenera la vida y energía de todos los Nexos activos
     */
    private void regenerarTodosLosNexos() {
        for (Nexo nexo : nexosPorMundo.values()) {
            if (nexo.estaActivo()) {
                // Verificar si requiere jugadores cerca
                if (configManager.isRequiereJugadoresCerca()) {
                    if (!nexo.hayJugadoresCerca(configManager.getRadioMinimoJugadores(), 
                                               configManager.getMinimoJugadores())) {
                        continue; // Saltar regeneración si no hay jugadores cerca
                    }
                }
                
                // Regenerar energía
                int energiaActual = nexo.getEnergia();
                int energiaMaxima = configManager.getEnergiaMaxima();
                int regeneracionEnergia = configManager.getEnergiaPorSegundo();
                
                if (energiaActual < energiaMaxima) {
                    nexo.setEnergia(Math.min(energiaMaxima, energiaActual + regeneracionEnergia));
                }
                
                // Regenerar vida
                int vidaActual = nexo.getVida();
                int vidaMaxima = configManager.getVidaMaxima();
                int regeneracionVida = configManager.getVidaPorSegundo();
                
                if (vidaActual < vidaMaxima && nexo.getEnergia() > 0) {
                    nexo.setVida(Math.min(vidaMaxima, vidaActual + regeneracionVida));
                }
            }
        }
    }
    
    /**
     * Consume energía de todos los Nexos activos
     */
    private void consumirEnergiaTodosLosNexos() {
        int consumoPorMinuto = configManager.getConsumoEnergiaPorMinuto();
        
        for (Nexo nexo : nexosPorMundo.values()) {
            if (nexo.estaActivo()) {
                int energiaActual = nexo.getEnergia();
                int nuevaEnergia = Math.max(0, energiaActual - consumoPorMinuto);
                nexo.setEnergia(nuevaEnergia);
                
                // Si se queda sin energía, desactivar el Nexo
                if (nuevaEnergia <= 0) {
                    nexo.desactivar();
                    logger.warning("§c⚠️ Nexo en " + nexo.getUbicacion().getWorld().getName() + 
                                  " se ha desactivado por falta de energía");
                }
                
                if (configManager.isDebugHabilitado() && configManager.isMostrarCalculosEnergia()) {
                    logger.info("§e[DEBUG] Nexo consumió " + consumoPorMinuto + " energía. " +
                               "Energía actual: " + nuevaEnergia + "/" + configManager.getEnergiaMaxima());
                }
            }
        }
    }
    
    // ==========================================
    // MÉTODOS DE GUARDADO Y CARGA
    // ==========================================
    
    /**
     * Guarda todos los Nexos
     */
    public void guardarTodosLosNexos() {
        for (Nexo nexo : nexosPorMundo.values()) {
            nexo.guardar();
        }
        
        if (configManager.isDebugHabilitado()) {
            logger.info("§e[DEBUG] Guardado automático de Nexos completado");
        }
    }
    
    /**
     * Recarga la configuración y reinicia los sistemas si es necesario
     */
    public void recargarConfiguracion() {
        logger.info("§6⟳ Recargando configuración del NexoManager...");
        
        // Detener tareas actuales
        if (taskRegeneracion != null && !taskRegeneracion.isCancelled()) {
            taskRegeneracion.cancel();
        }
        if (taskConsumoEnergia != null && !taskConsumoEnergia.isCancelled()) {
            taskConsumoEnergia.cancel();
        }
        if (taskGuardadoAutomatico != null && !taskGuardadoAutomatico.isCancelled()) {
            taskGuardadoAutomatico.cancel();
        }
        
        // Recargar configuración de todos los Nexos
        for (Nexo nexo : nexosPorMundo.values()) {
            nexo.recargarConfiguracion(configManager);
        }
        
        // Reiniciar tareas automáticas
        startAutomaticTasks();
        
        logger.info("§a✅ Configuración del NexoManager recargada");
    }
    
    // ==========================================
    // MÉTODOS DE INFORMACIÓN Y ESTADÍSTICAS
    // ==========================================
    
    /**
     * Obtiene información de todos los Nexos
     */
    public Map<UUID, Nexo> getTodosLosNexos() {
        return new HashMap<>(nexosPorMundo);
    }
    
    /**
     * Obtiene el número total de Nexos activos
     */
    public int getNexosActivos() {
        return (int) nexosPorMundo.values().stream()
                .filter(Nexo::estaActivo)
                .count();
    }
    
    /**
     * Obtiene el número total de Nexos
     */
    public int getTotalNexos() {
        return nexosPorMundo.size();
    }
    
    // ==========================================
    // GETTERS
    // ==========================================
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public NexoAndCorruption getPlugin() {
        return plugin;
    }
}