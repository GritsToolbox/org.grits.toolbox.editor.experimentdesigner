package org.grits.toolbox.editor.experimentdesigner.io;

public class ProtocolEntry {

	String uri;
	String category;
	String name;
	String filename;
	
	boolean fromJar=false;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public boolean isFromJar() {
		return fromJar;
	}
	public void setFromJar(boolean fromJar) {
		this.fromJar = fromJar;
	}
	
}
