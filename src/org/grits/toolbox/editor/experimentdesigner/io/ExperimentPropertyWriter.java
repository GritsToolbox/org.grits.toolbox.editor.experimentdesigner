package org.grits.toolbox.editor.experimentdesigner.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.grits.toolbox.core.datamodel.io.PropertyWriter;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.datamodel.property.PropertyDataFile;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.jdom.Attribute;
import org.jdom.Element;

public class ExperimentPropertyWriter implements PropertyWriter {
	@Override
    public void write(Property a_property, Element a_propertyElement) throws IOException
    {
		
		if(a_property instanceof ExperimentProperty)
		{
			ExperimentProperty pp = (ExperimentProperty)a_property;
			if(pp.getExperimentFile() != null && pp.getExperimentFile().getName() != null)
			{
				Element fileElement = new Element("file");
				List<Attribute> attributes = new ArrayList<Attribute>();
				attributes.add(new Attribute("name", pp.getExperimentFile().getName()));
				String version = pp.getExperimentFile().getVersion() == null ? 
						ExperimentGraph.CURRENT_VERSION : pp.getExperimentFile().getVersion();
				attributes.add(new Attribute("version", version));
				attributes.add(new Attribute("type", PropertyDataFile.DEFAULT_TYPE));
				fileElement.setAttributes(attributes);
				a_propertyElement.setContent(fileElement);
			}
			else
				throw new IOException("Property could not be added as its experiment file (or name) is null.");
		}
		else
		{
			throw new IOException("This property is not a Experiment Property");
		}
    }
}
