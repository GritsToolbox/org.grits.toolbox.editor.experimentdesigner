package org.grits.toolbox.editor.experimentdesigner.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.swt.graphics.RGB;

@XmlRootElement(name="color")
public class MyColor {

	int red;
	int blue;
	int green;
	
	public MyColor() {
		//default color light blue
		this.red=127;
		this.green=127;
		this.blue=255;
	}
	
	public MyColor(RGB rgb) {
		this.red = rgb.red;
		this.green = rgb.green;
		this.blue = rgb.blue;
	}
	
	@XmlAttribute
	public int getRed() {
		return red;
	}
	public void setRed(int red) {
		this.red = red;
	}
	
	@XmlAttribute
	public int getBlue() {
		return blue;
	}
	public void setBlue(int blue) {
		this.blue = blue;
	}
	
	@XmlAttribute
	public int getGreen() {
		return green;
	}
	public void setGreen(int green) {
		this.green = green;
	}
}
