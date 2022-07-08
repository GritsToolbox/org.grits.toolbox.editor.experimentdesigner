package org.grits.toolbox.editor.experimentdesigner.property;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.io.PropertyWriter;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.core.datamodel.util.DataModelSearch;
import org.grits.toolbox.core.utilShare.DeleteUtils;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;
import org.grits.toolbox.editor.experimentdesigner.io.ExperimentPropertyWriter;
import org.grits.toolbox.entry.sample.property.SampleProperty;

public class ExperimentProperty extends Property
{
	//log4J Logger
    private static final Logger logger = Logger.getLogger(ExperimentProperty.class);
    
    public static final String TYPE = "org.grits.toolbox.property.experiment";
    protected static ImageDescriptor imageDescriptor = ImageRegistry.getImageDescriptor(ExperimentDesignerImage.EXPERIMENTDESIGNICON);
    protected static PropertyWriter writer = new ExperimentPropertyWriter();
    
    public ExperimentProperty()
    {
        super();
    }

    public PropertyDataFile getExperimentFile()
	{
		PropertyDataFile experimentFile = null;
		for(PropertyDataFile dataFile : dataFiles)
		{
			if(PropertyDataFile.DEFAULT_TYPE.equals(dataFile.getType()))
			{
				experimentFile = dataFile;
				break;
			}
		}
		return experimentFile;
	}

    @Override
    public String getType() {
        return ExperimentProperty.TYPE;
    }

    @Override
    public PropertyWriter getWriter() {
        return ExperimentProperty.writer;
    }

    @Override
    public ImageDescriptor getImage() {
        return ExperimentProperty.imageDescriptor;
    }

    @Override
    public void delete(Entry entry) {
        String fileLocation = ExperimentProperty.getExperimentDesignLocation(entry) 
                + File.separator 
                + getExperimentFile().getName();
        try
        {
            DeleteUtils.delete(new File(fileLocation));
        } catch (IOException e)
        {
            ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), "Cannot delete experiment design", e);
            logger.error(Activator.PLUGIN_ID + " Cannot delete experiment design", e);
        }
    }

    @Override
    public Object clone() {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public Property getParentProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	public static String getExperimentDesignLocation (Entry entry) {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator
                + DataModelSearch.findParentByType(entry, ProjectProperty.TYPE).getDisplayName(); // need to find the parent of the sample
        String experimentGroupFolderLocation = projectFolderLocation
                + File.separator
                + ExperimentConfig.EXPERIMENT_FOLDER_NAME;
       
		return experimentGroupFolderLocation;
	}
	
	@Override
	public void makeACopy(Entry currentEntry, Entry destinationEntry)
			throws IOException {
		try
		{
			File currentExperimentFile = new File(ExperimentProperty.getExperimentDesignLocation(currentEntry), getExperimentFile().getName());
			if(currentExperimentFile.exists())
			{
				File destinationFolder = new File(
						ExperimentProperty.getExperimentDesignLocation(destinationEntry.getParent()));
				if(!destinationFolder.exists() || !destinationFolder.isDirectory()) 
				{
					destinationFolder.mkdir();
				}
				// destinationEntry.getParent() should be a sample, find its filename
				String sampleFileName = ((SampleProperty)destinationEntry.getParent().getProperty()).getSampleFile().getName();
				File destinationFile = new File(destinationFolder, sampleFileName.replace("sample", ExperimentConfig.FILE_NAME_PREFIX)); 
				Files.copy(currentExperimentFile.toPath(), destinationFile.toPath());
				List<PropertyDataFile> dataFiles = new ArrayList<PropertyDataFile>();
				PropertyDataFile currentDataFile = getExperimentFile();
				dataFiles.add(new PropertyDataFile(destinationFile.getName(), 
						currentDataFile.getVersion(), currentDataFile.getType()));
				ExperimentProperty experimentProperty = new ExperimentProperty();
				experimentProperty.setDataFiles(dataFiles);
				experimentProperty.setRemoved(!exists());
				experimentProperty.setVersion(getVersion());
				experimentProperty.setViewerRank(getViewerRank());
				destinationEntry.setProperty(experimentProperty);
			}
			else throw new FileNotFoundException("Could not find experiment design file for selected experiment design \"" 
					+ currentEntry.getDisplayName() + "\" in project \"" 
					+ DataModelSearch.findParentByType(currentEntry, ProjectProperty.TYPE).getDisplayName()
					+ "\"");
		} catch (FileNotFoundException ex)
		{
			throw ex;
		} catch (IOException ex)
		{
			throw new IOException("Error copying experiment design information.\n" + ex.getMessage(), ex);
		}
		
	}

}
