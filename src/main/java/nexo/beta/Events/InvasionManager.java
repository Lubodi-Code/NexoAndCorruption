package nexo.beta.Events;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
    /** Indica si existe una invasión en curso (desde el aviso hasta su fin). */
    private boolean invasionEnCurso = false;
    private boolean invasionActiva = false;
    private boolean invasionInfinita = false;
    private final Map<Nexo, Boolean> estadoPrevio = new HashMap<>();

    public InvasionManager(NexoAndCorruption plugin, NexoManager nexoManager, ConfigManager config) {
        this.plugin = plugin;
        this.nexoManager = nexoManager;
        this.config = config;
    }

    public void start() {
        long intervalo = config.getInvasionCheckInterval() * 20L; // convertir segundos a ticks
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                verificarCondiciones();
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
        invasionEnCurso = false;
        estadoPrevio.clear();
    }

    public void reload(ConfigManager newConfig) {
        shutdown();
        this.config = newConfig;
        start();
    }

    public void forzarInvasion() {
        if (!invasionEnCurso) {
            iniciarCuentaRegresiva();
        }
    }

    private void verificarCondiciones() {
        if (!invasionEnCurso) {
            if (nexoManager.getNexosActivos() <= 0) {
                return;
            }
            double prob = config.getInvasionProbabilidad();
            if (Math.random() <= prob) {
                iniciarCuentaRegresiva();
            }
        }
    }

    private void iniciarCuentaRegresiva() {
        invasionEnCurso = true;
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajePrevioEvento()));
        new BukkitRunnable() {
            int tiempo = config.getInvasionAdvertencia();
            @Override
            public void run() {
                if (tiempo <= 0) {
                    iniciarInvasion(config.getInvasionDuracion());
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

    private void iniciarInvasion(int duracion) {
        invasionActiva = true;
        invasionInfinita = duracion < 0;
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajeInvasionActivada()));
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajeInicioEvento()));
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
        if (!invasionInfinita) {
            Bukkit.broadcastMessage(Utils.colorize(
                config.getPrefijo() + "La invasión durará " + formatTime(duracion) + "."));
        }
        estadoPrevio.clear();
        for (Nexo nexo : nexoManager.getTodosLosNexos().values()) {
            estadoPrevio.put(nexo, nexo.estaActivo());
            if (nexo.estaActivo()) {
                nexo.desactivar();
            }
        }
        spawnTask = new BukkitRunnable() {
            int tiempo = duracion;
            @Override
            public void run() {
                if (!invasionInfinita) {
                    if (tiempo <= 0) {
                        finalizarInvasion();
                        cancel();
                        return;
                    }
                    if (tiempo <= 5) {
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                            p.spawnParticle(org.bukkit.Particle.SMOKE, p.getLocation().add(0,1,0), 10, 0.5, 0.5, 0.5, 0.01);
                        }
                    }
                    tiempo--;
                }
                generarEntidades();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void finalizarInvasion() {
        invasionActiva = false;
        invasionEnCurso = false;
        invasionInfinita = false;
        Bukkit.broadcastMessage(Utils.colorize(config.getMensajeFinEvento()));
        for (Map.Entry<Nexo, Boolean> entry : estadoPrevio.entrySet()) {
            if (entry.getValue()) {
                entry.getKey().activar();
            }
        }
        estadoPrevio.clear();

        // Efecto de onda de choque al finalizar
        for (Nexo nexo : nexoManager.getTodosLosNexos().values()) {
            Location origin = nexo.getUbicacion();
            if (origin.getWorld() == null) continue;
            for (double r = 0; r < 2.5; r += 0.25) {
                origin.getWorld().spawnParticle(
                        Particle.CLOUD,
                        origin.clone().add(0, 0.1, 0),
                        20,
                        r, 0.1, r,
                        0.02
                );
            }
            origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
        }
    }

    public void detenerInvasion() {
        if (invasionEnCurso) {
            finalizarInvasion();
        }
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
            int maxDist = config.getInvasionRadioSpawn();
            int minDist = config.getInvasionRadioSpawnMin();
            double distancia = minDist + rnd.nextDouble() * (maxDist - minDist);
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

    private static String formatTime(int seconds) {
        int days = seconds / 86400;
        seconds %= 86400;
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Indica si existe una invasión en curso (contando o activa).
     */
    public boolean isInvasionEnCurso() {
        return invasionEnCurso;
    }
}
