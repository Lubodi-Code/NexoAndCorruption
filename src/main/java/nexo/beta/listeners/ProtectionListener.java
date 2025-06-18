package nexo.beta.listeners;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;

import nexo.beta.NexoAndCorruption;
import nexo.beta.classes.Nexo;
import nexo.beta.managers.ConfigManager;
import nexo.beta.managers.NexoManager;

public class ProtectionListener implements Listener {

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) {
                attacker = p;
            }
        }
        if (attacker == null) return;

        NexoManager manager = NexoAndCorruption.getNexoManagerStatic();
        ConfigManager config = NexoAndCorruption.getConfigManagerStatic();
        if (manager == null || config == null) return;

        Nexo nexo = manager.getNexoCercano(victim.getLocation());
        if (nexo == null || !nexo.estaActivo()) return;

        double dist = nexo.getUbicacion().distance(victim.getLocation());
        if (dist <= config.getRadioProteccion()
                && config.isProteccionPvPHabilitada()
                && config.isBloquearPvP()) {
            attacker.sendMessage(config.getMensajeProteccionPvP());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBlockEdit(event.getPlayer(), event.getBlock().getLocation(), true, event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockEdit(event.getPlayer(), event.getBlock().getLocation(), false, event);
    }

    private void handleBlockEdit(Player player, Location loc, boolean place, org.bukkit.event.Cancellable event) {
        NexoManager manager = NexoAndCorruption.getNexoManagerStatic();
        ConfigManager config = NexoAndCorruption.getConfigManagerStatic();
        if (manager == null || config == null) return;

        Nexo nexo = manager.getNexoCercano(loc);
        if (nexo == null || !nexo.estaActivo()) return;

        double dist = nexo.getUbicacion().distance(loc);
        int sub = config.getRadioRestriccionBloques();
        if (dist <= sub && config.isProteccionBloquesHabilitada()) {
            boolean permitir = place ? config.isPermitirConstruccion() : config.isPermitirDestruccion();
            if (!permitir) {
                player.sendMessage(config.getMensajeProteccionBloques());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof InventoryHolder)) return;

        NexoManager manager = NexoAndCorruption.getNexoManagerStatic();
        ConfigManager config = NexoAndCorruption.getConfigManagerStatic();
        if (manager == null || config == null) return;

        Nexo nexo = manager.getNexoCercano(event.getClickedBlock().getLocation());
        if (nexo == null || !nexo.estaActivo()) return;

        double dist = nexo.getUbicacion().distance(event.getClickedBlock().getLocation());
        if (dist <= config.getRadioRestriccionContenedores()
                && config.isProteccionContenedoresHabilitada()) {
            event.getPlayer().sendMessage(config.getMensajeProteccionContenedores());
            event.setCancelled(true);
        }
    }
}
