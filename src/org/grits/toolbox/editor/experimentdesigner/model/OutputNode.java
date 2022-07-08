package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.swt.graphics.Image;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;

@XmlRootElement(name="output")
public class OutputNode extends GraphNode {
	private static final long serialVersionUID = 1L;
	private static final Image OUTPUTNODE_ICON = ImageRegistry.getImageDescriptor(ExperimentDesignerImage.ELLIPSEICON).createImage();
	
	List<Parameter> parameters;
	List<ParameterGroup> parameterGroups;
	
	
	@Override
	public Image getIcon() {
		return OUTPUTNODE_ICON;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public List<ParameterGroup> getParameterGroups() {
		return parameterGroups;
	}

	public void setParameterGroups(List<ParameterGroup> parameterGroups) {
		this.parameterGroups = parameterGroups;
	}
}
