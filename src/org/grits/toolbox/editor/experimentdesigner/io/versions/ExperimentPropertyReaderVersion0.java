package org.grits.toolbox.editor.experimentdesigner.io.versions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.datamodel.io.PropertyReader;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.jdom.Attribute;
import org.jdom.Element;

public class ExperimentPropertyReaderVersion0 {
	public static Property read(Element propertyElement, ExperimentProperty property) throws IOException, UnsupportedVersionException
	{		
		Element experimentElement = propertyElement.getChild("experiment");
        if ( experimentElement == null )
               throw new IOException("Experiment property misses element \"experiment\".");
        
        Attribute fileNameElement = experimentElement.getAttribute("filename");
        if(fileNameElement != null)
        {
        	List<PropertyDataFile> dataFiles = new ArrayList<PropertyDataFile>();
			PropertyDataFile dataFile = new PropertyDataFile(fileNameElement.getValue(), 
					ExperimentGraph.CURRENT_VERSION, 
					PropertyDataFile.DEFAULT_TYPE);

			dataFiles.add(dataFile);
			property.setDataFiles(dataFiles);
			PropertyReader.UPDATE_PROJECT_XML = true;
			return property;
        }
        else
        {
        	throw new UnsupportedVersionException("This version is not supported! Experiment property misses filename attribute", "older than version 1.0");
        }
	}
}
