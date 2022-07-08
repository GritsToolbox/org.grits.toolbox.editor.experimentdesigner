package org.grits.toolbox.editor.experimentdesigner.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.part.EventPart;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.views.ParameterView;

public class DeleteParameterAction {
	private static Logger logger = Logger.getLogger(DeleteParameterAction.class);
	
	//Parameter toBeDeleted;
	//ParameterGroup groupToBeDeleted;
	boolean onlyOne;
	
	@Inject IEventBroker eventBroker;
	GraphEditor editor;
	
	@Execute
	public void run(@Named (IServiceConstants.ACTIVE_SHELL) Shell shell, MApplication application, MPart protocolPart, 
			@Optional @Named (IServiceConstants.ACTIVE_SELECTION) Parameter toBeDeleted, 
			@Optional @Named (IServiceConstants.ACTIVE_SELECTION) ParameterGroup groupToBeDeleted) {
		if(protocolPart != null && protocolPart.getObject() != null)
		{
			ProtocolNode protocol = ((ParameterView) protocolPart.getObject()).getProtocol();
			if (protocol == null) 
				return;
			
			editor = application.getContext().get(GraphEditor.class);
			if (editor == null) {
				logger.error("Cannot get the reference to Graph Editor");
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Update Error", "Cannot get the reference to Graph Editor");
				return;
			}
			
			TreeViewer viewer = ((ParameterView) protocolPart.getObject()).getTreeViewer();
			// check to see if deleting this parameter breaks the template
			// if so, give a warning and remove template information
			try {
				if (protocol.getTemplateUri() != null) {
					ProtocolNode protocolTemplate = new ExperimentDesignOntologyAPI().getProtocolByUri(protocol.getTemplateUri());
					if (protocolTemplate != null) {
						if (toBeDeleted != null) {
							List<Parameter> parameters = protocolTemplate.getParameters();
							if (parameters.contains(toBeDeleted)) {
								// it belongs to the original template
								boolean confirm = MessageDialog.openConfirm(shell, "Information", "This parameter belongs to the original template.");
								if (confirm) {
									protocol.setTemplate(null);
									protocol.setTemplateUri(null);
									protocol.setTemplateChanged(true);
									eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
									eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, "");  // template removed
									editor.refreshProtocolNode(protocol);
									//sourceProvider2.templateRemoved();
									//sourceProvider.detailsChanged();
								} else 
									toBeDeleted = null;
							}
						}
						else if (groupToBeDeleted != null) {
							// check if it is the only one first
							List<ParameterGroup> groups = protocol.getParameterGroups();
							onlyOne = true;
							int count = 0;
							for (Iterator<ParameterGroup> iterator = groups.iterator(); iterator
									.hasNext();) {
								ParameterGroup parameterGroup = (ParameterGroup) iterator
										.next();
								if (parameterGroup.getLabel().equals(groupToBeDeleted.getLabel())) {
									count++;
									if (count > 1) {
										// there is at least one more
										// no need to check the template, safely remove and keep the template information
										onlyOne = false;
										break;
									}
									
								}
							}
							if (onlyOne) {
								List<ParameterGroup> templateGroups = protocolTemplate.getParameterGroups();
								if (templateGroups.contains(groupToBeDeleted)) {
									// it belongs to the original template
									boolean confirm = MessageDialog.openConfirm(shell, "Information", "This parameter group belongs to the original template.");
									if (confirm) {
										protocol.setTemplate(null);
										protocol.setTemplateUri(null);
										protocol.setTemplateChanged(true);
										eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
										eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, "");  // template removed
										editor.refreshProtocolNode(protocol);
										//sourceProvider2.templateRemoved();
										//sourceProvider.detailsChanged();
									} else {
										groupToBeDeleted = null;
									}
								}
							}
							else {
								// it is not onlyOne but need to check if it is the required one
								if (groupToBeDeleted.getRequired()) {
									boolean confirm = MessageDialog.openConfirm(shell, "Information", "This parameter group is required in the original template.");
									if (confirm) {
										protocol.setTemplate(null);
										protocol.setTemplateUri(null);
										protocol.setTemplateChanged(true);
										eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
										eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, "");  // template removed
										editor.refreshProtocolNode(protocol);
									//	sourceProvider2.templateRemoved();
									//	sourceProvider.detailsChanged();
									} else {
										groupToBeDeleted = null;
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error(Activator.PLUGIN_ID + " Error getting the template information from the ontology: ", e);
				MessageDialog.openError(shell, "Cannot retrieve template", "Error getting the template information from the ontology: " + e.getMessage());
				return;
			}
			
			try {
				// delete the parameter or parameter group from the protocol
				// and refresh the treeViewer
				if (toBeDeleted != null)  {	
					boolean deleted = protocol.getParameters().remove(toBeDeleted);
					if (deleted) {
						List<Object> paramsAndParamGroups = new ArrayList<>();
						paramsAndParamGroups.addAll(protocol.getParameters());
						if (protocol.getParameterGroups() != null) 
							paramsAndParamGroups.addAll(protocol.getParameterGroups());
						viewer.setInput(paramsAndParamGroups);
						viewer.refresh();
						viewer.expandAll();
						eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
						editor.refreshProtocolNode(protocol);
						toBeDeleted = null;
						//sourceProvider.detailsChanged();
					}
				} else if (groupToBeDeleted != null) {
					// if it is not the only one of the same group, we need to re-arrange the ids of the remaining ones
					List<ParameterGroup> parameterGroups = protocol.getParameterGroups();
					if (!onlyOne) {
						for (ParameterGroup group : parameterGroups) {
							if (group != groupToBeDeleted && group.getLabel().equals(groupToBeDeleted.getLabel())) {
								if (group.getId() > 1) {
									// decrement the id
									group.setId(group.getId()-1);
									// modify the groupids of the parameters as well
									List<Parameter> params = group.getParameters();
									for (Parameter parameter : params) {
										parameter.setGroupId(group.getId());
									}
								}
							}
						}
					}
					boolean deleted = parameterGroups.remove(groupToBeDeleted);
					if (deleted) {
						List<Object> paramsAndParamGroups = new ArrayList<>();
						if (protocol.getParameters() != null)
							paramsAndParamGroups.addAll(protocol.getParameters());
						paramsAndParamGroups.addAll(protocol.getParameterGroups());
						viewer.setInput(paramsAndParamGroups);
						viewer.refresh();
						viewer.expandAll();
						eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
						editor.refreshProtocolNode(protocol);
						//sourceProvider.detailsChanged();
						groupToBeDeleted = null;
					}
				}
			} catch (Exception e) {
				logger.error(Activator.PLUGIN_ID + " Error deleting the parameter ", e);
				MessageDialog.openError(shell, "Error", "Error deleting the parameter " + e.getMessage());
			}
		}
	}
	
	@CanExecute
	public boolean canExecute(@Optional @Named (IServiceConstants.ACTIVE_SELECTION) Parameter toBeDeleted, 
			@Optional @Named (IServiceConstants.ACTIVE_SELECTION) ParameterGroup groupToBeDeleted) {
		return toBeDeleted != null || groupToBeDeleted != null;
	}	
	
}
