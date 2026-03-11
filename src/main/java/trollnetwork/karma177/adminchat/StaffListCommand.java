package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;

public class StaffListCommand implements SimpleCommand {

    private final ChatManager chatManager;

    public StaffListCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if(!PermissionChecker.hasStaffChatPermission(invocation)) {
            invocation.source().sendMessage(Component.text("Non hai il permesso per usare questo comando.", NamedTextColor.RED));
            return;
        }

        List<String> onlineStaff = chatManager.getOnlineStaffNames();

        if (onlineStaff.isEmpty()) {
            invocation.source().sendMessage(Component.text("Nessun membro dello staff è attualmente online.", NamedTextColor.RED));
        } else {
            String staffNames = String.join(", ", onlineStaff);
            invocation.source().sendMessage(
                Component.text("Staff online (" + onlineStaff.size() + "): ", NamedTextColor.AQUA)
                    .append(Component.text(staffNames, NamedTextColor.AQUA))
            );
        }
    }
}
