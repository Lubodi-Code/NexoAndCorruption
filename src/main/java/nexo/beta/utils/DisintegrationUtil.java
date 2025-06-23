package nexo.beta.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import nexo.beta.NexoAndCorruption;

public class DisintegrationUtil {

    /**
     * Aplica un efecto de desintegración al entity: anillos de partículas,
     * humo, sonido de explosión final y remoción.
     */
    public static void disintegrate(Entity entity) {
        Location origin = entity.getLocation();

        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                // Si el mob ya está muerto o pasó el número de pasos, remuévelo con explosión final
                if (step > 12 || entity.isDead()) {
                    origin.getWorld().spawnParticle(Particle.EXPLOSION, origin, 1);
                    origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
                    entity.remove();
                    cancel();
                    return;
                }

                // Radio crece cada tick para simular expansión
                double radius = 0.1 + step * 0.15;

                // Genera un anillo de partículas CRIT_MAGIC y SMOKE_NORMAL
                for (int i = 0; i < 24; i++) {
                    double angle = 2 * Math.PI * i / 24;
                    double dx = Math.cos(angle) * radius;
                    double dz = Math.sin(angle) * radius;
                    Location p = origin.clone().add(dx, 0.5, dz);

                    origin.getWorld().spawnParticle(Particle.CRIMSON_SPORE, p, 1, 0, 0, 0, 0);
                    origin.getWorld().spawnParticle(Particle.SMOKE, p, 1, 0, 0, 0, 0);
                }

                // Sonido sutil de chispea a mitad de animación
                if (step == 4 || step == 8) {
                    origin.getWorld().playSound(origin, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.2f);
                }

                step++;
            }
        // Ajusta NexoAndCorruption.class a la clase principal de tu plugin
        }.runTaskTimer(JavaPlugin.getPlugin(NexoAndCorruption.class), 0L, 2L);
    }
}
