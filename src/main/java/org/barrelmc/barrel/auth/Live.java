package org.barrelmc.barrel.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.packetlib.Session;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.barrelmc.barrel.server.ProxyServer;
import org.barrelmc.barrel.utils.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Timer;
import java.util.TimerTask;

public class Live {

    private static final String LIVE_CONNECT_URL = "https://login.live.com/oauth20_connect.srf";
    private static final String LIVE_TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private Logger logger;
    public Timer requestLiveToken(Session session, String username) throws Exception {
        this.logger = ProxyServer.getInstance().getLogger();
        Logger Log = this.logger;
        JsonObject d = AuthManager.getInstance().getXboxLive().startDeviceAuth();

        Component linkComponent = Component.text(d.get("verification_uri").getAsString())
                .clickEvent(ClickEvent.openUrl(d.get("verification_uri").getAsString()))
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.UNDERLINED);

        Component codeComponent = Component.text(d.get("user_code").getAsString())
                .color(NamedTextColor.GREEN)
                .hoverEvent(Component.text("Click to copy code to clipboard."))
                .clickEvent(ClickEvent.copyToClipboard(d.get("user_code").getAsString()));

        TextComponent textComponent = Component.text()
                .append(Component.text("§eAuthenticate at ").append(linkComponent))
                .append(Component.text(" §eusing the code ").append(codeComponent))
                .append(Component.text(". §eThis code will expire in ")
                        .append(Component.text(d.get("expires_in").getAsString() + " seconds.")))
                .build();
        session.send(new ClientboundSystemChatPacket(textComponent, false));

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    JsonObject r = AuthManager.getInstance().getXboxLive().pollDeviceAuth(d.get("device_code").getAsString());

                    if (r.has("error")) {
                        Log.error(r.get("error_description").getAsString());
                        return;
                    }

                    AuthManager.getInstance().getAccessTokens().put(username, r.get("access_token").getAsString());
                    AuthManager.getInstance().getLoginPlayers().put(username, true);

                    AuthManager.getInstance().getTimers().remove(username);

                    session.send(new ClientboundSystemChatPacket(Component.text("§eSuccessfully authenticated with Xbox Live. Please rejoin!"), false));
                    Log.info(username + " authenticated");

                    timer.cancel();
                } catch (Exception ignored) {
                }
            }
        }, 0, d.get("interval").getAsLong() * 1000);

        return timer;
    }

    public JsonObject startDeviceAuth() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder().build();

        String requestBody = "client_id=00000000441cc96b&scope=service::user.auth.xboxlive.com::MBI_SSL&response_type=device_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LIVE_CONNECT_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        if (statusCode != 200) {
            throw new Exception("Failed to start device auth");
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    public JsonObject pollDeviceAuth(String deviceCode) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        String requestBody = "client_id=00000000441cc96b&grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=" + deviceCode;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(LIVE_TOKEN_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception(response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (json.has("error")) {
            if (json.get("error").getAsString().equals("authorization_pending")) {
                return null;
            } else {
                throw new Exception("non-empty unknown poll error: " + json.get("error").getAsString());
            }
        } else {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
    }
}
