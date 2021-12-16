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

package rocks.gravili.notquests.Structs.Conditions.hooks;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Conditions.ConditionFor;
import rocks.gravili.notquests.Structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class TownyNationNameCondition extends Condition {

    private final NotQuests main;
    private String townyNationName = "";

    public TownyNationNameCondition(final NotQuests main) {
        super(main);
        this.main = main;
    }

    public void setTownyNationName(final String newTownyNationName){
        this.townyNationName = newTownyNationName;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.isTownyEnabled()) {
            return;
        }

        manager.command(builder.literal("TownyNationName")
                .argument(StringArgument.<CommandSender>newBuilder("Nation Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Required nation name (put between \"\" if using spaces)]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<Enter required nation name (put between \"\" if using spaces)>");
                            return completions;
                        }
                ).quoted().build(), ArgumentDescription.of("Name of the nation which the player needs to be a member of"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new TownyNationName Requirement to a quest")
                .handler((context) -> {
                    final String townyNationName = context.get("Nation Name");

                    TownyNationNameCondition townyNationNameCondition = new TownyNationNameCondition(main);
                    townyNationNameCondition.setTownyNationName(townyNationName);

                    main.getConditionsManager().addCondition(townyNationNameCondition, context);
                }));
    }

    public final String getTownyNationName() {
        return townyNationName;
    }



    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        final Player player = questPlayer.getPlayer();
        if (player != null) {
            if (!main.isTownyEnabled()) {
                return "\n§eError: The server does not have Towny enabled. Please ask the Owner to install Towny for Towny stuff to work.";
            } else {
                Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUUID());
                try{
                    if(resident != null && resident.getTown() != null && resident.hasTown() && resident.getTown().hasNation()){

                        Nation nation = resident.getNationOrNull();
                        if(nation != null && nation.getName().replace("_", " ").equals(getTownyNationName())){
                            return "";
                        }else{
                            if(nation != null){
                                return "\n§eYou need to be in the nation §b" + getTownyNationName() + "§e. However, you are currently in §b" + nation.getName().replace("_", " ");
                            }else{
                                return "\n§eYou need to be in the nation §b" + getTownyNationName();
                            }
                        }
                    }else{
                        return "\n§eYou need to be in the nation §b" + getTownyNationName();
                    }
                }catch (Exception e){
                    return "\n§eYou need to be in the nation §b" + getTownyNationName();
                }




            }
        } else {
            return "\n§eError reading TownyNationName requirement...";

        }
    }

    @Override
    public String getConditionDescription() {
        return "§7-- Member of nation: " + getTownyNationName() + "\n";
    }

    @Override
    public void save(String initialPath) {
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.townyNationName", getTownyNationName());

    }

    @Override
    public void load(String initialPath) {
        this.townyNationName = main.getDataManager().getQuestsConfig().getString(initialPath + ".specifics.townyNationName");

    }
}
