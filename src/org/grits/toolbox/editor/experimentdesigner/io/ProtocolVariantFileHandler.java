package org.grits.toolbox.editor.experimentdesigner.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ProtocolVariantFileHandler {
	private static Integer identifier = 0;
	   
    //log4J Logger
    private static final Logger logger = Logger.getLogger(ProtocolVariantFileHandler.class);

    /**
     * 
     * @param folder
     * @throws IOException
     */
    public static void createIndexFile(String folder) throws IOException 
    {
        identifier = 0;
        //set the root node
        Element protocol = new Element("protocols");
        Document doc = new Document(protocol);
        doc.setRootElement(protocol);

        XMLOutputter xmlOutput = new XMLOutputter();
        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        try 
        {
            FileWriter fileWriter = new FileWriter(folder+ File.separator + ExperimentConfig.PROTOCOLVARIANT_INDEXFILE);
            xmlOutput.output(doc, fileWriter);
            fileWriter.close();
        } 
        catch (IOException e) 
        {
            logger.error(Activator.PLUGIN_ID + " Cannot create the index file for protocol variants.", e);
            throw e;
        }
    }
    
    public static void addProtocolVariant(ProtocolEntry protocol, String folder) throws IOException, ProtocolVariantExistsException, DataConversionException
    {
        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(folder + File.separator + ExperimentConfig.PROTOCOLVARIANT_INDEXFILE);

        //check if file exists!
        if (xmlFile.exists())
        {
            // Get the root element
            Document doc;
            try {
                doc = builder.build(xmlFile);
            } catch (JDOMException e) {
                logger.error(Activator.PLUGIN_ID + "Protocol xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]", e);
                throw new IOException("Protocol xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]: " + e.getMessage());
            }

            Element root = doc.getRootElement();
            
            // check to see if a protocol variant with the same URI already exists
            List children = root.getChildren();
            if (children != null) {
	            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
					Element child = (Element) iterator.next();
					Attribute nameAttr = child.getAttribute("name");
					Attribute idAttr = child.getAttribute("id");
					if (idAttr != null) {
						identifier = Math.max(identifier, idAttr.getIntValue());
					}
					if (nameAttr != null && nameAttr.getValue().trim().equalsIgnoreCase(protocol.getName())) {
						logger.info(Activator.PLUGIN_ID + " Cannot save the protocol variant since there is one with the same name");
						throw new ProtocolVariantExistsException ("Protocol variant " + protocol.getName() + " already exists!");
					}
				}
            }

            identifier ++;
	        // create the entry tag and add standard information
	        Element t_currentElement = new Element("protocol");
	        t_currentElement.setAttribute("id", identifier.toString());
	        t_currentElement.setAttribute("name", protocol.getName());
	        t_currentElement.setAttribute("uri", protocol.getUri());
	        t_currentElement.setAttribute("filename", protocol.getFilename());
	        if (protocol.getCategory() != null) t_currentElement.setAttribute("category", protocol.getCategory());
	        root.addContent(t_currentElement);
	         
	        
	        // save the .template.xml file
	        XMLOutputter xmlOutput = new XMLOutputter();
	        // display nice nice
	        xmlOutput.setFormat(Format.getPrettyFormat());
	        try 
	        {
	            FileWriter fileWriter = new FileWriter(folder+ File.separator + ExperimentConfig.PROTOCOLVARIANT_INDEXFILE);
	            xmlOutput.output(doc, fileWriter);
	            fileWriter.close();
	        } 
	        catch (IOException e) 
	        {
	            logger.error(Activator.PLUGIN_ID + " Cannot update the index file for protocol variants.", e);
	            throw e;
	        }
        } else {
        	throw new IOException ("protocol index file does not exists!");
        }
    }
    
    public static List<ProtocolEntry> getAllProtocolVariants (String folder) throws IOException {
    	File file = new File (folder);
    	return getAllProtocolVariants (file, false);
    }
    
    public static List<ProtocolEntry> getAllProtocolVariants (File folder, boolean fromJar) throws IOException {
    	List<ProtocolEntry> entries = new ArrayList<>();
    	
    	if (folder == null) return entries;
    	
    	File xmlFile = new File (folder.getAbsolutePath() + File.separator + ExperimentConfig.PROTOCOLVARIANT_INDEXFILE);
    	if (xmlFile.exists()) {
    		SAXBuilder builder = new SAXBuilder();
			// Get the root element
            Document doc;
            try {
                doc = builder.build(xmlFile);
            } catch (JDOMException e) {
                logger.error(Activator.PLUGIN_ID + "Protocol xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]", e);
                throw new IOException("Protocol xml is not a valid xml file[" + xmlFile.getAbsolutePath() + "]: " + e.getMessage());
            }

            Element root = doc.getRootElement();
            
            List children = root.getChildren();
            if (children != null) {
	            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
					Element child = (Element) iterator.next();
					ProtocolEntry entry = new ProtocolEntry();
					Attribute nameAttr = child.getAttribute("name");
					entry.setName(nameAttr.getValue());
					Attribute categoryAttr = child.getAttribute("category");
					if (categoryAttr != null) entry.setCategory(categoryAttr.getValue());
					entry.setUri(child.getAttributeValue("uri"));
			        entry.setFilename(child.getAttributeValue("filename"));
			        entry.setFromJar(fromJar);
			        entries.add(entry);
				}
            }
		}
        
        return entries;
    }
}
