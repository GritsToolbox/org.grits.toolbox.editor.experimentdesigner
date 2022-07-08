package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.List;

import org.eclipse.swt.graphics.Color;

public class ProtocolPaletteEntry {
	String label;
	Color color;
	String uri;
	String description;
	String creator;
	String url;
	String file;
	ProtocolCategory category;
	List<Parameter> parameters;
	List<ParameterGroup> parameterGroups;
	List<Paper> papers;
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	
	public void setCategory (ProtocolCategory cat) {
		this.category = cat;
	}
	public ProtocolCategory getCategory() {
		return category;
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
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public void setDescription(String comment) {
		this.description = comment;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public String getDescription() {
		return description;
	}
	
	public List<Paper> getPapers() {
		return papers;
	}
	public void setPapers(List<Paper> allPapersForProtocol) {
		this.papers = allPapersForProtocol;
	}
	
}
