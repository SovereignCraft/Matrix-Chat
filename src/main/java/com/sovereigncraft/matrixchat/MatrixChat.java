package com.sovereigncraft.matrixchat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MatrixChat extends JavaPlugin {
    private String matrixServerUrl;
    private String matrixServerName;
    private String adminAccessToken;
    private HttpClient httpClient;
    private Gson gson;

    @Override
    public void onEnable() {
        // Initialize configuration
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        matrixServerUrl = config.getString("matrix.server-url", "https://matrix.example.com");
        matrixServerName = config.getString("matrix.server-name", "example.com");
        adminAccessToken = config.getString("matrix.admin-token", "");

        // Initialize HTTP client and Gson
        httpClient = HttpClient.newHttpClient();
        gson = new Gson();

        // Register command
        getCommand("chat").setExecutor(new ChatCommand());
        getLogger().info("Matrix Chat Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Matrix Chat Plugin disabled!");
    }

    // Command to handle /chat subcommands
    class ChatCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            if (args.length < 1 || !args[0].equalsIgnoreCase("register")) {
                player.sendMessage("Usage: /chat register <password>");
                return false;
            }

            if (args.length != 2) {
                player.sendMessage("Usage: /chat register <password>");
                return false;
            }

            String password = args[1];
            String username = player.getName();
            boolean isBedrock = username.startsWith(".");
            if (isBedrock) {
                username = "br-" + username.substring(1).toLowerCase();
            } else {
                username = username.toLowerCase();
            }

            try {
                // Create Matrix account
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("username", username);
                requestBody.addProperty("password", password);
                requestBody.addProperty("inhibit_login", false);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(matrixServerUrl + "/_matrix/client/v3/register"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode == 200) {
                    player.sendMessage("Successfully registered Matrix account: @" + username + ":" + matrixServerName);
                } else {
                    JsonObject error = gson.fromJson(response.body(), JsonObject.class);
                    String errorMessage = error.get("errcode").getAsString() + ": " + error.get("error").getAsString();
                    player.sendMessage("Registration failed: " + errorMessage);
                }
            } catch (Exception e) {
                player.sendMessage("Error during registration: " + e.getMessage());
                e.printStackTrace();
            }

            return true;
        }
    }
}