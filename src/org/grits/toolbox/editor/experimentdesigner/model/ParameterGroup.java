package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ParameterGroup implements EntityWithPosition{

	Integer id;
	String label;
	String uri;
	List<Parameter> parameters;
	Boolean required;
	String description;
	Integer position;
	List<String> guidelineURIs;
	
	public ParameterGroup () {	
	}
	
	public ParameterGroup (ParameterGroup group) {
		this.id = group.id;
		this.label = group.label;
		this.uri = group.uri;
		this.required = group.required;
		this.description = group.description;
		this.position = group.position;
		this.guidelineURIs = group.guidelineURIs;
		List<Parameter> groupParams = group.getParameters();
		parameters = new ArrayList<>();
		for (Parameter parameter : groupParams) {
			parameters.add(new Parameter(parameter, group.id));
		}
	}

	@XmlAttribute
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlAttribute
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@XmlAttribute
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	@XmlAttribute
	public Integer getPosition() {
		return position;
	}
	
	public void setPosition(Integer position) {
		this.position = position;
	}
	
	public List<String> getGuidelineURIs() {
		return guidelineURIs;
	}
	
	public void setGuidelineURIs(List<String> guidelineURIs) {
		this.guidelineURIs = guidelineURIs;
	}

	@XmlElement(name="parameter")
	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}
	
	@XmlAttribute
	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	@XmlAttribute
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ParameterGroup) {
			if (this.id != null && ((ParameterGroup)obj).getId() != null) 
				return ((ParameterGroup)obj).getLabel().equals(this.label) && ((ParameterGroup)obj).getId().equals(this.id);
			else
				return ((ParameterGroup)obj).getLabel().equals(this.label);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return label.hashCode();
	}

	public ParameterGroup getACopy()
	{
		ParameterGroup parameterGroup = new ParameterGroup();
		parameterGroup.setUri(uri);
		parameterGroup.setLabel(label);
		parameterGroup.setDescription(description);
		parameterGroup.setId(id);
		parameterGroup.setPosition(position);
		parameterGroup.setGuidelineURIs(guidelineURIs);
		if(parameters != null)
		{
			List<Parameter> parameterList = new ArrayList<Parameter>();
			for(Parameter parameter : parameters)
			{
				parameterList.add(parameter.getACopy());
			}
			parameterGroup.setParameters(parameterList);
		}
		parameterGroup.setRequired(required);
		return parameterGroup;
	}
	
}
