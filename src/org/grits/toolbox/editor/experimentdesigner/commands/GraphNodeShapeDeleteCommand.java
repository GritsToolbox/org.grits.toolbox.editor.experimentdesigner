/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
?* All rights reserved. This program and the accompanying materials
?* are made available under the terms of the Eclipse Public License v1.0
?* which accompanies this distribution, and is available at
?* http://www.eclipse.org/legal/epl-v10.html
?*
?* Contributors:
?*????Elias Volanakis - initial API and implementation
?*******************************************************************************/
package org.grits.toolbox.editor.experimentdesigner.commands;

import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.grits.toolbox.editor.experimentdesigner.model.Connection;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.InputNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;

/**
 * A command to remove a shape from its parent. The command can be undone or
 * redone.
 * 
 * @author Elias Volanakis
 */
public class GraphNodeShapeDeleteCommand extends Command {
	/** Shape to remove. */
	private final GraphNode child;

	/** ShapeDiagram to remove from. */
	private final ExperimentGraph parent;
	/** Holds a copy of the outgoing connections of child. */
	private List sourceConnections;
	/** Holds a copy of the incoming connections of child. */
	private List targetConnections;
	/** True, if child was removed from its parent. */
	private boolean wasRemoved;

	/**
	 * Create a command that will remove the node from its parent.
	 * 
	 * @param parent
	 *            the Graph containing the child
	 * @param child
	 *            the GraphNode to remove
	 * @throws IllegalArgumentException
	 *             if any parameter is null
	 */
	public GraphNodeShapeDeleteCommand(ExperimentGraph parent, GraphNode child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel("shape deletion");
		this.parent = parent;
		this.child = child;
	}

	/**
	 * Reconnects a List of Connections with their previous endpoints.
	 * 
	 * @param connections
	 *            a non-null List of connections
	 */
	private void addConnections(List connections) {
		for (Iterator iter = connections.iterator(); iter.hasNext();) {
			Connection conn = (Connection) iter.next();
			conn.reconnect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#canUndo()
	 */
	public boolean canUndo() {
		return wasRemoved;
	}
	
	@Override
	public boolean canExecute() {
		if (child instanceof InputNode || child instanceof OutputNode)  // cannot delete input/output nodes
			return false;
		return super.canExecute();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		// store a copy of incoming & outgoing connections before proceeding
		sourceConnections = child.getSourceConnections();
		targetConnections = child.getTargetConnections();
		redo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		// remove the child and disconnect its connections
		wasRemoved = parent.removeChild(child);
		if (wasRemoved) {
			removeConnections(sourceConnections);
			removeConnections(targetConnections);
		}
	}

	/**
	 * Disconnects a List of Connections from their endpoints.
	 * 
	 * @param connections
	 *            a non-null List of connections
	 */
	private void removeConnections(List connections) {
		for (Iterator iter = connections.iterator(); iter.hasNext();) {
			Connection conn = (Connection) iter.next();
			conn.disconnect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		// add the child and reconnect its connections
		if (parent.addChild(child)) {
			addConnections(sourceConnections);
			addConnections(targetConnections);
		}
	}
}