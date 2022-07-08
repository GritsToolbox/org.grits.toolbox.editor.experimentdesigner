 
package org.grits.toolbox.editor.experimentdesigner.handler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.dataShare.IGritsConstants;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.io.ProjectFileHandler;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.editor.EditorHandler;
import org.grits.toolbox.core.service.IGritsDataModelService;
import org.grits.toolbox.core.service.IGritsUIService;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.dialog.NewExperimentDialog;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolFileHandler;
import org.grits.toolbox.editor.experimentdesigner.model.Connection;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.InputNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentTemplateEntry;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.grits.toolbox.entry.sample.model.Component;
import org.grits.toolbox.entry.sample.model.Sample;
import org.grits.toolbox.entry.sample.property.SampleProperty;

public class NewExperimentHandler {
	
	@Inject @Named (IGritsConstants.WORKSPACE_LOCATION) private static String workspaceLocation;
	
	@Inject private static IGritsDataModelService gritsDataModelService = null;
	@Inject static IGritsUIService gritsUIService = null;
	
	private static final Logger logger = Logger.getLogger(NewExperimentHandler.class);
	public static final int COPY = 2;  // status code from NewExperimentDialog for "copy from" button click (0=OK, 1=CANCEL)
	public static final int OVERRIDE = 3;  // status code from NewExperimentDialog for overriding the existing entry (creating from template or new design)
	public static final int COPY_OVERRIDE = 4; // status code for overriding the entry by copying
	public static final int TEMPLATE = 5;
	public static final int TEMPLATE_OVERRIDE = 6;
	
	boolean overwrite = false; // if true, remove the existing experiment design file when creating the new experiment design

	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object object,
			IEventBroker eventBroker, @Named (IServiceConstants.ACTIVE_SHELL) Shell shell) {
		
		Entry experimentEntry = null;
		Entry selectedEntry = null;
		if(object instanceof Entry)
		{
			selectedEntry = (Entry) object;
		}
		else if (object instanceof StructuredSelection)
		{
			if(((StructuredSelection) object).getFirstElement() instanceof Entry)
			{
				selectedEntry = (Entry) ((StructuredSelection) object).getFirstElement();
			}
		}

		// try getting the last selection from the data model
		if(selectedEntry == null
				&& gritsDataModelService.getLastSelection() != null
				&& gritsDataModelService.getLastSelection().getFirstElement() instanceof Entry)
		{
			selectedEntry = (Entry) gritsDataModelService.getLastSelection().getFirstElement();
		}

		if(selectedEntry != null)
		{
			if(selectedEntry.getProperty() == null 
					|| !SampleProperty.TYPE.equals(selectedEntry.getProperty().getType()))
			{
				selectedEntry = null;
			}
		}
		
		experimentEntry = createNewExperimentDialog(shell, selectedEntry);
        
		if(experimentEntry != null)
		{
			eventBroker.post(IGritsDataModelService.EVENT_SELECT_ENTRY, experimentEntry);
			//EditorHandler.openEditorForEntry(experimentEntry);
	        //PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective");
	        //PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective.<Experiment Design>");
		}
	}
	
	private Entry createNewExperimentDialog(Shell shell, Entry sample) {
    	
    	NewExperimentDialog dialog = new NewExperimentDialog(PropertyHandler.getModalDialog(shell));
    	dialog.setBlockOnOpen(true);
	    //set parent entry
	    dialog.setSampleEntry(sample);
        int status = dialog.open();
        Entry toBeRemoved = dialog.getToBeRemoved();
        removeExistingDesign (shell, toBeRemoved);
        
        if (status == Window.OK || status == NewExperimentHandler.OVERRIDE) {
        	Entry selectedSampleEntry = dialog.getSampleEntry();
            try
            {
            	// check if the experiment folder is empty else create one
                String experimentFileName = NewExperimentHandler.createExperimentFileInSample(selectedSampleEntry, selectedSampleEntry.getDisplayName());
                
                boolean experimentExists = false;
                Entry experimentEntry = null;
                // check if an experiment design already exists for this sample
                if (status == Window.OK) {
                	for (Entry child: selectedSampleEntry.getChildren()) {
                		if (child.getProperty().getType() == ExperimentProperty.TYPE) {
                			// there is already an entry for this sample
                			// check to make sure it is the same design
                			if (((ExperimentProperty)child.getProperty()).getExperimentFile().getName().equals(experimentFileName)) {
                				experimentEntry = child;
                				experimentExists = true;
                				break;
                			}
                		}
                	}
                }
                if (!experimentExists) {
	                // create an entry
	                experimentEntry = NewExperimentHandler.createEntry(selectedSampleEntry.getDisplayName(), experimentFileName);
	                experimentEntry.setParent(selectedSampleEntry);
	                
	                gritsDataModelService.addEntry(selectedSampleEntry, experimentEntry);
					try
					{
						ProjectFileHandler.saveProject(selectedSampleEntry.getParent());
					} catch (IOException e)
					{
						logger.error("Something went wrong while saving analyte entry \n" + e.getMessage(),e);
						throw e;
					}
                }
                if (status == NewExperimentHandler.OVERRIDE) {
                	EditorHandler.openEditorForEntry(experimentEntry, true);
                	//gritsUIService.closePartForEntry(experimentEntry);
                }
                else {
                	EditorHandler.openEditorForEntry(experimentEntry);
                	//gritsUIService.openEntryInPart(experimentEntry);	
                }
                // Switch to the ExperimentDesign perspective defined in fragment.e4xmi 
				PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective");
		        PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective.<Experiment Design>"); // compatibility layer issues require to use both versions
                return experimentEntry;
                
            } catch (IOException e)
            {
                logger.error(Activator.PLUGIN_ID + " " + e.getMessage() ,e);
                ErrorUtils.createErrorMessageBox(shell, e.getMessage(),e);
            }
            
        } else if (status == NewExperimentHandler.COPY || status == NewExperimentHandler.COPY_OVERRIDE) {
        	Entry experimentEntry = dialog.getSelectedExperiment();
        	if (experimentEntry == null) {
        		throw new RuntimeException ("An existing experiment design is not selected");
        	}
        	sample = dialog.getSampleEntry();
        	// Copy the file and create entry with the new name
        	// get the experiment folder
            File experimentFolder = NewExperimentHandler.getExperimentDirectory(sample);
            // create a unique file name inside the folder
            String experimentFileNameCopy = ((SampleProperty)sample.getProperty()).getSampleFile().getName().replace("sample", ExperimentConfig.FILE_NAME_PREFIX);
            
            File originalExperimentFolder = NewExperimentHandler.getExperimentDirectory(experimentEntry.getParent());
            File originalFile = new File (originalExperimentFolder.getAbsolutePath() 
                    + File.separator + ((ExperimentProperty)experimentEntry.getProperty()).getExperimentFile().getName());
            
            FileOutputStream newFile;
			try {
				newFile = new FileOutputStream(experimentFolder.getAbsolutePath() 
				        + File.separator + experimentFileNameCopy);
				Files.copy(originalFile.toPath(), newFile);
				newFile.close();
				
				replaceInputOutputNodes (experimentFolder.getAbsolutePath() 
				        + File.separator + experimentFileNameCopy, sample, experimentEntry.getParent());
				
				Entry newExperimentEntry = NewExperimentHandler.createEntry(sample.getDisplayName(), experimentFileNameCopy);
	            newExperimentEntry.setParent(sample);
	            
	            gritsDataModelService.addEntry(sample, newExperimentEntry);
				try
				{
					ProjectFileHandler.saveProject(sample.getParent());
				} catch (IOException e)
				{
					logger.error("Something went wrong while saving analyte entry \n" + e.getMessage(),e);
					throw e;
				}
	           // gritsUIService.closePartForEntry(newExperimentEntry);
	           // gritsUIService.openEntryInPart(newExperimentEntry);
	            EditorHandler.openEditorForEntry(newExperimentEntry, true);
	         // Switch to the ExperimentDesign perspective defined in fragment.e4xmi 
				PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective");
		        PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective.<Experiment Design>");
		        
	            return newExperimentEntry;
			} catch (FileNotFoundException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design",e);
			} catch (IOException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design",e);
			} catch (JAXBException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design",e);
			}	
        }
        else if (status == NewExperimentHandler.TEMPLATE || status == NewExperimentHandler.TEMPLATE_OVERRIDE) {
        	ExperimentTemplateEntry templateFile = dialog.getTemplateFile();
        	sample = dialog.getSampleEntry();
        	// Copy the file and create entry with the new name
        	// get the experiment folder
            File experimentFolder = NewExperimentHandler.getExperimentDirectory(sample);
            // create a unique file name inside the folder
            String experimentFileNameCopy = ((SampleProperty)sample.getProperty()).getSampleFile().getName().replace("sample", ExperimentConfig.FILE_NAME_PREFIX);
            File originalFile=null;
            if (templateFile.isFromJar()) {
            	// get it from jar
            	URL url = ExperimentConfig.EXPERIMET_TEMPLATE_RESOURCE_URL;
        		if (url == null) {
        			logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!" + " no template directory found in jar");
    				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template. Reason: No Template directory found in jar");
        		}
        		try {
        			URL resourceFileUrl = FileLocator.toFileURL(url);
        			originalFile = new File(resourceFileUrl.toURI().getRawPath() + File.separator + templateFile.getFilename());
        		} catch (IOException | URISyntaxException e) {
        			logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!", e);
    				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template",e);
        		}
            }
            else {
            	originalFile = new File (ExperimentDesignOntologyAPI.getTemplateFolderLocation()
                        + File.separator + templateFile.getFilename());
            }
            FileOutputStream newFile;
			try {
				newFile = new FileOutputStream(experimentFolder.getAbsolutePath() 
				        + File.separator + experimentFileNameCopy);
				Files.copy(originalFile.toPath(), newFile);
				newFile.close();
				
				replaceInputOutputNodes (experimentFolder.getAbsolutePath() 
				        + File.separator + experimentFileNameCopy, sample, null);
				
				Entry newExperimentEntry = NewExperimentHandler.createEntry(sample.getDisplayName(), experimentFileNameCopy);
	            newExperimentEntry.setParent(sample);
	            
	            
	            // saveEntry adds this entry to the project tree and saves the .project file
	            gritsDataModelService.addEntry(sample, newExperimentEntry);
				try
				{
					ProjectFileHandler.saveProject(sample.getParent());
				} catch (IOException e)
				{
					logger.error("Something went wrong while saving analyte entry \n" + e.getMessage(),e);
					throw e;
				}
	          //  gritsUIService.closePartForEntry(newExperimentEntry);
	          //  gritsUIService.openEntryInPart(newExperimentEntry);
				EditorHandler.openEditorForEntry(newExperimentEntry, true);
				// Switch to the ExperimentDesign perspective defined in fragment.e4xmi 
				PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective");
		        PropertyHandler.changePerspective("org.grits.toolbox.editor.experimentdesigner.designPerspective.<Experiment Design>");
	            return newExperimentEntry;
			} catch (FileNotFoundException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template",e);
			} catch (IOException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template",e);
			} catch (JAXBException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template",e);
			}	
        }
        
        
        
        return null;
    }
	
	private void removeExistingDesign (Shell shell, Entry toBeRemoved) {
		try {
	        if (toBeRemoved != null) {
	        	gritsDataModelService.deleteEntry(toBeRemoved);
	        }
        } catch (IOException e) {
			logger.error ("Could not replace the existing experiment design", e);
			ErrorUtils.createErrorMessageBox(shell, "Could not replace the existing experiment design", e);
		}
	}

    @SuppressWarnings("rawtypes")
	private static void replaceInputOutputNodes(String experimentFile, Entry sample, Entry original) throws JAXBException, IOException {
		// load the model from the experimentFile and remove existing input and output nodes
    	FileInputStream inputStream;
    	int locationY = 210; // default for output nodes
		inputStream = new FileInputStream(experimentFile);
		InputStreamReader reader = new InputStreamReader(inputStream, PropertyHandler.GRITS_CHARACTER_ENCODING);
		JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ExperimentGraph graph = (ExperimentGraph) unmarshaller.unmarshal(reader);
        graph.setConnectionsToNodes();
        graph.setName(sample.getDisplayName());   // need to replace the name with the current sample's name
        graph.setProjectEntry(sample.getParent());
        reader.close();
        inputStream.close();
        List children = graph.getChildren();
        List<GraphNode> nodesToRemove = new ArrayList<>();
        for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			GraphNode graphNode = (GraphNode) iterator.next();
			locationY = Math.max(graphNode.getLocation().y, locationY);
			if (graphNode instanceof InputNode || graphNode instanceof OutputNode) {
				nodesToRemove.add(graphNode);
			}
			// handle file copying
			if (graphNode instanceof ProtocolNode) {
				if (((ProtocolNode) graphNode).getFile() != null) {
					if (original == null) {
						if (ProtocolFileHandler.copyFromConfigToWorkspace(((ProtocolNode) graphNode).getFile(), sample.getParent().getDisplayName()) == null) {
							logger.warn("Could not copy the associated file for the protocol: " + ((ProtocolNode) graphNode).getFile());
						}
					} else {
						if (!sample.getParent().getDisplayName().equals (original.getParent().getDisplayName()))
							if (ProtocolFileHandler.copyFromProjectToAnother(((ProtocolNode) graphNode).getFile(), sample.getParent().getDisplayName(), original.getParent().getDisplayName()) == null)
								logger.warn("Could not copy the associated file for the protocol: " + ((ProtocolNode) graphNode).getFile());
					}
				}
			}
		}
        
        for (Iterator iterator = nodesToRemove.iterator(); iterator.hasNext();) {
			GraphNode node = (GraphNode) iterator.next();
			List<Connection> connections = null;
			if (node instanceof InputNode)
				connections = node.getSourceConnections();
			else if (node instanceof OutputNode) {
				connections = node.getTargetConnections();
			}
			// remove the node and disconnect its connections
			boolean wasRemoved = graph.removeChild(node);
			if (wasRemoved && connections != null) {
				for (Iterator iter = connections.iterator(); iter.hasNext();) {
					Connection conn = (Connection) iter.next();
					conn.disconnect();
				}
			}
        }
        
        addInputOutputNodes (sample, graph, locationY);
        
        // write it back
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        context = JAXBContext.newInstance(ExperimentGraph.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, PropertyHandler.GRITS_CHARACTER_ENCODING);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(graph, os);

        //write the serialized data to the folder
        FileWriter fileWriter = new FileWriter(experimentFile);
        fileWriter.write(os.toString((String) marshaller.getProperty(Marshaller.JAXB_ENCODING)));
        fileWriter.close();
        os.close();
 	}

	public static String createExperimentFileInSample(Entry selectedSampleEntry, String experimentName) throws IOException
    {
        // get the experiment folder
        File experimentFolder = NewExperimentHandler.getExperimentDirectory(selectedSampleEntry);
        // create a unique file name inside the folder
        String experimentFileName = ((SampleProperty)selectedSampleEntry.getProperty()).getSampleFile().getName().replace("sample", ExperimentConfig.FILE_NAME_PREFIX);
        // check if the file already exists, then we cannot create a new one but need to load the existing one.
        File file = new File (experimentFolder.getAbsolutePath() 
                + File.separator + experimentFileName);
        if (!file.exists()) {
        	NewExperimentHandler.createFileContent(selectedSampleEntry, experimentFolder, experimentFileName, experimentName);
        }
        return experimentFileName;
    }
	
	@SuppressWarnings("rawtypes")
	private static void addInputOutputNodes (Entry sampleEntry, ExperimentGraph experimentDesign, int outputLocation) throws IOException{
		Point originalLocation = new Point (50, 10);
        // get the components from the sample and create InputNode for each
        try {
			Sample sample = SampleProperty.loadAnalyte(sampleEntry);
			List<Component> components = sample.getComponents();
			for (Iterator iterator = components.iterator(); iterator.hasNext();) {
				Component component = (Component) iterator.next();
				InputNode newNode = new InputNode();
				newNode.setLabel(component.getLabel());
				newNode.setColor(ColorConstants.red);
				newNode.setUri(component.getUri());
				newNode.setComponentId(component.getComponentId());
				newNode.setDescription(component.getDescription());
				originalLocation = originalLocation.translate(100, 0);
				newNode.setLocation(originalLocation);
				experimentDesign.addChild(newNode);
			}
			
			originalLocation = new Point (50, outputLocation + 100);
			// get MS Experiments and create OutputNode for each
			List<Entry> children = sampleEntry.getChildren();
			for (Iterator iterator = children.iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				Property prop = entry.getProperty();
				if (prop.getType() != ExperimentProperty.TYPE) {  // get all the children other than ourselves
					OutputNode newNode = new OutputNode();
					newNode.setLabel(entry.getDisplayName());
					originalLocation = originalLocation.translate(100, 0);
					newNode.setLocation(originalLocation);
					newNode.setColor(ColorConstants.lightGreen);
					experimentDesign.addChild(newNode);
				}
			}
		} catch (JAXBException e1) {
			throw new IOException("Error loading components/experiments from " + sampleEntry, e1);
		}
	}

	private static void createFileContent(Entry selectedSampleEntry, File experimentFolder, String experimentFileName, String experimentName) throws IOException
    {
        //create and fill an experimentDesign object from the descriptor dialog which should have these values
        ExperimentGraph experimentDesign = new ExperimentGraph();
        experimentDesign.setName(experimentName);
       // experimentDesign.setDescription(description);
        experimentDesign.setDateCreated(new Date());

        // serialize the experiment object to xml
        try
        {
        	addInputOutputNodes (selectedSampleEntry, experimentDesign, 210);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, PropertyHandler.GRITS_CHARACTER_ENCODING);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(experimentDesign, os);

            //write the serialized data to the folder
            FileWriter fileWriter = new FileWriter(experimentFolder.getAbsolutePath() 
                    + File.separator + experimentFileName);
            fileWriter.write(os.toString((String)marshaller.getProperty(Marshaller.JAXB_ENCODING)));
            fileWriter.close();
            os.close();
        } catch (JAXBException e) 
        {
        	throw new IOException("Error creating new file", e);
        } catch (IOException e)
        {
            throw new IOException("Error creating new file");
        } 

    }

    public static Entry createEntry(String newEntryName, String experimentFileName)
    {
        Entry newEntry = new Entry();
        newEntry.setEntryType(Entry.ENTRY_TYPE_HIDDEN);
        newEntry.setDisplayName(newEntryName);

        ExperimentProperty property = new ExperimentProperty();
        List<PropertyDataFile> dataFiles = new ArrayList<PropertyDataFile>();
		PropertyDataFile samplePropertyFile = new PropertyDataFile(experimentFileName,
				ExperimentGraph.CURRENT_VERSION,
				PropertyDataFile.DEFAULT_TYPE);
		dataFiles.add(samplePropertyFile);
		property.setDataFiles(dataFiles);

        newEntry.setProperty(property);

        return newEntry;
    }


    public static File getExperimentDirectory(Entry selectedSampleEntry)
    {
    	Entry projectEntry = selectedSampleEntry.getParent();
        //String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator
                + projectEntry.getDisplayName();
        String experimentFolderLocation = projectFolderLocation
                + File.separator
                + ExperimentConfig.EXPERIMENT_FOLDER_NAME;
        File experimentFolder = new File(experimentFolderLocation);
        if(!experimentFolder.exists() || !experimentFolder.isDirectory()) 
        {
        	experimentFolder.mkdir();
        }
        return experimentFolder;
    }	
}