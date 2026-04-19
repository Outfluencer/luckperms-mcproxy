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

import dev.outfluencer.mcproxy.api.events.PermissionCheckEvent;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.mcproxy.LPMCProxyPlugin;
import net.lenni0451.lambdaevents.EventHandler;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

public class MCProxyPermissionCheckListener {
    private final LPMCProxyPlugin plugin;

    public MCProxyPermissionCheckListener(LPMCProxyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPermissionCheck(PermissionCheckEvent e) {
        User user = this.plugin.getUserManager().getIfLoaded(e.getPlayer().getUuid());
        if (user == null) {
            return;
        }

        QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(e.getPlayer());
        TristateResult result = user.getCachedData().getPermissionData(queryOptions)
                .checkPermission(e.getPermission(), CheckOrigin.PLATFORM_API_HAS_PERMISSION);

        if (result.result() != Tristate.UNDEFINED) {
            e.setHasPermission(result.result().asBoolean());
        }

        this.plugin.getVerboseHandler().offerPermissionCheckEvent(
                CheckOrigin.PLATFORM_API_HAS_PERMISSION,
                VerboseCheckTarget.user(user),
                queryOptions,
                e.getPermission(),
                result
        );
        this.plugin.getPermissionRegistry().offer(e.getPermission());
    }
}
