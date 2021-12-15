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

package rocks.gravili.notquests.Managers;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import rocks.gravili.notquests.Commands.CommandNotQuests;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.AdminCommands;
import rocks.gravili.notquests.Commands.newCMDs.AdminConversationCommands;
import rocks.gravili.notquests.Commands.newCMDs.AdminEditCommands;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ActionSelector;
import rocks.gravili.notquests.Commands.newCMDs.arguments.ApplyOnSelector;
import rocks.gravili.notquests.Commands.newCMDs.arguments.QuestSelector;
import rocks.gravili.notquests.Conversation.ConversationManager;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandManager {
    private final NotQuests main;
    private final boolean useNewCommands = true;
    private PaperCommandManager<CommandSender> commandManager;
    private MinecraftHelp<CommandSender> minecraftHelp;
    private Command.Builder<CommandSender> adminCommandBuilder;
    private Command.Builder<CommandSender> adminEditCommandBuilder;
    private Command.Builder<CommandSender> adminConversationCommandBuilder;


    private Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddRequirementCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddRewardCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddTriggerCommandBuilder;

    private Command.Builder<CommandSender> adminEditObjectiveAddConditionCommandBuilder;

    private Command.Builder<CommandSender> editObjectivesBuilder;

    private AdminCommands adminCommands;
    private AdminEditCommands adminEditCommands;
    private AdminConversationCommands adminConversationCommands;


    //Re-usable value flags
    public final CommandFlag<String[]> nametag_containsany;
    public final CommandFlag<String[]> nametag_equals;
    public final CommandFlag<String> taskDescription;
    public final CommandFlag<Integer> maxDistance;


    public final CommandFlag<String> speakerColor;

    public final CommandFlag<Integer> applyOn; //0 = Quest
    public final CommandFlag<World> world;
    public final CommandFlag<String> triggerWorldString;

    public CommandManager(final NotQuests main) {
        this.main = main;

        nametag_containsany = CommandFlag
                .newBuilder("nametag_containsany")
                .withArgument(StringArrayArgument.of("nametag_containsany",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender((CommandSender) context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter nametag_containsany flag value>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<nametag_containsany flag value>");
                            return completions;
                        }
                ))
                .withDescription(ArgumentDescription.of("This word or every word seperated by a space needs to be part of the nametag"))
                .build();

        nametag_equals = CommandFlag
                .newBuilder("nametag_equals")
                .withArgument(StringArrayArgument.of("nametag_equals",
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender((CommandSender) context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "<Enter nametag_equals flag value>", "");
                            ArrayList<String> completions = new ArrayList<>();
                            completions.add("<nametag_equals flag value>");
                            return completions;
                        }
                ))
                .withDescription(ArgumentDescription.of("What the nametag has to be equal"))
                .build();

        taskDescription = CommandFlag
                .newBuilder("taskDescription")
                .withArgument(StringArgument.<CommandSender>newBuilder("Task Description").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Enter task description (put between \" \" if you want to use spaces)]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter task description (put between \" \" if you want to use spaces)>");
                            return completions;
                        }
                ).quoted())
                .withDescription(ArgumentDescription.of("Custom description of the task"))
                .build();

        speakerColor = CommandFlag
                .newBuilder("speakerColor")
                .withArgument(StringArgument.<CommandSender>newBuilder("Speaker Color").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Enter speaker color (default: <WHITE>)]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<WHITE>");
                            completions.add("<BLUE>");
                            return completions;
                        }
                ).single())
                .withDescription(ArgumentDescription.of("Color of the speaker name"))
                .build();

        maxDistance = CommandFlag
                .newBuilder("maxDistance")
                .withArgument(IntegerArgument.of("maxDistance"))
                .withDescription(ArgumentDescription.of("Enter maximum distance of two locations."))
                .build(); //0 = Quest

        world = CommandFlag
                .newBuilder("world")
                .withArgument(WorldArgument.of("world"))
                .withDescription(ArgumentDescription.of("World Name"))
                .build();


        applyOn = CommandFlag
                .newBuilder("applyOn")
                .withArgument(ApplyOnSelector.of("applyOn", main, "quest"))
                .withDescription(ArgumentDescription.of("To which part of the Quest it should apply (Examples: 'Quest', 'O1', 'O2. (O1 = Objective 1)."))
                .build(); //0 = Quest

        triggerWorldString = CommandFlag
                .newBuilder("world_name")
                .withArgument(StringArgument.<CommandSender>newBuilder("world_name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[World Name / 'ALL']", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("ALL");

                            for (final World world : Bukkit.getWorlds()) {
                                completions.add(world.getName());
                            }

                            return completions;
                        }
                ).single().build())
                .withDescription(ArgumentDescription.of("World where the Trigger applies (Examples: 'world_the_end', 'farmworld', 'world', 'ALL')."))
                .build();
    }

    public void preSetupCommands() {
        if (useNewCommands) {
            //Cloud command framework
            try {
                commandManager = new PaperCommandManager<>(
                        /* Owning plugin */ main,
                        /* Coordinator function */ CommandExecutionCoordinator.simpleCoordinator(),
                        /* Command Sender -> C */ Function.identity(),
                        /* C -> Command Sender */ Function.identity()
                );
            } catch (final Exception e) {
                main.getLogManager().severe("There was an error setting up the commands.");
                return;
            }


            adminCommandBuilder = commandManager.commandBuilder("notquestsadmin", ArgumentDescription.of("Admin commands for NotQuests"),
                            "nquestsadmin", "nquestadmin", "notquestadmin", "qadmin", "questadmin", "qa", "qag", "nqa")
                    .permission("notquests.admin");

            adminEditCommandBuilder = adminCommandBuilder
                    .literal("edit", "e")
                    .argument(QuestSelector.of("quest", main), ArgumentDescription.of("Quest Name"));

            adminConversationCommandBuilder = adminCommandBuilder
                    .literal("conversations", "c");


            adminEditAddObjectiveCommandBuilder = adminEditCommandBuilder
                    .literal("objectives", "o")
                    .literal("add");
            adminEditAddRequirementCommandBuilder = adminEditCommandBuilder
                    .literal("requirements", "req")
                    .literal("add");
            adminEditAddRewardCommandBuilder = adminEditCommandBuilder
                    .literal("rewards", "rew")
                    .literal("add");
            adminEditAddTriggerCommandBuilder = adminEditCommandBuilder
                    .literal("triggers", "t")
                    .literal("add")
                    .argument(ActionSelector.of("action", main), ArgumentDescription.of("Action which will be executed when the Trigger triggers."));


            editObjectivesBuilder = adminEditCommandBuilder.literal("objectives").literal("edit")
                    .argument(IntegerArgument.<CommandSender>newBuilder("Objective ID").withMin(1).withSuggestionsProvider(
                                    (context, lastString) -> {
                                        final List<String> allArgs = context.getRawInput();
                                        final Audience audience = main.adventure().sender(context.getSender());
                                        main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Objective ID]", "[...]");

                                        ArrayList<String> completions = new ArrayList<>();

                                        final Quest quest = context.get("quest");
                                        for (final Objective objective : quest.getObjectives()) {
                                            completions.add("" + objective.getObjectiveID());
                                        }

                                        return completions;
                                    }
                            ).withParser((context, lastString) -> { //TODO: Fix this parser. It isn't run at all.
                                final int ID = context.get("Objective ID");
                                final Quest quest = context.get("quest");
                                final Objective foundObjective = quest.getObjectiveFromID(ID);
                                if (foundObjective == null) {
                                    return ArgumentParseResult.failure(new IllegalArgumentException("Objective with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                                } else {
                                    return ArgumentParseResult.success(ID);
                                }
                            })
                            , ArgumentDescription.of("Objective ID"));


            adminEditObjectiveAddConditionCommandBuilder = editObjectivesBuilder
                    .literal("conditions")
                    .literal("add");



            //asynchronous completions
            if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }

            //brigadier/commodore
            try {
                commandManager.registerBrigadier();
                commandManager.brigadierManager().setNativeNumberSuggestions(false);
            } catch (final Exception e) {
                main.getLogger().warning("Failed to initialize Brigadier support: " + e.getMessage());
            }

            minecraftHelp = new MinecraftHelp<>(
                    "/qa help",
                    main.adventure()::sender,
                    commandManager
            );

            minecraftHelp.setHelpColors(MinecraftHelp.HelpColors.of(
                    NotQuestColors.main,
                    NamedTextColor.WHITE,
                    NotQuestColors.highlight,
                    NamedTextColor.GRAY,
                    NamedTextColor.DARK_GRAY
            ));
        }

    }

    public void setupCommands() {

       /* PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadminold");
        if (notQuestsAdminCommand != null) {
            final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
            notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
            notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);


            registerCommodoreCompletions(commodore, notQuestsAdminCommand);
        }*/
        //Register the notquests command & tab completer. This command will be used by Players
        final PluginCommand notQuestsCommand = main.getCommand("notquests");
        if (notQuestsCommand != null) {
            final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
            notQuestsCommand.setExecutor(commandNotQuests);
            notQuestsCommand.setTabCompleter(commandNotQuests);


        }


        if (!useNewCommands) {
            /*final PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadmin");
            if (notQuestsAdminCommand != null) {
                final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
                notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
                notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);
            }
            //Register the notquests command & tab completer. This command will be used by Players
            final PluginCommand notQuestsCommand = main.getCommand("notquests");
            if (notQuestsCommand != null) {
                final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
                notQuestsCommand.setExecutor(commandNotQuests);
                notQuestsCommand.setTabCompleter(commandNotQuests);
            }*/
        } else {





            constructCommands();
        }


    }


    public void constructCommands() {


       /* final Command.Builder<CommandSender> helpBuilder = commandManager.commandBuilder("notquestsadmin", "qa");
        commandManager.command(helpBuilder.meta(CommandMeta.DESCRIPTION, "fwefwe")
                .senderType(Player.class)
                .handler(commandContext -> {
                    minecraftHelp.queryCommands(commandContext.getOrDefault("", ""), commandContext.getSender());
                }));

        helpBuilder.literal("notquestsadmin",
                ArgumentDescription.of("Your Description"));*/


       /* commandManager.command(
                builder.literal("help", new String[0])
                .argument(com.magmaguy.shaded.cloud.arguments.standard.StringArgument.optional("query", com.magmaguy.shaded.cloud.arguments.standard.StringArgument.StringMode.GREEDY)).handler((context) -> {
                    this.minecraftHelp.queryCommands((String)context.getOrDefault("query", ""), context.getSender());
                })
        );*/


        //Help menu
        commandManager.command(adminCommandBuilder.meta(CommandMeta.DESCRIPTION, "Opens the help menu").handler((context) -> {
            minecraftHelp.queryCommands("qa *", context.getSender());
            final Audience audience = main.adventure().sender(context.getSender());
            final List<String> allArgs = context.getRawInput();
            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[What would you like to do?]", "[...]");

        }));
        commandManager.command(
                adminCommandBuilder.literal("help")
                        .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))

                        .handler(context -> {
                            minecraftHelp.queryCommands(context.getOrDefault("query", "qa *"), context.getSender());
                        })
        );


        MinecraftExceptionHandler<CommandSender> exceptionHandler = new MinecraftExceptionHandler<CommandSender>()
                .withArgumentParsingHandler()
                .withInvalidSenderHandler()
                .withInvalidSyntaxHandler()
                .withNoPermissionHandler()
                .withCommandExecutionHandler()
                .withDecorator(message -> {

                            return Component.text("NotQuests > ").color(NotQuestColors.main).append(Component.space()).append(message);
                        }
                )
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX, (sender, e) -> {
                    minecraftHelp.queryCommands(e.getMessage().split("syntax is: ")[1], sender);

                    return Component.text(e.getMessage(), NamedTextColor.RED);
                });

        exceptionHandler.apply(commandManager, main.adventure()::sender);

        adminCommands = new AdminCommands(main, commandManager, adminCommandBuilder);

        adminEditCommands = new AdminEditCommands(main, commandManager, adminEditCommandBuilder);


    }

    public void setupAdminConversationCommands(final ConversationManager conversationManager) { //Has to be done after ConversationManager is initialized
        adminConversationCommands = new AdminConversationCommands(main, commandManager, adminConversationCommandBuilder, conversationManager);
    }

    public final PaperCommandManager<CommandSender> getPaperCommandManager() {
        return commandManager;
    }

    public final Command.Builder<CommandSender> getAdminEditCommandBuilder() {
        return adminEditCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminConversationCommandBuilder() {
        return adminConversationCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddObjectiveCommandBuilder() {
        return adminEditAddObjectiveCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddRequirementCommandBuilder() {
        return adminEditAddRequirementCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditObjectiveAddConditionCommandBuilder(){
        return adminEditObjectiveAddConditionCommandBuilder;
    }

    public final Command.Builder<CommandSender> getEditObjectivesBuilder(){
        return editObjectivesBuilder;
    }


    public final Command.Builder<CommandSender> getAdminEditAddRewardCommandBuilder() {
        return adminEditAddRewardCommandBuilder;
    }

    public final Command.Builder<CommandSender> getAdminEditAddTriggerCommandBuilder() {
        return adminEditAddTriggerCommandBuilder;
    }


    public final AdminCommands getAdminCommands() {
        return adminCommands;
    }

    public final AdminEditCommands getAdminEditCommands() {
        return adminEditCommands;
    }

    public final AdminConversationCommands getAdminConversationCommands() {
        return adminConversationCommands;
    }
}
