package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import trollnetwork.karma177.adminchat.Exceptions.ChatAlreadyDisabledException;
import trollnetwork.karma177.adminchat.Exceptions.ChatAlreadyEnabledException;

public class ChatCommand implements SimpleCommand {

    private final ChatManager chatManager;

    public ChatCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
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
            case 0 -> chatToggle(source); // /a
            case 1 -> {
                if(args[0].equalsIgnoreCase("disable"))
                    disableStaffChat(source);
                else if(args[0].equalsIgnoreCase("enable"))
                    enableStaffChat(source);
                else quickMessage(source, args); // /a <messaggio>
            }
            default -> quickMessage(source, args); // /a <messaggio>
        }
    }

    private void disableStaffChat(CommandSource source) {
        Player p = source instanceof Player player ? player : null;
        if(p == null) {
            chatManager.disableConsoleTranscript();
            source.sendMessage(Component.text("Chat staff disabilitata per la console.", NamedTextColor.RED));
            return;
        }
        source.sendMessage(Component.text("Hai disabilitato i messaggi (in entrata) della StaffChat!", NamedTextColor.RED));
        try {
            chatManager.disableChat(p);
        } catch (ChatAlreadyDisabledException e) {
            source.sendMessage(Component.text("La StaffChat era già disabilitata.", NamedTextColor.RED));
        }
    }

    private void enableStaffChat(CommandSource source) {
        Player p = source instanceof Player player ? player : null;
        if(p == null) {
            chatManager.enableConsoleTranscript();
            source.sendMessage(Component.text("Chat staff abilitata per la console.", NamedTextColor.AQUA));
            return;
        }
        source.sendMessage(Component.text("Hai abilitato i messaggi (in entrata) della StaffChat!", NamedTextColor.AQUA));
        try {
            chatManager.enableChat(p);
        } catch (ChatAlreadyEnabledException e) {
            source.sendMessage(Component.text("La StaffChat era già abilitata.", NamedTextColor.GREEN));
        }
    }


    
    private void quickMessage(CommandSource source, String[] args) {
        String message = String.join(" ", args);
        // Identifichiamo il mittente (se non è un Player, compare come CONSOLE)
        String senderName = (source instanceof Player player) ? player.getUsername() : "CONSOLE";
        Component formattedMsg = ChatManager.formatStaffMessage(senderName, message);
        chatManager.broadcastStaffMessage(formattedMsg);
    }

    private void chatToggle(CommandSource source) {
        // Modalità toggle
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("La console non può abilitare la modalità toggle. Usa: /a <messaggio>", NamedTextColor.RED));
            return;
        }

        if (chatManager.toggleChat(player))
            player.sendMessage(Component.text("StaffChat abilitata. I tuoi prossimi messaggi verranno inviati solo allo staff.", NamedTextColor.AQUA));
        else
            player.sendMessage(Component.text("StaffChat disabilitata.", NamedTextColor.RED));
    }
}