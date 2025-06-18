package nexo.beta.managers;

import nexo.beta.NexoAndCorruption;

public class PluginManager {
    private static PluginManager instance;
    private ConfigManager configManager;
    private NexoManager nexoManager;
    private NexoAndCorruption plugin;
    
    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }
    
    /**
     * Inicializa todos los managers del plugin
     */
    public void initialize(NexoAndCorruption plugin) {
        this.plugin = plugin;
        
        try {
            // 1. Inicializar ConfigManager primero
            initializeConfigManager();
            
            // 2. Inicializar NexoManager
            initializeNexoManager();
            
            plugin.getLogger().info("§a✅ Todos los managers inicializados correctamente");
            
        } catch (Exception e) {
            plugin.getLogger().severe("§c❌ Error crítico al inicializar managers: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }
    
    /**
     * Inicializa el ConfigManager
     */
    private void initializeConfigManager() {
        configManager = new ConfigManager(plugin);
        plugin.getLogger().info("§a✅ ConfigManager inicializado");
    }
    
    /**
     * Inicializa el NexoManager
     */
    private void initializeNexoManager() {
        NexoManager.initialize(plugin, configManager);
        nexoManager = NexoManager.getInstance();
        plugin.getLogger().info("§a✅ NexoManager inicializado");
    }
    
    /**
     * Detiene todos los managers
     */
    public void shutdown() {
        plugin.getLogger().info("§6⚡ Deteniendo todos los managers...");
        
        // Detener NexoManager
        if (nexoManager != null) {
            nexoManager.shutdown();
        }
        
        plugin.getLogger().info("§a✅ Todos los managers detenidos correctamente");
    }
    
    /**
     * Recarga todos los managers
     */
    public void reload() {
        plugin.getLogger().info("§6⟳ Recargando todos los managers...");
        
        try {
            // Recargar ConfigManager
            if (configManager != null) {
                configManager.reloadConfigs();
            }
            
            // Recargar NexoManager
            if (nexoManager != null) {
                nexoManager.recargarConfiguracion();
            }
            
            plugin.getLogger().info("§a✅ Todos los managers recargados correctamente");
            
        } catch (Exception e) {
            plugin.getLogger().severe("§c❌ Error al recargar managers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==========================================
    // GETTERS
    // ==========================================
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public NexoManager getNexoManager() {
        return nexoManager;
    }
    
    public NexoAndCorruption getPlugin() {
        return plugin;
    }
}