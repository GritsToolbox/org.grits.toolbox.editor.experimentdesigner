package org.grits.toolbox.editor.experimentdesigner.actions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.grits.toolbox.core.part.EventPart;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.actions.AddParameterGroupAction.ArrayLabelProvider;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.EntityWithPosition;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.views.ParameterView;

public class AddParameterAction {
	private static Logger logger = Logger.getLogger(AddParameterAction.class);
	
	Parameter parameter;
	boolean isRequired = false;
	
	@Inject IEventBroker eventBroker;
	GraphEditor editor;
	
	@Execute
	public void run(@Named (IServiceConstants.ACTIVE_SHELL) Shell shell, MApplication application, MPart protocolPart) {
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
			
			TreeViewer treeViewer = ((ParameterView) protocolPart.getObject()).getTreeViewer();
			try {
				ExperimentDesignOntologyAPI ontologyAPI = new ExperimentDesignOntologyAPI();
				List<Parameter> parameters = ontologyAPI.getAllParameters();
				// open up a dialog with the list of parameters
				ListDialog dialog = new ListDialog(shell) {
					
					@Override
					protected Control createDialogArea(Composite container) {
						Composite parent =  (Composite)super.createDialogArea(container);
						Composite newContainer = new Composite(parent, SWT.NONE);
						newContainer.setLayout(new GridLayout(2, false));
						GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
						Label label = new Label(newContainer, SWT.NONE);      
						label.setText("Required?");
						label.setLayoutData(gd);
						gd = new GridData(SWT.FILL, SWT.NONE, true, false);
						final Button required = new Button(newContainer, SWT.CHECK);
						required.addSelectionListener(new SelectionListener() {
							
							@Override
							public void widgetSelected(SelectionEvent e) {
								isRequired = required.getSelection();		
							}
							
							@Override
							public void widgetDefaultSelected(SelectionEvent e) {
							}
						});
						required.setText("");
						required.setSelection(false);
						required.setLayoutData(gd);
						return parent;
					}
				};
				dialog.setContentProvider(new ArrayContentProvider());
				dialog.setTitle("Parameter Selection");
				dialog.setLabelProvider(new ArrayLabelProvider() {
					@Override
					public String getText(Object element) {
						return ((Parameter)element).getName();
					}
					
					@Override
					public String getColumnText(Object element, int columnIndex) {
						return ((Parameter)element).getName();
					}
					
				});
				
				dialog.setInput(parameters);
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					for (int i = 0; i < result.length; i++) {   //NOTE: single selection is allowed for the dialog
						parameter = (Parameter) result[i];
						parameter.setRequired(isRequired);
						List<Object> paramsAndParamGroups = new ArrayList<>();
						
						List<Parameter> existingParameters = protocol.getParameters();
						if (existingParameters != null) {
							if (existingParameters.contains(parameter)) {   // same parameter exists, do not allow adding it again
								return;
							}
							else {
								int position = findPosition (protocol);
								parameter.setPosition(position);
								existingParameters.add(parameter);
								paramsAndParamGroups.addAll(existingParameters);
							}
						} else {  // first parameter
							List<Parameter> newParameters = new ArrayList<>();
							int position = findPosition (protocol);
							parameter.setPosition(position);
							newParameters.add(parameter);
							protocol.setParameters(newParameters);
							paramsAndParamGroups.addAll(newParameters);
						}
						if (protocol.getParameterGroups() != null)
							paramsAndParamGroups.addAll(protocol.getParameterGroups());
						treeViewer.setInput(paramsAndParamGroups);
						treeViewer.refresh();
						treeViewer.expandAll();
						
						// mark the protocol as dirty (differ from its original template)
						protocol.setTemplateChanged(true);
						// notify the editor that model has been changed
						eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
						editor.refreshProtocolNode(protocol);
						//sourceProvider.detailsChanged();
					}
				}
				isRequired = false;
				
			} catch (Exception e) {
				logger.error(Activator.PLUGIN_ID + " Error getting available parameters from the ontology", e); 
	            MessageDialog.openError(Display.getCurrent().getActiveShell(), 
	                    "Error", "Error getting available parameters from the ontology");
			}
		}
	}

	private int findPosition(ProtocolNode protocol) {
		int maxPosition = 0;
		List<EntityWithPosition> allEntries = new ArrayList<>();
		allEntries.addAll(protocol.getParameterGroups());
		allEntries.addAll(protocol.getParameters());
		
		for (EntityWithPosition entityWithPosition : allEntries) {
			if (entityWithPosition.getPosition() != null && entityWithPosition.getPosition() > maxPosition)
				maxPosition = entityWithPosition.getPosition();
		}
		
		return maxPosition+1;
	}
}
