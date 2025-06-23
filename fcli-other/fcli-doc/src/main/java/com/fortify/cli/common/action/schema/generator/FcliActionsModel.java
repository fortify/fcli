package com.fortify.cli.common.action.schema.generator;

/**
 * This class holds the information read from each FCLI action with the name,
 * type, and description.
 * 
 * @author svijaykumar
 */
public class FcliActionsModel {

	private final String actionName;
	private final String actionType;
	private final String actionDescription;

	public FcliActionsModel(String actionName, String actionType, String actionDescription) {
		this.actionName = actionName;
		this.actionType = actionType;
		this.actionDescription = actionDescription;
	}

	public String getActionName() {
		return actionName;
	}

	public String getActionType() {
		return actionType;
	}

	public String getActionDescription() {
		return actionDescription;
	}

	@Override
	public String toString() {
		return String.format("Name: %s%nDescription: %s%nType: %s", actionName, actionDescription, actionType);
	}
}
