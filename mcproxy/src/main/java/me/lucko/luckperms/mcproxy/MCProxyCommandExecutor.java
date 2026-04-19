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

package me.lucko.luckperms.mcproxy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.outfluencer.mcproxy.api.command.CommandSource;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MCProxyCommandExecutor extends CommandManager {
    private static final String PRIMARY_ALIAS = "luckpermsmcproxy";
    private static final String[] ALIASES = {"lpm"};

    private final LPMCProxyPlugin plugin;

    public MCProxyCommandExecutor(LPMCProxyPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        dev.outfluencer.mcproxy.api.command.CommandManager commandManager = this.plugin.getBootstrap().getProxy().getCommandManager();

        commandManager.register(
                LiteralArgumentBuilder.<CommandSource>literal(PRIMARY_ALIAS)
                        .executes(this::execute)
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("args", StringArgumentType.greedyString())
                                .suggests(this::suggest)
                                .executes(this::execute)
                        )
        );

        // register aliases
        for (String alias : ALIASES) {
            commandManager.register(
                    LiteralArgumentBuilder.<CommandSource>literal(alias)
                            .executes(this::execute)
                            .then(RequiredArgumentBuilder.<CommandSource, String>argument("args", StringArgumentType.greedyString())
                                    .suggests(this::suggest)
                                    .executes(this::execute)
                            )
            );
        }
    }

    private int execute(CommandContext<CommandSource> ctx) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(ctx.getSource());
        String args;
        try {
            args = ctx.getArgument("args", String.class);
        } catch (IllegalArgumentException e) {
            args = "";
        }
        List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
        executeCommand(wrapped, "lpm", arguments);
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSource> ctx, SuggestionsBuilder builder) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(ctx.getSource());
        String args;
        try {
            args = ctx.getArgument("args", String.class);
        } catch (IllegalArgumentException e) {
            args = "";
        }

        List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
        List<String> completions = tabCompleteCommand(wrapped, arguments);

        // Offset the builder to the start of the last argument so suggestions
        // only replace the current word, not the entire greedy string.
        int lastSpace = args.lastIndexOf(' ');
        SuggestionsBuilder offsetBuilder = builder.createOffset(builder.getStart() + lastSpace + 1);

        for (String completion : completions) {
            offsetBuilder.suggest(completion);
        }
        return offsetBuilder.buildFuture();
    }
}
