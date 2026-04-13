package br.com.gorvax.core.managers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * B8 — Classe utilitária para enviar payloads JSON ao Discord via Webhook HTTP POST.
 * Todas as requisições são executadas de forma assíncrona via CompletableFuture.
 */
public class DiscordWebhook {

    private final String webhookUrl;
    private final Logger logger;

    public DiscordWebhook(String webhookUrl, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.logger = logger;
    }

    /**
     * Envia uma mensagem de texto simples ao Discord.
     *
     * @param content texto a ser exibido no canal
     */
    public void send(String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        // Escapa aspas e barras invertidas para JSON válido
        String escaped = escapeJson(content);
        String json = "{\"content\":\"" + escaped + "\"}";

        postAsync(json);
    }

    /**
     * Envia uma mensagem com embed formatado ao Discord.
     *
     * @param title       título do embed
     * @param description descrição/corpo do embed
     * @param color       cor lateral do embed (decimal, ex: 0xFF5555 = vermelho)
     */
    public void sendEmbed(String title, String description, int color) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String json = "{\"embeds\":[{"
                + "\"title\":\"" + escapeJson(title) + "\","
                + "\"description\":\"" + escapeJson(description) + "\","
                + "\"color\":" + color
                + "}]}";

        postAsync(json);
    }

    /**
     * Executa o POST HTTP de forma assíncrona.
     */
    private void postAsync(String jsonPayload) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    logger.warning("[GorvaxCore] Discord webhook retornou HTTP " + responseCode);
                }

                conn.disconnect();
            } catch (IOException e) {
                logger.log(Level.WARNING, "[GorvaxCore] Discord webhook falhou: " + e.getMessage());
            }
        });
    }

    /**
     * Escapa caracteres especiais para JSON válido.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
