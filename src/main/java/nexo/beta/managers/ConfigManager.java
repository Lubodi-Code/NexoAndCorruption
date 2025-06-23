package nexo.beta.managers;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {


    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration nexoConfig;
    private File nexoFile;
    private FileConfiguration corruptionConfig;
    private File corruptionFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeConfigs();
    }

    private void initializeConfigs() {
        // Cargar config.yml por defecto
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Cargar nexo.yml
        nexoFile = new File(plugin.getDataFolder(), "nexo.yml");
        if (!nexoFile.exists()) {
            plugin.saveResource("nexo.yml", false);
        }
        nexoConfig = YamlConfiguration.loadConfiguration(nexoFile);

        // Cargar corruption.yml
        corruptionFile = new File(plugin.getDataFolder(), "corruption.yml");
        if (!corruptionFile.exists()) {
            plugin.saveResource("corruption.yml", false);
        }
        corruptionConfig = YamlConfiguration.loadConfiguration(corruptionFile);
    }

    /**
     * Recarga todas las configuraciones
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        nexoConfig = YamlConfiguration.loadConfiguration(nexoFile);
        corruptionConfig = YamlConfiguration.loadConfiguration(corruptionFile);
    }

    // ==========================================
    // MÉTODOS PARA CONFIGURACIÓN BÁSICA DEL NEXO
    // ==========================================

    public int getVidaMaxima() {
        return nexoConfig.getInt("nexo.vida_maxima", 1000);
    }

    public int getEnergiaMaxima() {
        return nexoConfig.getInt("nexo.energia_maxima", 1000);
    }

    public int getConsumoEnergiaBase() {
        return nexoConfig.getInt("nexo.energia_consumo_base", 17);
    }

    public int getIntervaloConsumoEnergia() {
        return nexoConfig.getInt("nexo.energia_consumo_intervalo", 10);
    }

    public int getRadioProteccion() {
        return nexoConfig.getInt("nexo.radio_proteccion", 500);
    }

    // ==========================================
    // MÉTODOS PARA UBICACIÓN DEL NEXO
    // ==========================================

    public Location getUbicacionNexo() {
        String mundo = nexoConfig.getString("nexo.ubicacion.mundo", "world");
        double x = nexoConfig.getDouble("nexo.ubicacion.x", 0);
        double y = nexoConfig.getDouble("nexo.ubicacion.y", 100);
        double z = nexoConfig.getDouble("nexo.ubicacion.z", 0);

        World world = Bukkit.getWorld(mundo);
        if (world == null) {
            world = Bukkit.getWorlds().get(0); // Usar el primer mundo disponible
        }

        return new Location(world, x, y, z);
    }

    // ==========================================
    // MÉTODOS PARA INVASIONES
    // ==========================================

    public String getMensajePrevioEvento() {
        return nexoConfig.getString("nexo.invasion.mensaje_previo",
            "‼️ El Nexo siente una gran perturbación en la energía...");
    }

    public String getMensajeInicioEvento() {
        return nexoConfig.getString("nexo.invasion.mensaje_inicio",
            "🔥 ¡INVASIÓN ACTIVADA! Las defensas del Nexo han fallado.");
    }

    public String getMensajeFinEvento() {
        return nexoConfig.getString("nexo.invasion.mensaje_fin",
            "✅ El Nexo ha recuperado su estabilidad. La invasión ha terminado.");
    }

    public double getInvasionProbabilidad() {
        return nexoConfig.getDouble("nexo.invasion.probabilidad", 0.1);
    }

    public int getInvasionAdvertencia() {
        return nexoConfig.getInt("nexo.invasion.advertencia", 30);
    }

    public int getInvasionDuracion() {
        return nexoConfig.getInt("nexo.invasion.duracion", 30);
    }

    public int getInvasionRadioSpawn() {
        return nexoConfig.getInt("nexo.invasion.radio_spawn", 100);
    }

    public int getInvasionRadioSpawnMin() {
        return nexoConfig.getInt("nexo.invasion.radio_spawn_min", 80);
    }

    public int getInvasionCheckInterval() {
        return nexoConfig.getInt("nexo.invasion.check_interval", 60);
    }

    public Map<String, Double> getInvasionMobs() {
        Map<String, Double> result = new java.util.HashMap<>();
        if (nexoConfig.isConfigurationSection("nexo.invasion.mobs")) {
            for (String key : nexoConfig.getConfigurationSection("nexo.invasion.mobs").getKeys(false)) {
                result.put(key.toUpperCase(), nexoConfig.getDouble("nexo.invasion.mobs." + key));
            }
        }
        return result;
    }

    // ==========================================
    // MÉTODOS PARA PROTECCIONES
    // ==========================================

    public boolean isProteccionExplosionesHabilitada() {
        return nexoConfig.getBoolean("nexo.protecciones.explosiones.habilitado", true);
    }

    public boolean isBloquearExplosiones() {
        return nexoConfig.getBoolean("nexo.protecciones.explosiones.bloquear", true);
    }

    public String getMensajeProteccionExplosiones() {
        return nexoConfig.getString("nexo.protecciones.explosiones.mensaje",
            "🛡️ El Nexo protege esta área de explosiones.");
    }

    public boolean isProteccionPvPHabilitada() {
        return nexoConfig.getBoolean("nexo.protecciones.pvp.habilitado", true);
    }

    public boolean isBloquearPvP() {
        return nexoConfig.getBoolean("nexo.protecciones.pvp.bloquear", true);
    }

    public String getMensajeProteccionPvP() {
        return nexoConfig.getString("nexo.protecciones.pvp.mensaje",
            "⚔️ El Nexo no permite combate en esta zona protegida.");
    }

    public boolean isProteccionBloquesHabilitada() {
        return nexoConfig.getBoolean("nexo.protecciones.bloques.habilitado", true);
    }

    public boolean isPermitirConstruccion() {
        return nexoConfig.getBoolean("nexo.protecciones.bloques.permitir_construccion", false);
    }

    public boolean isPermitirDestruccion() {
        return nexoConfig.getBoolean("nexo.protecciones.bloques.permitir_destruccion", false);
    }

    public int getRadioRestriccionBloques() {
        return nexoConfig.getInt("nexo.protecciones.bloques.radio_restriccion", 30);
    }

    public String getMensajeProteccionBloques() {
        return nexoConfig.getString("nexo.protecciones.bloques.mensaje",
            "🏗️ El Nexo protege los bloques de esta área.");
    }

    public boolean isProteccionContenedoresHabilitada() {
        return nexoConfig.getBoolean("nexo.protecciones.contenedores.habilitado", true);
    }

    public int getRadioRestriccionContenedores() {
        return nexoConfig.getInt("nexo.protecciones.contenedores.radio_restriccion", getRadioRestriccionBloques());
    }

    public String getMensajeProteccionContenedores() {
        return nexoConfig.getString("nexo.protecciones.contenedores.mensaje",
            "📦 Los contenedores están protegidos por el Nexo.");
    }

    // ==========================================
    // MÉTODOS PARA REGENERACIÓN
    // ==========================================

    public boolean isRegeneracionHabilitada() {
        return nexoConfig.getBoolean("nexo.regeneracion.habilitado", true);
    }

    public int getEnergiaPorSegundo() {
        return nexoConfig.getInt("nexo.regeneracion.energia_por_segundo", 2);
    }

    public int getVidaPorSegundo() {
        return nexoConfig.getInt("nexo.regeneracion.vida_por_segundo", 1);
    }

    public boolean isRequiereJugadoresCerca() {
        return nexoConfig.getBoolean("nexo.regeneracion.requiere_jugadores_cerca", false);
    }

    public int getRadioMinimoJugadores() {
        return nexoConfig.getInt("nexo.regeneracion.radio_minimo_jugadores", 20);
    }

    public int getMinimoJugadores() {
        return nexoConfig.getInt("nexo.regeneracion.minimo_jugadores", 1);
    }

    // ==========================================
    // MÉTODOS PARA EFECTOS VISUALES
    // ==========================================

    public boolean isParticulasHabilitadas() {
        return nexoConfig.getBoolean("nexo.efectos.particulas.habilitado", true);
    }

    public String getTipoParticula() {
        return nexoConfig.getString("nexo.efectos.particulas.tipo", "ENCHANTMENT_TABLE");
    }

    public int getCantidadParticulas() {
        return nexoConfig.getInt("nexo.efectos.particulas.cantidad", 10);
    }

    public int getIntervaloParticulas() {
        return nexoConfig.getInt("nexo.efectos.particulas.intervalo", 5);
    }

    public boolean isHaloHabilitado() {
        return nexoConfig.getBoolean("nexo.efectos.halo.habilitado", true);
    }

    public boolean isSonidoBeaconHabilitado() {
        return nexoConfig.getBoolean("nexo.efectos.sonido_beacon.habilitado", true);
    }

    // ==========================================
    // MÉTODOS PARA REPRESENTACIÓN DEL NEXO
    // ==========================================

    public boolean isWardenInvisible() {
        return nexoConfig.getBoolean("nexo.representacion.warden_invisible", false);
    }

    public boolean isSonidosHabilitados() {
        return nexoConfig.getBoolean("nexo.efectos.sonidos.habilitado", true);
    }

    public Sound getSonidoEnergiaBaja() {
        String soundName = nexoConfig.getString("nexo.efectos.sonidos.energia_baja", "BLOCK_NOTE_BLOCK_BASS");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_BASS;
        }
    }

    public Sound getSonidoVidaBaja() {
        String soundName = nexoConfig.getString("nexo.efectos.sonidos.vida_baja", "ENTITY_VILLAGER_HURT");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_VILLAGER_HURT;
        }
    }

    public Sound getSonidoReinicio() {
        String soundName = nexoConfig.getString("nexo.efectos.sonidos.reinicio", "BLOCK_END_PORTAL_SPAWN");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_END_PORTAL_SPAWN;
        }
    }

    public Sound getSonidoMuerte() {
        String soundName = nexoConfig.getString("nexo.efectos.sonidos.muerte", "ENTITY_WITHER_DEATH");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_WITHER_DEATH;
        }
    }

    // ==========================================
    // MÉTODOS PARA ESTADOS CRÍTICOS
    // ==========================================

    public int getPorcentajeVidaBaja() {
        return nexoConfig.getInt("nexo.estados_criticos.vida_baja.porcentaje", 25);
    }

    public String getMensajeVidaBaja() {
        return nexoConfig.getString("nexo.estados_criticos.vida_baja.mensaje",
            "🆘 ¡ALERTA! El Nexo está gravemente dañado ({vida}/{vida_maxima})");
    }

    public int getIntervaloMensajeVidaBaja() {
        return nexoConfig.getInt("nexo.estados_criticos.vida_baja.intervalo_mensaje", 30);
    }

    public int getPorcentajeEnergiaBaja() {
        return nexoConfig.getInt("nexo.estados_criticos.energia_baja.porcentaje", 20);
    }

    public String getMensajeEnergiaBaja() {
        return nexoConfig.getString("nexo.estados_criticos.energia_baja.mensaje",
            "⚡ El Nexo tiene poca energía ({energia}/{energia_maxima}). Las protecciones se debilitan.");
    }

    public int getIntervaloMensajeEnergiaBaja() {
        return nexoConfig.getInt("nexo.estados_criticos.energia_baja.intervalo_mensaje", 45);
    }

    public String getMensajeNexoInactivo() {
        return nexoConfig.getString("nexo.estados_criticos.nexo_inactivo.mensaje",
            "💀 ¡EL NEXO HA CAÍDO! Las protecciones han desaparecido.");
    }

    public boolean isBroadcastNexoInactivo() {
        return nexoConfig.getBoolean("nexo.estados_criticos.nexo_inactivo.broadcast", true);
    }

    // ==========================================
    // MÉTODOS PARA COMANDOS
    // ==========================================

    public boolean isComandoEstadoHabilitado() {
        return nexoConfig.getBoolean("nexo.comandos.estado.habilitado", true);
    }

    public String getPermisoComandoEstado() {
        return nexoConfig.getString("nexo.comandos.estado.permiso", "nexo.estado");
    }

    public boolean isComandoRecargarHabilitado() {
        return nexoConfig.getBoolean("nexo.comandos.recargar.habilitado", true);
    }

    public String getPermisoComandoRecargar() {
        return nexoConfig.getString("nexo.comandos.recargar.permiso", "nexo.admin.recargar");
    }

    public boolean isComandoReiniciarHabilitado() {
        return nexoConfig.getBoolean("nexo.comandos.reiniciar.habilitado", true);
    }

    public String getPermisoComandoReiniciar() {
        return nexoConfig.getString("nexo.comandos.reiniciar.permiso", "nexo.admin.reiniciar");
    }

    // ==========================================
    // MÉTODOS PARA GUARDADO
    // ==========================================

    public int getIntervaloGuardadoAuto() {
        return nexoConfig.getInt("nexo.guardado.intervalo_auto", 300);
    }

    public boolean isGuardarAlApagar() {
        return nexoConfig.getBoolean("nexo.guardado.guardar_al_apagar", true);
    }

    public String getArchivoGuardado() {
        return nexoConfig.getString("nexo.guardado.archivo", "nexo_data.yml");
    }

    // ==========================================
    // MÉTODOS PARA MENSAJES
    // ==========================================

    public String getPrefijo() {
        return nexoConfig.getString("nexo.mensajes.prefijo", "§8[§6Nexo§8] §r");
    }

    public String getMensajeNexoActivado() {
        return getPrefijo() + nexoConfig.getString("nexo.mensajes.nexo_activado",
            "§a✅ El Nexo ha sido activado y está protegiendo el área.");
    }

    public String getMensajeNexoDesactivado() {
        return getPrefijo() + nexoConfig.getString("nexo.mensajes.nexo_desactivado",
            "§c❌ El Nexo ha sido desactivado. ¡Cuidado!");
    }

    public String getMensajeNexoDanado() {
        return getPrefijo() + nexoConfig.getString("nexo.mensajes.nexo_danado",
                "§c⚠️ ¡El Nexo está siendo atacado! ({vida}/{vida_maxima})");
    }

    public String getMensajeInvasionActivada() {
        return getPrefijo() + nexoConfig.getString("nexo.mensajes.invasion_activada",
                "§4⚠️ ¡La invasión ha comenzado!");
    }

    public String getMensajeEstadoVida() {
        return nexoConfig.getString("nexo.mensajes.estado_vida",
            "§6Vida del Nexo: §e{vida}§6/§e{vida_maxima} §6({porcentaje}%)");
    }

    public String getMensajeEstadoEnergia() {
        return nexoConfig.getString("nexo.mensajes.estado_energia",
            "§b⚡ Energía: §e{energia}§b/§e{energia_maxima} §b({porcentaje}%)");
    }

    public String getMensajeNexoNoEncontrado() {
        return getPrefijo() + nexoConfig.getString("nexo.mensajes.nexo_no_encontrado",
            "§cError: No se pudo encontrar el Nexo.");
    }

    public String getMensajeSinPermisos() {
        return getPrefijo() + nexoConfig.getString("nexo.mensajes.sin_permisos",
            "§cNo tienes permisos para usar este comando.");
    }

    // ==========================================
    // MÉTODOS PARA DEBUG
    // ==========================================

    public boolean isDebugHabilitado() {
        return nexoConfig.getBoolean("debug.habilitado", false);
    }

    public boolean isMostrarCoordenadas() {
        return nexoConfig.getBoolean("debug.mostrar_coordenadas", true);
    }

    public boolean isMostrarCalculosEnergia() {
        return nexoConfig.getBoolean("debug.mostrar_calculos_energia", false);
    }

    public boolean isMostrarEventos() {
        return nexoConfig.getBoolean("debug.mostrar_eventos", true);
    }

    // ==========================================
    // MÉTODOS PARA LA CORRUPCIÓN
    // ==========================================

    public boolean isCorruptionEnabled() {
        return corruptionConfig.getBoolean("corruption.habilitado", true);
    }

    public int getCorruptionSpreadInterval() {
        return corruptionConfig.getInt("corruption.intervalo_expansion", 200);
    }

    public int getCorruptionBlocksPerCycle() {
        return corruptionConfig.getInt("corruption.bloques_por_ciclo", 5);
    }

    public int getCorruptionAreaMin() {
        return corruptionConfig.getInt("corruption.area_min", 100);
    }

    public int getCorruptionAreaMax() {
        return corruptionConfig.getInt("corruption.area_max", 200);
    }

    public List<Material> getCorruptionBlocks() {
        List<String> names = corruptionConfig.getStringList("corruption.bloques");
        List<Material> mats = new ArrayList<>();
        for (String name : names) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                mats.add(mat);
            }
        }
        if (mats.isEmpty()) {
            mats.add(Material.NETHERRACK);
        }
        return mats;
    }

    public double getCorruptionSurfaceProbability() {
        return corruptionConfig.getDouble("corruption.probabilidad_superficie", 0.8);
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    /**
     * Reemplaza placeholders en mensajes
     */
    public String replacePlaceholders(String message, Map<String, Object> placeholders) {
        String result = message;
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * Obtiene el archivo de configuración del nexo
     */
    public FileConfiguration getNexoConfig() {
        return nexoConfig;
    }

    /**
     * Obtiene el archivo de configuración de la corrupción
     */
    public FileConfiguration getCorruptionConfig() {
        return corruptionConfig;
    }

    /**
     * Obtiene el archivo de configuración principal
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
