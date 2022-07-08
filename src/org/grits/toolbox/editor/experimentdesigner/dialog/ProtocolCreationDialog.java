package org.grits.toolbox.editor.experimentdesigner.dialog;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;

public class ProtocolCreationDialog extends FormDialog {
	
	private static final Logger logger = Logger.getLogger(ProtocolCreationDialog.class);
	
	private GraphNode templateNode;
	private Text nameText;
	private Text descriptionText;
	
	Label errorLabel;	
	ControlDecoration dec;
	
	String name="";
	String description;
	
	ProtocolNode existing;
	
	Shell shell;
	
	ListDialog dialog;
	Text selectedProtocolDescription;

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

	public ProtocolNode getExisting() {
		return existing;
	}

	public void setExisting(ProtocolNode existing) {
		this.existing = existing;
	}

	public ProtocolCreationDialog(Shell shell, GraphNode node) {
		super(shell);
		this.templateNode = node;
		this.shell = shell;
	}
	
	@Override
	protected void createFormContent(IManagedForm mform) {
			
		mform.getForm().setText("Please select a protocol");
		ScrolledForm scrolledForm = mform.getForm();
		TableWrapLayout tableWrapLayout = new TableWrapLayout();
		tableWrapLayout.numColumns = 3;
		scrolledForm.getBody().setLayout(tableWrapLayout);
		
		FormToolkit toolkit = mform.getToolkit();
		
		Label nameLabel = toolkit.createLabel(scrolledForm.getBody(), "Name *", SWT.NONE);
		TableWrapData twd_nameLabel = new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1);
		twd_nameLabel.align = TableWrapData.LEFT;
		nameLabel.setLayoutData(twd_nameLabel);
		if (templateNode != null) 
			nameText = toolkit.createText(scrolledForm.getBody(), templateNode.getLabel(), SWT.NONE);
		else 
			nameText = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
		nameText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1));
		// Create a control decoration for the control.
		dec = new ControlDecoration(nameText, SWT.TOP | SWT.LEFT);
		// Specify the decoration image and description
		Image image = JFaceResources.getImage(DLG_IMG_MESSAGE_ERROR);
		dec.setImage(image);
		dec.setDescriptionText("Name cannot be left empty");
		dec.hide();
		
		Button btnBrowseButton = new Button(scrolledForm.getBody(), SWT.PUSH);
		btnBrowseButton.setText("Browse");
		btnBrowseButton.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected(SelectionEvent event) 
			{
				dialog = new ListDialog(shell) {
					@Override
					protected void okPressed() {
						super.okPressed();
						Object[] result = getResult();
						for (int i = 0; i < result.length; i++) {
							existing = (ProtocolNode)result[i];
							nameText.setText(existing.getLabel());
							descriptionText.setText(existing.getDescription());
						}
					}
					
					@Override
					protected Control createDialogArea(Composite container) {
						Composite parent =  (Composite)super.createDialogArea(container);
						// add a textarea to display the description of the selected protocols
						GridData descriptionTextData = new GridData();
						descriptionTextData.minimumHeight = 80;
						descriptionTextData.grabExcessHorizontalSpace = true;
						descriptionTextData.grabExcessVerticalSpace = true;
						descriptionTextData.horizontalAlignment = GridData.FILL;
						descriptionTextData.horizontalSpan = 3;
						selectedProtocolDescription = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
						selectedProtocolDescription.setLayoutData(descriptionTextData);
						selectedProtocolDescription.setEditable(false);
						return parent;
					}
				};
				dialog.setContentProvider(new ArrayContentProvider());
				dialog.setTitle("Protocol Selection");
				dialog.setLabelProvider(new ArrayLabelProvider());
				List<ProtocolNode> input = new ArrayList<ProtocolNode>(); 
				
				try {
					if (templateNode != null && ((ProtocolNode) templateNode).getTemplateUri() != null) {
						input = new ExperimentDesignOntologyAPI().getProtocolVariantsByUri (((ProtocolNode) templateNode).getTemplateUri());
					} else {
						input = new ExperimentDesignOntologyAPI().getAllProtocolVariants();
					}
				} catch (Exception e) {
					logger.warn (Activator.PLUGIN_ID + " Cannot load the protocol variants! ", e);
				}
				
				dialog.setInput(input);
				dialog.setBlockOnOpen(false);
				dialog.open();
				
				dialog.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {			
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						Object object = event.getSelection();
						Object node = ((StructuredSelection)object).getFirstElement();
						selectedProtocolDescription.setText(((ProtocolNode)node).getDescription());
						
					}
				});
			}
		});
		toolkit.adapt(btnBrowseButton, true, true);
		
		Label lblDescription = toolkit.createLabel(scrolledForm.getBody(), "Description", SWT.NONE);
		lblDescription.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		descriptionText = toolkit.createText(scrolledForm.getBody(), "", SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		descriptionText.setTextLimit(ExperimentConfig.DESCRIPTION_LENGTH);
		TableWrapData twd_description = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1);
		twd_description.heightHint = 60;
		descriptionText.setLayoutData(twd_description);
		// have to do this for Windows environment, works on Mac without the listener
		descriptionText.addKeyListener( new KeyListener() {
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
		
		
		if (templateNode != null && templateNode.getDescription() != null) 
			descriptionText.setText(templateNode.getDescription());
		
		Label empty = toolkit.createLabel(scrolledForm.getBody(), "", SWT.NONE);
		empty.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
		
		errorLabel = toolkit.createLabel(scrolledForm.getBody(), "", SWT.NONE);
		errorLabel.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.BOTTOM, 3, 3));
		
		toolkit.paintBordersFor(scrolledForm.getBody());
	}
	
	static class ArrayLabelProvider extends LabelProvider implements ITableLabelProvider{
		public String getText(Object element) {
			return ((ProtocolNode)element).getLabel();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return ((ProtocolNode)element).getLabel();
		}
		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			super.dispose();
		}
		
	}
	
	class StringRequiredValidator implements IValidator {
		 
	    private final String errorText;
	    private final ControlDecoration controlDecoration;
	 
	    public StringRequiredValidator(String errorText,
	        ControlDecoration controlDecoration) {
	        super();
	        this.errorText = errorText;
	        this.controlDecoration = controlDecoration;
	    }
	 
	    public IStatus validate(Object value) {
	        if (value instanceof String) {
	            String text = (String) value;
	            if (text.trim().length() == 0) {
	                controlDecoration.show();
	                return ValidationStatus
	                        .error(errorText);
	            }
	        }
	        controlDecoration.hide();
	        errorLabel.setText("");
	        return Status.OK_STATUS;
	    }
	}
	
	@Override
	protected void okPressed() {
		name = nameText.getText();
		description = descriptionText.getText();
		
		DataBindingContext dataBindingContext = new DataBindingContext();
		Binding binding = dataBindingContext.bindValue(
			    SWTObservables.observeText(nameText, SWT.Modify),
			    PojoProperties.value(ProtocolCreationDialog.class, "name").observe(this),
			    new UpdateValueStrategy()
			        .setAfterConvertValidator(new StringRequiredValidator(
			             "Name cannot be left empty",
			              dec)),
			    null);
		
		if (((Status)binding.getValidationStatus().getValue()).isOK()) {
			// check if the name is unique
			try {
				if (templateNode == null || templateNode.getLabel() == null) {
					if (existing == null) { // no protocol variant is chosen
						List<ProtocolNode> existingTemplates = new ExperimentDesignOntologyAPI().getProtocolsByLabel(name);
						if (existingTemplates != null && existingTemplates.size() > 0) {
							// a protocol template with this name already exists, do not allow to create a new node with
							// the same name
							// display errorMessage
							errorLabel.setForeground(ColorConstants.red);
							errorLabel.setText("Please provide a unique name. A protocol template with this name already exists!");
							return;
						}
					}
				}
			} catch (Exception e) {
				logger.warn (Activator.PLUGIN_ID + " Cannot retrieve the protocol templates! ", e);
			}
			super.okPressed();
		}
		else {
			// display errorMessage
			errorLabel.setForeground(ColorConstants.red);
			errorLabel.setText("Please provide a name");
		}
			
	}
	
	
}
