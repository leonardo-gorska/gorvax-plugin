package br.com.gorvax.core.managers;

import br.com.gorvax.core.GorvaxCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * Gerencia o envio de Bedrock Forms nativos via Floodgate/Cumulus API.
 * Toda integração é soft-dependency: se Floodgate não estiver presente, os métodos retornam false.
 * As callbacks dos forms rodam FORA da main thread — este manager envolve as callbacks
 * em Bukkit.getScheduler().runTask() para thread-safety.
 */
public class BedrockFormManager {

    private final GorvaxCore plugin;
    private final boolean floodgateAvailable;

    public BedrockFormManager(GorvaxCore plugin) {
        this.plugin = plugin;
        this.floodgateAvailable = Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    /**
     * Verifica se a Floodgate API está disponível no servidor.
     */
    public boolean isAvailable() {
        return floodgateAvailable;
    }

    /**
     * Envia um SimpleForm (menu de botões) ao jogador Bedrock.
     *
     * @param player   Jogador Bedrock
     * @param title    Título do form
     * @param content  Descrição/conteúdo
     * @param buttons  Lista de textos dos botões
     * @param callback Callback com o índice do botão clicado (-1 se fechado)
     * @return true se o form foi enviado com sucesso
     */
    public boolean sendSimpleForm(Player player, String title, String content,
                                  List<String> buttons, Consumer<Integer> callback) {
        if (!floodgateAvailable) return false;

        try {
            org.geysermc.cumulus.form.SimpleForm.Builder builder =
                    org.geysermc.cumulus.form.SimpleForm.builder()
                            .title(title)
                            .content(content);

            for (String btn : buttons) {
                builder.button(btn);
            }

            builder.validResultHandler((form, response) -> {
                int clickedIndex = response.clickedButtonId();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(clickedIndex));
            });

            builder.closedOrInvalidResultHandler((form, response) -> {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(-1));
            });

            return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                    .sendForm(player.getUniqueId(), builder);
        } catch (Throwable t) {
            plugin.getLogger().warning("[BedrockForms] Erro ao enviar SimpleForm: " + t.getMessage());
            return false;
        }
    }

    /**
     * Envia um CustomForm (formulário com campos de input) ao jogador Bedrock.
     *
     * @param player      Jogador Bedrock
     * @param title       Título do form
     * @param label       Label do campo de texto
     * @param defaultText Texto padrão do campo
     * @param callback    Callback com o texto digitado (null se fechado/cancelado)
     * @return true se o form foi enviado com sucesso
     */
    public boolean sendCustomForm(Player player, String title, String label,
                                  String defaultText, Consumer<String> callback) {
        if (!floodgateAvailable) return false;

        try {
            org.geysermc.cumulus.form.CustomForm.Builder builder =
                    org.geysermc.cumulus.form.CustomForm.builder()
                            .title(title)
                            .input(label, defaultText != null ? defaultText : "");

            builder.validResultHandler((form, response) -> {
                String text = response.asInput();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(text));
            });

            builder.closedOrInvalidResultHandler((form, response) -> {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            });

            return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                    .sendForm(player.getUniqueId(), builder);
        } catch (Throwable t) {
            plugin.getLogger().warning("[BedrockForms] Erro ao enviar CustomForm: " + t.getMessage());
            return false;
        }
    }

    /**
     * Envia um ModalForm (confirmação SIM/NÃO) ao jogador Bedrock.
     *
     * @param player   Jogador Bedrock
     * @param title    Título do form
     * @param content  Descrição/conteúdo
     * @param yesText  Texto do botão afirmativo
     * @param noText   Texto do botão negativo
     * @param callback Callback com true=sim, false=não, null=fechado
     * @return true se o form foi enviado com sucesso
     */
    public boolean sendModalForm(Player player, String title, String content,
                                 String yesText, String noText, Consumer<Boolean> callback) {
        if (!floodgateAvailable) return false;

        try {
            org.geysermc.cumulus.form.ModalForm.Builder builder =
                    org.geysermc.cumulus.form.ModalForm.builder()
                            .title(title)
                            .content(content)
                            .button1(yesText)
                            .button2(noText);

            builder.validResultHandler((form, response) -> {
                boolean clickedFirst = response.clickedButtonId() == 0;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(clickedFirst));
            });

            builder.closedOrInvalidResultHandler((form, response) -> {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            });

            return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                    .sendForm(player.getUniqueId(), builder);
        } catch (Throwable t) {
            plugin.getLogger().warning("[BedrockForms] Erro ao enviar ModalForm: " + t.getMessage());
            return false;
        }
    }
}
