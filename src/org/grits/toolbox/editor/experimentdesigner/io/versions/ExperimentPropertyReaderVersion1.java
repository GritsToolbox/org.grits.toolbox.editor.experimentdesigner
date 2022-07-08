package org.grits.toolbox.editor.experimentdesigner.io.versions;

import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.jdom.Element;

public class ExperimentPropertyReaderVersion1 {
	public static Property read(Element propertyElement, ExperimentProperty property)
	{
		return property;
	}
}
