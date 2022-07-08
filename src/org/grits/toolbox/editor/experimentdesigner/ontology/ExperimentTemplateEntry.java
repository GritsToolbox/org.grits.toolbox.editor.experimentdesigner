package org.grits.toolbox.editor.experimentdesigner.ontology;

import java.util.Date;

public class ExperimentTemplateEntry {

	String name;
	String description;
	String creator;
	String filename;
	Date dateCreated;
	boolean fromJar=false;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public boolean isFromJar() {
		return fromJar;
	}
	public void setFromJar(boolean fromJar) {
		this.fromJar = fromJar;
	}
}
