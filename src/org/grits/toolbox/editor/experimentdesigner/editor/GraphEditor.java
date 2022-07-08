package org.grits.toolbox.editor.experimentdesigner.editor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.DataModelHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.io.ProjectFileHandler;
import org.grits.toolbox.core.datamodel.property.Property;
import org.grits.toolbox.core.editor.ViewInput;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.actions.InsertExperimentDesignAction;
import org.grits.toolbox.editor.experimentdesigner.actions.SaveExperimentTemplateAction;
import org.grits.toolbox.editor.experimentdesigner.actions.SelectProtocolAction;
import org.grits.toolbox.editor.experimentdesigner.commands.ResizeAllNodesCommand;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;
import org.grits.toolbox.editor.experimentdesigner.model.Connection;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.InputNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.parts.GraphNodeEditPart;
import org.grits.toolbox.editor.experimentdesigner.parts.GraphNodeEditPartFactory;
import org.grits.toolbox.editor.experimentdesigner.parts.ProtocolFigure;
import org.grits.toolbox.editor.experimentdesigner.pdfgeneration.ExperimentDesignRDFReport;
import org.grits.toolbox.editor.experimentdesigner.print.PrintExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.grits.toolbox.entry.sample.model.Component;
import org.grits.toolbox.entry.sample.model.Sample;
import org.grits.toolbox.entry.sample.property.SampleProperty;

public class GraphEditor extends GraphicalEditorWithFlyoutPalette {
	private static final Logger logger = Logger.getLogger(GraphEditor.class);

	public static final String SAVE_AS_TEMPLATE = "SaveAsTemplate";
	public static final String INSERT_DESIGN = "InsertDesign";
	public static final String RESIZE_ALL = "ResizeAllNodes";
	
	/** This is the root of the editor's model. */
	private ExperimentGraph graph;
	
	@Inject ESelectionService selectionService;
	
	/** to handle model changes that do not affect visuals such as name, description, parameters etc. */
	boolean graphModelChanged = false;
	
	/** Palette component, holding the tools and nodes. */
	private static PaletteRoot PALETTE_MODEL;
	
	public GraphEditor() {
		setEditDomain(new DefaultEditDomain(this));
	}
	
	@Override
	public DefaultEditDomain getEditDomain() {
		return super.getEditDomain();
	}
	
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		super.selectionChanged(part, selection);
	}
	
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();

		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new GraphNodeEditPartFactory());
		viewer.setRootEditPart(new ScalableFreeformRootEditPart());
		
		GraphicalViewerKeyHandler keyHandler =  new GraphicalViewerKeyHandler(viewer);
		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
		keyHandler.put(KeyStroke.getPressed(SWT.BS, SWT.BS, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
		viewer.setKeyHandler(keyHandler);
		
		// configure the context menu provider
		ContextMenuProvider cmProvider = new GraphEditorContextMenuProvider(
				viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		
		getSite().registerContextMenu(cmProvider, viewer);
	}
	
	public GraphicalViewer getViewer() {
		return getGraphicalViewer();
	}
	
	/*
	 * (non-Javadoc)
	 * Overwritten to add new actions to the context menu for creating protocols by copying existing ones, for printing etc.
	 * 
	 */
	@Override
	protected void createActions() {
		super.createActions();
		ActionRegistry actionRegistry = getActionRegistry(); 
		IAction protocolCopyAction = new SelectProtocolAction(this); 
		actionRegistry.registerAction(protocolCopyAction); 
		
		SaveExperimentTemplateAction saveTemplateAction = new SaveExperimentTemplateAction(this);
		saveTemplateAction.setText("Save as Template");
		actionRegistry.registerAction(saveTemplateAction);
		
		InsertExperimentDesignAction insertTemplateAction = new InsertExperimentDesignAction(this);
		insertTemplateAction.setText("Insert from Template");
	 	actionRegistry.registerAction(insertTemplateAction);
	 	
		actionRegistry.registerAction(new PrintAction(this) {
			@Override
			public void run() {
				logger.info("Print action starts");
				GraphicalViewer viewer;
		        viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);

	            PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), SWT.NULL);
	            PrinterData data = dialog.open();

	            if (data != null) {
	            	PrintExperimentGraph op = new PrintExperimentGraph(new Printer(data), viewer, graph);
	                op.run(getWorkbenchPart().getTitle());
	            }
	            logger.info("Done printing");
			}
			
			@Override
			public String getId() {
				return ActionFactory.PRINT.getId();
			}
			
			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageRegistry.getImageDescriptor(ExperimentDesignerImage.PRINTICON);
			}
		});
		
		actionRegistry.registerAction(new Action("Resize All Nodes") {
			@Override
			public void run() {
				logger.info("Resizing nodes");
				if (graph != null) {
					ResizeAllNodesCommand resize = new ResizeAllNodesCommand(graph);
					getEditDomain().getCommandStack().execute(resize);
				}
				logger.info("Done resizing");
			}
			
			@Override
			public String getId() {
				return GraphEditor.RESIZE_ALL;
			}
			
			@Override
			public String getToolTipText() {
				return "Make all nodes the same size (as the largest node)";
			}
		});
		
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.ui.parts.GraphicalEditor#commandStackChanged(java.util
	 * .EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}
	
	/**@Override
    public  void configurePaletteViewer () {
		PaletteViewer viewer = getPaletteViewer();
		viewer.addDragSourceListener(new TemplateTransferDragSourceListener(
						viewer));
	} */
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#
	 * createPaletteViewerProvider()
	 */
	protected PaletteViewerProvider createPaletteViewerProvider() {
		return new PaletteViewerProvider(getEditDomain()) {
			protected void configurePaletteViewer(PaletteViewer viewer) {
				super.configurePaletteViewer(viewer);
				// create a drag source listener for this palette viewer
				// together with an appropriate transfer drop target listener,
				// this will enable
				// model element creation by dragging a
				// CombinatedTemplateCreationEntries
				// from the palette into the editor
				// @see GraphEditor#createTransferDropTargetListener()
				viewer.addDragSourceListener(new TemplateTransferDragSourceListener(
						viewer));
			}
		};
	}

	/**
	 * Create a transfer drop target listener. When using a
	 * CombinedTemplateCreationEntry tool in the palette, this will enable model
	 * element creation by dragging from the palette.
	 * 
	 * @see #createPaletteViewerProvider()
	 */
	private TransferDropTargetListener createTransferDropTargetListener() {
		return new TemplateTransferDropTargetListener(getGraphicalViewer()) {
			@SuppressWarnings("rawtypes")
			protected CreationFactory getFactory(Object template) {
				if (template instanceof CreationFactory) { 
					// Use the passed in factory 
					return (CreationFactory)template; 
				} else { 
					// Create a simple factory for the template type 
					return new SimpleFactory ((Class)template); 
				}
			}
		};
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = getEditorInput();
		if(input instanceof ViewInput) {
            Entry entry = ((ViewInput) input).getEntry();
            
            String exampleFolderLocation = ExperimentProperty.getExperimentDesignLocation(entry);
            ExperimentProperty experimentProperty = ((ExperimentProperty) entry.getProperty());
            // name of the experiment xml file 
            String fileName = experimentProperty.getExperimentFile().getName();
      
            //file with absolute path
            String fileLocation= exampleFolderLocation 
                    + File.separator 
                    + fileName;
            
            String imageFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".png";
            String imageFileLocation = exampleFolderLocation 
                    + File.separator + imageFileName;
            
            // serialize the experiment object to xml
            try
            {
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
                
                // write the design image to a file
                File outputfile = new File(imageFileLocation);
                ImageIO.write(this.getBufferedImage(), "png", outputfile);
                
                getCommandStack().markSaveLocation();
                // we have to clear this flag and firePropertyChange 
                // so that isDirty() gets called and * next to the editor gets cleared. 
                // Otherwise it looks as dirty but it actually is not.
                graphModelChanged = false;
                firePropertyChange(PROP_DIRTY);
            } catch (JAXBException | IOException e)
            {
            	ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), "Cannot save the Experiment", e);
            	logger.error(Activator.PLUGIN_ID + " Cannot save the Experiment", e);
            } 
		} else {
			ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), "Cannot save the Experiment. Internal Error!");
			logger.error(Activator.PLUGIN_ID + " Cannot save the Experiment - Internal Error");
		}
	}

	@Override
	public void doSaveAs() {
		// not supported
	}
	
	@SuppressWarnings("rawtypes")
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		try {
			
			if(input instanceof ViewInput) {
	            Entry entry = ((ViewInput) input).getEntry();
	            String exampleFolderLocation = ExperimentProperty.getExperimentDesignLocation(entry);
	            ExperimentProperty experimentProperty = ((ExperimentProperty) entry.getProperty());
	            // name of the experiment xml file 
	            String fileName = experimentProperty.getExperimentFile().getName();
	      
	            //file with absolute path
	            String fileLocation= exampleFolderLocation 
	                    + File.separator 
	                    + fileName;
	            File experimentFile = new File(fileLocation);
	            FileInputStream inputStream = new FileInputStream(experimentFile.getAbsolutePath());
	            InputStreamReader reader = new InputStreamReader(inputStream, PropertyHandler.GRITS_CHARACTER_ENCODING);
	            JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
	            Unmarshaller unmarshaller = context.createUnmarshaller();
	            graph = (ExperimentGraph) unmarshaller.unmarshal(reader);
	            graph.setProjectEntry(entry.getParent().getParent());
	            graph.setConnectionsToNodes();     // this needs to be executed every time the graph is unmarshalled
	            List children = graph.getChildren();
	            Point location = new Point (50, 100);
	            int locationY = 100;
	            for (Object node : children) {
					if (node instanceof GraphNode) {
						// if the graph is downloaded from GlycoVault, nodes do not have location set
						if (((GraphNode) node).getLocation() == null) {  
							((GraphNode) node).setLocation(location);
							location = location.translate(100, 0);
						} else {
							locationY = Math.max(locationY, ((GraphNode) node).getLocation().y );
						}
					}
				}
	            reader.close();
	            inputStream.close();
	            if (!entry.getParent().getDisplayName().equals(entry.getDisplayName())) { // parent renamed, need to rename this experiment design entry
	            	try
	    			{
	    				ProjectFileHandler.renameEntryInProject(entry, entry.getParent().getDisplayName());
	    				DataModelHandler dm = PropertyHandler.getDataModel();
	    				dm.renameEntry(entry, entry.getParent().getDisplayName());
	    			} catch (Exception e) {
	    				logger.error(e.getMessage(),e);
	    				ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), 
	    						"Unable to update project xml", e);
	    			}
	            }
				setPartName(entry.getDisplayName());
				
				reloadInputOutputNodes (entry, locationY);
			}
		} catch (IOException e) {
			ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), "Cannot create the Experiment. Using default model", e);
			logger.error(Activator.PLUGIN_ID + " Cannot create the Experiment. Using default model", e);
			graph = new ExperimentGraph();
		} catch (JAXBException e) {
			ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), "Cannot Load the Experiment. Using default model", e);
			logger.warn(Activator.PLUGIN_ID + " Cannot Load the Experiment. Using default model", e);
			graph = new ExperimentGraph();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void reloadInputOutputNodes (Entry entry, int lastYIndex) throws FileNotFoundException, JAXBException, UnsupportedEncodingException {
		// need to reload the sample to update the input to the experiment
		// check the parent entry to locate the sample
		Entry sampleEntry = entry.getParent();
		if (sampleEntry != null) {
			Sample sample;
			
			sample = SampleProperty.loadAnalyte(sampleEntry);
			List<Component> components = sample.getComponents();
			Point originalLocation = new Point (50, 10);
			List<Component> missingComponents = new ArrayList<>();
			Map<InputNode, Boolean> inputNodeMap = new HashMap<>();
			for (Iterator iterator = components.iterator(); iterator.hasNext();) {
				Component component = (Component) iterator.next();
				boolean compFound = false;
				List existingNodes = graph.getChildren();
				// we have to check whether all the existing input nodes match the new components
				for (Iterator iterator2 = existingNodes.iterator(); iterator2
						.hasNext();) {
					Object object = (Object) iterator2.next();
					if (object instanceof InputNode) {
						InputNode inputNode = (InputNode)object;
						// if it is already matched, skip it
						Boolean matchedAlready = inputNodeMap.get(inputNode);
						if (matchedAlready != null && matchedAlready) 
							continue;
						inputNodeMap.put(inputNode, new Boolean(false));   // false by default
						if (inputNode.getComponentId() == component.getComponentId()) {
							// same component exists
							compFound = true;
							inputNodeMap.remove(inputNode);
							inputNodeMap.put(inputNode, new Boolean(true));
							
							// check if the name has been changed - component might have been renamed
							if (!inputNode.getLabel().equals(component.getLabel())) {
								inputNode.setLabelModify(component.getLabel());
							}
							break;
						}
					}
				}
				if (!compFound) {
					missingComponents.add(component);
				}
			}
			// check if there are entries in the inputNodeMap that are not true
			// if so, we need to remove those input nodes from the graph
			for (Iterator iterator2 = inputNodeMap.keySet().iterator(); iterator2
					.hasNext();) {
				InputNode node = (InputNode) iterator2.next();
				Boolean exists = inputNodeMap.get(node);
				if (exists != null && exists.booleanValue() == true) {
					// keep the node
					originalLocation = node.getLocation();
				} else {
					// remove the node
					originalLocation = originalLocation.translate(-100, 0);
					List<Connection> sourceConnections = node.getSourceConnections();
					// remove the node and disconnect its connections
					boolean wasRemoved = graph.removeChild(node);
					if (wasRemoved) {
						for (Iterator iter = sourceConnections.iterator(); iter.hasNext();) {
							Connection conn = (Connection) iter.next();
							conn.disconnect();
						}
					}
				}
			}
			
			// need to add the missing components
			for (Iterator iterator = missingComponents.iterator(); iterator
					.hasNext();) {
				Component component = (Component) iterator.next();
				InputNode newNode = new InputNode();
				newNode.setLabel(component.getLabel());
				newNode.setColor(ColorConstants.red);
				newNode.setUri(component.getUri());
				newNode.setComponentId(component.getComponentId());
				newNode.setDescription(component.getDescription());
				originalLocation = originalLocation.translate(100, 0);
				newNode.setLocation(originalLocation);
				graph.addChild(newNode);
			}
			
			// do the same for the output nodes
			List<Entry> children = sampleEntry.getChildren();
			List<Entry> missingOutput = new ArrayList<>();
			originalLocation = new Point (50, lastYIndex+100);
			Map<OutputNode, Boolean> outputNodeMap = new HashMap<>();
			for (Iterator iterator = children.iterator(); iterator.hasNext();) {
				Entry outputEntry = (Entry) iterator.next();
				Property prop = outputEntry.getProperty();
				if (prop.getType() != ExperimentProperty.TYPE) {  
					// check if msEntry is a new one or if it already existed in the graph as an output node
					boolean entryFound = false;
					List existingNodes = graph.getChildren();
					// we have to check whether all the existing input nodes match the new components
					for (Iterator iterator2 = existingNodes.iterator(); iterator2
							.hasNext();) {
						Object object = (Object) iterator2.next();
						if (object instanceof OutputNode) {
							OutputNode outputNode = (OutputNode)object;
							// if it is already matched, skip it
							Boolean matchedAlready = outputNodeMap.get(outputNode);
							if (matchedAlready != null && matchedAlready) 
								continue;
							outputNodeMap.put(outputNode, new Boolean(false));   // false by default
							if (outputNode.getLabel().equalsIgnoreCase(outputEntry.getDisplayName())) {
								// same output node exists
								entryFound = true;
								outputNodeMap.remove(outputNode);
								outputNodeMap.put(outputNode, new Boolean(true));
								break;
							}
						}
					}
					if (!entryFound) {
						missingOutput.add(outputEntry);
					}
				}
			}
				
			// check if there are entries in the outputNodeMap that are not true
			// if so, we need to remove those output nodes from the graph
			for (Iterator iterator2 = outputNodeMap.keySet().iterator(); iterator2
					.hasNext();) {
				OutputNode node = (OutputNode) iterator2.next();
				Boolean exists = outputNodeMap.get(node);
				if (exists != null && exists.booleanValue() == true) {
					// keep the node
					originalLocation = node.getLocation();
				} else {
					// remove the node
					originalLocation = originalLocation.translate(-100, 0);
					List<Connection> targetConnections = node.getTargetConnections();
					// remove the node and disconnect its connections
					boolean wasRemoved = graph.removeChild(node);
					if (wasRemoved) {
						for (Iterator iter = targetConnections.iterator(); iter.hasNext();) {
							Connection conn = (Connection) iter.next();
							conn.disconnect();
						}
					}
				}
			}
			
			for (Iterator iterator2 = missingOutput.iterator(); iterator2
					.hasNext();) {
				Entry entry2 = (Entry) iterator2.next();
				OutputNode newNode = new OutputNode();
				newNode.setLabel(entry2.getDisplayName());
				newNode.setLocation(originalLocation.translate(100, 0));
				newNode.setColor(ColorConstants.lightGreen);
				graph.addChild(newNode);
			}
		} else {
			// we have a problem in design
			// need to be able to get a hold of the sample for the experiment design
			throw new RuntimeException ("Internal Error! Experiment design does not have the connection to its input!");
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}


	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}


	@Override
	protected PaletteRoot getPaletteRoot() {
		if (PALETTE_MODEL == null)
			PALETTE_MODEL = GraphEditorPaletteFactory.createPalette();
		return PALETTE_MODEL;
	}
	
	public ExperimentGraph getModel() {
		return graph;
	}

	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setContents(getModel()); // set the contents of this editor

		// listen for dropped parts
		viewer.addDropTargetListener(createTransferDropTargetListener());
		
		// we have to do this to synchronize the sizes stored in the graph nodes with the actual size of the figure shown
	    // since figure width may have been changed to fit the label to the figure during figure's placement but
        // model node is not aware of this change
		// fix graph node sizes
		EditPart root = viewer.getContents();
		for (Object node: root.getChildren()) {
			if (node instanceof GraphNodeEditPart) {
				((GraphNodeEditPart) node).updateBoundsForModel();
			}
		}
	}
	
	/** in order to handle model changes that occurs outside this editor
	 *  we have to override isDirty(). Otherwise even if we firepropertychange 
	 *  the editor is not getting marked dirty.
	 */
	@Override
	public boolean isDirty() {
		if (graphModelChanged) {
			return true;
		}
		else
			return super.isDirty();
	}
	
	public void refreshProtocolNode(ProtocolNode protocol)
	{
		if(protocol != null)
		{
			graphModelChanged = true;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}
	

	public void refreshParameter( Parameter parameter)
	{
		if(parameter != null)
		{
			graphModelChanged = true;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}
	

	public void refreshPalette( String newTemplate)
	{
		if(newTemplate != null)
		{
			PALETTE_MODEL = GraphEditorPaletteFactory.createPalette();
			getEditDomain().setPaletteRoot(getPaletteRoot());
		}
	}

	public void setGraphModelChanged(boolean b) {
		graphModelChanged = true;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	public BufferedImage getBufferedImage () {
		GraphicalViewer viewer = this.getViewer();
		LayerManager lm = (LayerManager) viewer.getEditPartRegistry().get(LayerManager.ID);
		IFigure f = lm.getLayer(LayerConstants.PRINTABLE_LAYERS);
		
		// Dimension size=f.getPreferredSize();
		Dimension size = graph.calculateSize();
		Image image = new Image(Display.getDefault(), size.width, size.height);
		GC gc = new GC(image);
		SWTGraphics graphics = new SWTGraphics(gc);
		f.paint(graphics);
		
		BufferedImage imageForPDF = ExperimentDesignRDFReport.SWTimageToAWTImage(image.getImageData());
		return imageForPDF;
	}
}
