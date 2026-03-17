package trollnetwork.karma177.adminchat;

import trollnetwork.karma177.adminchat.commands.ChatCommand;
import trollnetwork.karma177.adminchat.commands.StaffListCommand;
import trollnetwork.karma177.adminchat.utils.Messages;
import trollnetwork.karma177.adminchat.utils.PermissionChecker;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.nio.file.Path;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;

@Plugin(
        id = "adminchat",
        name = "AdminChat",
        version = "1.3-TROLL",
        description = "Velocity Admin Chat Plugin",
        authors = {"Karma177"}
)
public class AdminChat {

    private String version = "1.3-TROLL";
    private final ProxyServer server;
    private final Logger logger;
    private final ChatManager chatManager;
    private final Path dataDirectory;

    @Inject
    public AdminChat(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.chatManager = new ChatManager(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("AdminChat plugin initialized!");
        // Carica messaggi (o da file o da risorse interne)
        Messages.init(dataDirectory.toString()+"/messages.yml", this);
        logger.info("Caricando messaggi da: " + dataDirectory.toString()+"/messages.yml");

        // Registrazione dei comandi
        CommandManager commandManager = server.getCommandManager();
        
        // /adminchat (alias /a)
        CommandMeta commandMeta = commandManager.metaBuilder("adminchat")
                .aliases("a","aa" )
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new ChatCommand(chatManager, this));

        // /stafflist
        CommandMeta staffListMeta = commandManager.metaBuilder("stafflist")
                .plugin(this)
                .build();
        commandManager.register(staffListMeta, new StaffListCommand(chatManager));
    }

    // Aggiunge lo staff in cache nel momento esatto in cui ha terminato il login
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (PermissionChecker.hasStaffChatPermission(player)) {
            chatManager.addStaff(player);
            chatManager.notifyLogin(player);
        }
    }

    // Aggiorna dinamicamente la cache dello staff se cambiano i permessi (es. LuckPerms update)
    /* not working :(
    @Subscribe
    public void onPermissionsSetup(PermissionsSetupEvent event) {
        this.logger.info("Permissions setup event triggered, updating staff cache...");
        if (event.getSubject() instanceof Player player) {
            this.logger.info("Checking permissions for player: " + player.getUsername());
            Tristate permission = PermissionChecker.hasPermissionForFunction(event.createFunction(player));
            this.logger.info("Permission check result for player " + player.getUsername() + ": " + permission);
            // Velocity esegue questo evento anche all'accesso; controlliamo in tempo reale i nuovi valori permessi
            if (permission == Tristate.TRUE) {
                this.logger.info("Player " + player.getUsername() + " has staff chat permission, adding to cache.");
                chatManager.addStaff(player);
            } else {
                this.logger.info("Player " + player.getUsername() + " does NOT have staff chat permission, removing from cache.");
                chatManager.removeStaff(player);
            }
        }
    }
    */

    
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if(PermissionChecker.hasStaffChatPermission(event.getPlayer())){
            chatManager.removeStaff(event.getPlayer());
            chatManager.notifyLogout(event.getPlayer());
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        // Se il giocatore ha l'admin chat togglata e ha il permesso
        if (PermissionChecker.hasStaffChatPermission(player) && chatManager.isToggled(player)) {
            event.setResult(PlayerChatEvent.ChatResult.denied()); // Blocchiamo il messaggio originale dalla chat pubblica
            String serverName = ChatManager.getServerName(player);
            Component formattedMsg = ChatManager.formatStaffMessage(player, serverName, event.getMessage()); 
            chatManager.broadcastStaffMessage(formattedMsg); // E lo inoltriamo allo staff 
        }
    }

    public void reloadMessages() {
        Messages.init(dataDirectory.toString()+"/messages.yml", this);
        logger.info("Configurazione ricaricata.");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getDescription() {
        return "&c[&6AC&c] &7Made by &b@Karma177&7 on GitHub. Version: &b"+this.version+"&7.";
    }
}