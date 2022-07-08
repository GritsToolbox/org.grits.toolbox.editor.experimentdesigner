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
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolFileHandler;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolVariantExistsException;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.views.ProtocolView;

public class SaveProtocolVariantAction {

	private static final Logger logger = Logger.getLogger(SaveProtocolVariantAction.class);
	
	@Inject ProtocolView view;
	
	@Execute
	public void run() {
		logger.debug(Activator.PLUGIN_ID  + " BEGIN SaveProtocolVariantAction");
		openDialog (view.getProtocol());
		logger.debug(Activator.PLUGIN_ID  + " END SaveProtocolVariantAction");
	}
	
	@CanExecute
	public boolean canExecute () {
		return view.getProtocol() != null;
	}
	
	private void openDialog (ProtocolNode protocol) {
		if (protocol != null) {
			Shell shell = Display.getCurrent().getActiveShell();
			String uri = protocol.getTemplateUri();
			if (uri == null) {
				// do not allow variant creation
				logger.info (Activator.PLUGIN_ID + " Could not save it as a protocol variant since there is not template. ");
				MessageDialog.openInformation(shell, "Not Allowed", "Protocol does not follow any protocol templates. Please save it as a template first before saving as a protocol variant");
				return;
			}
			SaveAsDialog dialog = new SaveAsDialog(shell, SaveAsDialog.PROTOCOL_VARIANT, uri);
			dialog.create();
			dialog.getShell().setSize(550, 300);
			if (dialog.open() == Window.OK) {
				try {
					// create a variant of the protocol and save 
					ProtocolNode protocolVariant = new ProtocolNode();
					protocolVariant.setLabel(dialog.getName());
	            	protocolVariant.setCategory(protocol.getCategory());
	            	protocolVariant.setDescription(protocol.getDescription());
	            	protocolVariant.setColor(protocol.getColor());
	            	protocolVariant.setCreator(protocol.getCreator());
	            	protocolVariant.setTemplate(protocol.getTemplate());
	            	protocolVariant.setTemplateUri(protocol.getTemplateUri());
	            	protocolVariant.setUri(protocol.getUri());
	            	protocolVariant.setUrl(protocol.getUrl());
	            	if (protocol.getFile() != null) {
	            		protocolVariant.setFile(ProtocolFileHandler.copyFromWorkspaceToConfig(protocol.getFile(), view.getProjectEntry().getDisplayName()));
	            	}
	            	//protocolVariant.setId(protocol.getId());
	            	protocolVariant.setLocation(protocol.getLocation());
	            	protocolVariant.setSize(protocol.getSize());
	            	protocolVariant.setMyColor(protocol.getMyColor());
	            	protocolVariant.setPapers(protocol.getPapers());
	            	protocolVariant.setParameters(protocol.getParameters());
	            	protocolVariant.setParameterGroups(protocol.getParameterGroups());
				
					new ExperimentDesignOntologyAPI().createProtocolVariant(protocolVariant);
				} catch (ProtocolVariantExistsException e) {
					MessageDialog.openError(shell, "Error", "Protocol with name: " + dialog.getName() + " already exists. Please choose a different name");
					openDialog(protocol);
				} catch (Exception e) {
					logger.error (Activator.PLUGIN_ID + " Could not save it as a protocol variant. ", e);
					MessageDialog.openError(shell, "Error", "Could not save it as a protocol variant. " + e);
    			}
			}
		}
	}
}
