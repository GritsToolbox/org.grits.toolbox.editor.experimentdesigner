package org.grits.toolbox.editor.experimentdesigner.actions;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.EntityWithPosition;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.views.ParameterView;

public class AddParameterGroupAction {
	
	private static Logger logger = Logger.getLogger(AddParameterGroupAction.class);
	
	ParameterGroup group;
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
				List<ParameterGroup> groups = ontologyAPI.getParameterGroups();
				// open up a dialog with the list of groups
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
						final Button required = new Button(newContainer, SWT.CHECK);
						gd = new GridData(SWT.FILL, SWT.NONE, true, false);
						required.setText("");
						required.setSelection(false);
						required.setLayoutData(gd);
						required.addSelectionListener(new SelectionListener() {
							
							@Override
							public void widgetSelected(SelectionEvent e) {
								isRequired = required.getSelection();
							}
							
							@Override
							public void widgetDefaultSelected(SelectionEvent e) {
							}
						});
						return parent;
					}
				};
				dialog.setContentProvider(new ArrayContentProvider());
				dialog.setTitle("Parameter Group Selection");
				dialog.setLabelProvider(new ArrayLabelProvider());
				
				dialog.setInput(groups);
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					for (int i = 0; i < result.length; i++) {   //NOTE: only single selection is allowed for the dialog
						group = (ParameterGroup) result[i];
						
						List<Object> paramsAndParamGroups = new ArrayList<>();
						
						List<ParameterGroup> existingGroups = protocol.getParameterGroups();
						if (existingGroups != null) {
							int nextGroupId = 0;  // find the biggest group id
							for (Iterator<ParameterGroup> iterator = existingGroups.iterator(); iterator
									.hasNext();) {
								ParameterGroup parameterGroup = (ParameterGroup) iterator
										.next();
								nextGroupId = Math.max(nextGroupId, parameterGroup.getId());
							}
							nextGroupId ++;
							
							ParameterGroup newCopy = new ParameterGroup();
							newCopy.setLabel(group.getLabel());
							newCopy.setUri(group.getUri());
							newCopy.setDescription(group.getDescription());
							newCopy.setRequired(isRequired);
							newCopy.setId(nextGroupId);
							newCopy.setPosition(findPosition(protocol));
							List<Parameter> parameters = group.getParameters();
							if (parameters != null) {  // should never be null
								List<Parameter> clonedList = new ArrayList<Parameter>(parameters.size());
							    for (Parameter p : parameters) {
							        clonedList.add(new Parameter(p, newCopy.getId()));
							    }
							    newCopy.setParameters(clonedList);
							}
							existingGroups.add(newCopy);
							paramsAndParamGroups.addAll(existingGroups);
						} else {
							List<ParameterGroup> newGroups = new ArrayList<>();
							group.setId(1);
							group.setRequired(isRequired);
							group.setPosition(findPosition(protocol));
							newGroups.add(group);
							protocol.setParameterGroups(newGroups);
							paramsAndParamGroups.addAll(newGroups);
						}
						if (protocol.getParameters() != null)
							paramsAndParamGroups.addAll(protocol.getParameters());
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
				logger.error(Activator.PLUGIN_ID + " Error Reading Ontology", e); 
	            MessageDialog.openError(Display.getCurrent().getActiveShell(), 
	                    "Error", "Error Reading Ontology");
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
	
	static class ArrayLabelProvider extends LabelProvider implements ITableLabelProvider{
		public String getText(Object element) {
			return ((ParameterGroup)element).getLabel();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return ((ParameterGroup)element).getLabel();
		}
		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			super.dispose();
		}
		
	}

}
