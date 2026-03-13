package trollnetwork.karma177.adminchat;

import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;

public class PermissionChecker {
    /**
    * Controlla se un giocatore ha il permesso per accedere alla chat dello staff.
    * @param player Il giocatore da verificare.
    * @return true se il giocatore ha il permesso, false altrimenti.
    */
    public static boolean hasStaffChatPermission(Player player) {
        return player.hasPermission("adminchat.staffchat");
    }

    /**
    * Controlla se un giocatore che effettua il comando ha il permesso per accedere alla chat dello staff.
    * @param player Il giocatore da verificare.
    * @return true se il giocatore ha il permesso, false altrimenti.
    */
    public static boolean hasStaffChatPermission(Invocation invocation) {
        return invocation.source().hasPermission("adminchat.staffchat");
    }

    /**
    * Controlla se una funzione di permessi in fase di setup concede il permesso al momento.
    * Utile usarlo durante il PermissionsSetupEvent prima che vengano assegnati i permessi reali al proxy.
    * @param function La funzione di permessi nativa.
    * @return true se c'è un riscontro esplicito (TRUE), altrimenti false.
    */
    public static Tristate hasPermissionForFunction(PermissionFunction function) {
        return function.getPermissionValue("adminchat.staffchat");
    }

    /**
     * Controlla se un giocatore che effettua il comando ha il permesso per ricaricare la configurazione del plugin.
     * @param invocation L'invocazione del comando da verificare.
     * @return true se il giocatore ha il permesso, false altrimenti.
     */
    public static boolean hasReloadPermission(Invocation invocation) {
        return invocation.source().hasPermission("adminchat.reload");
    }
}
