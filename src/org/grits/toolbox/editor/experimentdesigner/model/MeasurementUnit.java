package org.grits.toolbox.editor.experimentdesigner.model;

public class MeasurementUnit {

	String uri;
	String label;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public MeasurementUnit getACopy()
	{
		MeasurementUnit measurementUnit = new MeasurementUnit();
		measurementUnit.setUri(uri);
		measurementUnit.setLabel(label);
		return measurementUnit;
	}
}
