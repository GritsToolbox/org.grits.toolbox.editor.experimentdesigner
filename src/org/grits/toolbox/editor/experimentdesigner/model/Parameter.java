package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.grits.toolbox.editor.experimentdesigner.ontology.OntologyManager;

import com.hp.hpl.jena.vocabulary.XSD;

@XmlRootElement(name="parameter")
public class Parameter implements EntityWithPosition {

	String uri;
	String name;
	String value;
	MeasurementUnit unit;
	Boolean required;
	String description;
	Integer position;
	
	List<String> guidelineURIs;
	
	List<MeasurementUnit> availableUnits;
	
	String namespace;
	String namespaceFile;
	Boolean shortNamespace = false;
	Integer groupId;
	
	public Parameter() {
	}
	
	public Parameter(Parameter p) {
		this.uri = p.getUri();
		this.name = p.getName();
		this.value = p.getValue();
		this.description = p.getDescription();
		this.required = p.getRequired();
		this.availableUnits = p.getAvailableUnits();
		this.unit = p.getUnit();
		this.namespace = p.getNamespace();
		this.namespaceFile = p.getNamespaceFile();
		this.shortNamespace = p.getShortNamespace();
		this.position = p.getPosition();
		this.guidelineURIs = p.getGuidelineURIs();
	}
	
	public Parameter(Parameter p, int gid) {
		this(p);
		this.groupId = gid;
	}
	
	@XmlAttribute
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	@XmlAttribute
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlAttribute
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@XmlElement
	public MeasurementUnit getUnit() {
		return unit;
	}
	public void setUnit(MeasurementUnit unit) {
		this.unit = unit;
	}
	
	@XmlElementWrapper
	public List<MeasurementUnit> getAvailableUnits() {
		return availableUnits;
	}
	public void setAvailableUnits(List<MeasurementUnit> availableUnits) {
		this.availableUnits = availableUnits;
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
	
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		// fix previous versions
		if (namespace != null && namespace.equals(XSD.xdouble.getURI())) {  
			namespace = OntologyManager.baseURI + "double";
		} else if (namespace != null && namespace.equals(XSD.xstring.getURI())) {
			namespace = OntologyManager.baseURI + "string";
		}
 		this.namespace = namespace;
	}
	
	
	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Parameter) {
			if (this.groupId != null && ((Parameter)obj).getGroupId() != null)
				return ((Parameter)obj).getName().equals(this.name) && ((Parameter)obj).getGroupId().equals(this.groupId);
			else
				return ((Parameter)obj).getName().equals(this.name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public String getNamespaceFile() {
		return namespaceFile;
	}

	public void setNamespaceFile(String namespaceFile) {
		this.namespaceFile = namespaceFile;
	}

	@XmlAttribute
	public Boolean getShortNamespace() {
		return shortNamespace;
	}

	public void setShortNamespace(Boolean isShortNamespace) {
		this.shortNamespace = isShortNamespace;
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
	
	public Parameter getACopy()
	{
		Parameter parameter = new Parameter();
		parameter.setUri(uri);
		parameter.setName(name);
		parameter.setDescription(description);
		parameter.setGroupId(groupId);
		parameter.setNamespace(namespace);
		parameter.setNamespaceFile(namespaceFile);
		parameter.setPosition(getPosition());
		parameter.setGuidelineURIs(getGuidelineURIs());
		if(shortNamespace != null)
			parameter.setShortNamespace(shortNamespace.booleanValue());
		if(unit != null)
			parameter.setUnit(unit.getACopy());
		if(availableUnits != null)
		{
			List<MeasurementUnit> unitList = new ArrayList<MeasurementUnit>();
			for(MeasurementUnit availableUnit : availableUnits)
			{
				unitList.add(availableUnit.getACopy());
			}
			parameter.setAvailableUnits(unitList);
		}
		parameter.setValue(value);
		parameter.setRequired(required);
		return parameter;
	}
}
