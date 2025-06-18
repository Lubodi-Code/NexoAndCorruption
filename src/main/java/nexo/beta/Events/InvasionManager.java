package nexo.beta.Events;

import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nexo.beta.NexoAndCorruption;
import nexo.beta.classes.Nexo;
import nexo.beta.managers.ConfigManager;
import nexo.beta.managers.NexoManager;
import nexo.beta.utils.Utils;

public class InvasionManager {

    private final NexoAndCorruption plugin;
    private final NexoManager nexoManager;
    private ConfigManager config;

    private BukkitTask checkTask;
    private BukkitTask spawnTask;
    private boolean invasionActiva = false;

    public InvasionManager(NexoAndCorruption plugin, NexoManager nexoManager, ConfigManager config) {
        this.plugin = plugin;
        this.nexoManager = nexoManager;
        this.config = config;
    }

    public void start() {
        long intervalo = 20L * 60; // cada minuto
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                verificarInvasion();
            }
        }.runTaskTimer(plugin, intervalo, intervalo);
    }

    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        invasionActiva = false;
    }

    public void reload(ConfigManager newConfig) {
        shutdown();
        this.config = newConfig;
        start();
    }

    public void forzarInvasion() {
        if (!invasionActiva) {
            iniciarCuentaRegresiva();
        }
    }

    private void verificarInvasion() {
        if (invasionActiva) return;
        double prob = config.getInvasionProbabilidad();
        if (Math.random() <= prob) {
            iniciarCuentaRegresiva();
        }
    }

    private void iniciarCuentaRegresiva() {
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajePrevioEvento()));
        new BukkitRunnable() {
            int tiempo = config.getInvasionAdvertencia();
            @Override
            public void run() {
                if (tiempo <= 0) {
                    iniciarInvasion();
                    cancel();
                    return;
                }
                if (tiempo % 10 == 0 || tiempo <= 5) {
                    Bukkit.broadcastMessage(Utils.colorize(config.getPrefijo() + "§cInvasión en " + tiempo + "s"));
                }
                tiempo--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void iniciarInvasion() {
        invasionActiva = true;
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajeInicioEvento()));
        spawnTask = new BukkitRunnable() {
            int tiempo = config.getInvasionDuracion();
            @Override
            public void run() {
                if (tiempo <= 0) {
                    finalizarInvasion();
                    cancel();
                    return;
                }
                generarEntidades();
                tiempo--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void finalizarInvasion() {
        invasionActiva = false;
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajeFinEvento()));
    }

    private void generarEntidades() {
        Map<String, Double> mobs = config.getInvasionMobs();
        if (mobs.isEmpty()) return;
        Random rnd = new Random();
        for (Nexo nexo : nexoManager.getTodosLosNexos().values()) {
            Location centro = nexo.getUbicacion();
            World world = centro.getWorld();
            if (world == null) continue;

            double angulo = rnd.nextDouble() * 2 * Math.PI;
            double distancia = rnd.nextDouble() * config.getInvasionRadioSpawn();
            double x = centro.getX() + Math.cos(angulo) * distancia;
            double z = centro.getZ() + Math.sin(angulo) * distancia;
            double y = world.getHighestBlockYAt((int) x, (int) z) + 1;
            Location loc = new Location(world, x, y, z);

            EntityType tipo = elegirTipo(mobs, rnd.nextDouble());
            Entity entidad = world.spawnEntity(loc, tipo);
            if (entidad instanceof Monster monster) {
                monster.setTarget(nexo.getWarden());
            }
        }
    }

    private EntityType elegirTipo(Map<String, Double> mapa, double random) {
        double acumulado = 0;
        for (Map.Entry<String, Double> entry : mapa.entrySet()) {
            acumulado += entry.getValue();
            if (random <= acumulado) {
                try {
                    return EntityType.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    // ignore invalid
                }
            }
        }
        return EntityType.ZOMBIE;
    }
}
