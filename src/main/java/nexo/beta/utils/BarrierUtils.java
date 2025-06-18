package nexo.beta.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class BarrierUtils {

    private static final Map<String, BukkitTask> activeBarriers = new ConcurrentHashMap<>();
    private static final Map<String, BarrierData> barrierConfigs = new ConcurrentHashMap<>();

    /**
     * Clase para almacenar configuración de barrera
     */
    public static class BarrierData {
        private final Location center;
        private final double radius;
        private final Particle particle;
        private final Color color;
        private final int density;
        private final boolean pulseEffect;

        public BarrierData(Location center, double radius, Particle particle,
                           Color color, int density, boolean detectPlayers, boolean pulseEffect) {
            this.center = center.clone();
            this.radius = radius;
            this.particle = particle;
            this.color = color;
            this.density = density;
            this.pulseEffect = pulseEffect;
        }

        public Location getCenter() { return center.clone(); }
        public double getRadius() { return radius; }
        public Particle getParticle() { return particle; }
        public Color getColor() { return color; }
        public int getDensity() { return density; }
        public boolean hasPulseEffect() { return pulseEffect; }
    }

    /**
     * Crea una barrera circular en el suelo (2D)
     */
    public static void createGroundBarrier(String id, Location center, double radius,
                                           Particle particle, Color color, int density) {
        removeBarrier(id);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (center.getWorld() == null) {
                    removeBarrier(id);
                    return;
                }

                for (int i = 0; i < density; i++) {
                    double angle = (Math.PI * 2 * i) / density;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLocation = center.clone().add(x, 0.1, z);

                    if (particle == Particle.REDSTONE && color != null) {
                        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 4.0f);
                        center.getWorld().spawnParticle(particle, particleLocation,
                                3, 0.2, 0.2, 0.2, 0.2, dustOptions);
                    } else {
                        center.getWorld().spawnParticle(particle, particleLocation,
                                3, 0.2, 0.2, 0.2, 0.2);
                    }
                }
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NexoAndCorruption"),
                0L, 10L);

        activeBarriers.put(id, task);
    }

    /**
     * Crea una barrera esférica básica
     */
    public static void createBasicBarrier(String id, Location center, double radius,
                                          Particle particle, Color color) {
        createAdvancedBarrier(id, center, radius, particle, color, 600, false, false, 4);
    }

    /**
     * Crea una barrera con densidad personalizada
     */
    public static void createCustomBarrier(String id, Location center, double radius,
                                           Particle particle, Color color, int density) {
        createAdvancedBarrier(id, center, radius, particle, color, density, false, false, 4);
    }

    /**
     * Crea una barrera con efecto de pulso
     */
    public static void createPulseBarrier(String id, Location center, double radius,
                                          Particle particle, Color color) {
        createAdvancedBarrier(id, center, radius, particle, color, 60, false, true, 4);
    }

    /**
     * Crea una barrera avanzada con todas las opciones
     */
    public static void createAdvancedBarrier(String id, Location center, double radius,
                                             Particle particle, Color color, int density,
                                             boolean detectPlayers, boolean pulseEffect,
                                             long updateInterval) {
        removeBarrier(id);

        BarrierData barrierData = new BarrierData(center, radius, particle, color,
                density, detectPlayers, pulseEffect);
        barrierConfigs.put(id, barrierData);

        BukkitTask task = new BukkitRunnable() {
            private double pulsePhase = 0;

            @Override
            public void run() {
                if (center.getWorld() == null) {
                    removeBarrier(id);
                    return;
                }

                generateBarrierParticles(barrierData, pulsePhase);

                if (pulseEffect) {
                    pulsePhase += 0.1;
                    if (pulsePhase >= Math.PI * 2) {
                        pulsePhase = 0;
                    }
                }
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NexoAndCorruption"),
                0L, updateInterval);

        activeBarriers.put(id, task);
    }

    private static void generateBarrierParticles(BarrierData data, double pulsePhase) {
        Location center = data.getCenter();
        double radius = data.getRadius();

        if (data.hasPulseEffect()) {
            radius += Math.sin(pulsePhase) * (radius * 0.1);
        }

        int numPoints = data.getDensity();
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < numPoints; i++) {
            double y = 1 - (i / (double) (numPoints - 1)) * 2;
            double radiusAtY = Math.sqrt(1 - y * y);
            double theta = goldenAngle * i;

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            Vector point = new Vector(x * radius, y * radius, z * radius);
            Location particleLocation = center.clone().add(point);

            if (data.getParticle() == Particle.REDSTONE && data.getColor() != null) {
                Particle.DustOptions dustOptions = new Particle.DustOptions(
                        data.getColor(), 1.0f
                );
                center.getWorld().spawnParticle(data.getParticle(), particleLocation,
                        1, 0, 0, 0, 0, dustOptions);
            } else {
                center.getWorld().spawnParticle(data.getParticle(), particleLocation,
                        1, 0, 0, 0, 0);
            }
        }
    }

    public static BarrierData getBarrierData(String id) {
        return barrierConfigs.get(id);
    }

    public static void removeBarrier(String id) {
        BukkitTask task = activeBarriers.remove(id);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        barrierConfigs.remove(id);
    }

    public static void removeAllBarriers() {
        for (BukkitTask task : activeBarriers.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        activeBarriers.clear();
        barrierConfigs.clear();
    }

    public static boolean barrierExists(String id) {
        return activeBarriers.containsKey(id);
    }

    public static void createNexoBarrier(String nexoId, Location center, double radius,
                                         boolean isActive, boolean isCritical) {
        Color color;
        Particle particle = Particle.REDSTONE;
        int density;

        if (!isActive) {
            color = Color.GRAY;
            density = 600;
            createPulseBarrier(nexoId + "_barrier", center, radius, particle, color);
        } else if (isCritical) {
            color = Color.RED;
            density = 500;
            createCustomBarrier(nexoId + "_barrier", center, radius, particle, color, density);
        } else {
            color = Color.BLUE;
            density = 600;
            createBasicBarrier(nexoId + "_barrier", center, radius, particle, color);
        }
    }
}
