package nexo.beta.classes;



import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nexo.beta.managers.ConfigManager;
import nexo.beta.utils.BarrierUtils;
import nexo.beta.utils.Utils;

public class Nexo {

    private final Location ubicacion;
    private ConfigManager configManager;
    private final Logger logger;
    private final String barrierId;
    private static final String WARDEN_TAG = "nexo_warden";
    private static final String DISINTEGRATING_TAG = "nexo_disintegrating";

    // Estados del Nexo
    private int vida;
    private int energia;
    private boolean activo;
    private boolean enEstadoCritico;

    // Radio de protección
    private int radioBase;
    private int radioActual;

    // Sistema de partículas y efectos
    private BukkitTask taskParticulas;
    private BukkitTask taskEfectos;
    private BukkitTask taskEliminarMobs;
    private int stepRunas = 0;

    // Representación física del Nexo
    private Warden warden;
    private ArmorStand texturaStand;

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
        this.barrierId = "nexo_" + this.ubicacion.getWorld().getUID();

        // Inicializar estados
        this.vida = configManager.getVidaMaxima();
        this.energia = configManager.getEnergiaMaxima();
        this.activo = true;
        this.enEstadoCritico = false;
        this.radioBase = configManager.getRadioProteccion();
        this.radioActual = this.radioBase;

        // Inicializar archivo de guardado
        inicializarArchivoGuardado();

        // Cargar datos existentes
        cargarDatos();

        // Crear entidad representativa
        spawnRepresentation();

        // Iniciar efectos visuales
        iniciarEfectosVisuales();

        // Crear barrera inicial
        actualizarBarrera();

        iniciarEliminacionMobs();

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

        if (datosNexo.contains("radio")) {
            this.radioActual = datosNexo.getInt("radio", configManager.getRadioProteccion());
        } else {
            this.radioActual = configManager.getRadioProteccion();
        }

        this.radioBase = configManager.getRadioProteccion();

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
            datosNexo.set("radio", radioActual);
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
     * Crea la representación física del Nexo como un Warden con un ArmorStand
     */
    private void spawnRepresentation() {
        if (ubicacion.getWorld() == null) return;

        warden = (Warden) ubicacion.getWorld().spawnEntity(ubicacion, EntityType.WARDEN);
        warden.setAI(false);
        warden.setSilent(true);
        warden.addScoreboardTag(WARDEN_TAG);
        warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(configManager.getVidaMaxima());
        warden.setHealth(Math.min(vida, configManager.getVidaMaxima()));

        texturaStand = (ArmorStand) ubicacion.getWorld().spawnEntity(ubicacion, EntityType.ARMOR_STAND);
        texturaStand.setInvisible(true);
        texturaStand.setMarker(true);
        texturaStand.setGravity(false);
        ItemStack flint = new ItemStack(Material.FLINT);
        ItemMeta meta = flint.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("NexoAndCorruption"), "texturaNexo");
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
            flint.setItemMeta(meta);
        }
        texturaStand.getEquipment().setHelmet(flint);
        warden.addPassenger(texturaStand);
    }

    /**
     * Elimina la representación física del Nexo
     */
    private void destroyRepresentation() {
        if (texturaStand != null && !texturaStand.isDead()) {
            texturaStand.remove();
        }
        if (warden != null && !warden.isDead()) {
            warden.remove();
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

            for (int i = 0; i < 36; i++) {
                double angle = Math.toRadians(i * 10 + (stepRunas * 5));
                Location p = ubicacion.clone().add(Math.cos(angle) * 1.2, 0.05, Math.sin(angle) * 1.2);
                ubicacion.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, p, 1, 0, 0, 0, 0);
            }
            stepRunas++;

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

        boolean nuevoEstado = estaVidaBaja() || estaEnergiaBaja();
        if (nuevoEstado != enEstadoCritico) {
            enEstadoCritico = nuevoEstado;
            actualizarBarrera();
        } else {
            enEstadoCritico = nuevoEstado;
        }
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
     * Muestra efectos de daño al Nexo
     */
    private void mostrarDanio() {
        if (ubicacion.getWorld() == null) return;

        ubicacion.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                ubicacion.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);

        if (configManager.isSonidosHabilitados()) {
            for (Player player : ubicacion.getWorld().getPlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_HURT,
                        1.0f, 1.0f);
            }
        }

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("vida", vida);
        placeholders.put("vida_maxima", configManager.getVidaMaxima());
        String msg = configManager.replacePlaceholders(
                configManager.getMensajeNexoDanado(), placeholders);
        Bukkit.broadcastMessage(msg);

        if (estaVidaBaja()) {
            enviarMensajeVidaBaja();
        }
    }

    /**
     * Muestra un destello y sonido de explosión al destruirse el Nexo
     */
    private void mostrarDestruccion() {
        if (ubicacion.getWorld() == null) return;

        ubicacion.getWorld().spawnParticle(Particle.FLASH, ubicacion, 50, 1, 1, 1, 0);
        ubicacion.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, ubicacion, 1);
        ubicacion.getWorld().playSound(ubicacion, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
    }

    /**
     * Envía un mensaje global y reproduce un sonido cuando el Nexo muere.
     */
    private void enviarMensajeMuerte() {
        String mensaje = configManager.getPrefijo() + configManager.getMensajeNexoInactivo();
        if (configManager.isBroadcastNexoInactivo()) {
            Bukkit.broadcastMessage(mensaje);
        } else if (ubicacion.getWorld() != null) {
            for (Player player : ubicacion.getWorld().getPlayers()) {
                player.sendMessage(mensaje);
            }
        }

        if (configManager.isSonidosHabilitados()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), configManager.getSonidoMuerte(), 1.0f, 1.0f);
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

        detenerEliminacionMobs();

        actualizarBarrera();

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

        iniciarEliminacionMobs();

        actualizarBarrera();

        logger.info("§a✅ Nexo activado en " + ubicacion.getWorld().getName());
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

        // Eliminar representación física
        destroyRepresentation();

        detenerEliminacionMobs();

        eliminarBarrera();

        eliminarArchivoGuardado();

        logger.info("§c❌ Nexo destruido en " + ubicacion.getWorld().getName());
    }

    /**
     * Recarga la configuración del Nexo
     */
    public void recargarConfiguracion(ConfigManager nuevoConfigManager) {
        this.configManager = nuevoConfigManager;

        this.radioBase = configManager.getRadioProteccion();
        if (radioActual < radioBase) {
            radioActual = radioBase;
        }

        // Reiniciar efectos visuales con nueva configuración
        if (taskParticulas != null && !taskParticulas.isCancelled()) {
            taskParticulas.cancel();
        }

        if (taskEfectos != null && !taskEfectos.isCancelled()) {
            taskEfectos.cancel();
        }

        detenerEliminacionMobs();

        iniciarEfectosVisuales();
        if (activo) {
            iniciarEliminacionMobs();
        }
        actualizarBarrera();

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
        int vidaAnterior = this.vida;
        this.vida = Math.max(0, Math.min(vida, configManager.getVidaMaxima()));

        if (warden != null && !warden.isDead()) {
            double max = warden.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            warden.setHealth(Math.max(0.0, Math.min(this.vida, max)));
        }

        if (vidaAnterior > this.vida) {
            mostrarDanio();
        }

        // Si la vida llega a 0, eliminar el Nexo por completo
        if (this.vida <= 0 && activo) {
            mostrarDestruccion();
            enviarMensajeMuerte();
            nexo.beta.managers.NexoManager manager = nexo.beta.managers.NexoManager.getInstance();
            if (manager != null) {
                manager.eliminarNexo(ubicacion.getWorld());
            } else {
                destruir();
            }
        }
    }

    public int getEnergia() {
        return energia;
    }

    public void setEnergia(int energia) {
        int max = configManager.getEnergiaMaxima();
        this.energia = Math.max(0, Math.min(energia, max));

        if (this.energia <= 0 && activo) {
            desactivar();
        }

        if (!activo && this.energia >= max / 2) {
            activar();
            nexo.beta.managers.PluginManager.getInstance().getInvasionManager().detenerInvasion();
        }
    }

    public void alimentar(int cantidad) {
        setEnergia(this.energia + cantidad);
    }

    public boolean estaActivo() {
        return activo;
    }

    public boolean estaEnEstadoCritico() {
        return enEstadoCritico;
    }


    public Warden getWarden() {
        return warden;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    // ==========================================
    // MÉTODOS DE RADIO DE PROTECCIÓN
    // ==========================================

    public int getRadioActual() {
        return radioActual;
    }

    public void expandirRadio(int cantidad) {
        this.radioActual = Math.max(radioBase, radioActual + cantidad);
        actualizarBarrera();
    }

    public void resetRadio() {
        this.radioActual = radioBase;
        actualizarBarrera();
    }

    // ==========================================
    // MÉTODOS DE BARRERA
    // ==========================================

    private void actualizarBarrera() {
        if (ubicacion.getWorld() != null) {
            BarrierUtils.createNexoBarrier(barrierId, ubicacion, radioActual, activo, enEstadoCritico);
        }
    }

    private void eliminarBarrera() {
        BarrierUtils.removeBarrier(barrierId + "_barrier");
    }

    // ==========================================
    // ELIMINACIÓN DE MOBS HOSTILES SIN NOMBRE
    // ==========================================

    private void iniciarEliminacionMobs() {
        if (taskEliminarMobs != null && !taskEliminarMobs.isCancelled()) return;

        taskEliminarMobs = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activo || ubicacion.getWorld() == null) return;
                for (org.bukkit.entity.Entity e : ubicacion.getWorld().getNearbyEntities(ubicacion, radioActual, radioActual, radioActual)) {
                    if (e instanceof org.bukkit.entity.Monster m) {
                        if (m.getScoreboardTags().contains(WARDEN_TAG)) {
                            continue;
                        }
                        String name = m.getCustomName();
                        if (name == null || name.isBlank()) {
                            if (!m.getScoreboardTags().contains(DISINTEGRATING_TAG)) {
                                m.addScoreboardTag(DISINTEGRATING_TAG);
                                nexo.beta.utils.DisintegrationUtil.disintegrate(m);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("NexoAndCorruption"), 20L, 20L);
    }

    private void detenerEliminacionMobs() {
        if (taskEliminarMobs != null && !taskEliminarMobs.isCancelled()) {
            taskEliminarMobs.cancel();
        }
    }

    private void eliminarArchivoGuardado() {
        if (archivoGuardado != null && archivoGuardado.exists()) {
            if (!archivoGuardado.delete()) {
                logger.warning("§c⚠️ No se pudo eliminar el archivo del Nexo: " + archivoGuardado.getName());
            }
        }
    }
}
