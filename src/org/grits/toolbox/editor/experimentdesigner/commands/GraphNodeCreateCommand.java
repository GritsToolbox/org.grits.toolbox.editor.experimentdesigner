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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.editor.experimentdesigner.dialog.ProtocolCreationDialog;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolFileHandler;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;

/**
 * A command to add a GraphNode to a ExperimentGraph. The command can be undone or
 * redone.
 * 
 * @author Elias Volanakis, @modified by Sena Arpinar
 */
public class GraphNodeCreateCommand extends Command {
	
	/** The new node. */
	private GraphNode newNode;
	/** ExperimentGraph to add to. */
	private final ExperimentGraph parent;
	private final GraphicalViewer graphViewer;
	/** The bounds of the new GraphNode. */
	private Rectangle bounds;

	/**
	 * Create a command that will add a new GraphNode to an ExperimentGraph.
	 * 
	 * @param newNode
	 *            the new GraphNode that is to be added
	 * @param editPart 
	 *            the GraphEditor this node is being added into
	 * @param parent
	 *            the ExperimentGraph that will hold the new element
	 * @param bounds
	 *            the bounds of the new node; the size can be (-1, -1) if not
	 *            known
	 * @throws IllegalArgumentException
	 *             if any parameter is null, or the request does not provide a
	 *             new GraphNode instance
	 */
	public GraphNodeCreateCommand(GraphNode newNode, GraphicalViewer viewer, ExperimentGraph parent,
			Rectangle bounds) {
		this.newNode = newNode;
		this.parent = parent;
		this.bounds = bounds;
		this.graphViewer = viewer;
		setLabel("node creation");
	}

	/**
	 * Can execute if all the necessary information has been provided.
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		return newNode != null && parent != null && bounds != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		newNode.setLocation(bounds.getLocation());
		Dimension size = bounds.getSize();
		if (size.width > 0 && size.height > 0)
			newNode.setSize(size);
		
		Shell shell = Display.getCurrent().getActiveShell();
		ProtocolCreationDialog dialog = new ProtocolCreationDialog(shell, newNode);
		dialog.create();
		dialog.getShell().setSize(500, 500);
		if (dialog.open() == Window.OK) {
			if (newNode instanceof ProtocolNode) {
				ProtocolNode instanceNode = new ProtocolNode();
				ProtocolNode node = dialog.getExisting();
				if (node != null) { // creating from a protocol variant
					// need to get all the values from the existing node
					instanceNode.setLabel(node.getLabel());
					if (dialog.getDescription() != null)
						instanceNode.setDescription(dialog.getDescription());
					else
						instanceNode.setDescription(node.getDescription());
					instanceNode.setCategory(node.getCategory());
					instanceNode.setPapers(node.getPapers());
					instanceNode.setParameters(node.getParameters());
					instanceNode.setParameterGroups(node.getParameterGroups());
					instanceNode.setColor(node.getColor());
					instanceNode.setTemplate(node.getTemplate());
					instanceNode.setTemplateUri(node.getTemplateUri());
					instanceNode.setUri(node.getUri());
					instanceNode.setCreator(node.getCreator());
					instanceNode.setUrl(node.getUrl());
					if (node.getFile() != null) {
						instanceNode.setFile(ProtocolFileHandler.copyFromConfigToWorkspace(node.getFile(), parent.getProjectEntry().getDisplayName()));
					}
				}
				else { // creating from the palette
					instanceNode.setLabel(dialog.getName());
					instanceNode.setCategory(((ProtocolNode) newNode).getCategory());
					instanceNode.setColor(newNode.getColor());
					instanceNode.setUri(newNode.getUri());
					instanceNode.setUrl(((ProtocolNode) newNode).getUrl());
					// need to copy the file into the project folder, if it not there
					if (((ProtocolNode) newNode).getFile() != null) {
						String filename = null;
						if ((filename = ProtocolFileHandler.copyFromConfigToWorkspace(((ProtocolNode) newNode).getFile(), parent.getProjectEntry().getDisplayName())) != null) {
							instanceNode.setFile(filename);
						}
						else {						
							instanceNode.setFile(ProtocolFileHandler.copyFromJarToWorkspace(((ProtocolNode) newNode).getFile(), parent.getProjectEntry().getDisplayName()));
						}
					}
					instanceNode.setDescription(dialog.getDescription());
					instanceNode.setCreator(((ProtocolNode) newNode).getCreator());
					instanceNode.setTemplate(newNode.getLabel());	
					instanceNode.setTemplateUri(((ProtocolNode) newNode).getTemplateUri());
					// get parameters from the palette node
					// make a copy so that updates to the protocol node created, do not modify the palette node
					List<Parameter> params = ((ProtocolNode) newNode).getParameters();
					List<Parameter> parameters = new ArrayList<>();	
					if (params != null) {
						for (Parameter p : params) {
							parameters.add (new Parameter (p));
						}
					}
					instanceNode.setParameters(parameters);
					List<ParameterGroup> paramGroups = ((ProtocolNode) newNode).getParameterGroups();
					List<ParameterGroup> parameterGroups = new ArrayList<>();
					if (paramGroups != null) {
						for (ParameterGroup g : paramGroups) {
							parameterGroups.add (new ParameterGroup (g));
						}
					}
					instanceNode.setParameterGroups(parameterGroups);
					List<Paper> papers = ((ProtocolNode) newNode).getPapers();
					if (papers != null)
						instanceNode.setPapers(new ArrayList<>(papers));
					else
						instanceNode.setPapers(new ArrayList<Paper>());
				}
				
				instanceNode.setLocation(newNode.getLocation());
				instanceNode.setSize(newNode.getSize());
				newNode = instanceNode;
			} 
			redo();
		} 
		
		//redo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		parent.addChild(newNode);
		// automatically select the node added
		EditPart part = (EditPart) graphViewer.getEditPartRegistry().get(newNode);
		graphViewer.select(part);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		parent.removeChild(newNode);
	}

}