package org.grits.toolbox.editor.experimentdesigner.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.grits.toolbox.core.datamodel.io.ProjectFileHandler;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentTemplateEntry;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ExperimentTemplateFileHandler {
	public static SimpleDateFormat DATEFORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",Locale.US);
    
    private static Integer identifier = 0;
   
    //log4J Logger
    private static final Logger logger = Logger.getLogger(ExperimentTemplateFileHandler.class);

    /**
     * 
     * @param folder
     * @throws IOException
     */
    public static void createTemplateIndexFile(String folder) throws IOException 
    {
        identifier = 0;
        //set the root node
        Element template = new Element("experimentTemplates");
        Document doc = new Document(template);
        doc.setRootElement(template);

        XMLOutputter xmlOutput = new XMLOutputter();
        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        try 
        {
            FileWriter fileWriter = new FileWriter(folder+ File.separator + ExperimentConfig.EXPERIMENTTEMPLATE_INDEXFILE);
            xmlOutput.output(doc, fileWriter);
            fileWriter.close();
        } 
        catch (IOException e) 
        {
            logger.error(Activator.PLUGIN_ID + " Cannot create the index file for experiment templates.", e);
            throw e;
        }
    }
    
    public static void addTemplate(ExperimentTemplateEntry template, String folder) throws IOException, TemplateExistsException, DataConversionException
    {
        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(folder + File.separator + ExperimentConfig.EXPERIMENTTEMPLATE_INDEXFILE);

        //check if file exists!
        if (xmlFile.exists())
        {
            // Get the root element
            Document doc;
            try {
                doc = builder.build(xmlFile);
            } catch (JDOMException e) {
                logger.error(Activator.PLUGIN_ID + "Template xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]", e);
                throw new IOException("Template xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]: " + e.getMessage());
            }

            Element root = doc.getRootElement();
            
            // check to see if the template with the same name already exists
            List children = root.getChildren();
            if (children != null) {
	            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
					Element child = (Element) iterator.next();
					Attribute idAttr = child.getAttribute("id");
					if (idAttr != null) {
						identifier = Math.max(identifier, idAttr.getIntValue());
					}
					Attribute nameAttr = child.getAttribute("name");
					if (nameAttr != null && nameAttr.getValue().trim().equalsIgnoreCase(template.getName())) {
						logger.info(Activator.PLUGIN_ID + " Cannot save the template since there is one with the same name");
						throw new TemplateExistsException ("Template with given name " + template.getName() + " already exists!");
					}
				}
            }

            identifier++;
	        // create the entry tag and add standard information
	        Element t_currentElement = new Element("template");
	        t_currentElement.setAttribute("id", identifier.toString());
	        t_currentElement.setAttribute("name", template.getName());
	        t_currentElement.setAttribute("creationTime", ProjectFileHandler.DATEFORMATER.format(template.getDateCreated()));
	        t_currentElement.setAttribute("filename", template.getFilename());
	        if (template.getCreator() != null) t_currentElement.setAttribute("creator", template.getCreator());
	        root.addContent(t_currentElement);
	         
	        if (template.getDescription() != null) {
	        	//save description
		        Element description = new Element("description");
		        description.setText(template.getDescription());
		        t_currentElement.addContent(description);
	        }
	        
	        // save the .template.xml file
	        XMLOutputter xmlOutput = new XMLOutputter();
	        // display nice nice
	        xmlOutput.setFormat(Format.getPrettyFormat());
	        try 
	        {
	            FileWriter fileWriter = new FileWriter(folder+ File.separator + ExperimentConfig.EXPERIMENTTEMPLATE_INDEXFILE);
	            xmlOutput.output(doc, fileWriter);
	            fileWriter.close();
	        } 
	        catch (IOException e) 
	        {
	            logger.error(Activator.PLUGIN_ID + " Cannot update the index file for experiment templates.", e);
	            throw e;
	        }
        } else {
        	throw new IOException ("Template index file does not exists!");
        }
    }
    
    public static List<ExperimentTemplateEntry> getAllTemplates (String folder) throws IOException {
    	File file = new File(folder);
    	return getAllTemplates(file, false);
    }
    
    public static List<ExperimentTemplateEntry> getAllTemplates (File folder, boolean fromJar) throws IOException {
    	List<ExperimentTemplateEntry> entries = new ArrayList<>();
        
    	if (folder == null)
    		return entries;
    	
    	File xmlFile = new File(folder.getAbsolutePath() + File.separator + ExperimentConfig.EXPERIMENTTEMPLATE_INDEXFILE);

        //check if file exists!
        if (xmlFile.exists()) {
        	SAXBuilder builder = new SAXBuilder();
            Document doc;
            try {
                doc = builder.build(xmlFile);
            } catch (JDOMException e) {
                logger.error(Activator.PLUGIN_ID + "Template xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]", e);
                throw new IOException("Template xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]: " + e.getMessage());
            }

            Element root = doc.getRootElement();
            
            List children = root.getChildren();
            if (children != null) {
	            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
					Element child = (Element) iterator.next();
					ExperimentTemplateEntry entry = new ExperimentTemplateEntry();
					Attribute nameAttr = child.getAttribute("name");
					entry.setName(nameAttr.getValue());
					Attribute creatorAttr = child.getAttribute("creator");
					if (creatorAttr != null) entry.setCreator(creatorAttr.getValue());
					String dateAttr = child.getAttributeValue("creationTime");
					
			        if ( dateAttr == null )
			        {
			            entry.setDateCreated(new Date());
			        }
			        else
			        {
			            try
			            {
			                Date t_date = ProjectFileHandler.DATEFORMATER.parse(dateAttr);
			                entry.setDateCreated(t_date);
			            }
			            catch(Exception e)
			            {
			                logger.error("Unable to parse creation date for entry (id=" + child.getAttributeValue("name") + "): " + dateAttr, e );
			                entry.setDateCreated(new Date());
			            }
			        }
			        
			        entry.setFilename(child.getAttributeValue("filename"));
			        entry.setFromJar(fromJar);
			        Element descElement = child.getChild("description");
			        if (descElement != null) entry.setDescription(descElement.getText());
			        
			        entries.add(entry);
				}
            }
	            
		}
        
        return entries;
    }
        
    
    

}
