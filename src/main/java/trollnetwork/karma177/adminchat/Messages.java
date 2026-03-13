package trollnetwork.karma177.adminchat;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe responsabile della gestione dei messaggi del plugin.
 * Legge i messaggi dal file messages.yml e li rende disponibili in memoria.
 */
public class Messages {

    // Mappa che contiene i messaggi caricati: Chiave -> Messaggio
    private static final Map<String, String> messages = new HashMap<>();
    private static AdminChat plugin;

    /**
     * Inizializza il sistema di messaggi.
     * Cerca il file messages.yml nel percorso specificato:
     * - Se non esiste, lo crea copiandolo dalle risorse interne del JAR.
     * - Successivamente, carica i messaggi in memoria.
     *
     * @param filePath Il percorso completo del file messages.yml
     */
    public static void init(String filePath, AdminChat plugin) {
        File file = new File(filePath);
        Messages.plugin = plugin;

        // Controllo se il file esiste, altrimenti procedo alla creazione
        if (!file.exists()) {
            try {
                // Assicuriamoci che la cartella genitore esista
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }

                // Carico il file dalle risorse (dentro il file .jar)
                try (InputStream in = Messages.class.getClassLoader().getResourceAsStream("messages.yml")) {
                    if (in != null) {
                        Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("File configuration 'messages.yml' creato con successo.");
                    } else {
                        plugin.getLogger().info("ERRORE: Impossibile trovare 'messages.yml' nel JAR!");
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().info("Errore durante la creazione di messages.yml: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Carico i messaggi dal file (ora sicuramente esistente)
        loadMessages(file);
    }

    /**
     * Metodo interno per leggere il file YAML e salvare i valori nella mappa.
     * Utilizza SnakeYAML per il parsing.
     *
     * @param file Il file da leggere
     */
    private static void loadMessages(File file) {
        Yaml yaml = new Yaml();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            Map<String, Object> loaded = yaml.load(inputStream);
            messages.clear();
            if (loaded != null) {
                for (Map.Entry<String, Object> entry : loaded.entrySet()) {
                    // Il file messages.yml ha una struttura chiave: valore
                    // Salviamo sempre come stringa per semplicità
                    if (entry.getValue() != null) {
                        messages.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }else{
                        messages.put(entry.getKey(), ""); // Se il valore è null, lo salviamo come stringa vuota
                        System.out.println("Attenzione: Il messaggio con chiave '" + entry.getKey() + "' è null. Salvato come stringa vuota.");
                    }
                }
                plugin.getLogger().info("Caricati " + messages.size() + " messaggi dalla configurazione.");
            }
        } catch (Exception e) {
            plugin.getLogger().info("Errore critico nel caricamento di messages.yml:");
            e.printStackTrace();
        }
    }

    /**
     * Restituisce un messaggio dalla configurazione usando la sua chiave.
     * Se la chiave non viene trovata, restituisce la chiave stessa (utile per individuare errori).
     *
     * @param key La chiave del messaggio (es. "no_permission")
     * @return La stringa del messaggio associata
     */
    public static String get(String key) {
        return messages.getOrDefault(key, "Messaggio non trovato: " + key);
    }
}
