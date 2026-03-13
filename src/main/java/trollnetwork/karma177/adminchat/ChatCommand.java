package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ChatCommand implements SimpleCommand {

    private final ChatManager chatManager;
    private final AdminChat plugin;

    public ChatCommand(ChatManager chatManager, AdminChat plugin) {
        this.chatManager = chatManager;
        this.plugin = plugin;
    }

    private Component toComponent(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    private void sendHelp(CommandSource source, Invocation invocation) {
        source.sendMessage(toComponent(Messages.get("usage_adminchat_quick")));
        source.sendMessage(toComponent(Messages.get("usage_adminchat_help")));
        source.sendMessage(toComponent(Messages.get("usage_adminchat_enable")));
        source.sendMessage(toComponent(Messages.get("usage_adminchat_disable")));
        source.sendMessage(toComponent(Messages.get("usage_adminchat_toggle")));
        source.sendMessage(toComponent(Messages.get("usage_adminchat_version")));
        if (PermissionChecker.hasReloadPermission(invocation)) {
            source.sendMessage(toComponent(Messages.get("usage_adminchat_reload")));
        }
    }

    @Override
    public void execute(Invocation invocation) {
        if(!PermissionChecker.hasStaffChatPermission(invocation)) {
            invocation.source().sendMessage(toComponent(Messages.get("no_permission")));
            return;
        }

        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if(source instanceof Player && !chatManager.isCached((Player) source)) {
            chatManager.addStaff((Player) source);
        }

        switch(args.length) {
            case 0 -> {
                switch(invocation.alias().toLowerCase()){
                    case "adminchat" -> {
                            sendHelp(source, invocation);
                            return;
                        }
                    case "a" -> {
                            source.sendMessage(toComponent(Messages.get("usage_a")));
                            return;
                        }
                    case "aa" -> {
                        chatToggle(source); // /aa senza argomenti funziona come toggle rapido
                        return;
                    }
                }
            }
            case 1 -> {
                switch(invocation.alias().toLowerCase()){
                    case "adminchat" -> {
                        switch(args[0].toLowerCase()) {
                            case "enable" -> enableStaffChat(source);
                            case "disable" -> disableStaffChat(source);
                            case "toggle" -> chatToggle(source);
                            case "reload" -> {
                                if (PermissionChecker.hasReloadPermission(invocation)) 
                                    reloadPlugin(source);
                                else
                                    source.sendMessage(toComponent(Messages.get("no_permission")));
                            }
                            case "version" -> {
                                source.sendMessage(Component.text(this.plugin.getDescription(), NamedTextColor.GRAY));
                            }
                            case "help" -> {
                                sendHelp(source, invocation);
                            }
                            default -> {
                                sendHelp(source, invocation);
                            }
                        }
                    }
                    case "a" -> {
                        quickMessage(source, args); // /a <messaggio>
                    }
                    case "aa" -> {
                        chatToggle(source); // /aa senza argomenti funziona come toggle rapido
                    }
                }
            }
            default -> {
                switch(invocation.alias().toLowerCase()){
                    case "adminchat" -> {
                        sendHelp(source, invocation);
                        return;
                    }
                    case "a" -> {
                        quickMessage(source, args); // /a <messaggio>        
                    }
                    case "aa" -> {
                        chatToggle(source); // /aa con argomenti funziona comunque come toggle rapido
                    }
                }
            }
        }
    }

    private void reloadPlugin(CommandSource source) {
        chatManager.reload();
        plugin.reloadMessages();
        source.sendMessage(toComponent(Messages.get("adminchat.reloaded")));
    }

    private void disableStaffChat(CommandSource source) {
        Player p = source instanceof Player player ? player : null;
        if(p == null) {
            if(!chatManager.getConsoleTranscript()) {
                source.sendMessage(toComponent(Messages.get("console_transcript_already_disabled")));
                return;
            }
            chatManager.disableConsoleTranscript();
            source.sendMessage(toComponent(Messages.get("console_transcript_disabled")));
            return;
        }
        
        chatManager.disableChat(p);
        //source.sendMessage(toComponent(Messages.get("adminchat.disabled_player"))); unused?

    }

    private void enableStaffChat(CommandSource source) {
        Player p = source instanceof Player player ? player : null;
        if(p == null) {
            if(chatManager.getConsoleTranscript()) {
                source.sendMessage(toComponent(Messages.get("console_transcript_already_enabled")));
                return;
            }

            chatManager.enableConsoleTranscript();
            source.sendMessage(toComponent(Messages.get("console_transcript_enabled")));
            return;
        }

        chatManager.enableChat(p);        
        //source.sendMessage(toComponent(Messages.get("adminchat.enabled_player"))); unused?
    }


    
    private void quickMessage(CommandSource source, String[] args) {
        String message = String.join(" ", args);
        // Identifichiamo il mittente (se non è un Player, compare come CONSOLE)
        String senderName = (source instanceof Player player) ? player.getUsername() : "CONSOLE";
        if(senderName.equals("CONSOLE") && !chatManager.getConsoleTranscript()) {
            source.sendMessage(toComponent(Messages.get("console_transcript_already_disabled")));
            return;
        }

        if(source instanceof Player && !chatManager.hasChatEnabled((Player) source)) {
            source.sendMessage(toComponent(Messages.get("adminchat.message_when_incoming_disabled"))); 
            return;
        }

        Component formattedMsg = ChatManager.formatStaffMessage(senderName, message);
        chatManager.broadcastStaffMessage(formattedMsg);
    }

    private void chatToggle(CommandSource source) {
        // Modalità toggle
        if (!(source instanceof Player player)) {
            source.sendMessage(toComponent(Messages.get("console_only_toggle")));
            return;
        }

        if(!chatManager.hasChatEnabled(player)) {
             source.sendMessage(toComponent(Messages.get("adminchat.disabled_player")));
            return;
        }

        if (chatManager.toggleChat(player))
            player.sendMessage(toComponent(Messages.get("adminchat.toggle_enabled")));
        else
            player.sendMessage(toComponent(Messages.get("adminchat.toggle_disabled")));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        // Se non hanno il permesso, evitiamo di dare suggerimenti
        if(!PermissionChecker.hasStaffChatPermission(invocation)) {
            return List.of();
        }

        // Vogliamo suggerire questi argomenti solo per /adminchat e non per l'alias veloce
        if (!invocation.alias().equalsIgnoreCase("adminchat")) {
            return List.of();
        }

        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("enable");
            suggestions.add("disable");
            suggestions.add("toggle");
            suggestions.add("version");
            suggestions.add("help");
            if (PermissionChecker.hasReloadPermission(invocation))
                suggestions.add("reload");
            return suggestions;
        } else if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            if ("enable".startsWith(prefix)) suggestions.add("enable");
            if ("disable".startsWith(prefix)) suggestions.add("disable");
            if ("toggle".startsWith(prefix)) suggestions.add("toggle");
            if ("version".startsWith(prefix)) suggestions.add("version");
            if ("help".startsWith(prefix)) suggestions.add("help");
            if ("reload".startsWith(prefix) && PermissionChecker.hasReloadPermission(invocation)) suggestions.add("reload");
            return suggestions;
        }
        
        return List.of();
    }
}