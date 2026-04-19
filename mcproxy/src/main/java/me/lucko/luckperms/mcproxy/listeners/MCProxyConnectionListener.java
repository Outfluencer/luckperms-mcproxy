/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.mcproxy.listeners;

import dev.outfluencer.mcproxy.api.connection.Player;
import dev.outfluencer.mcproxy.api.events.PlayerDisconnectEvent;
import dev.outfluencer.mcproxy.api.events.PlayerLoggedInEvent;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.mcproxy.LPMCProxyPlugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.mcstructs.text.components.StringComponent;

import java.util.concurrent.CompletableFuture;

public class MCProxyConnectionListener extends AbstractConnectionListener {
    private final LPMCProxyPlugin plugin;

    public MCProxyConnectionListener(LPMCProxyPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLoggedIn(PlayerLoggedInEvent e) {
        final Player player = e.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUuid() + " - " + player.getName());
        }

        // Register an intent to delay the event until we finish loading
        CompletableFuture<Void> intent = new CompletableFuture<>();
        e.registerIntent(intent);

        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                User user = loadUser(player.getUuid(), player.getName());
                recordConnection(player.getUuid());
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUuid(), player.getName(), user);
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Exception occurred whilst loading data for " + player.getUuid() + " - " + player.getName(), ex);

                if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                    e.setCancelled(true);
                    e.setDisconnectMessage(new StringComponent(
                            PlainTextComponentSerializer.plainText().serialize(
                                    TranslationManager.render(Message.LOADING_DATABASE_ERROR.build())
                            )
                    ));
                }
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUuid(), player.getName(), null);
            } finally {
                intent.complete(null);
            }
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        handleDisconnect(e.player().getUuid());
    }
}
