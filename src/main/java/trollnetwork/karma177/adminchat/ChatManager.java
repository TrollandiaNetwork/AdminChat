package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import trollnetwork.karma177.adminchat.Exceptions.ChatAlreadyDisabledException;
import trollnetwork.karma177.adminchat.Exceptions.ChatAlreadyEnabledException;

import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatManager {

    // Utilizziamo un Set thread-safe per via del multithreading nativo di Velocity
    private final Set<Player> staffCache = ConcurrentHashMap.newKeySet();
    private final Set<Player> toggledPlayers = ConcurrentHashMap.newKeySet();
    private final Set<Player> chatEnabledPlayers = ConcurrentHashMap.newKeySet();
    private boolean consoleTranscript = true;
    
    private final AdminChat plugin;

    public ChatManager(AdminChat plugin) {
        this.plugin = plugin;
    }
    

    public static Component formatStaffMessage(String sender, String message) {
        String senderName = (sender != null) ? sender : "CONSOLE";
        return Component.text("[STAFF] ", NamedTextColor.AQUA)
            .append(Component.text(senderName + ": ", NamedTextColor.AQUA))
            .append(Component.text(message, NamedTextColor.AQUA));
    }

    public void cacheUpdate(){
        staffCache.forEach( 
            (staff) -> {
                if(!staff.isActive() || !PermissionChecker.hasStaffChatPermission(staff)) {
                    plugin.getLogger().info("Rimuovendo " + staff.getUsername() + " dalla cache dello staff (non più attivo o senza permesso)");
                    removeStaff(staff);
                }
            }
        );
        plugin.getLogger().info("Cache dello staff aggiornata: " + staffCache.size() + " membri in cache, " + toggledPlayers.size() + " con chat togglata");
    }
    
    public void addStaff(Player player) {
        if(isCached(player))
            return;
        try{
            enableChat(player);
        }catch(ChatAlreadyEnabledException e){ 
            plugin.getLogger().warn("Tentativo di abilitare la chat per " + player.getUsername() + " ma era già abilitata.");
        }
        staffCache.add(player);
    }

    public void removeStaff(Player player) {
        if(!isCached(player))
            return;
        try{
            disableChat(player);
        }catch(ChatAlreadyDisabledException e){
            plugin.getLogger().warn("Tentativo di disabilitare la chat per " + player.getUsername() + " ma era già disabilitata.");
        }
        staffCache.remove(player);
        toggledPlayers.remove(player);
    }

    /**
     * Ritorna una lista contenente i nomi dei membri dello staff attualmente connessi
     */
    public List<String> getOnlineStaffNames() {
        cacheUpdate();
        return staffCache.stream()
            .map(Player::getUsername)
            .collect(Collectors.toList());
    }

    public boolean toggleChat(Player player) {
        if (isToggled(player)) {
            toggledPlayers.remove(player);
            return false;
        } else {
            toggledPlayers.add(player);
            return true;
        }
    }

    public void enableChat(Player player) throws ChatAlreadyEnabledException {
        if(chatEnabledPlayers.contains(player))
            throw new ChatAlreadyEnabledException("La chat è già abilitata!");
        chatEnabledPlayers.add(player);
        toggledPlayers.add(player);
    }

    public void disableChat(Player player) throws ChatAlreadyDisabledException {
        if(!chatEnabledPlayers.contains(player))
            throw new ChatAlreadyDisabledException("La chat è già disabilitata!");
        chatEnabledPlayers.remove(player);
        if(toggledPlayers.remove(player))
            player.sendMessage(Component.text("La modalità toggle è stata disabilitata poiché la StaffChat è stata disabilitata.", NamedTextColor.RED));
    }

    /**
     * Invia un messaggio a tutti gli staffer in cache.
     * "lazy update": se il player non è più attivo, lo toglie.
     */
    public void broadcastStaffMessage(Component message) {
        cacheUpdate();
        staffCache.forEach( (staff) -> { if(chatEnabledPlayers.contains(staff)) staff.sendMessage(message); });            
        if(consoleTranscript) plugin.getServer().getConsoleCommandSource().sendMessage(message);
    }

    public void disableConsoleTranscript(){
        consoleTranscript = false;
    }

    public void enableConsoleTranscript(){
        consoleTranscript = true;
    }

    private boolean isToggled(Player player) {
        return toggledPlayers.contains(player);
    }

    public boolean isCached(Player player) {
        return staffCache.contains(player);
    }


    public boolean hasChatEnabled(Player player) {
        return chatEnabledPlayers.contains(player);
    }
}