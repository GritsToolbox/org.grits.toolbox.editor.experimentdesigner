package org.grits.toolbox.editor.experimentdesigner.actions;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;
import org.grits.toolbox.editor.experimentdesigner.dialog.SaveAsDialog;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolFileHandler;
import org.grits.toolbox.editor.experimentdesigner.io.TemplateExistsException;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;

public class SaveExperimentTemplateAction extends Action implements
		IWorkbenchAction {

	private static final Logger logger = Logger.getLogger(SaveExperimentTemplateAction.class);
	
	ExperimentGraph experimentDesign;
	GraphEditor editor;
	
	public SaveExperimentTemplateAction(GraphEditor editor) {
		this.editor = editor;
		this.experimentDesign = editor.getModel();
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageRegistry.getImageDescriptor(ExperimentDesignerImage.SAVEASTEMPLATE);
	}
	
	@Override
	public String getId() {
		return GraphEditor.SAVE_AS_TEMPLATE;
	}
	
	@Override
	public void run() {
		logger.debug(Activator.PLUGIN_ID + " BEGIN SaveExperimentTemplateAction...Saving the experiment design as a template");
		openDialog(null, null);
		logger.debug(Activator.PLUGIN_ID + " END SaveExperimentTemplateAction.");
	}
	
	private void openDialog (String name, String description) {
		Shell shell = Display.getCurrent().getActiveShell();
		SaveAsDialog dialog = new SaveAsDialog(shell, SaveAsDialog.EXPERIMENT_TEMPLATE);
		dialog.setName(name);
		dialog.setDescription(description);
		dialog.create();
		dialog.getShell().setSize(500, 500);
		if (dialog.open() == Window.OK) {
			if (experimentDesign != null) {
				// create a copy and save that one.
				ExperimentGraph experimentTemplate = new ExperimentGraph();
				experimentTemplate.setName(dialog.getName());
				experimentTemplate.setDescription(dialog.getDescription());
				experimentTemplate.setDateCreated(new Date());
				experimentTemplate.setCreatedBy(dialog.getCreator());
				experimentTemplate.setNodes(experimentDesign.getNodes());
				experimentTemplate.setConnections(experimentDesign.getConnections());
				experimentTemplate.setUri(experimentDesign.getUri());
				try {
					new ExperimentDesignOntologyAPI().createTemplateForExperimentGraph (experimentTemplate);
					// need to copy the files required for the protocols
					List<GraphNode> nodes = experimentTemplate.getNodes();
					for (GraphNode graphNode : nodes) {
						if (graphNode instanceof ProtocolNode) {
							if (((ProtocolNode) graphNode).getFile() != null) {
								((ProtocolNode) graphNode).setFile(ProtocolFileHandler.copyFromWorkspaceToConfig(((ProtocolNode) graphNode).getFile(), experimentDesign.getProjectEntry().getDisplayName()));
							}
						}
					}
					
				} catch (TemplateExistsException e) {
					MessageDialog.openError(shell, "Error", "Template with name: " + dialog.getName() + " already exists. Please choose a different name");
					openDialog(dialog.getName(), dialog.getDescription());
				} catch (Exception e) {
					logger.error (Activator.PLUGIN_ID + " Could not save it as a template. ", e);
					MessageDialog.openError(shell, "Error", "Could not save it as a template. " + e);
				}
			} else {
				logger.error (Activator.PLUGIN_ID + " Could not save it as a template. ");
				MessageDialog.openError(shell, "Error", "Could not save it as a template. ");
			}
		}
	}
	
	@Override
	public void dispose() {
	}

}
