package nexo.beta.classes;



import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nexo.beta.managers.ConfigManager;

public class Nexo {
    
    private final Location ubicacion;
    private ConfigManager configManager;
    private final Logger logger;
    
    // Estados del Nexo
    private int vida;
    private int energia;
    private boolean activo;
    private boolean enEstadoCritico;
    
    // Sistema de partículas y efectos
    private BukkitTask taskParticulas;
    private BukkitTask taskEfectos;
    
    // Control de mensajes críticos
    private long ultimoMensajeVidaBaja = 0;
    private long ultimoMensajeEnergiaBaja = 0;
    
    // Archivo de guardado individual
    private File archivoGuardado;
    private FileConfiguration datosNexo;
    
    /**
     * Constructor del Nexo
     */
    public Nexo(Location ubicacion, ConfigManager configManager) {
        this.ubicacion = ubicacion.clone();
        this.configManager = configManager;
        this.logger = Bukkit.getLogger();
        
        // Inicializar estados
        this.vida = configManager.getVidaMaxima();
        this.energia = configManager.getEnergiaMaxima();
        this.activo = true;
        this.enEstadoCritico = false;
        
        // Inicializar archivo de guardado
        inicializarArchivoGuardado();
        
        // Cargar datos existentes
        cargarDatos();
        
        // Iniciar efectos visuales
        iniciarEfectosVisuales();
        
        if (configManager.isDebugHabilitado()) {
            logger.info("§e[DEBUG] Nexo creado en: " + 
                       ubicacion.getWorld().getName() + " " +
                       ubicacion.getBlockX() + ", " + 
                       ubicacion.getBlockY() + ", " + 
                       ubicacion.getBlockZ());
        }
    }
    
    /**
     * Inicializa el archivo de guardado del Nexo
     */
    private void inicializarArchivoGuardado() {
        String nombreArchivo = "nexo_" + ubicacion.getWorld().getName() + "_" + 
                              ubicacion.getBlockX() + "_" + 
                              ubicacion.getBlockY() + "_" + 
                              ubicacion.getBlockZ() + ".yml";
        
        archivoGuardado = new File(Bukkit.getPluginManager().getPlugin("NexoAndCorruption").getDataFolder(), 
                                  "nexos/" + nombreArchivo);
        
        if (!archivoGuardado.getParentFile().exists()) {
            archivoGuardado.getParentFile().mkdirs();
        }
        
        if (!archivoGuardado.exists()) {
            try {
                archivoGuardado.createNewFile();
            } catch (IOException e) {
                logger.severe("§c❌ Error al crear archivo de guardado del Nexo: " + e.getMessage());
            }
        }
        
        datosNexo = YamlConfiguration.loadConfiguration(archivoGuardado);
    }
    
    /**
     * Carga los datos guardados del Nexo
     */
    private void cargarDatos() {
        if (datosNexo.contains("vida")) {
            this.vida = datosNexo.getInt("vida", configManager.getVidaMaxima());
        }
        
        if (datosNexo.contains("energia")) {
            this.energia = datosNexo.getInt("energia", configManager.getEnergiaMaxima());
        }
        
        if (datosNexo.contains("activo")) {
            this.activo = datosNexo.getBoolean("activo", true);
        }
        
        // Validar que los valores no excedan los máximos
        this.vida = Math.min(this.vida, configManager.getVidaMaxima());
        this.energia = Math.min(this.energia, configManager.getEnergiaMaxima());
        
        if (configManager.isDebugHabilitado()) {
            logger.info("§e[DEBUG] Datos del Nexo cargados - Vida: " + vida + 
                       ", Energía: " + energia + ", Activo: " + activo);
        }
    }
    
    /**
     * Guarda los datos del Nexo
     */
    public void guardar() {
        try {
            datosNexo.set("vida", vida);
            datosNexo.set("energia", energia);
            datosNexo.set("activo", activo);
            datosNexo.set("ultima_actualizacion", System.currentTimeMillis());
            
            // Guardar ubicación
            datosNexo.set("ubicacion.mundo", ubicacion.getWorld().getName());
            datosNexo.set("ubicacion.x", ubicacion.getX());
            datosNexo.set("ubicacion.y", ubicacion.getY());
            datosNexo.set("ubicacion.z", ubicacion.getZ());
            
            datosNexo.save(archivoGuardado);
            
            if (configManager.isDebugHabilitado()) {
                logger.info("§e[DEBUG] Datos del Nexo guardados correctamente");
            }
            
        } catch (IOException e) {
            logger.severe("§c❌ Error al guardar datos del Nexo: " + e.getMessage());
        }
    }
    
    /**
     * Inicia los efectos visuales del Nexo
     */
    private void iniciarEfectosVisuales() {
        if (!configManager.isParticulasHabilitadas()) return;
        
        // Task para partículas
        taskParticulas = new BukkitRunnable() {
            @Override
            public void run() {
                if (activo && ubicacion.getWorld() != null) {
                    mostrarParticulas();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("NexoAndCorruption"), 
                      0L, configManager.getIntervaloParticulas() * 20L);
        
        // Task para efectos de estado crítico
        taskEfectos = new BukkitRunnable() {
            @Override
            public void run() {
                verificarEstadosCriticos();
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("NexoAndCorruption"), 
                      20L, 20L); // Cada segundo
    }
    
    /**
     * Muestra partículas del Nexo
     */
    private void mostrarParticulas() {
        if (ubicacion.getWorld() == null) return;
        
        try {
            Particle particula = Particle.valueOf(configManager.getTipoParticula());
            
            // Crear un efecto circular alrededor del Nexo
            for (int i = 0; i < configManager.getCantidadParticulas(); i++) {
                double angulo = 2 * Math.PI * i / configManager.getCantidadParticulas();
                double x = ubicacion.getX() + Math.cos(angulo) * 2;
                double z = ubicacion.getZ() + Math.sin(angulo) * 2;
                double y = ubicacion.getY() + 0.5;
                
                Location particleLocation = new Location(ubicacion.getWorld(), x, y, z);
                ubicacion.getWorld().spawnParticle(particula, particleLocation, 1, 0, 0, 0, 0);
            }
            
            // Partículas adicionales si está en estado crítico
            if (enEstadoCritico) {
                ubicacion.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                                                 ubicacion.clone().add(0, 1, 0), 
                                                 5, 0.5, 0.5, 0.5, 0.01);
            }
            
        } catch (IllegalArgumentException e) {
            logger.warning("§c⚠️ Tipo de partícula inválido: " + configManager.getTipoParticula());
        }
    }
    
    /**
     * Verifica los estados críticos del Nexo
     */
    private void verificarEstadosCriticos() {
        long tiempoActual = System.currentTimeMillis();
        
        // Verificar vida baja
        if (estaVidaBaja()) {
            if (tiempoActual - ultimoMensajeVidaBaja >= configManager.getIntervaloMensajeVidaBaja() * 1000L) {
                enviarMensajeVidaBaja();
                ultimoMensajeVidaBaja = tiempoActual;
            }
        }
        
        // Verificar energía baja
        if (estaEnergiaBaja()) {
            if (tiempoActual - ultimoMensajeEnergiaBaja >= configManager.getIntervaloMensajeEnergiaBaja() * 1000L) {
                enviarMensajeEnergiaBaja();
                ultimoMensajeEnergiaBaja = tiempoActual;
            }
        }
        
        // Actualizar estado crítico
        enEstadoCritico = estaVidaBaja() || estaEnergiaBaja();
    }
    
    /**
     * Envía mensaje de vida baja
     */
    private void enviarMensajeVidaBaja() {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("vida", vida);
        placeholders.put("vida_maxima", configManager.getVidaMaxima());
        placeholders.put("porcentaje", getPorcentajeVida());
        
        String mensaje = configManager.replacePlaceholders(
            configManager.getPrefijo() + configManager.getMensajeVidaBaja(), 
            placeholders
        );
        
        Bukkit.broadcastMessage(mensaje);
        
        // Reproducir sonido
        if (configManager.isSonidosHabilitados()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(ubicacion.getWorld())) {
                    player.playSound(player.getLocation(), configManager.getSonidoVidaBaja(), 1.0f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Envía mensaje de energía baja
     */
    private void enviarMensajeEnergiaBaja() {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("energia", energia);
        placeholders.put("energia_maxima", configManager.getEnergiaMaxima());
        placeholders.put("porcentaje", getPorcentajeEnergia());
        
        String mensaje = configManager.replacePlaceholders(
            configManager.getPrefijo() + configManager.getMensajeEnergiaBaja(), 
            placeholders
        );
        
        Bukkit.broadcastMessage(mensaje);
        
        // Reproducir sonido
        if (configManager.isSonidosHabilitados()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(ubicacion.getWorld())) {
                    player.playSound(player.getLocation(), configManager.getSonidoEnergiaBaja(), 1.0f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Verifica si hay jugadores cerca del Nexo
     */
    public boolean hayJugadoresCerca(int radio, int minimoJugadores) {
        if (ubicacion.getWorld() == null) return false;
        
        int jugadoresCerca = 0;
        for (Player player : ubicacion.getWorld().getPlayers()) {
            if (player.getLocation().distance(ubicacion) <= radio) {
                jugadoresCerca++;
                if (jugadoresCerca >= minimoJugadores) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Desactiva el Nexo
     */
    public void desactivar() {
        if (!activo) return;
        
        activo = false;
        
        // Enviar mensaje de desactivación
        Bukkit.broadcastMessage(configManager.getPrefijo() + configManager.getMensajeNexoDesactivado());
        
        // Reproducir sonido de desactivación
        if (configManager.isSonidosHabilitados()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(ubicacion.getWorld())) {
                    player.playSound(player.getLocation(), configManager.getSonidoReinicio(), 1.0f, 0.5f);
                }
            }
        }
        
        // Guardar estado
        guardar();
        
        logger.info("§c❌ Nexo desactivado en " + ubicacion.getWorld().getName());
    }
    
    /**
     * Activa el Nexo
     */
    public void activar() {
        if (activo) return;
        
        activo = true;
        
        // Enviar mensaje de activación
        Bukkit.broadcastMessage(configManager.getPrefijo() + configManager.getMensajeNexoActivado());
        
        // Reproducir sonido de activación
        if (configManager.isSonidosHabilitados()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(ubicacion.getWorld())) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                }
            }
        }
        
        // Guardar estado
        guardar();
        
        logger.info("§a✅ Nexo activado en " + ubicacion.getWorld().getName());
    }
    
    /**
     * Reinicia el Nexo con valores máximos
     */
    public void reiniciar() {
        this.vida = configManager.getVidaMaxima();
        this.energia = configManager.getEnergiaMaxima();
        this.activo = true;
        this.enEstadoCritico = false;
        
        // Enviar mensaje de reinicio
        Bukkit.broadcastMessage(configManager.getPrefijo() + "§a🔄 El Nexo ha sido reiniciado completamente.");
        
        // Reproducir sonido de reinicio
        if (configManager.isSonidosHabilitados()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(ubicacion.getWorld())) {
                    player.playSound(player.getLocation(), configManager.getSonidoReinicio(), 1.0f, 1.0f);
                }
            }
        }
        
        // Guardar estado
        guardar();
        
        logger.info("§a🔄 Nexo reiniciado en " + ubicacion.getWorld().getName());
    }
    
    /**
     * Destruye el Nexo y limpia sus recursos
     */
    public void destruir() {
        // Cancelar tasks
        if (taskParticulas != null && !taskParticulas.isCancelled()) {
            taskParticulas.cancel();
        }
        
        if (taskEfectos != null && !taskEfectos.isCancelled()) {
            taskEfectos.cancel();
        }
        
        // Guardar antes de destruir
        guardar();
        
        logger.info("§c❌ Nexo destruido en " + ubicacion.getWorld().getName());
    }
    
    /**
     * Recarga la configuración del Nexo
     */
    public void recargarConfiguracion(ConfigManager nuevoConfigManager) {
        this.configManager = nuevoConfigManager;
        
        // Reiniciar efectos visuales con nueva configuración
        if (taskParticulas != null && !taskParticulas.isCancelled()) {
            taskParticulas.cancel();
        }
        
        if (taskEfectos != null && !taskEfectos.isCancelled()) {
            taskEfectos.cancel();
        }
        
        iniciarEfectosVisuales();
        
        logger.info("§a✅ Configuración del Nexo recargada");
    }
    
    // ==========================================
    // MÉTODOS DE VERIFICACIÓN DE ESTADO
    // ==========================================
    
    public boolean estaVidaBaja() {
        int porcentaje = (vida * 100) / configManager.getVidaMaxima();
        return porcentaje <= configManager.getPorcentajeVidaBaja();
    }
    
    public boolean estaEnergiaBaja() {
        int porcentaje = (energia * 100) / configManager.getEnergiaMaxima();
        return porcentaje <= configManager.getPorcentajeEnergiaBaja();
    }
    
    public int getPorcentajeVida() {
        return (vida * 100) / configManager.getVidaMaxima();
    }
    
    public int getPorcentajeEnergia() {
        return (energia * 100) / configManager.getEnergiaMaxima();
    }
    
    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    
    public Location getUbicacion() {
        return ubicacion.clone();
    }
    
    public int getVida() {
        return vida;
    }
    
    public void setVida(int vida) {
        this.vida = Math.max(0, Math.min(vida, configManager.getVidaMaxima()));
        
        // Si la vida llega a 0, desactivar el Nexo
        if (this.vida <= 0 && activo) {
            desactivar();
        }
    }
    
    public int getEnergia() {
        return energia;
    }
    
    public void setEnergia(int energia) {
        this.energia = Math.max(0, Math.min(energia, configManager.getEnergiaMaxima()));
    }
    
    public boolean estaActivo() {
        return activo;
    }
    
    public boolean estaEnEstadoCritico() {
        return enEstadoCritico;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
}