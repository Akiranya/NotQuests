package rocks.gravili.notquests.paper.structs.actions;

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


import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class BooleanAction extends Action {

    private String variableName;
    private String operator;

    private HashMap<String, String> additionalStringArguments;

    private String newValueExpression;

    public final String getOperator(){
        return operator;
    }
    public void setOperator(final String mathOperator){
        this.operator = mathOperator;
    }

    public final String getVariableName(){
        return variableName;
    }

    public void setVariableName(final String variableName){
        this.variableName = variableName;
    }

    public void setNewValueExpression(final String newValueExpression){
        this.newValueExpression = newValueExpression;
    }

    public final String getNewValueExpression(){
        return newValueExpression;
    }

    private void setAdditionalStringArguments(HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }



    public BooleanAction(final NotQuests main) {
        super(main);
        additionalStringArguments = new HashMap<>();
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {


        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);

            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.BOOLEAN){
                continue;
            }

            manager.command(main.getVariablesManager().registerVariableCommands(variableString, builder)
                    .argument(StringArgument.<CommandSender>newBuilder("operator").withSuggestionsProvider((context, lastString) -> {
                        ArrayList<String> completions = new ArrayList<>();

                        completions.add("set");
                        completions.add("setNot");

                        final List<String> allArgs = context.getRawInput();
                        main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Operator]", "[...]");

                        return completions;
                    }).build(), ArgumentDescription.of("Operator."))
                    .argument(BooleanVariableValueArgument.newBuilder("expression", main), ArgumentDescription.of("Expression"))
                    .handler((context) -> {

                        final String expression = context.get("expression");

                        final String operator = context.get("operator");

                        BooleanAction booleanAction = new BooleanAction(main);
                        booleanAction.setVariableName(variableString);
                        booleanAction.setOperator(operator);
                        booleanAction.setNewValueExpression(expression);


                        HashMap<String, String> additionalStringArguments = new HashMap<>();
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            additionalStringArguments.put(stringArgument.getName(), context.get(stringArgument.getName()));
                        }
                        booleanAction.setAdditionalStringArguments(additionalStringArguments);

                        main.getActionManager().addAction(booleanAction, context);

                    })
            );
        }
    }

    public final boolean evaluateExpression(final QuestPlayer questPlayer){
        boolean booleanRequirement = false;

        try{
            booleanRequirement = Boolean.parseBoolean(getNewValueExpression());
        }catch (Exception e){
            //Might be another variable
            for(final String otherVariableName : main.getVariablesManager().getVariableIdentifiers()){
                if(!getNewValueExpression().equalsIgnoreCase(otherVariableName)){
                    continue;
                }
                final Variable<?> otherVariable = main.getVariablesManager().getVariableFromString(otherVariableName);
                if(otherVariable == null || otherVariable.getVariableDataType() != VariableDataType.BOOLEAN){
                    main.getLogManager().debug("Null variable: <highlight>" + otherVariableName);
                    continue;
                }
                if(otherVariable.getValue(questPlayer.getPlayer(), questPlayer) instanceof Boolean bool){
                    booleanRequirement = bool;
                    break;
                }
            }
        }
        return booleanRequirement;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);

        if(variable == null){
            main.sendMessage(player, "<ERROR>Error: variable </highlight>" + variableName + "<highlight> not found. Report this to the Server owner.");
            return;
        }

        if(additionalStringArguments != null && !additionalStringArguments.isEmpty()){
            variable.setAdditionalStringArguments(additionalStringArguments);
        }

        QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

        Object currentValueObject = variable.getValue(player, questPlayer, objects);

        /*boolean currentValue = false;
        if (currentValueObject instanceof Boolean bool) {
            currentValue = bool;
        }else{
            currentValue = (boolean) currentValueObject;
        }*/

        boolean nextNewValue = evaluateExpression(questPlayer);

        if(getOperator().equalsIgnoreCase("set")){
            nextNewValue = nextNewValue;
        }else if(getOperator().equalsIgnoreCase("setNot")){
            nextNewValue = !nextNewValue;
        }else{
            main.sendMessage(player, "<ERROR>Error: variable operator <highlight>" + getOperator() + "</highlight> is invalid. Report this to the Server owner.");
            return;
        }

        questPlayer.sendDebugMessage("New Value: " + nextNewValue);



        if(currentValueObject instanceof Boolean){
            ((Variable<Boolean>)variable).setValue(nextNewValue, player, objects);
        }else{
            main.getLogManager().warn("Cannot execute boolean action, because the number type " + currentValueObject.getClass().getName() + " is invalid.");
        }

    }

    @Override
    public String getActionDescription(final Player player, final Object... objects) {
        return variableName + ": " + evaluateExpression(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
    }



    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.expression", getNewValueExpression());

        configuration.set(initialPath + ".specifics.variableName", getVariableName());
        configuration.set(initialPath + ".specifics.operator", getOperator());

        for(final String key : additionalStringArguments.keySet()){
            configuration.set(initialPath + ".specifics.additionalStrings." + key, additionalStringArguments.get(key));
        }
    }


    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.newValueExpression = configuration.getString(initialPath + ".specifics.expression", "");

        this.variableName = configuration.getString(initialPath + ".specifics.variableName");
        this.operator = configuration.getString(initialPath + ".specifics.operator", "");

        final ConfigurationSection additionalStringsConfigurationSection = configuration.getConfigurationSection(initialPath + ".specifics.additionalStrings");
        if (additionalStringsConfigurationSection != null) {
            for (String key : additionalStringsConfigurationSection.getKeys(false)) {
                additionalStringArguments.put(key, configuration.getString(initialPath + ".specifics.additionalStrings." + key, ""));
            }
        }
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {

        this.variableName = arguments.get(0);

        this.operator = arguments.get(1);
        this.newValueExpression = arguments.get(2);

        if(arguments.size() >= 4){

            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableName);
            if(variable == null || !variable.isCanSetValue() || variable.getVariableDataType() != VariableDataType.BOOLEAN){
                return;
            }

            int counter = 0;
            for (String argument : arguments){
                counter++;
                if(counter >= 4){
                    additionalStringArguments.put(variable.getRequiredStrings().get(counter-4).getName(), argument);
                }
            }
        }


    }

}