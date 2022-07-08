package org.grits.toolbox.editor.experimentdesigner.editor;

import java.util.List;

import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.swt.graphics.Color;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolCategory;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;

public class ProtocolNodeCreationFactory implements CreationFactory {

	String text;
	Color color;
	ProtocolCategory category;
	String uri;
	String url;
	String creator;
	String description;
	String file;
	List<Parameter> parameters;
	List<ParameterGroup> parameterGroups;
	List<Paper> papers;
	
	@Override
	public Object getNewObject() {
		ProtocolNode object = new ProtocolNode();
		object.setLabel(text);
		object.setColor(color);
		object.setCategory(category);
		object.setTemplateUri(uri);
		object.setUrl(url);
		object.setCreator(creator);
		object.setParameters(parameters);
		object.setParameterGroups(parameterGroups);
		object.setPapers(papers);
		object.setDescription(description);
		object.setFile(file);
		return object;
	}

	@Override
	public Object getObjectType() {
		return ProtocolNode.class;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public void setColor (Color color) {
		this.color = color;
	}

	public ProtocolCategory getCategory() {
		return category;
	}

	public void setCategory(ProtocolCategory category) {
		this.category = category;
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

	public void setCreator(String creator) {
		this.creator = creator;
	}
	
	public String getCreator() {
		return creator;
	}

	public void setPapers(List<Paper> papers) {
		this.papers = papers;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

}
