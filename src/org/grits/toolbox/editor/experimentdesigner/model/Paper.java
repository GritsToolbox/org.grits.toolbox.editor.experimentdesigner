package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="paper-reference")
public class Paper {

	Integer pubMedId;
	String title;
	Integer year;
	String bibliographicCitation;
	List<String> authors;
	String formatedAuthor;
	
	@XmlElement(name="author")
	public List<String> getAuthors() {
		return authors;
	}
	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}
	
	public void addAuthor (String author) {
		if (authors == null) 
			authors = new ArrayList<>();
		if (!authors.contains(author))
			authors.add(author);
	}
	
	@XmlAttribute
	public Integer getPubMedId() {
		return pubMedId;
	}
	public void setPubMedId(Integer pubMedId) {
		this.pubMedId = pubMedId;
	}
	
	@XmlAttribute
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	
	@XmlAttribute
	public String getFormatedAuthor() {
		return formatedAuthor;
	}
	public void setFormatedAuthor(String formatedAuthor) {
		this.formatedAuthor = formatedAuthor;
	}
	
	@XmlAttribute
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@XmlAttribute
	public String getBibliographicCitation() {
		return bibliographicCitation;
	}
	public void setBibliographicCitation(String bibliographicCitation) {
		this.bibliographicCitation = bibliographicCitation;
	}
	@Override
	public String toString() {
		String toString = title + "\n" + formatedAuthor + "." ;
		toString += "\n" + bibliographicCitation;
		if (pubMedId != null)
			toString += "\nPMID: " + pubMedId;
		return toString;
	}
	
	@Override
	public boolean equals(Object arg0) {
		if (arg0 != null && arg0 instanceof Paper) {
			if (this.pubMedId != null && this.pubMedId.equals(((Paper)arg0).getPubMedId()))
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (pubMedId == null) 
			return -1;
		return pubMedId.intValue();
	}
	public Paper getACopy()
	{
		Paper paper = new Paper();
		paper.setTitle(title);
		paper.setYear(year);
		paper.setPubMedId(pubMedId);
		if(authors != null)
		{
			List<String> authorList = new ArrayList<String>();
			authorList.addAll(authors);
			paper.setAuthors(authorList);
		}
		paper.setFormatedAuthor(formatedAuthor);
		paper.setBibliographicCitation(bibliographicCitation);
		return paper;
	}
	
}
