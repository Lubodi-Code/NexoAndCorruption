package nexo.beta.CommandManager;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nexo.beta.NexoAndCorruption;
import nexo.beta.classes.Nexo;
import nexo.beta.managers.ConfigManager;
import nexo.beta.managers.NexoManager;

public class CommandNexoManager implements CommandExecutor {

    private final NexoAndCorruption plugin;
    private final NexoManager nexoManager;
    private final ConfigManager config;

    public CommandNexoManager(NexoAndCorruption plugin) {
        this.plugin = plugin;
        this.nexoManager = plugin.getNexoManager();
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUso: /" + label + " <crear|destruir|estado|activar|desactivar|reiniciar|recargar>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "crear":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cSolo jugadores pueden ejecutar este comando.");
                    return true;
                }
                Location loc = player.getLocation();
                nexoManager.crearNexo(loc);
                sender.sendMessage("§aNexo creado en tu ubicación.");
                break;
            case "destruir":
                World world = (sender instanceof Player p) ? p.getWorld() : plugin.getServer().getWorlds().get(0);
                if (nexoManager.eliminarNexo(world)) {
                    sender.sendMessage("§cNexo destruido.");
                } else {
                    sender.sendMessage(config.getMensajeNexoNoEncontrado());
                }
                break;
            case "estado":
                if (!config.isComandoEstadoHabilitado()) {
                    sender.sendMessage("§cComando deshabilitado.");
                    return true;
                }
                Nexo nexoEstado = nexoManager.getNexoEnMundo(sender instanceof Player p ? p.getWorld() : plugin.getServer().getWorlds().get(0));
                if (nexoEstado == null) {
                    sender.sendMessage(config.getMensajeNexoNoEncontrado());
                    return true;
                }
                Map<String, Object> phVida = new HashMap<>();
                phVida.put("vida", nexoEstado.getVida());
                phVida.put("vida_maxima", config.getVidaMaxima());
                phVida.put("porcentaje", nexoEstado.getPorcentajeVida());
                String msgVida = config.replacePlaceholders(config.getMensajeEstadoVida(), phVida);
                Map<String, Object> phEner = new HashMap<>();
                phEner.put("energia", nexoEstado.getEnergia());
                phEner.put("energia_maxima", config.getEnergiaMaxima());
                phEner.put("porcentaje", nexoEstado.getPorcentajeEnergia());
                String msgEner = config.replacePlaceholders(config.getMensajeEstadoEnergia(), phEner);
                sender.sendMessage(config.getPrefijo() + msgVida);
                sender.sendMessage(config.getPrefijo() + msgEner);
                break;
            case "activar":
                Nexo nexoAct = nexoManager.getNexoEnMundo(sender instanceof Player p ? p.getWorld() : plugin.getServer().getWorlds().get(0));
                if (nexoAct == null) {
                    sender.sendMessage(config.getMensajeNexoNoEncontrado());
                    return true;
                }
                nexoAct.activar();
                sender.sendMessage("§aNexo activado.");
                break;
            case "desactivar":
                Nexo nexoDes = nexoManager.getNexoEnMundo(sender instanceof Player p ? p.getWorld() : plugin.getServer().getWorlds().get(0));
                if (nexoDes == null) {
                    sender.sendMessage(config.getMensajeNexoNoEncontrado());
                    return true;
                }
                nexoDes.desactivar();
                sender.sendMessage("§cNexo desactivado.");
                break;
            case "reiniciar":
                if (!config.isComandoReiniciarHabilitado()) {
                    sender.sendMessage("§cComando deshabilitado.");
                    return true;
                }
                Nexo nexoRei = nexoManager.getNexoEnMundo(sender instanceof Player p ? p.getWorld() : plugin.getServer().getWorlds().get(0));
                if (nexoRei == null) {
                    sender.sendMessage(config.getMensajeNexoNoEncontrado());
                    return true;
                }
                nexoRei.reiniciar();
                sender.sendMessage("§aNexo reiniciado.");
                break;
            case "recargar":
                if (!config.isComandoRecargarHabilitado()) {
                    sender.sendMessage("§cComando deshabilitado.");
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage("§aPlugin recargado.");
                break;
            default:
                sender.sendMessage("§eUso: /" + label + " <crear|destruir|estado|activar|desactivar|reiniciar|recargar>");
                break;
        }
        return true;
    }
}
