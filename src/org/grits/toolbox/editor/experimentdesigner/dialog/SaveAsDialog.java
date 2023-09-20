package org.grits.toolbox.editor.experimentdesigner.dialog;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolCategory;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;

public class SaveAsDialog extends FormDialog {
	
	public static int PROTOCOL_VARIANT=0;
	public static int EXPERIMENT_TEMPLATE=1;
	public static int PROTOCOL_TEMPLATE=2;

	private static final Logger logger = Logger.getLogger(SaveAsDialog.class);
	
	int mode;
	Label errorLabel;
	Combo categoryCombo;
	Text nameText;
	Text descriptionText;
	Text uriText;
	String[] categoryNames = null;
	List<ProtocolCategory> protocolCategories;
	
	ControlDecoration dec;
	String name;
	String description;
	ProtocolCategory category;
	String creator;
	String protocolUri;
	
	private Combo paletteCategoryCombo;
	List<ProtocolCategory> topLevelCategories;
	private String[] topLevelCategoryNames;
	private ProtocolCategory paletteCategory;
	
	ExperimentDesignOntologyAPI api = null;
	
	public SaveAsDialog(Shell parentShell, int mode) {
		super(parentShell);
		this.mode = mode;
		if (mode == PROTOCOL_TEMPLATE) {
			try {
				api = new ExperimentDesignOntologyAPI();
				topLevelCategories = api.getTopLevelCategories();
				topLevelCategoryNames = new String[topLevelCategories.size()];
				int i=0;
				for (ProtocolCategory protocolCategory : topLevelCategories) {
					topLevelCategoryNames[i++]  = protocolCategory.getName();
				}
				
				protocolCategories = api.getProtocolCategories();
				categoryNames = new String[protocolCategories.size()];
				i=0;
				for (Iterator<ProtocolCategory> iterator = protocolCategories.iterator(); iterator
						.hasNext();) {
					ProtocolCategory protocolCategory = (ProtocolCategory) iterator
							.next();
					categoryNames[i++] = protocolCategory.getName();
				}
			} catch (Exception e1) {
				logger.error(Activator.PLUGIN_ID + " Error getting protocols from the ontology. ", e1);
				MessageDialog.openError(this.getShell(), "Error", "Error getting protocols from the ontology. Cannot continue");
				this.cancelPressed();
			}
		}
	}
	
	public SaveAsDialog(Shell parentShell, int mode, String protocolUri) {
		this (parentShell, mode);
		this.protocolUri = protocolUri;
	}
	

	public SaveAsDialog(Shell shell, int mode,
			ProtocolCategory category) {
		this (shell, mode);
		this.category = category;
	}

	public SaveAsDialog(Shell shell, int mode, ProtocolCategory category,
			ProtocolCategory topLevelCategory) {
		this (shell, mode);
		this.category = category;
		this.paletteCategory = topLevelCategory;
		if (paletteCategory != null) {
			protocolCategories = api.getProtocolCategoriesByTopLevelCategory(paletteCategory);
			categoryNames = new String[protocolCategories.size()];
			int i=0;
			for (Iterator<ProtocolCategory> iterator = protocolCategories.iterator(); iterator
					.hasNext();) {
				ProtocolCategory protocolCategory = (ProtocolCategory) iterator
						.next();
				categoryNames[i++] = protocolCategory.getName();
			}
		}
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		if (mode == PROTOCOL_VARIANT)
			mform.getForm().setText("Please provide protocol name");
		else if (mode == PROTOCOL_TEMPLATE)
			mform.getForm().setText("Please provide a protocol name, select a category and a top level category");
		else
			mform.getForm().setText("Please provide template details");
		ScrolledForm scrolledForm = mform.getForm();
		TableWrapLayout tableWrapLayout = new TableWrapLayout();
		tableWrapLayout.numColumns = 2;
		scrolledForm.getBody().setLayout(tableWrapLayout);
		
		FormToolkit toolkit = mform.getToolkit();
		
		Label nameLabel = toolkit.createLabel(scrolledForm.getBody(), "Name *", SWT.NONE);
		TableWrapData twd_nameLabel = new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1);
		twd_nameLabel.align = TableWrapData.LEFT;
		nameLabel.setLayoutData(twd_nameLabel);
		
		nameText = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
		nameText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1));
		if (name != null) nameText.setText(name);
		// Create a control decoration for the control.
		dec = new ControlDecoration(nameText, SWT.TOP | SWT.LEFT);
		// Specify the decoration image and description
		Image image = JFaceResources.getImage(DLG_IMG_MESSAGE_ERROR);
		dec.setImage(image);
		dec.setDescriptionText("Name cannot be left empty");
		dec.hide();
		
		if (mode == PROTOCOL_VARIANT) {
			Label uriLabel = toolkit.createLabel(scrolledForm.getBody(), "Protocol URI *", SWT.NONE);
			TableWrapData twd_uriLabel = new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1);
			twd_uriLabel.align = TableWrapData.LEFT;
			uriLabel.setLayoutData(twd_uriLabel);
			
			uriText = toolkit.createText(scrolledForm.getBody(), protocolUri, SWT.READ_ONLY);
			uriText.setBackground(ColorConstants.button);
			uriText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1));
		}

		if (mode == PROTOCOL_TEMPLATE) {
			Label topLevelCategoryLabel = toolkit.createLabel(scrolledForm.getBody(), "Palette Category *", SWT.NONE);
			TableWrapData twd_categoryLabel1 = new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1);
			twd_categoryLabel1.align = TableWrapData.LEFT;
			topLevelCategoryLabel.setLayoutData(twd_categoryLabel1);
			
			paletteCategoryCombo = new Combo(scrolledForm.getBody(), SWT.READ_ONLY);
			paletteCategoryCombo.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1));
			paletteCategoryCombo.setItems(topLevelCategoryNames);
			if (paletteCategory != null)
				paletteCategoryCombo.setText(paletteCategory.getName());
			else
				paletteCategoryCombo.setText(topLevelCategoryNames[0]);
			paletteCategoryCombo.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					int selected = paletteCategoryCombo.getSelectionIndex();
					if (selected != -1) {
						ProtocolCategory topLevel = topLevelCategories.get(selected);
						protocolCategories = api.getProtocolCategoriesByTopLevelCategory(topLevel);
						categoryNames = new String[protocolCategories.size()];
						int i=0;
						for (Iterator<ProtocolCategory> iterator = protocolCategories.iterator(); iterator
								.hasNext();) {
							ProtocolCategory protocolCategory = (ProtocolCategory) iterator
									.next();
							categoryNames[i++] = protocolCategory.getName();
						}
						categoryCombo.setItems(categoryNames);
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			
			Label categoryLabel = toolkit.createLabel(scrolledForm.getBody(), "Category *", SWT.NONE);
			TableWrapData twd_categoryLabel = new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1);
			twd_categoryLabel.align = TableWrapData.LEFT;
			categoryLabel.setLayoutData(twd_categoryLabel);
			
			categoryCombo = new Combo(scrolledForm.getBody(), SWT.READ_ONLY);
			categoryCombo.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1));
			categoryCombo.setItems(categoryNames);
			if (category != null)
				categoryCombo.setText(category.getName());
			else
				categoryCombo.setText(categoryNames[0]);
		}
		
		if (mode == PROTOCOL_TEMPLATE || mode == EXPERIMENT_TEMPLATE) {
			Label lblDescription = toolkit.createLabel(scrolledForm.getBody(), "Description", SWT.NONE);
			lblDescription.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 1));
			descriptionText = toolkit.createText(scrolledForm.getBody(), "", SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
			descriptionText.setTextLimit(ExperimentConfig.DESCRIPTION_LENGTH);
			TableWrapData twd_description = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 1);
			twd_description.heightHint = 60;
			descriptionText.setLayoutData(twd_description);
			if (description != null) descriptionText.setText(description);
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
		}
		
		errorLabel = toolkit.createLabel(scrolledForm.getBody(), "", SWT.NONE);
		errorLabel.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.BOTTOM, 3, 2));
		
		toolkit.paintBordersFor(scrolledForm.getBody());
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
		if (mode != PROTOCOL_VARIANT)
			description = descriptionText.getText();
		
		DataBindingContext dataBindingContext = new DataBindingContext();
		Binding binding = dataBindingContext.bindValue(
				WidgetProperties.text(SWT.Modify).observe(nameText),
			    PojoProperties.value(SaveAsDialog.class, "name").observe(this),
			    new UpdateValueStrategy()
			        .setAfterConvertValidator(new StringRequiredValidator(
			             "Name cannot be left empty",
			              dec)),
			    null);
		
		if (((Status)binding.getValidationStatus().getValue()).isOK()) {
			if (mode == PROTOCOL_TEMPLATE) {
				int selected = paletteCategoryCombo.getSelectionIndex();
				if (selected != -1) {
					if (topLevelCategories != null) {
						paletteCategory = topLevelCategories.get(selected); 
					}
					
					selected = categoryCombo.getSelectionIndex();
					if (selected != -1) {
						if (protocolCategories != null) {
							category = protocolCategories.get(selected);
							super.okPressed(); 
						}
					}
					else {
						// display errorMessage
						errorLabel.setForeground(ColorConstants.red);
						errorLabel.setText("Please select a category");
					}	
				}
				else {
					// display errorMessage
					errorLabel.setForeground(ColorConstants.red);
					errorLabel.setText("Please select a palette category");
				}	
			} else if (mode == PROTOCOL_VARIANT) {
				// check to make sure the name is unique
				List<ProtocolNode> existingTemplates;
				try {
					existingTemplates = new ExperimentDesignOntologyAPI().getProtocolsByLabel(name);
					if (existingTemplates != null && existingTemplates.size() > 0) {
						errorLabel.setForeground(ColorConstants.red);
						errorLabel.setText("Please select a unique name, a protocol template with this name exists in the system");
					} else {
						super.okPressed();
					}
				} catch (Exception e) {
					logger.error("Could not check for existing protocol templates", e);
				}
			}
			else {
				super.okPressed();
			}
		}
		else {
			// display errorMessage
			errorLabel.setForeground(ColorConstants.red);
			errorLabel.setText("Please provide a name");
		}
				
	}

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

	public ProtocolCategory getCategory() {
		return category;
	}

	public void setCategory(ProtocolCategory category) {
		this.category = category;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}


	public void setProtocolUri(String protocolUri) {
		this.protocolUri = protocolUri;
	}
	
	public ProtocolCategory getPaletteCategory() {
		return paletteCategory;
	}
	
	public void setPaletteCategory(ProtocolCategory paletteCategory) {
		this.paletteCategory = paletteCategory;
	}
}
