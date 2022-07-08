package org.grits.toolbox.editor.experimentdesigner.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.geometry.Point;
import org.grits.toolbox.core.dataShare.IGritsConstants;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.editor.experimentdesigner.exception.NoExperimentDesignException;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;

public class ExperimentDesignerUtils {
	@Inject @Named (IGritsConstants.WORKSPACE_LOCATION) private static String workspaceLocation;
	
	public static void addOutputNode (Entry sampleEntry, ExternalNode node) throws Exception {
		
		ExperimentGraph graph = ExperimentDesignerUtils.loadExperimentDesign(sampleEntry);
        graph.setConnectionsToNodes();
        graph.setProjectEntry(sampleEntry.getParent());
        List children = graph.getChildren();
        Point location = new Point (50, 100);
        int locationY = 100;
        for (Object graphNode : children) {
			if (graphNode instanceof GraphNode) {
				if (((GraphNode) graphNode).getLocation() == null) {  
					((GraphNode) graphNode).setLocation(location);
					location = location.translate(100, 0);
				} else {
					locationY = Math.max(locationY, ((GraphNode) graphNode).getLocation().y );
				}
			}
		}
        
		addOutputNode (graph, node, locationY);
		
		// save the graph back
		ExperimentProperty experimentProperty = getExperimentProperty(sampleEntry);
        // name of the experiment xml file 
        String fileName = experimentProperty.getExperimentFile().getName();
		String experimentFolderLocation = ExperimentDesignerUtils.getExperimentFolderLocation(sampleEntry);
		//file with absolute path
        String fileLocation= experimentFolderLocation 
                + File.separator 
                + fileName;
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, PropertyHandler.GRITS_CHARACTER_ENCODING);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(graph, os);

        //write the serialized data to the folder
        FileWriter fileWriter = new FileWriter(fileLocation);
        fileWriter.write(os.toString((String)marshaller.getProperty(Marshaller.JAXB_ENCODING)));
        fileWriter.close();
        os.close();
	}
	
	@SuppressWarnings("rawtypes")
	static void addOutputNode(ExperimentGraph graph, ExternalNode node, int locationY) {
		
		int locationX = 50;
		List existingNodes = graph.getChildren();
		
		boolean entryFound = false;
		
		for (Iterator iterator2 = existingNodes.iterator(); iterator2
				.hasNext();) {
			Object object = (Object) iterator2.next();
			if (object instanceof OutputNode) {
				OutputNode outputNode = (OutputNode)object;
				locationX = outputNode.getLocation().x;
				if (outputNode.getLabel().equalsIgnoreCase(node.getLabel())) {
					//same outputNode exists, replace it
					outputNode.setDescription(node.getDescription());
					outputNode.setParameterGroups(node.getParameterGroups());
					outputNode.setParameters(node.getParameters());
					entryFound = true;
					break;
				}
			}
		}
			
		Point originalLocation = new Point (locationX, locationY+100);
		if (!entryFound) {
			OutputNode newNode = new OutputNode();
			newNode.setLabel(node.getLabel());
			newNode.setParameterGroups(node.getParameterGroups());
			newNode.setParameters(node.getParameters());
			newNode.setLocation(originalLocation.translate(100, 0));
			newNode.setColor(ColorConstants.lightGreen);
			graph.addChild(newNode);
		}
	}
	
	static ExperimentProperty getExperimentProperty (Entry sampleEntry) throws NoExperimentDesignException {
        List<Entry> childEntries = sampleEntry.getChildren();
        ExperimentProperty experimentProperty = null;
        for (Iterator<Entry> iterator = childEntries.iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			Property prop = entry.getProperty();
			if (prop.getType() == ExperimentProperty.TYPE) { 
				experimentProperty = (ExperimentProperty) prop;
			}
			
		}
        if (experimentProperty == null) {
        	throw new NoExperimentDesignException("Experiment Design for " + sampleEntry.getDisplayName() + " does not exists!");
        }
        
        return experimentProperty;
	}
	
	static String getExperimentFolderLocation (Entry sampleEntry) {
		String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator
                + sampleEntry.getParent().getDisplayName();
		String exampleFolderLocation = projectFolderLocation
	                + File.separator
	                + "experiments";
		return exampleFolderLocation;
	}
	
	public static ExperimentGraph loadExperimentDesign (Entry sampleEntry) throws Exception {
		String experimentFolderLocation = ExperimentDesignerUtils.getExperimentFolderLocation(sampleEntry);
        ExperimentProperty experimentProperty = getExperimentProperty(sampleEntry);
        // name of the experiment xml file 
        String fileName = experimentProperty.getExperimentFile().getName();
  
        //file with absolute path
        String fileLocation= experimentFolderLocation 
                + File.separator 
                + fileName;
        File experimentFile = new File(fileLocation);
        
        if (!experimentFile.exists()) {
        	throw new NoExperimentDesignException("Experiment Design for " + sampleEntry.getDisplayName() + " does not exists!");
        }
        FileInputStream inputStream = new FileInputStream(experimentFile.getAbsolutePath());
        InputStreamReader reader = new InputStreamReader(inputStream, PropertyHandler.GRITS_CHARACTER_ENCODING);
        JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ExperimentGraph graph = (ExperimentGraph) unmarshaller.unmarshal(reader);
        
        reader.close();
        inputStream.close();
        return graph;
	}
	
	/**
	 * return the image saved for the experiment design for the given sample entry
	 * @param sampleEntry sample entry to load the design image for
	 * @return null if there is no image, or the image for the design
	 * @throws Exception
	 */
	public static BufferedImage loadExperimentDesignImage (Entry sampleEntry) throws Exception {
		String experimentFolderLocation = ExperimentDesignerUtils.getExperimentFolderLocation(sampleEntry);
        ExperimentProperty experimentProperty = getExperimentProperty(sampleEntry);
        // name of the experiment xml file 
        String fileName = experimentProperty.getExperimentFile().getName();
        String imageFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".png";
        String imageFileLocation = experimentFolderLocation 
                + File.separator + imageFileName;
        //file with absolute path
        File imageFile = new File(imageFileLocation);
        if (imageFile.exists())
        	return ImageIO.read(imageFile);
		return null;
	}
	
	/**
	 * get list of all parameters with the given name in the given protocol, which includes any such parameter in parameter groups as well
	 * @param sampleEntry sample entry that the experiment design belongs
	 * @param protocolName name of the protocol
	 * @param parameterName name of the parameter
	 * @return list of all parameters with the given name in the given protocol
	 * @throws Exception
	 */
	public static List<Parameter> getParameterFromExperiment (Entry sampleEntry, String protocolName, String parameterName) throws Exception {
		List<Parameter> parameters = new ArrayList<>();
		ExperimentGraph graph = ExperimentDesignerUtils.loadExperimentDesign(sampleEntry);
		for (Object graphNode : graph.getChildren()) {
			if (graphNode instanceof ProtocolNode) {
				ProtocolNode protocol = (ProtocolNode)graphNode;
				if (protocol.getLabel().equalsIgnoreCase(protocolName)) {
					if(protocol.getParameters() != null){
						for (Parameter parameter: protocol.getParameters()) {
							if (parameter.getName().equalsIgnoreCase(parameterName)) {
								parameters.add(parameter);
							}
	 					}						
					}
					if(protocol.getParameterGroups() != null){
						for (ParameterGroup group: protocol.getParameterGroups()) {
							for (Parameter parameter: group.getParameters()) {
								if (parameter.getName().equalsIgnoreCase(parameterName)) {
									parameters.add(parameter);
								}
							}
						}						
					}
				}
			}
		}
		return parameters;
	}
	
	/** 
	 * return all parameterGroups with the given name in the given protocol
	 * 
	 * @param sampleEntry sample entry that the experiment design belongs
	 * @param protocolName name of the protocol
	 * @param parameterGroupName name of the parameter group
	 * @return list of all parameter groups with the given name in the given protocol
	 * @throws Exception
	 */
    public static List<ParameterGroup> getParameterGroupFromExperiment (Entry sampleEntry, String protocolName, String parameterGroupName) throws Exception {
    	List<ParameterGroup> parameterGroups = new ArrayList<>();
		ExperimentGraph graph = ExperimentDesignerUtils.loadExperimentDesign(sampleEntry);
		for (Object graphNode : graph.getChildren()) {
			if (graphNode instanceof ProtocolNode) {
				ProtocolNode protocol = (ProtocolNode)graphNode;
				if (protocol.getLabel().equalsIgnoreCase(protocolName)) {	
					for (ParameterGroup group: protocol.getParameterGroups()) {
						if (group.getLabel().equalsIgnoreCase(parameterGroupName)) {
							parameterGroups.add(group);
						}
					}
				}
			}
		}
		return parameterGroups;
    }
    
}
