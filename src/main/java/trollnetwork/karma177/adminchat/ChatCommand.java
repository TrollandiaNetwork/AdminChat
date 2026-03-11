package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import trollnetwork.karma177.adminchat.Exceptions.ChatAlreadyDisabledException;
import trollnetwork.karma177.adminchat.Exceptions.ChatAlreadyEnabledException;

public class ChatCommand implements SimpleCommand {

    private final ChatManager chatManager;
    private final AdminChat plugin;

    public ChatCommand(ChatManager chatManager, AdminChat plugin) {
        this.chatManager = chatManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if(!PermissionChecker.hasStaffChatPermission(invocation)) {
            invocation.source().sendMessage(Component.text("Non hai il permesso per usare questo comando.", NamedTextColor.RED));
            return;
        }


        CommandSource source = invocation.source();
        String[] args = invocation.arguments();


        if(source instanceof Player && !chatManager.isCached((Player) source)) {
            chatManager.addStaff((Player) source);
        }

        switch(args.length) {
            case 0 -> {
                if (invocation.alias().equalsIgnoreCase("adminchat")){
                    source.sendMessage(Component.text(this.plugin.getDescription(), NamedTextColor.DARK_AQUA));
                    source.sendMessage(Component.text("Uso: /adminchat <enable | disable | toggle>", NamedTextColor.DARK_AQUA));
                    return;
                }

                chatToggle(source); // /a
            }
            case 1 -> {
                if (invocation.alias().equalsIgnoreCase("adminchat")){
                    switch(args[0].toLowerCase()) {
                        case "enable" -> enableStaffChat(source);
                        case "disable" -> disableStaffChat(source);
                        case "toggle" -> chatToggle(source);
                        default -> {
                            source.sendMessage(Component.text("Uso: /adminchat <enable | disable | toggle>", NamedTextColor.RED));
                        }
                    }
                }
                else quickMessage(source, args); // /a <messaggio>
            }
            default -> quickMessage(source, args); // /a <messaggio>
        }
    }

    private void disableStaffChat(CommandSource source) {
        Player p = source instanceof Player player ? player : null;
        if(p == null) {
            if(!chatManager.getConsoleTranscript()) {
                source.sendMessage(Component.text("La console ha già la StaffChat disabilitata.", NamedTextColor.RED));
                return;
            }

            chatManager.disableConsoleTranscript();
            source.sendMessage(Component.text("Chat staff disabilitata per la console.", NamedTextColor.RED));
            return;
        }
        
        try {
            chatManager.disableChat(p);
        } catch (ChatAlreadyDisabledException e) {
            source.sendMessage(Component.text("La StaffChat era già disabilitata.", NamedTextColor.RED));
            return;
        }
        source.sendMessage(Component.text("Hai disabilitato i messaggi (in entrata) della StaffChat!", NamedTextColor.RED));

    }

    private void enableStaffChat(CommandSource source) {
        Player p = source instanceof Player player ? player : null;
        if(p == null) {
            if(chatManager.getConsoleTranscript()) {
                source.sendMessage(Component.text("La console ha già la StaffChat abilitata.", NamedTextColor.AQUA));
                return;
            }

            chatManager.enableConsoleTranscript();
            source.sendMessage(Component.text("Chat staff abilitata per la console.", NamedTextColor.AQUA));
            return;
        }

        try {
            chatManager.enableChat(p);
        } catch (ChatAlreadyEnabledException e) {
            source.sendMessage(Component.text("La StaffChat era già abilitata.", NamedTextColor.AQUA));
            return;
        }
        source.sendMessage(Component.text("Hai abilitato i messaggi (in entrata) della StaffChat!", NamedTextColor.AQUA));
    }


    
    private void quickMessage(CommandSource source, String[] args) {
        String message = String.join(" ", args);
        // Identifichiamo il mittente (se non è un Player, compare come CONSOLE)
        String senderName = (source instanceof Player player) ? player.getUsername() : "CONSOLE";
        if(senderName.equals("CONSOLE") && !chatManager.getConsoleTranscript()) {
            source.sendMessage(Component.text("La console ha la StaffChat disabilitata. Usa: /adminchat enable", NamedTextColor.RED));
            return;
        }

        if(source instanceof Player && !chatManager.hasChatEnabled((Player) source)) {
            source.sendMessage(Component.text("Hai la StaffChat disabilitata. Usa: /adminchat enable per abilitarla.", NamedTextColor.RED));
            return;
        }

        Component formattedMsg = ChatManager.formatStaffMessage(senderName, message);
        chatManager.broadcastStaffMessage(formattedMsg);
    }

    private void chatToggle(CommandSource source) {
        // Modalità toggle
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("La console non può abilitare la modalità toggle. Usa: /a <messaggio>", NamedTextColor.RED));
            return;
        }

        if(!chatManager.hasChatEnabled(player)) {
            player.sendMessage(Component.text("Hai la StaffChat disabilitata. Usa: /adminchat enable per abilitarla.", NamedTextColor.RED));
            return;
        }

        if (chatManager.toggleChat(player))
            player.sendMessage(Component.text("StaffChat abilitata. I tuoi prossimi messaggi verranno inviati solo allo staff.", NamedTextColor.AQUA));
        else
            player.sendMessage(Component.text("StaffChat disabilitata.", NamedTextColor.RED));
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
            return List.of("enable", "disable", "toggle");
        } else if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            if ("enable".startsWith(prefix)) suggestions.add("enable");
            if ("disable".startsWith(prefix)) suggestions.add("disable");
            if ("toggle".startsWith(prefix)) suggestions.add("toggle");
            return suggestions;
        }
        
        return List.of();
    }
}