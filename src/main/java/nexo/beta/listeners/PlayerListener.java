package nexo.beta.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Warden;

import nexo.beta.NexoAndCorruption;
import nexo.beta.classes.Nexo;
import nexo.beta.managers.NexoManager;

public class PlayerListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Handle player join event
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Warden warden)) return;

        NexoManager manager = NexoAndCorruption.getNexoManagerStatic();
        if (manager == null) return;

        Nexo nexo = manager.getNexoEnMundo(warden.getWorld());
        if (nexo == null || nexo.getWarden() == null) return;

        if (!warden.getUniqueId().equals(nexo.getWarden().getUniqueId())) return;

        int damage = (int) Math.ceil(event.getFinalDamage());
        nexo.setVida(nexo.getVida() - damage);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Warden warden)) return;

        NexoManager manager = NexoAndCorruption.getNexoManagerStatic();
        if (manager == null) return;

        Nexo nexo = manager.getNexoEnMundo(warden.getWorld());
        if (nexo == null || nexo.getWarden() == null) return;

        if (!warden.getUniqueId().equals(nexo.getWarden().getUniqueId())) return;

        manager.eliminarNexo(warden.getWorld());
    }
}
