package org.grits.toolbox.editor.experimentdesigner.actions;

import org.apache.log4j.Logger;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.actions.ActionFactory;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.commands.GraphNodeCreateCommand;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;


public class SelectProtocolAction extends Action{
	
	private static final Logger logger = Logger.getLogger(SelectProtocolAction.class);
	
	GraphEditor graphEditor;
	
	public SelectProtocolAction(GraphEditor graphEditor) {
		this.graphEditor = graphEditor;
		this.setToolTipText("Select from existing protocols");
		this.setText("Create Protocol");
	}
	
	@Override
	public String getId() {
		return ActionFactory.COPY.getId();
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageRegistry.getSmallIcon("255-255-255.gif");
	}

	public void run() {
    	logger.debug(Activator.PLUGIN_ID + " BEGIN SelectProtocolAction...Creating a node by selecting existing protocols");
		GraphNodeCreateCommand createCommand = new GraphNodeCreateCommand(new ProtocolNode(), graphEditor.getViewer(),
				(ExperimentGraph) graphEditor.getModel(),
				new Rectangle(new Point(50, 100), new Dimension(120, 50)));
		graphEditor.getEditDomain().getCommandStack().execute(createCommand);
		logger.debug(Activator.PLUGIN_ID + " END SelectProtocolAction.");
	}
}
