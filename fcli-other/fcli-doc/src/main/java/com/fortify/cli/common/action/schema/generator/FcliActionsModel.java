package com.fortify.cli.common.action.schema.generator;

/**
 * This class holds the information read from each FCLI action with the name,
 * type, and description.
 * 
 * @author svijaykumar
 */
public class FcliActionsModel {

	private final String actionName;
	private final String actionTypeStr;
	private final Class<?> actionType;
	private final String actionDescription;

	public FcliActionsModel(String actionName, Class<?> actionType, String actionTypeStr, String actionDescription) {
		this.actionName = actionName;
		this.actionTypeStr = actionTypeStr == null ? actionType.getSimpleName() : actionTypeStr;
		this.actionDescription = actionDescription;
		this.actionType = actionType;
	}

	public String getActionName() {
		return actionName;
	}

	public String getActionTypeStr() {
		return actionTypeStr;
	}

	/**
	 * @return the actionType
	 */
	public Class<?> getActionType() {
		return actionType;
	}

	public String getActionDescription() {
		return actionDescription;
	}

	@Override
	public String toString() {
		return String.format("Name: %s\nDescription: %sType: %s\n", actionName, actionDescription, actionTypeStr);
	}
}
