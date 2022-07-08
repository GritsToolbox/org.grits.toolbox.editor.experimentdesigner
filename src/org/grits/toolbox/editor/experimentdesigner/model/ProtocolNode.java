package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.swt.graphics.Image;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;

@XmlRootElement(name="protocol")
public class ProtocolNode extends GraphNode {
	private static final long serialVersionUID = 1L;
	private static final Image PROTOCOLNODE_ICON = ImageRegistry.getSmallIcon("rectangle16.gif").createImage();
	
	ProtocolCategory category;
	List<Parameter> parameters;
	List<ParameterGroup> parameterGroups;
	List<Paper> papers;
	
	String template;
	String templateUri;
	String creator;
	
	String url;
	String file = null;
	
	boolean templateChanged = false;
	
	@Override
	public Image getIcon() {
		
		return PROTOCOLNODE_ICON;
	}

	@XmlElement
	public ProtocolCategory getCategory() {
		return category;
	}

	public void setCategory(ProtocolCategory category) {
		this.category = category;
	}

	@XmlElement(name="parameter")
	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	@XmlElement(name="paper-reference")
	public List<Paper> getPapers() {
		return papers;
	}

	public void setPapers(List<Paper> papers) {
		this.papers = papers;
	}

	@XmlAttribute
	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	@XmlAttribute
	public String getCreator() {
		return creator;
	}
	
	public void setCreator(String creator) {
		this.creator = creator;
	}

	@XmlElement(name="parameter-group")
	public List<ParameterGroup> getParameterGroups() {
		return parameterGroups;
	}

	public void setParameterGroups(List<ParameterGroup> parameterGroups) {
		this.parameterGroups = parameterGroups;
	}

	public boolean isTemplateChanged() {
		return templateChanged;
	}

	public void setTemplateChanged(boolean templateChanged) {
		this.templateChanged = templateChanged;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTemplateUri() {
		return templateUri;
	}

	public void setTemplateUri(String templateUri) {
		this.templateUri = templateUri;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
	
/*	@Override
	public String getUri() {
		if (uri != null) 
			return uri;
		else if (templateUri != null)
			return templateUri;
		return null;
	}*/
}
