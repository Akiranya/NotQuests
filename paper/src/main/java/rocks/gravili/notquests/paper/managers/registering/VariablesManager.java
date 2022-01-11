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

package rocks.gravili.notquests.paper.managers.registering;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.*;
import rocks.gravili.notquests.paper.structs.variables.hooks.*;

import java.util.Collection;
import java.util.HashMap;


public class VariablesManager {
    private final NotQuests main;

    private final HashMap<String, Class<? extends Variable<?>>> variables;


    public VariablesManager(final NotQuests main) {
        this.main = main;
        variables = new HashMap<>();

        registerDefaultVariables();

    }


    public void registerDefaultVariables() {
        variables.clear();
        registerVariable("QuestPoints", QuestPointsVariable.class);
        registerVariable("Money", MoneyVariable.class);
        registerVariable("UltimateClansClanLevel", UltimateClansClanLevelVariable.class);
        registerVariable("ActiveQuests", ActiveQuestsVariable.class);
        registerVariable("CompletedQuests", CompletedQuestsVariable.class);
        registerVariable("Permission", PermissionVariable.class);

        if(main.getIntegrationsManager().isPlaceholderAPIEnabled()){
            registerVariable("PlaceholderAPINumber", PlaceholderAPINumberVariable.class);
            registerVariable("PlaceholderAPIString", PlaceholderAPIStringVariable.class);
        }
        if(main.getIntegrationsManager().isTownyEnabled()){
            registerVariable("TownyNationTownCount", TownyNationTownCountVariable.class);
            registerVariable("TownyTownResidentCount", TownyTownResidentCountVariable.class);
            registerVariable("TownyTownPlotCount", TownyTownPlotCountVariable.class);
            registerVariable("TownyNationName", TownyNationNameVariable.class);
        }


    }

    public Command.Builder<CommandSender> registerVariableCommands(String variableString, Command.Builder<CommandSender> builder){
        Command.Builder<CommandSender> newBuilder = builder.literal(variableString, ArgumentDescription.of("Variable Name"));

        Variable<?> variable = getVariableFromString(variableString);
        if(variable != null && variable.getRequiredStrings() != null){
            for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                newBuilder = newBuilder.argument(stringArgument, ArgumentDescription.of("Optional Argument"));
            }
        }
        return newBuilder;
    }


    public void registerVariable(final String identifier, final Class<? extends Variable<?>> Variable) {
        main.getLogManager().info("Registering Variable <highlight>" + identifier);
        variables.put(identifier, Variable);


        /*try {
            Method commandHandler = Variable.getMethod("handleCommands", main.getClass(), PaperCommandManager.class, Command.Builder.class, VariableFor.class);
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditAddRequirementCommandBuilder(), VariableFor.QUEST);
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditObjectiveAddVariableCommandBuilder(), VariableFor.OBJECTIVE);
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminAddVariableCommandBuilder(), VariableFor.variablesYML); //For Actions.yml
            commandHandler.invoke(Variable, main, main.getCommandManager().getPaperCommandManager(), main.getCommandManager().getAdminEditActionsAddVariableCommandBuilder(), VariableFor.Action); //For Actions.yml
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }*/
    }


    public final Class<? extends Variable<?>> getVariableClass(final String type) {
        return variables.get(type);
    }

    public final String getVariableType(final Class<? extends Variable> variable) {
        for (final String VariableType : variables.keySet()) {
            if (variables.get(VariableType).equals(variable)) {
                return VariableType;
            }
        }
        return null;
    }

    public final HashMap<String, Class<? extends Variable<?>>> getVariablesAndIdentifiers() {
        return variables;
    }

    public final Collection<Class<? extends Variable<?>>> getVariables() {
        return variables.values();
    }

    public final Collection<String> getVariableIdentifiers() {
        return variables.keySet();
    }

    public void addVariable(Variable<?> Variable, CommandContext<CommandSender> context) {

    }

    public final Variable<?> getVariableFromString(final String variableString) {
        Class<? extends Variable<?>> variableClass = getVariableClass(variableString);
        try{
            return variableClass.getDeclaredConstructor(NotQuests.class).newInstance(main);
        }catch (Exception e){
            return null;
        }
    }


    public double evaluateExpression(String expression, final Player player, final Object... objects){

        for(String variableString : main.getVariablesManager().getVariableIdentifiers()){
            if(!expression.contains(variableString)){
                continue;
            }
            Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
            if(variable == null || variable.getVariableDataType() != VariableDataType.NUMBER){
                main.getLogManager().debug("Null variable: <highlight>" + variableString);
                continue;
            }
            Object valueObject = variable.getValue(player, objects);
            if(valueObject instanceof Number n){
                expression = expression.replace(variableString, ""+n.doubleValue());
            }else{
                main.getLogManager().debug("Wrong valueObject for " + variableString +". Null?: " + (valueObject == null) );

            }
        }

        main.getLogManager().debug("To evaluate: <highlight>" + expression);

        CompiledExpression exp = Crunch.compileExpression(expression);


        return exp.evaluate();
    }
}