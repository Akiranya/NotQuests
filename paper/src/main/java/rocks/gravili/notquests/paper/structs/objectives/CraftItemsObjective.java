/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

public class CraftItemsObjective extends Objective {

    private ItemStack itemToCraft;
    private boolean craftAnyItem = false;
    private String nqItemName = "";


    public CraftItemsObjective(NotQuests main) {
        super(main);
    }

    public void setNQItem(final String nqItemName){
        this.nqItemName = nqItemName;
    }
    public final String getNQItem(){
        return nqItemName;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be crafted."))
                .argument(NumberVariableValueArgument.newBuilder("amount", main, null), ArgumentDescription.of("Amount of items which need to be crafted"))
                .handler((context) -> {
                    final String amountExpression = context.get("amount");

                    boolean craftAnyItem = false;
                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToCraft;
                    if (materialOrHand.material.equalsIgnoreCase("any")) {
                        craftAnyItem = true;
                        itemToCraft = null;
                    } else {
                        itemToCraft = main.getItemsManager().getItemStack(materialOrHand);
                    }

                    CraftItemsObjective craftItemsObjective = new CraftItemsObjective(main);

                    if(main.getItemsManager().getItem(materialOrHand.material) != null){
                        craftItemsObjective.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                    }else{
                        craftItemsObjective.setItemToCraft(itemToCraft);
                    }

                    craftItemsObjective.setCraftAnyItem(craftAnyItem);
                    craftItemsObjective.setProgressNeededExpression(amountExpression);

                    main.getObjectiveManager().addObjective(craftItemsObjective, context);
                }));
    }

    public final boolean isCraftAnyItem() {
        return craftAnyItem;
    }

    public void setCraftAnyItem(final boolean craftAnyItem) {
        this.craftAnyItem = craftAnyItem;
    }

    public void setItemToCraft(final ItemStack itemToCraft) {
        this.itemToCraft = itemToCraft;
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

    public final ItemStack getItemToCraft() {
        if(!getNQItem().isBlank()){
            return main.getItemsManager().getItem(getNQItem()).getItemStack().clone();
        }else{
            return itemToCraft;
        }
    }


    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        final String displayName;
        if (!isCraftAnyItem()) {
            if (getItemToCraft().getItemMeta() != null) {
                displayName = PlainTextComponentSerializer.plainText().serializeOr(getItemToCraft().getItemMeta().displayName(), getItemToCraft().getType().name());
            } else {
                displayName = getItemToCraft().getType().name();
            }
        } else {
            displayName = "Any";
        }

        String itemType = isCraftAnyItem() ? "Any" : getItemToCraft().getType().name();


        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.craftItems.base", questPlayer, activeObjective, Map.of(
                    "%ITEMTOCRAFTTYPE%", itemType,
                    "%ITEMTOCRAFTNAME%", displayName,
                    "%(%", "(",
                    "%)%", "<RESET>)"
            ));
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.craftItems.base", questPlayer, activeObjective, Map.of(
                    "%ITEMTOCRAFTTYPE%", itemType,
                    "%ITEMTOCRAFTNAME%", displayName,
                    "%(%", "",
                    "%)%", ""
            ));
        }


    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if(!getNQItem().isBlank()){
            configuration.set(initialPath + ".specifics.nqitem", getNQItem());
        }else {
            configuration.set(initialPath + ".specifics.itemToCraft.itemstack", getItemToCraft());
        }

        configuration.set(initialPath + ".specifics.craftAnyItem", isCraftAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");
        if(nqItemName.isBlank()){
            itemToCraft = configuration.getItemStack(initialPath + ".specifics.itemToCraft.itemstack");
        }

        craftAnyItem = configuration.getBoolean(initialPath + ".specifics.craftAnyItem", false);
    }
}
