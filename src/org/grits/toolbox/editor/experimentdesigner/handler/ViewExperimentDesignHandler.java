 
package org.grits.toolbox.editor.experimentdesigner.handler;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.EditorHandler;
import org.grits.toolbox.core.service.IGritsDataModelService;
import org.grits.toolbox.core.service.IGritsUIService;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.grits.toolbox.entry.sample.property.SampleProperty;

@SuppressWarnings("restriction")
public class ViewExperimentDesignHandler {
	
	private static final Logger logger = Logger.getLogger(ViewExperimentDesignHandler.class);
	
	@Inject private static IGritsDataModelService gritsDataModelService = null;
	@Inject static IGritsUIService gritsUIService = null;
	@Inject ECommandService commandService;
	@Inject EHandlerService handlerService;
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object object,
			IEventBroker eventBroker, @Named (IServiceConstants.ACTIVE_SHELL) Shell shell) {
		Entry entry = null;
		if(object instanceof Entry)
		{
			entry = (Entry) object;
		}
		else if (object instanceof StructuredSelection)
		{
			if(((StructuredSelection) object).getFirstElement() instanceof Entry)
			{
				entry = (Entry) ((StructuredSelection) object).getFirstElement();
			}
		}
		if (entry == null && gritsDataModelService.getLastSelection() != null
				&& gritsDataModelService.getLastSelection().getFirstElement() instanceof Entry)
			// try getting the last selection from the data model
		{
			entry = (Entry) gritsDataModelService.getLastSelection().getFirstElement();
		}
		
		if (entry == null)
			return;
        
        Property prop = entry.getProperty();
        if (prop instanceof SampleProperty) {
        	String experimentFileName = ((SampleProperty) prop).getSampleFile().getName().replace("sample", ExperimentConfig.FILE_NAME_PREFIX);
        	File experimentFolder = NewExperimentHandler.getExperimentDirectory(entry);
        	File file = new File (experimentFolder.getAbsolutePath() 
                    + File.separator + experimentFileName);
            if (!file.exists()) {  // experiment design does not exist
            	ParameterizedCommand myCommand = commandService.createCommand("projectexplorer.command.newexperiment", null);
            	
            	try {
            		handlerService.executeHandler(myCommand);
    			} catch (Exception ex) {
    				ErrorUtils.createErrorMessageBox(shell, "Cannot create the Experiment", ex);
    				logger.error(Activator.PLUGIN_ID + " Cannot create the Experiment", ex);
    			}
            } else {
            	// find the experiment entry and open it in the editor
            	List<Entry> children = entry.getChildren();
            	for (Iterator iterator = children.iterator(); iterator
						.hasNext();) {
					Entry entry2 = (Entry) iterator.next();
					Property prop2 = entry2.getProperty();
					if (prop2.getType().equals(ExperimentProperty.TYPE)) {
						// Switch to the ExperimentDesign perspective
				       // PropertyHandler.changePerspective(PerspectiveFactory.PERSPECTIVE_ID+".<Experiment Design>");
				        EditorHandler.openEditorForEntry(entry2);
				        PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective");
				        PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective.<Experiment Design>");
				       // gritsUIService.openEntryInPart(entry2);
				        break;
					}
				}
            }
        }
	}
	
	@CanExecute
	public boolean canExecute(@Named(IServiceConstants.ACTIVE_SELECTION) Object object) {
		Entry entry = null;
		if(object instanceof Entry)
		{
			entry = (Entry) object;
		}
		else if (object instanceof StructuredSelection)
		{
			if(((StructuredSelection) object).getFirstElement() instanceof Entry)
			{
				entry = (Entry) ((StructuredSelection) object).getFirstElement();
			}
		}
		if (entry == null && gritsDataModelService.getLastSelection() != null
				&& gritsDataModelService.getLastSelection().getFirstElement() instanceof Entry)
			// try getting the last selection from the data model
		{
			entry = (Entry) gritsDataModelService.getLastSelection().getFirstElement();
		}
		
        if (entry != null)
        {
            if (entry.getProperty().getType().equals(SampleProperty.TYPE))
            	return true;
        }
		return false;
	}
		
}