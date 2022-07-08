package org.grits.toolbox.editor.experimentdesigner.io;

import java.io.IOException;

import org.grits.toolbox.core.datamodel.UnsupportedVersionException;
import org.grits.toolbox.core.datamodel.io.PropertyReader;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.editor.experimentdesigner.io.versions.ExperimentPropertyReaderVersion0;
import org.grits.toolbox.editor.experimentdesigner.io.versions.ExperimentPropertyReaderVersion1;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.jdom.Element;

/**
 * Reader for experiment entry. Should check for empty values
 *
 */
public class ExperimentPropertyReader extends PropertyReader
{
    @Override
    public Property read(Element propertyElement) throws IOException, UnsupportedVersionException
    {
        ExperimentProperty property = new ExperimentProperty();

        PropertyReader.addGenericInfo(propertyElement, property);

		if(property.getVersion() == null)
		{
			return ExperimentPropertyReaderVersion0.read(propertyElement, property);
		}
		else if(property.getVersion().equals("1.0"))
		{
			return ExperimentPropertyReaderVersion1.read(propertyElement, property);
		}
		else 
			throw new UnsupportedVersionException("This version is currently not supported.",
					property.getVersion());
    }
}
