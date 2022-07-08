package org.grits.toolbox.editor.experimentdesigner.actions;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.dialog.SaveAsDialog;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolExistsException;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolFileHandler;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.views.ProtocolView;

public class SaveProtocolTemplateAction {
	
	private static final Logger logger = Logger.getLogger(SaveProtocolTemplateAction.class);
	@Inject ProtocolView view;
	
	@Execute
	public void run() {
		logger.debug(Activator.PLUGIN_ID  + " BEGIN SaveProtocolTemplateAction");
		openDialog (this.view);
		logger.debug(Activator.PLUGIN_ID  + " END SaveProtocolTemplateAction");
	}
	
	@CanExecute
	public boolean canExecute() {
		return view.getProtocol() != null;
	}
	
	private void openDialog (ProtocolView view) {
		ProtocolNode protocol = view.getProtocol();
		if (protocol != null) {
			Shell shell = Display.getCurrent().getActiveShell();
			if (!protocol.isTemplateChanged()) {
				// no changes have been made, ask the user if s/he still wants to save it as a template
				boolean yes = MessageDialog.openQuestion(shell, "Warning", "No changes have been made to the protocol."
						+ "\nDo you still wish to save it as a new template?");
				if (!yes)
					return;
			}
			// find the protocol's top level category
			ExperimentDesignOntologyAPI api;
			try {
				api = new ExperimentDesignOntologyAPI();
				SaveAsDialog dialog = new SaveAsDialog(shell, SaveAsDialog.PROTOCOL_TEMPLATE, protocol.getCategory(), api.getTopLevelCategoryForProtocol(protocol));
				dialog.setDescription(protocol.getDescription());
				dialog.create();
				dialog.getShell().setSize(500, 500);
				if (dialog.open() == Window.OK) {
					try {
		            	// create a copy of the protocol and save 
						ProtocolNode protocolTemplate = new ProtocolNode();
						protocolTemplate.setLabel(dialog.getName().trim());
						protocolTemplate.setCategory(dialog.getCategory());
						protocolTemplate.setDescription(dialog.getDescription().trim());
						protocolTemplate.setColor(protocol.getColor());
						protocolTemplate.setCreator(protocol.getCreator());
						protocolTemplate.setUri(protocol.getUri());
						protocolTemplate.setUrl(protocol.getUrl());
						if (protocol.getFile() != null) {
							protocolTemplate.setFile(ProtocolFileHandler.copyFromWorkspaceToConfig(protocol.getFile(), view.getProjectEntry().getDisplayName()));
						}
						protocolTemplate.setLocation(protocol.getLocation());
						protocolTemplate.setSize(protocol.getSize());
						protocolTemplate.setMyColor(protocol.getMyColor());
						protocolTemplate.setPapers(protocol.getPapers());
						protocolTemplate.setParameters(protocol.getParameters());
						protocolTemplate.setParameterGroups(protocol.getParameterGroups());
					
						//TODO check to see if there are really any changes from the original template
						String templateURI = api.createProtocolTemplate(protocolTemplate, dialog.getPaletteCategory().getUri());
						view.templateAdded(templateURI);
						
						// modify the protocol's template information
						protocol.setTemplate(protocolTemplate.getLabel());
						protocol.setTemplateUri(templateURI);
						protocol.setTemplateChanged(false);
						view.templateChanged(protocol.getTemplate(), templateURI);
					} catch (ProtocolExistsException e) {
						MessageDialog.openError(shell, "Error", "Protocol template with name: " + dialog.getName() + " already exists. Please choose a different name");
						openDialog(view);
					} catch (Exception e) {
						logger.error (Activator.PLUGIN_ID + " Could not save it as a protocol template. ", e);
						MessageDialog.openError(shell, "Error", "Could not save it as a protocol template. " + e);
	    			}
				}
			} catch (Exception e1) {
				logger.error("Cannot access the ontology", e1);
				MessageDialog.openError(shell, "Error", "Cannot access the ontology. " + e1);
			}
		}
	}
}
