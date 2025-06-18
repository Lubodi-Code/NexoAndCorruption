package nexo.beta;

import org.bukkit.plugin.java.JavaPlugin;

import nexo.beta.listeners.PlayerListener;
import nexo.beta.listeners.ProtectionListener;
import nexo.beta.managers.ConfigManager;
import nexo.beta.managers.NexoManager;
import nexo.beta.managers.PluginManager;
import nexo.beta.CommandManager.CommandNexoManager;

public class NexoAndCorruption extends JavaPlugin {
    
    private static NexoAndCorruption instance;
    private PluginManager pluginManager;
    
    @Override
    public void onEnable() {
        // Establecer instancia
        instance = this;
        
        getLogger().info("§6⚡ Iniciando NexoAndCorruption...");
        
        try {
            // Initialize managers
            pluginManager = PluginManager.getInstance();
            pluginManager.initialize(this);
            
            // Register listeners
            registerListeners();
            
            // Register commands (si los tienes)
            registerCommands();
            
            getLogger().info("§a✅ NexoAndCorruption has been enabled!");
            
            // Mostrar información de debug si está habilitado
            if (getConfigManager().isDebugHabilitado()) {
                showDebugInfo();
            }
            
        } catch (Exception e) {
            getLogger().severe("§c❌ Error crítico durante la inicialización: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("§6⚡ Deteniendo NexoAndCorruption...");
        
        try {
            // Shutdown managers
            if (pluginManager != null) {
                pluginManager.shutdown();
            }
            
            getLogger().info("§a✅ NexoAndCorruption has been disabled!");
            
        } catch (Exception e) {
            getLogger().severe("§c❌ Error durante el apagado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra los listeners del plugin
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        getLogger().info("§a✅ Listeners registrados");
    }
    
    /**
     * Registra los comandos del plugin
     */
    private void registerCommands() {
        getCommand("nexo").setExecutor(new CommandNexoManager(this));
        getLogger().info("§a✅ Comandos registrados");
    }
    
    /**
     * Muestra información de debug al iniciar
     */
    private void showDebugInfo() {
        ConfigManager config = getConfigManager();
        NexoManager nexo = getNexoManager();
        
        getLogger().info("§e[DEBUG] ==========================================");
        getLogger().info("§e[DEBUG] INFORMACIÓN DE DEBUG DEL NEXO");
        getLogger().info("§e[DEBUG] ==========================================");
        getLogger().info("§e[DEBUG] Vida máxima: " + config.getVidaMaxima());
        getLogger().info("§e[DEBUG] Energía máxima: " + config.getEnergiaMaxima());
        getLogger().info("§e[DEBUG] Consumo de energía/min: " + config.getConsumoEnergiaPorMinuto());
        getLogger().info("§e[DEBUG] Radio de protección: " + config.getRadioProteccion());
        getLogger().info("§e[DEBUG] Ubicación del Nexo: " + config.getUbicacionNexo().toString());
        getLogger().info("§e[DEBUG] Nexos activos: " + nexo.getNexosActivos() + "/" + nexo.getTotalNexos());
        getLogger().info("§e[DEBUG] Regeneración habilitada: " + config.isRegeneracionHabilitada());
        getLogger().info("§e[DEBUG] Reinicio automático: " + config.isReinicioHabilitado());
        getLogger().info("§e[DEBUG] Eventos especiales: " + config.isEventosEspecialesHabilitado());
        getLogger().info("§e[DEBUG] ==========================================");
    }
    
    /**
     * Recarga todo el plugin
     */
    public void reloadPlugin() {
        getLogger().info("§6⟳ Recargando NexoAndCorruption...");
        
        try {
            if (pluginManager != null) {
                pluginManager.reload();
            }
            getLogger().info("§a✅ Plugin recargado correctamente");
        } catch (Exception e) {
            getLogger().severe("§c❌ Error al recargar el plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==========================================
    // MÉTODOS ESTÁTICOS DE ACCESO
    // ==========================================
    
    /**
     * Obtiene la instancia del plugin
     */
    public static NexoAndCorruption getInstance() {
        return instance;
    }
    
    /**
     * Obtiene el ConfigManager
     */
    public ConfigManager getConfigManager() {
        return pluginManager != null ? pluginManager.getConfigManager() : null;
    }
    
    /**
     * Obtiene el NexoManager
     */
    public NexoManager getNexoManager() {
        return pluginManager != null ? pluginManager.getNexoManager() : null;
    }
    
    /**
     * Obtiene el PluginManager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    // ==========================================
    // MÉTODOS DE UTILIDAD ESTÁTICOS
    // ==========================================
    
    /**
     * Método estático para obtener el ConfigManager
     */
    public static ConfigManager getConfigManagerStatic() {
        return getInstance().getConfigManager();
    }
    
    /**
     * Método estático para obtener el NexoManager
     */
    public static NexoManager getNexoManagerStatic() {
        return getInstance().getNexoManager();
    }
}