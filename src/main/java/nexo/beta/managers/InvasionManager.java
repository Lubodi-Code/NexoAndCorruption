package nexo.beta.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nexo.beta.NexoAndCorruption;
import nexo.beta.classes.Nexo;

/**
 * Maneja las invasiones que ocurren sobre el Nexo.
 */
public class InvasionManager {

    private final NexoAndCorruption plugin;
    private final ConfigManager config;
    private final Logger logger;

    private BukkitTask invasionTask;

    public InvasionManager(NexoAndCorruption plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    /**
     * Inicia una invasión en el Nexo indicado.
     */
    public void startInvasion(Nexo nexo) {
        if (invasionTask != null && !invasionTask.isCancelled()) {
            return; // Ya hay una invasión en curso
        }

        int porcentajeEnergia = nexo.getPorcentajeEnergia();
        // Duración base de 5 minutos con energía al 100%. Aumenta según se reduce la energía.
        int duracionMinutos = 5 + (100 - porcentajeEnergia) / 20;

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("minutos", duracionMinutos);

        String mensaje = config.replacePlaceholders(
                config.getMensajeInvasionDuracion(), placeholders);
        Bukkit.broadcastMessage(mensaje);

        if (config.isSonidosHabilitados()) {
            Sound sonido = Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), sonido, 1.0f, 1.0f);
            }
        }

        invasionTask = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(config.getMensajeFinInvasion());
                invasionTask = null;
            }
        }.runTaskLater(plugin, duracionMinutos * 60L * 20L);

        logger.info("Invasión iniciada con duración de " + duracionMinutos + " minutos");
    }

    /**
     * Detiene la invasión actual si existe.
     */
    public void stopInvasion() {
        if (invasionTask != null && !invasionTask.isCancelled()) {
            invasionTask.cancel();
            invasionTask = null;
            Bukkit.broadcastMessage(config.getMensajeInvasionCancelada());
            logger.info("Invasión detenida");
        }
    }

    public boolean isInvasionRunning() {
        return invasionTask != null && !invasionTask.isCancelled();
    }
}
