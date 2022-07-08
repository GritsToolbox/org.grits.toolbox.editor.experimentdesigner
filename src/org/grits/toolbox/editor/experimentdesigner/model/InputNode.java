package org.grits.toolbox.editor.experimentdesigner.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.swt.graphics.Image;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;

@XmlRootElement(name="input")
public class InputNode extends GraphNode {
	private static final long serialVersionUID = 1L;
	
	private static final Image INPUTNODE_ICON = ImageRegistry.getImageDescriptor(ExperimentDesignerImage.ELLIPSEICON).createImage();
	
	Integer componentId;
	
	@Override
	public Image getIcon() {
		return INPUTNODE_ICON;
	}

	@XmlAttribute
	public Integer getComponentId() {
		return componentId;
	}

	public void setComponentId(Integer componentId) {
		this.componentId = componentId;
	}

	
}
