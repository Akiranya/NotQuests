/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.Structs.Conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PermissionCondition extends Condition {

    private final NotQuests main;
    private String requiredPermission = "";


    public PermissionCondition(NotQuests main) {
        super(main);
        this.main = main;
    }

    public void setRequiredPermission(final String requiredPermission){
        this.requiredPermission = requiredPermission;
    }

    public final String getRequiredPermission() {
        return requiredPermission;
    }


    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        final String requiredPermission = getRequiredPermission();

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (!player.hasPermission(requiredPermission)) {
                return "\n§eYou need the following permission: §b" + requiredPermission + "§e.";
            }
        } else {
            return "\n§eYou need to be online.";
        }
        return "";
    }

    @Override
    public String getConditionDescription() {
        return "§7-- Permission needed: " + getRequiredPermission();
    }

    @Override
    public void save(String initialPath) {
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.requiredPermission", getRequiredPermission());

    }

    @Override
    public void load(String initialPath) {
        this.requiredPermission = main.getDataManager().getQuestsConfig().getString(initialPath + ".specifics.requiredPermission");

    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        manager.command(builder.literal("Permission")
                .argument(StringArgument.<CommandSender>newBuilder("Permission").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Required Permission Node]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<Enter required Permission node>");
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Permission node which the player needs in order to accept this Quest."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Permission Requirement to a quest")
                .handler((context) -> {

                    final String permissionNode = context.get("Permission");

                    PermissionCondition permissionCondition = new PermissionCondition(main);
                    permissionCondition.setRequiredPermission(permissionNode);

                    main.getConditionsManager().addCondition(permissionCondition, context);
                }));
    }
}
