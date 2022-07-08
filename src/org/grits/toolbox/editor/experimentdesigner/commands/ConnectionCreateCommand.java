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

import org.eclipse.gef.commands.Command;
import org.grits.toolbox.editor.experimentdesigner.model.Connection;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.InputNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;


/**
 * A command to create a connection between two shapes. The command can be
 * undone or redone.
 * <p>
 * This command is designed to be used together with a GraphicalNodeEditPolicy.
 * To use this command properly, following steps are necessary:
 * </p>
 * <ol>
 * <li>Create a subclass of GraphicalNodeEditPolicy.</li>
 * <li>Override the <tt>getConnectionCreateCommand(...)</tt> method, to create a
 * new instance of this class and put it into the CreateConnectionRequest.</li>
 * <li>Override the <tt>getConnectionCompleteCommand(...)</tt> method, to obtain
 * the Command from the ConnectionRequest, call setTarget(...) to set the target
 * endpoint of the connection and return this command instance.</li>
 * </ol>
 * 
 */
public class ConnectionCreateCommand extends Command {
	/** The connection instance. */
	private Connection connection;
	/** The desired line style for the connection (dashed or solid). */
	private final int lineStyle;

	/** Start endpoint for the connection. */
	private final GraphNode source;
	/** Target endpoint for the connection. */
	private GraphNode target;

	/**
	 * Instantiate a command that can create a connection between two shapes.
	 * 
	 * @param source
	 *            the source endpoint (a non-null Shape instance)
	 * @param lineStyle
	 *            the desired line style. See Connection#setLineStyle(int) for
	 *            details
	 * @throws IllegalArgumentException
	 *             if source is null
	 * @see Connection#setLineStyle(int)
	 */
	public ConnectionCreateCommand(GraphNode source, int lineStyle) {
		if (source == null) {
			throw new IllegalArgumentException();
		}
		setLabel("connection creation");
		this.source = source;
		this.lineStyle = lineStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		// disallow source -> source connections
		if (source.equals(target)) {
			return false;
		}
		
		// do not allow input nodes to be a target
		if (target instanceof InputNode)
			return false;
		// do not allow output nodes to be a source
		if (source instanceof OutputNode)
			return false;
		// return false, if the source -> target connection exists already
		for (Iterator iter = source.getSourceConnections().iterator(); iter
				.hasNext();) {
			Connection conn = (Connection) iter.next();
			if (conn.getTarget().equals(target)) {
				return false;
			}
		}
		
		// if adding this connection will create a cycle, disallow
		// check if there is an indirect connection from target->source already
		if (isConnected (target, source)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param t targetNode
	 * @param s sourceNode
	 * @return true if there is a path between target -> source 
	 */
	boolean isConnected (GraphNode t, GraphNode s) {
		boolean connected = false;
		if (t==s) {
			System.out.println ("target and source are the same!");
		}
		if (t != null && t.getSourceConnections() != null) {
			for (Iterator iter = t.getSourceConnections().iterator(); iter
					.hasNext();) {
				Connection conn = (Connection) iter.next();
				if (conn.getTarget().equals(s)) {
					return true;
				}
				else if (conn.getTarget() != null) {
					connected = isConnected (conn.getTarget(), s);
					if (connected)
						break;
				}
			}
		}
		
		return connected;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		// create a new connection between source and target
		connection = new Connection(source, target);
		// use the supplied line style
		connection.setLineStyle(lineStyle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		connection.reconnect();
	}

	/**
	 * Set the target endpoint for the connection.
	 * 
	 * @param target
	 *            that target endpoint (a non-null Node instance)
	 * @throws IllegalArgumentException
	 *             if target is null
	 */
	public void setTarget(GraphNode target) {
		if (target == null) {
			throw new IllegalArgumentException();
		}
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		connection.disconnect();
	}
}
