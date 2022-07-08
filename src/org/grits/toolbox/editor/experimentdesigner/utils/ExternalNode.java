package org.grits.toolbox.editor.experimentdesigner.utils;

import java.util.List;

import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;

public class ExternalNode {
	
	String label;
	String description;
	
	List<Parameter> parameters;
	List<ParameterGroup> parameterGroups;
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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
