package trollnetwork.karma177.adminchat;

import trollnetwork.karma177.adminchat.utils.Messages;
import trollnetwork.karma177.adminchat.utils.PermissionChecker;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatManager {

    // Utilizziamo un Set thread-safe per via del multithreading nativo di Velocity
    private final Set<Player> staffCache = ConcurrentHashMap.newKeySet();
    private final Set<Player> toggledPlayers = ConcurrentHashMap.newKeySet();
    
    private final AdminChat plugin;

    public ChatManager(AdminChat plugin) {
        this.plugin = plugin;
    }

    private static Component toComponent(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
    
    public static String getServerName(Player player) {
        return player.getCurrentServer()
            .map(server -> {
                String name = server.getServerInfo().getName();
                if (name.isEmpty()) return name;
                return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            })
            .orElse("Unknown");
    }

    public static Component formatStaffMessage(Player sender, String serverName, String message) {
        String senderName = (sender != null) ? sender.getUsername() : "CONSOLE";
        String format = (senderName.equals("CONSOLE")) ? Messages.get("staff_message_format_console") : Messages.get("staff_message_format");

        return toComponent(format
            .replace("{sender}", senderName)
            .replace("{server}", serverName)
            .replace("{message}", message));
    }

    private static Component formatLoginMessage(String user){
        String format = Messages.get("staff_notify_join");
        return toComponent(format.replace("{player}", user));
    }

    private static Component formatLogoutMessage(String user){
        String format = Messages.get("staff_notify_quit");
        return toComponent(format.replace("{player}", user));
    }

    public void cacheUpdate(){
        staffCache.forEach( 
            (staff) -> {
                if(!staff.isActive() || !PermissionChecker.hasStaffChatPermission(staff)) {
                    plugin.getLogger().info("Rimuovendo " + staff.getUsername() + " dalla cache dello staff.. non si è disconnesso correttamente?");
                    removeStaff(staff);
                }
            }
        );
        //plugin.getLogger().info("Cache dello staff aggiornata: " + staffCache.size() + " membri in cache, " + toggledPlayers.size() + " con chat togglata");
    }
    
    public void addStaff(Player player) {
        if(isCached(player))
            return;
       
        staffCache.add(player);
    }

    public void removeStaff(Player player) {
        if(!isCached(player))
            return;
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

    /**
     * Ritorna una lista contenente i nomi dei membri dello staff raggruppati per server.
     */
    public Map<String, List<String>> getStaffGroupedByServer() {
        cacheUpdate();
        return staffCache.stream()
            .collect(Collectors.groupingBy(
                player -> getServerName(player),
                Collectors.mapping(Player::getUsername, Collectors.toList())
            ));
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

    /**
     * Invia un messaggio a tutti gli staffer in cache.
     * "lazy update": se il player non è più attivo, lo toglie.
     */
    public void broadcastStaffMessage(Component message) {
        cacheUpdate();
        staffCache.forEach( (staff) -> { staff.sendMessage(message); });            
        plugin.getServer().getConsoleCommandSource().sendMessage(message);
    }

    public void notifyLogin(Player player){
        broadcastStaffMessage(formatLoginMessage(player.getUsername()));
    }

    public void notifyLogout(Player player){
        broadcastStaffMessage(formatLogoutMessage(player.getUsername()));
    }

    public boolean isToggled(Player player) {
        return toggledPlayers.contains(player);
    }

    public boolean isCached(Player player) {
        return staffCache.contains(player);
    }

    public void reload(){
        this.toggledPlayers.clear();
    }
}