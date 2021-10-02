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

package notquests.notquests.Hooks.BetonQuest.Conditions;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.CompletedQuest;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Requirements.*;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.Condition;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.id.ID;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.entity.Player;

public class BQRequirementsCondition extends Condition {

    private final NotQuests main;
    private Requirement requirement = null;

    /**
     * Creates new instance of the condition. The condition should parse
     * instruction string at this point and extract all the data from it. If
     * anything goes wrong, throw {@link InstructionParseException} with an
     * error message describing the problem.
     *
     * @param instruction the Instruction object; you can get one from ID instance with
     *                    {@link ID#generateInstruction()
     *                    ID.generateInstruction()} or create it from an instruction
     *                    string
     */
    public BQRequirementsCondition(Instruction instruction) throws InstructionParseException {
        super(instruction, false);
        this.main = NotQuests.getInstance();

        final String requirementTypeName = instruction.getPart(1);

        RequirementType requirementType;
        try {
            requirementType = RequirementType.valueOf(requirementTypeName);
        } catch (Exception e) {
            throw new InstructionParseException("Requirement type '" + requirementTypeName + "' does not exist.");
        }

        int requirementInt = 0;
        if (requirementType == RequirementType.OtherQuest) {
            String requirementString = instruction.getPart(2);
            try {
                requirementInt = Integer.parseInt(instruction.getPart(3));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number for second argument (amount of requirements needed).");
            }

            requirement = new OtherQuestRequirement(main, requirementString, requirementInt);

        } else if (requirementType == RequirementType.Money) {
            try {
                requirement = new MoneyRequirement(main, Long.parseLong(instruction.getPart(2)), false);

            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number for second argument (amount of requirements needed).");
            }

        } else if (requirementType == RequirementType.QuestPoints) {
            try {
                requirement = new QuestPointsRequirement(main, Long.parseLong(instruction.getPart(2)), false);

            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number for second argument (amount of requirements needed).");
            }
        } else if (requirementType == RequirementType.Permission) {
            String requirementString = instruction.getPart(2);


            requirement = new PermissionRequirement(main, requirementString);
        } else {
            throw new InstructionParseException("Requirement type '" + requirementTypeName + "' could not be created. Please contact the NotQuests author about it.");
        }

    }

    @Override
    protected Boolean execute(String playerID) throws QuestRuntimeException {
        if (requirement != null) {
            final Player player = PlayerConverter.getPlayer(playerID);

            if (requirement instanceof final OtherQuestRequirement otherQuestRequirement) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    final Quest otherQuest = otherQuestRequirement.getOtherQuest();

                    int otherQuestCompletedAmount = 0;

                    for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                        if (completedQuest.getQuest().equals(otherQuest)) {
                            otherQuestCompletedAmount += 1;
                        }
                    }
                    return otherQuestCompletedAmount >= otherQuestRequirement.getAmountOfCompletionsNeeded();
                }


            } else if (requirement instanceof final QuestPointsRequirement questPointsRequirement) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (questPlayer != null) {
                    final long questPointRequirementAmount = questPointsRequirement.getQuestPointRequirement();

                    return questPlayer.getQuestPoints() >= questPointRequirementAmount;
                }

            } else if (requirement instanceof final MoneyRequirement moneyRequirement) {
                final long moneyRequirementAmount = moneyRequirement.getMoneyRequirement();

                if (!main.isVaultEnabled() || main.getEconomy() == null) {
                    throw new QuestRuntimeException("The server does not have vault enabled. Please install Vault for money requirements to work.");
                } else
                    return !(main.getEconomy().getBalance(player, player.getWorld().getName()) < moneyRequirementAmount);


            } else if (requirement instanceof final PermissionRequirement permissionRequirement) {
                final String requiredPermission = permissionRequirement.getRequiredPermission();

                return player.hasPermission(requiredPermission);

            } else {
                throw new QuestRuntimeException("Requirement you entered is invalid.");
            }
        } else {
            throw new QuestRuntimeException("Requirement was not found.");
        }
        return null;
    }
}
