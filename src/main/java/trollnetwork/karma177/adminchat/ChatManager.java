package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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

    private static Component toComponent(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    public static Component formatStaffMessage(String sender, String message) {
        String senderName = (sender != null) ? sender : "CONSOLE";
        String format = Messages.get("staff_message_format");
        return toComponent(format
            .replace("{sender}", senderName)
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
       
        enableChatSilent(player);
        staffCache.add(player);
    }

    public void removeStaff(Player player) {
        if(!isCached(player))
            return;
        disableChat(player);
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

    public void enableChat(Player player){
        if(chatEnabledPlayers.contains(player)){
            player.sendMessage(toComponent(Messages.get("adminchat.already_enabled")));
            return;
        }
        chatEnabledPlayers.add(player);
        //player.sendMessage(toComponent(Messages.get("adminchat.toggle_enabled")));
    }

    private void enableChatSilent(Player player){
        chatEnabledPlayers.add(player);
    }

    public void disableChat(Player player){
        if(!chatEnabledPlayers.contains(player)){
            player.sendMessage(toComponent(Messages.get("adminchat.already_disabled")));
            return;
        }

        chatEnabledPlayers.remove(player);
        toggledPlayers.remove(player);
        //if(toggledPlayers.remove(player))
            //player.sendMessage(toComponent(Messages.get("adminchat.toggle_disabled")));
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

    public void notifyLogin(Player player){
        broadcastStaffMessage(formatLoginMessage(player.getUsername()));
    }

    public void notifyLogout(Player player){
        broadcastStaffMessage(formatLogoutMessage(player.getUsername()));
    }

    public void disableConsoleTranscript(){
        consoleTranscript = false;
    }

    public void enableConsoleTranscript(){
        consoleTranscript = true;
    }

    public boolean isToggled(Player player) {
        return toggledPlayers.contains(player);
    }

    public boolean isCached(Player player) {
        return staffCache.contains(player);
    }

    public boolean hasChatEnabled(Player player) {
        return chatEnabledPlayers.contains(player);
    }

    public void reload(){
        this.toggledPlayers.clear();
    }

    public boolean getConsoleTranscript() {
        return consoleTranscript;
    }
}