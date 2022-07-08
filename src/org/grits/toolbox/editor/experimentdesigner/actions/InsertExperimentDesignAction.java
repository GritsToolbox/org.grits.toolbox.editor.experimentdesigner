package org.grits.toolbox.editor.experimentdesigner.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;
import org.grits.toolbox.editor.experimentdesigner.dialog.SelectExperimentDialog;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.handler.NewExperimentHandler;
import org.grits.toolbox.editor.experimentdesigner.model.Connection;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.InputNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentTemplateEntry;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;

public class InsertExperimentDesignAction extends Action implements
		IWorkbenchAction {
	
	private static final Logger logger = Logger.getLogger(InsertExperimentDesignAction.class);
	
	ExperimentGraph experimentDesign;
	GraphEditor editor;
	
	public InsertExperimentDesignAction(GraphEditor editor) {
		this.editor = editor;
		this.experimentDesign = editor.getModel();
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageRegistry.getImageDescriptor(ExperimentDesignerImage.EXPERIMENTDESIGNICON);
	}
	
	@Override
	public String getId() {
		return GraphEditor.INSERT_DESIGN;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void run() {
		logger.debug(Activator.PLUGIN_ID + " BEGIN InsertExperimentDesignAction...Inserting an experiment graph into another");
		Shell shell = Display.getCurrent().getActiveShell();
		SelectExperimentDialog dialog = new SelectExperimentDialog(PropertyHandler.getModalDialog(shell));
		dialog.setBlockOnOpen(true);
		int status = dialog.open();
		File originalFile=null;
		if (status == Window.OK) {
			Entry experimentEntry = dialog.getExperimentSelected();
			ExperimentTemplateEntry templateFile = dialog.getTemplateFile();
			// load the contents of the experiment template and add the nodes into the existing experiment graph
			if (experimentEntry != null) {
				File originalExperimentFolder = NewExperimentHandler.getExperimentDirectory(experimentEntry.getParent());
	            originalFile = new File (originalExperimentFolder.getAbsolutePath() 
	                    + File.separator + ((ExperimentProperty)experimentEntry.getProperty()).getExperimentFile().getName());
			}
			else if (templateFile != null) {	
	            if (templateFile.isFromJar()) {
	            	// get it from jar
	            	URL url = ExperimentConfig.EXPERIMET_TEMPLATE_RESOURCE_URL;
	        		if (url == null) {
	        			logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!" + " no template directory found in jar");
	    				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template. Reason: No Template directory found in jar");
	        		}
	        		try {
	        			URL resourceFileUrl = FileLocator.toFileURL(url);
	        			originalFile = new File(resourceFileUrl.toURI().getRawPath() + File.separator + templateFile.getFilename());
	        		} catch (IOException | URISyntaxException e) {
	        			logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design template!", e);
	    				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design template",e);
	        		}
	            }
	            else {
	            	originalFile = new File (ExperimentDesignOntologyAPI.getTemplateFolderLocation()
	                        + File.separator + templateFile.getFilename());
	            }
			}
       	}
		
		if (originalFile != null) {
			FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(originalFile);
			
	    		InputStreamReader reader = new InputStreamReader(inputStream, PropertyHandler.GRITS_CHARACTER_ENCODING);
	    		JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
	            Unmarshaller unmarshaller = context.createUnmarshaller();
	            ExperimentGraph graph = (ExperimentGraph) unmarshaller.unmarshal(reader);
	            graph.setConnectionsToNodes();
	            copyFromGraph (graph);
	            this.editor.setGraphModelChanged (true);
			} catch (FileNotFoundException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot copy from existing experiment design! Design not found", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot copy from existing experiment design. Reason: design not found");
			} catch (UnsupportedEncodingException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot read existing experiment design!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot read existing experiment design");
			} catch (JAXBException e) {
				logger.error (Activator.PLUGIN_ID + " Cannot read existing experiment design!", e);
				ErrorUtils.createErrorMessageBox(shell, "Cannot read existing experiment design");
		}
		}
		logger.debug(Activator.PLUGIN_ID + " END InsertExperimentDesignAction.");
	}

	/**
	 * copy all nodes and connections from the given graph into the experimentDesign 
	 * @param graph
	 */
	private void copyFromGraph(ExperimentGraph graph) {
		if (this.experimentDesign == null || graph == null) 
			return;
		
		//find the left most node's location and modify the locations with that offset
		int offset = 0;
		List<GraphNode> existingNodes = experimentDesign.getNodes();
		for (GraphNode graphNode : existingNodes) {
			offset = Math.max(graphNode.getLocation().x + graphNode.getSize().width, offset);
		}
		
		removeInputOutputNodes (graph);
		List<GraphNode> nodes = graph.getNodes();
		for (GraphNode graphNode : nodes) {
			graphNode.setLocation(graphNode.getLocation().translate(offset, 0));
			this.experimentDesign.addChild(graphNode);  // should copy the connections as well
		}
	}

	private void removeInputOutputNodes(ExperimentGraph graph) {
		List<GraphNode> nodesToRemove = new ArrayList<>();
        for (GraphNode graphNode: graph.getNodes()) {
			if (graphNode instanceof InputNode || graphNode instanceof OutputNode) {
				nodesToRemove.add(graphNode);
			}
        }
        
        for (GraphNode node: nodesToRemove) {
			List<Connection> connections = null;
			if (node instanceof InputNode)
				connections = node.getSourceConnections();
			else if (node instanceof OutputNode) {
				connections = node.getTargetConnections();
			}
			// remove the node and disconnect its connections
			boolean wasRemoved = graph.removeChild(node);
			if (wasRemoved && connections != null) {
				for (Iterator<Connection> iter = connections.iterator(); iter.hasNext();) {
					Connection conn = (Connection) iter.next();
					conn.disconnect();
				}
			}
        }
	}

}
