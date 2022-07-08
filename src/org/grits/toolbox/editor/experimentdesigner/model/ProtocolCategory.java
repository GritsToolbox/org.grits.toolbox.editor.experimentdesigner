package org.grits.toolbox.editor.experimentdesigner.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.swt.graphics.RGB;

@XmlRootElement(name="category")
public class ProtocolCategory implements EntityWithPosition {

	String name;
	String uri;
	String description;
	MyColor color;
	Integer position;
	String icon;
	
	@XmlAttribute
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlAttribute
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	@XmlElement
	public MyColor getColor() {
		return color;
	}
	public void setColor(MyColor color) {
		this.color = color;
	}
	
	@XmlAttribute
	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}
	public void setDescription(String comment) {
		this.description = comment;
		
	}
	@XmlAttribute
	public String getDescription() {
		return description;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}

	public ProtocolCategory getACopy()
	{
		ProtocolCategory protocolCategory = new ProtocolCategory();
		protocolCategory.setUri(uri);
		protocolCategory.setName(name);
		protocolCategory.setDescription(description);
		protocolCategory.setIcon(icon);
		if(color != null)
			protocolCategory.setColor(new MyColor(
					new RGB(color.red, color.green, color.blue)));
		protocolCategory.setPosition(position);

		return protocolCategory;
	}
}
