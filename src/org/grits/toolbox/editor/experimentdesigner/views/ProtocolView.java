package org.grits.toolbox.editor.experimentdesigner.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.impl.DirectToolItemImpl;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.e4.ui.workbench.UIEvents.UIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.part.EventPart;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolFileHandler;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.InputNode;
import org.grits.toolbox.editor.experimentdesigner.model.OutputNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.parts.GraphNodeEditPart;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@SuppressWarnings("restriction")
public class ProtocolView implements ModifyListener {
	
	public final static String VIEW_ID ="org.grits.toolbox.editor.experimentdesigner.part.protocolview";
	
	private static Logger logger = Logger.getLogger(ProtocolView.class);
	
	private FormToolkit toolkit;
	private ScrolledForm scrolledForm;
	
	private Text name;
	private Text description;
	private Text creator;
	private Text protocolCategory;
	private Text template;
	private Text templateUri;
	private Text uri;
	private Text url;
	private Text file;
	
	private ProtocolNode protocol;
	
	@Inject IEventBroker eventBroker;
	GraphEditor editor;
	
	Button download;

	private Entry projectEntry;

	private Button delete;

	private Button browse;

	private DirectToolItemImpl saveVariantToolItem;

	private DirectToolItemImpl saveTemplateToolItem;
	
	public ProtocolNode getProtocol() {
		return protocol;
	}

	public Entry getProjectEntry() {
		return projectEntry;
	}

	public ProtocolView() {
	}

	@PostConstruct
	public void createPartControl(final Composite parent, final MPart part, EModelService modelService) {
		toolkit = new FormToolkit(parent.getDisplay());
		scrolledForm = toolkit.createScrolledForm(parent);
		{
			TableWrapLayout tableWrapLayout = new TableWrapLayout();
			tableWrapLayout.numColumns = 5;
			scrolledForm.getBody().setLayout(tableWrapLayout);
		}
		
		Label lblName = toolkit.createLabel(scrolledForm.getBody(), "Name", SWT.NONE);
		lblName.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		name = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
		name.setTextLimit(PropertyHandler.LABEL_TEXT_LIMIT);
		name.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		name.addModifyListener(this);
		
		Label lblUrl = toolkit.createLabel(scrolledForm.getBody(), "URL", SWT.NONE);
		lblUrl.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		url = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
		url.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		url.addModifyListener(this);
		
		Label lblFile = toolkit.createLabel(scrolledForm.getBody(), "File", SWT.NONE);
		lblFile.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		file = toolkit.createText(scrolledForm.getBody(), "", SWT.READ_ONLY);
		TableWrapData twd_file = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP, 1, 1);
		twd_file.grabHorizontal = true;
		file.setLayoutData(twd_file);
		
		browse = toolkit.createButton(scrolledForm.getBody(), "Select File", SWT.PUSH);
		browse.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		browse.setToolTipText("Select a file from the file system to upload");
		browse.setEnabled(false);
		browse.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// open up a file dialog to upload the file
				FileDialog fd = new FileDialog(parent.getDisplay().getActiveShell(), SWT.OPEN);
		        fd.setText("Upload");
		        String selected = fd.open();
		        String filename;
				try {
					if (selected != null && selected.trim().length() != 0) {
						filename = uploadFile (file.getText(), selected);
						// set the selected file name into file text field
				        if (protocol != null) {
				        	protocol.setFile(filename);
				        	file.setText(filename);
				        	eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
				        	//sourceProvider.detailsChanged();
				        	delete.setEnabled(true);
				        	download.setEnabled(true);
				        }
					}
				} catch (IOException e1) {
					logger.error("Could not upload file", e1);
					MessageDialog.openError(parent.getDisplay().getActiveShell(), "File Upload Error", "Could not upload the file into workspace");
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		delete = toolkit.createButton(scrolledForm.getBody(), "Remove", SWT.PUSH);
		delete.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		delete.setToolTipText("Delete the file associated with the protocol");
		delete.setEnabled(false);
		delete.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				String filename = file.getText();
				if (filename != null && filename.length() != 0) {
					deleteFile(filename);
					file.setText("");
					protocol.setFile(null);
					eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
					//sourceProvider.detailsChanged();
					delete.setEnabled(false);
					download.setEnabled(false);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		download = toolkit.createButton(scrolledForm.getBody(), "Download", SWT.PUSH);
		download.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		download.setToolTipText("Download the file into your computer");
		download.setEnabled(false);
		download.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (protocol.getFile() != null) {
					// open up a file dialog to download the file
					FileDialog fd = new FileDialog(parent.getDisplay().getActiveShell(), SWT.SAVE);
			        fd.setText("Download");
			        fd.setFileName(protocol.getFile());
			        fd.setOverwrite(true);
			        String selected = fd.open();
					try {
						if (selected != null && selected.trim().length() != 0) {
							downloadFile (protocol.getFile(), selected);
						}
					} catch (IOException e1) {
						logger.error("Could not download file", e1);
						MessageDialog.openError(parent.getDisplay().getActiveShell(), "File Download Error", "Could not download the file. It has been removed from the workspace");
					}
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Label lblDescription = toolkit.createLabel(scrolledForm.getBody(), "Description", SWT.NONE);
		lblDescription.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		description = toolkit.createText(scrolledForm.getBody(), "", SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		description.setTextLimit(ExperimentConfig.DESCRIPTION_LENGTH);
		TableWrapData twd_description = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4);
		twd_description.heightHint = 60;
		description.setLayoutData(twd_description);
		description.addModifyListener(this);
		// have to do this for Windows environment, works on Mac without the listener
		description.addKeyListener( new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {

			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL && e.keyCode == 'a') {
					((Text)e.widget).selectAll();
				}
				
			}
		});
		
		Label lblCreator = toolkit.createLabel(scrolledForm.getBody(), "Creator", SWT.NONE);
		lblCreator.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		creator = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
		creator.setTextLimit(PropertyHandler.LABEL_TEXT_LIMIT);
		creator.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		creator.addModifyListener(this);
		
		Label lblCategory = toolkit.createLabel(scrolledForm.getBody(), "Category", SWT.NONE);
		lblCategory.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		protocolCategory = toolkit.createText(scrolledForm.getBody(), "", SWT.READ_ONLY);
		protocolCategory.setBackground(ColorConstants.button);
		protocolCategory.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		
		Label lblTemplate = toolkit.createLabel(scrolledForm.getBody(), "Template", SWT.NONE);
		lblTemplate.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		template = toolkit.createText(scrolledForm.getBody(), "", SWT.READ_ONLY);
		template.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		template.setBackground(ColorConstants.button);
		
		Label lbltemplateURI = toolkit.createLabel(scrolledForm.getBody(), "Template URI", SWT.NONE);
		lbltemplateURI.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		templateUri = toolkit.createText(scrolledForm.getBody(), "", SWT.READ_ONLY);
		templateUri.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		templateUri.setBackground(ColorConstants.button);
		
		Label lblURI = toolkit.createLabel(scrolledForm.getBody(), "Protocol URI", SWT.NONE);
		lblURI.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		uri = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
		uri.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 4));
		uri.addModifyListener(this);
		
		toolkit.paintBordersFor(scrolledForm.getBody());
		
		part.getContext().set(ProtocolView.class, this);  
		
		saveVariantToolItem = (DirectToolItemImpl) modelService.find("org.grits.toolbox.editor.experimentdesigner.directtoolitem.savethisprotocolasavariant", part.getToolbar());
		saveTemplateToolItem = (DirectToolItemImpl) modelService.find("org.grits.toolbox.editor.experimentdesigner.directtoolitem.savethisprotocolasanewtemplate", part.getToolbar());
		
		eventBroker.subscribe(UIElement.TOPIC_TOBERENDERED, closePartEventHandler);
	}
	
	private final EventHandler closePartEventHandler = new EventHandler() {
		void clearFields () {
			if (name != null && !name.isDisposed()) {
				name.removeModifyListener(ProtocolView.this);
				description.removeModifyListener(ProtocolView.this);
				creator.removeModifyListener(ProtocolView.this);
				url.removeModifyListener(ProtocolView.this);
				uri.removeModifyListener(ProtocolView.this);
				
				cleanUp();
				
				name.addModifyListener(ProtocolView.this);
		    	description.addModifyListener(ProtocolView.this);
		    	creator.addModifyListener(ProtocolView.this);
		    	uri.addModifyListener(ProtocolView.this);
		    	url.addModifyListener(ProtocolView.this);
			}
		}
		
		@Override
		public void handleEvent(Event event) {
			Object part = event.getProperty(UIEvents.EventTags.ELEMENT);
			if (part == null)
				return;
			boolean toBeRendered = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
			if (!toBeRendered && part instanceof MPart) {
				if (((MPart) part).getObject() instanceof CompatibilityEditor) {
					if (((CompatibilityEditor) ((MPart) part).getObject()).getEditor() instanceof GraphEditor) {
						clearFields();
					}
				}
			}
		}
	};
	
	@Inject
	@Optional
	public void subscribeTopicPartActivation(@UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) org.osgi.service.event.Event event, IEclipseContext eclipseContext) {
		  Object element = event.getProperty(EventTags.ELEMENT);
		  if (!(element instanceof MPart)) {
		    return;
		  }
		  
		  MPart part = (MPart) element;
		  if (part.getObject() instanceof CompatibilityEditor) {
			  if (((CompatibilityEditor) part.getObject()).getEditor() instanceof GraphEditor) {
				  editor = (GraphEditor)((CompatibilityEditor) part.getObject()).getEditor();
				  logger.debug("ProtocolView - Active Graph Editor changed to: " + editor.getPartName());
			  }
		  }
	}
	
	@PreDestroy
	public void preDestroy(MPart part)
	{
		ContextInjectionFactory.uninject(this, part.getContext());
		//partService.removePartListener(partListener);
		eventBroker.unsubscribe(closePartEventHandler);
	}
	
	protected void downloadFile(String file, String newPath) throws IOException {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator;
        if (projectEntry != null) 
        	projectFolderLocation += projectEntry.getDisplayName();
        String uploadFolderLocation = projectFolderLocation
                + File.separator
                + "files";
        
        File workspaceFile = new File(uploadFolderLocation + File.separator + file);
        FileOutputStream out = new FileOutputStream(newPath);
        Files.copy(workspaceFile.toPath(), out);
		out.close();
	}
	
	private boolean deleteFile (String filename) {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator;
        if (projectEntry != null) 
        	projectFolderLocation += projectEntry.getDisplayName();
        String fileFolderLocation = projectFolderLocation
                + File.separator
                + "files";
        
        File fileToBeRemoved = new File (fileFolderLocation + File.separator + filename);
        if (fileToBeRemoved.exists()) {
        	return fileToBeRemoved.delete();
        }
        
        return false;
	}

	private String uploadFile (String oldFile, String path) throws IOException {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator;
        if (projectEntry != null) 
        	projectFolderLocation += projectEntry.getDisplayName();
        String uploadFolderLocation = projectFolderLocation
                + File.separator
                + "files";
        
        File uploadFolder = new File (uploadFolderLocation);
        if (!uploadFolder.exists()) {
        	uploadFolder.mkdirs();
        }
        
        if (oldFile != null && oldFile.length() != 0) {
        	// delete the old file
        	File old = new File (uploadFolderLocation + File.separator + oldFile);
        	if (old.exists()) old.delete();
        }
        
        File original = new File(path);
        
        File destinationFile = new File(uploadFolderLocation + File.separator + original.getName());
        String f = original.getName();
		while (destinationFile.exists()) { // do not overwrite, create a new version
			f = ProtocolFileHandler.generateUniqueFileName(f, uploadFolder.list());
			destinationFile = new File(uploadFolderLocation + File.separator + f);
		}
        FileOutputStream uploadedFile = new FileOutputStream(destinationFile);
		Files.copy(original.toPath(), uploadedFile);
		uploadedFile.close();
        return destinationFile.getName(); 
	}
	
	public void setProtocol(ProtocolNode node) {
		this.protocol = node;
	}

	@Override
	public void modifyText(ModifyEvent e) {	
		if (editor == null) {
			logger.error("Cannot get the reference to Graph Editor");
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Update Error", "Cannot get the reference to Graph Editor");
			return;
		}
		Text newText = (Text) e.widget;
		String newValue = newText.getText();
		if (newValue != null)
			newValue = newValue.trim();
		if (protocol != null) {
			if (e.getSource().equals(name)) {
				protocol.setLabelModify(newValue);
				eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
				editor.refreshProtocolNode(protocol);
				//sourceProvider.detailsChanged();
			}
			else if (e.getSource().equals(description)) {
				protocol.setDescription(newValue);
				eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
				editor.refreshProtocolNode(protocol);
				//sourceProvider.detailsChanged();
			} else if (e.getSource().equals(creator)) {
				protocol.setCreator(newValue);
				eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
				editor.refreshProtocolNode(protocol);
				//sourceProvider.detailsChanged();
			} else if (e.getSource().equals(url)) {
				protocol.setUrl(newValue);
				eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
				editor.refreshProtocolNode(protocol);
				//sourceProvider.detailsChanged();
			} else if (e.getSource().equals(uri)) {
				protocol.setUri(newValue);
				eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
				editor.refreshProtocolNode(protocol);
				//sourceProvider.detailsChanged();
			}
		}
	}

	@Optional
	@Inject
	void setSelection(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection) {
		if (selection != null && !(selection instanceof StructuredSelection))
			return;
		
		if (name == null || name.isDisposed())
			// do nothing
			return;
		
		
		if (selection == null || !(((StructuredSelection) selection).getFirstElement() instanceof GraphNodeEditPart)) {
			cleanUp();
		}
		else if (((StructuredSelection) selection).getFirstElement() != null && ((StructuredSelection) selection).getFirstElement() instanceof GraphNodeEditPart)
		{
			name.removeModifyListener(this);
			description.removeModifyListener(this);
			url.removeModifyListener(this);
			creator.removeModifyListener(this);
			uri.removeModifyListener(this);
			
			Object modelObject = ((GraphNodeEditPart)((StructuredSelection) selection).getFirstElement()).getModel();
			if (modelObject instanceof ProtocolNode) {
				// find the project entry
				ExperimentGraph graph = (ExperimentGraph)((GraphNodeEditPart)((StructuredSelection) selection).getFirstElement()).getParent().getModel();
				projectEntry = graph.getProjectEntry();
				 
				protocol = (ProtocolNode)modelObject;
				if (protocol.getTemplate() != null)
					template.setText(protocol.getTemplate());
				else
					template.setText("");
				if (protocol.getDescription() != null)
					description.setText(protocol.getDescription());
				else
					description.setText("");
				if (protocol.getCategory() != null)
					protocolCategory.setText(protocol.getCategory().getName());
				else 
					protocolCategory.setText("");
				if (protocol.getLabel() != null)
					name.setText(protocol.getLabel());
				else
					name.setText("");
				if (protocol.getCreator()  != null)
					creator.setText(protocol.getCreator());
				else
					creator.setText("");
				if (protocol.getTemplateUri() != null) {
					if (!protocol.getTemplateUri().equals(protocol.getUri()))
						uri.setText(protocol.getUri() == null ? "" : protocol.getUri());
					templateUri.setText(protocol.getTemplateUri());
				}
				else {
					uri.setText(protocol.getUri() == null ? "" : protocol.getUri());
					templateUri.setText("");
				}
				if (protocol.getUrl() != null)
					url.setText(protocol.getUrl());
				else
					url.setText("");
				if (protocol.getFile() != null) {
					file.setText(protocol.getFile());
					delete.setEnabled(true);
					download.setEnabled(true);
				}
				else
					file.setText("");
				browse.setEnabled(true);
				saveVariantToolItem.setEnabled(true);
				saveTemplateToolItem.setEnabled(true);
			} else if (modelObject instanceof OutputNode || modelObject instanceof InputNode) {
				name.setText(((GraphNode)modelObject).getLabel());
				if (((GraphNode)modelObject).getDescription() != null)
					description.setText(((GraphNode)modelObject).getDescription());
				else
					description.setText("");
				protocolCategory.setText("");
				template.setText("");
				creator.setText("");
				templateUri.setText("");
				file.setText("");
				url.setText("");
				protocol = null;
				download.setEnabled(false);
				delete.setEnabled(false);
				browse.setEnabled(false);
				saveVariantToolItem.setEnabled(false);
				saveTemplateToolItem.setEnabled(false);
			}
			else {
				cleanUp();
			}
			name.addModifyListener(this);
	    	description.addModifyListener(this);
	    	creator.addModifyListener(this);
	    	url.addModifyListener(this);
	    	uri.addModifyListener(this);
		}
		
		
	}
	
	void cleanUp () {
		protocol = null;
    	
		// clear out the form fields
    	name.setText("");
    	description.setText("");
    	template.setText("");
    	protocolCategory.setText("");
    	creator.setText("");
    	templateUri.setText("");
    	url.setText("");
    	uri.setText("");
    	file.setText("");
    	
		download.setEnabled(false);
		delete.setEnabled(false);
		browse.setEnabled(false);
		saveVariantToolItem.setEnabled(false);
		saveTemplateToolItem.setEnabled(false);
	}
	
	public void templateChanged (String newTemplate, String newTemplateUri) {
		template.setText(newTemplate);
		templateUri.setText(newTemplateUri);
	}
	
	public void templateAdded (String templateURI) {
		eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, templateURI);
		if (editor == null) {
			logger.error("Cannot get a reference to GraphEditor");
			return;
		}
		editor.refreshPalette(templateURI);
	}

	
	@Optional
	@Inject
	void refreshTemplate(
			@UIEventTopic(EventPart.EVENT_TOPIC_VALUE_MODIFIED) String newTemplate)
	{
		// clear template value
		template.setText("");
		templateUri.setText("");
	}
}
