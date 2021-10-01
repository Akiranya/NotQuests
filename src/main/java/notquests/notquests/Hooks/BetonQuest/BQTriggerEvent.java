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

package notquests.notquests.Hooks.BetonQuest;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.Objective;
import notquests.notquests.Structs.Objectives.TriggerCommandObjective;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.QuestEvent;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.entity.Player;

public class BQTriggerEvent extends QuestEvent {

    private final String triggerName;
    private final NotQuests main;

    /**
     * Creates new instance of the event. The event should parse instruction
     * string without doing anything else. If anything goes wrong, throw
     * {@link InstructionParseException} with error message describing the
     * problem.
     *
     * @param instruction the Instruction object representing this event; you need to
     *                    extract all required data from it and throw
     *                    {@link InstructionParseException} if there is anything wrong
     * @throws InstructionParseException when the is an error in the syntax or argument parsing
     */
    public BQTriggerEvent(Instruction instruction) throws InstructionParseException {
        super(instruction, true);
        this.triggerName = instruction.getPart(1);
        this.main = NotQuests.getInstance();

        boolean foundTrigger = false;
        for (Quest quest : main.getQuestManager().getAllQuests()) {
            for (Objective objective : quest.getObjectives()) {
                if (objective instanceof TriggerCommandObjective triggerCommandObjective) {
                    if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(triggerName)) {
                        foundTrigger = true;
                    }
                }
            }
        }

        if (!foundTrigger) {
            throw new InstructionParseException("NotQuests TriggerObjective trigger with the name '" + triggerName + "' does not exist.");
        }

    }

    @Override
    protected Void execute(String playerID) throws QuestRuntimeException {

        final Player player = PlayerConverter.getPlayer(playerID);


        if (player != null) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof TriggerCommandObjective triggerCommandObjective) {
                                    if (triggerCommandObjective.getTriggerName().equalsIgnoreCase(triggerName)) {
                                        activeObjective.addProgress(1, -1);

                                    }
                                }
                            }

                        }
                    }
                }
            }

        }
        return null;
    }
}
