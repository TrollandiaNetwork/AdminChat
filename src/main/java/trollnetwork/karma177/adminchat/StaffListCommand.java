package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.List;

public class StaffListCommand implements SimpleCommand {

    private final ChatManager chatManager;

    public StaffListCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    private Component toComponent(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    @Override
    public void execute(Invocation invocation) {
        if(!PermissionChecker.hasStaffChatPermission(invocation)) {
            invocation.source().sendMessage(toComponent(Messages.get("no_permission")));
            return;
        }

        List<String> onlineStaff = chatManager.getOnlineStaffNames();

        if (onlineStaff.isEmpty()) {
            invocation.source().sendMessage(toComponent(Messages.get("stafflist.none")));
        } else {
            String staffNames = String.join(", ", onlineStaff);
            String header = Messages.get("stafflist.header").replace("{count}", String.valueOf(onlineStaff.size()));
            invocation.source().sendMessage(
                toComponent(header)
                .append(Component.text(" "))
                .append(toComponent("&b" + staffNames)) // Adding a default color just in case or we could add another key
            );
        }
    }
}
