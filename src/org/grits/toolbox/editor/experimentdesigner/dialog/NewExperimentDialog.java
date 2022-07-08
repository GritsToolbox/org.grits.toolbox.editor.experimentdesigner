package org.grits.toolbox.editor.experimentdesigner.dialog;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.dialog.ModalDialog;
import org.grits.toolbox.core.datamodel.dialog.ProjectExplorerDialog;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.handler.NewExperimentHandler;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentTemplateEntry;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.grits.toolbox.entry.sample.property.SampleProperty;

/**
 * Dialog for creating a new experiment
 *
 */
public class NewExperimentDialog extends ModalDialog{
	
	private static final Logger logger = Logger.getLogger(NewExperimentDialog.class);
	
	private Text sampleNameText;
	private Label sampleNameLabel;
	
	private Entry sampleEntry = null;
	private Entry experimentSelected = null;
	private ExperimentTemplateEntry templateFile=null;
	private Entry toBeRemoved = null;
	
	protected Composite parent = null;
	Button copyFromButton= null;
	Button OKbutton;
	ComboViewer selectedTemplateCombo;
	Button fromTemplateRadio;
	
	Text existingDesignText;

	
	
	public NewExperimentDialog(Shell parentShell) {
		super(parentShell);
	}

    @Override
	public void create()
	{
		super.create();
		setTitle("New Experiment Design");
		setMessage("To create a new experiment design");
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		this.parent = parent;
		
		//has to be gridLayout, since it extends TitleAreaDialog
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.verticalSpacing = 10;
		parent.setLayout(gridLayout);
		
		/*
		 * First row starts: create sample textfield with a browse button
		 */
		GridData sampleNameData = new GridData();
		sampleNameLabel = new Label(parent, SWT.NONE);
		sampleNameLabel.setText("Sample");
		sampleNameLabel = setMandatoryLabel(sampleNameLabel);
		sampleNameLabel.setLayoutData(sampleNameData);
		
		GridData projectnameTextData = new GridData();
		projectnameTextData.grabExcessHorizontalSpace = true;
		projectnameTextData.horizontalAlignment = GridData.FILL;
		projectnameTextData.horizontalSpan = 1;
		sampleNameText = new Text(parent, SWT.BORDER);
		sampleNameText.setTextLimit(PropertyHandler.LABEL_TEXT_LIMIT);
		sampleNameText.setLayoutData(projectnameTextData);
		//for the first time if an entry was chosen by a user
		if(sampleEntry != null)
		{
			sampleNameText.setText(sampleEntry.getDisplayName());
		}
		sampleNameText.setEditable(false);
		
		// browse button
		GridData browseButtonData = new GridData();
		Button button = new Button(parent, SWT.PUSH);
		button.setText("Browse");
		button.setLayoutData(browseButtonData);
		button.addSelectionListener(new SelectionAdapter() 
		{
			public void widgetSelected(SelectionEvent event) 
			{
				Shell newShell = new Shell(parent.getShell(),SWT.PRIMARY_MODAL | SWT.SHEET);
				ProjectExplorerDialog dlg = new ProjectExplorerDialog(newShell);
				// Set the parent as a filter
				dlg.addFilter(SampleProperty.TYPE);
				// Change the title bar text
				dlg.setTitle("Sample Selection");
				// Customizable message displayed in the dialog
				dlg.setMessage("Choose a sample");
				// Calling open() will open and run the dialog.
				if (dlg.open() == Window.OK) {
					Entry selected = dlg.getEntry();
					if (selected != null) {
						sampleEntry = selected;
						// Set the text box as the sample text
						sampleNameText.setText(sampleEntry.getDisplayName());
						if (copyFromButton != null) copyFromButton.setEnabled(true);
					}
				}
			}
		});
		
		//then add separator
		createSeparator(4);
		
		Label label1 = new Label(parent, SWT.NONE);
		label1.setText ("Create Experiment Design: ");
		GridData gd = new GridData();
        gd.horizontalSpan = 4;
		label1.setLayoutData(gd); 
		
		Label emptyLabel0 = new Label(parent, SWT.NONE);
		emptyLabel0.setText ("");
	    gd = new GridData();
        gd.horizontalSpan = 1;
        emptyLabel0.setLayoutData(gd);
		
		Button radioButton1 = new Button(parent, SWT.RADIO);
		radioButton1.setSelection(true);
		radioButton1.setText("New Design");
		radioButton1.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.getSource();
				if (button.getSelection())  
					OKbutton.setEnabled(true);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Button button = (Button) e.getSource();
				if (button.getSelection())  
					OKbutton.setEnabled(true);
			}
		});
		
		Label emptyLabel = new Label(parent, SWT.NONE);
		emptyLabel.setText ("");
		gd = new GridData();
        gd.horizontalSpan = 2;
        emptyLabel.setLayoutData(gd);
 
        Label emptyLabel1 = new Label(parent, SWT.NONE);
        emptyLabel1.setText ("");
		gd = new GridData();
        gd.horizontalSpan = 1;
        emptyLabel1.setLayoutData(gd);
        
		fromTemplateRadio = new Button(parent, SWT.RADIO);
		fromTemplateRadio.setSelection(false);
		fromTemplateRadio.setText("From Template");
		fromTemplateRadio.setEnabled(false);
			
		fromTemplateRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.getSource();
				if (!button.getSelection())  // not selected, do nothing
					return;
				if (selectedTemplateCombo != null) {
					ISelection selection = selectedTemplateCombo.getSelection();
					if (selection instanceof IStructuredSelection) {
						Object s = ((IStructuredSelection) selection).getFirstElement();
						if (s instanceof ExperimentTemplateEntry) {
							templateFile = (ExperimentTemplateEntry) s;
							setReturnCode (NewExperimentHandler.TEMPLATE);	
							OKbutton.setEnabled(true);
						}
					}
					else {
						OKbutton.setEnabled(false);
					}
				}
			}
		});
		
		selectedTemplateCombo = new ComboViewer(parent, SWT.READ_ONLY);
		selectedTemplateCombo.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List)
					return ((List) inputElement).toArray();
				return null;
			}
		});
		selectedTemplateCombo.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((ExperimentTemplateEntry)element).getName();
			}
		});
		selectedTemplateCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		try {
			List<ExperimentTemplateEntry> templates = new ExperimentDesignOntologyAPI().getAllExperimentTemplateEntries();
			if (templates == null || templates.isEmpty()) {
				selectedTemplateCombo.getCombo().setEnabled(false);
				fromTemplateRadio.setEnabled(false);
			}
			else {
				selectedTemplateCombo.setInput(templates);
				selectedTemplateCombo.getCombo().setEnabled(true);
				selectedTemplateCombo.getCombo().select(0);
				fromTemplateRadio.setEnabled(true);
			}
		
		} catch (Exception e) {
			logger.warn(Activator.PLUGIN_ID + " Cannot load the existing templates! " + e);
			MessageDialog.openInformation(parent.getShell(), "Warning", "Cannot retrieve experiment design templates: " + e.getMessage());
			selectedTemplateCombo.getCombo().setEnabled(false);
		}
		
		selectedTemplateCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object s = ((IStructuredSelection) selection).getFirstElement();
					if (s instanceof ExperimentTemplateEntry) {
						templateFile = (ExperimentTemplateEntry) s;
						if (fromTemplateRadio.getSelection()) {
							setReturnCode(NewExperimentHandler.TEMPLATE);
						}
					}
				}
			}
		});
		
		Label emptyLabel2 = new Label(parent, SWT.NONE);
		emptyLabel2.setText ("");
		gd = new GridData();
        gd.horizontalSpan = 1;
        emptyLabel2.setLayoutData(gd);
        
        Label emptyLabel3 = new Label(parent, SWT.NONE);
        emptyLabel3.setText ("");
		gd = new GridData();
        gd.horizontalSpan = 1;
        emptyLabel3.setLayoutData(gd);
 
		copyFromButton = new Button(parent, SWT.RADIO);
		copyFromButton.setSelection(false);
		copyFromButton.setText("Copy existing design");
		copyFromButton.setToolTipText("Copy from an existing experiment design");

		copyFromButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Button button = (Button) event.getSource();
				if (!button.getSelection())  // not selected, do nothing
					return;
				OKbutton.setEnabled(false);   // disabled until a selection is made
				Shell newShell = new Shell(parent.getShell(),SWT.PRIMARY_MODAL | SWT.SHEET);
				
				// in order to change the filtering mechanism, overriding ProjectExplorerDialog
				ProjectExplorerDialog dlg = new ProjectExplorerDialog(newShell) {
					
					Entry selectedEntry = null;
					
					@Override
					protected void okPressed() {
						experimentSelected = selectedEntry;
						super.okPressed();
					}
					@Override
					public Control createDialogArea(Composite parent) {
						super.createDialogArea(parent);
						getViewer().addSelectionChangedListener(
							new ISelectionChangedListener() {
								@Override
								public void selectionChanged(SelectionChangedEvent event) {
									ISelection selection = event.getSelection();
									
									if(selection.isEmpty()) return;
									
									//convert it to TreeSelection object type
									TreeSelection to = (TreeSelection)selection;
									
									//check if only one is selected!
									if (to.size() == 1)
									{
										// allow only sample entries with an existing experiment design
										Entry node = (Entry)to.getFirstElement();
										boolean match = false;
										
										if(node.getProperty().getType().equals(SampleProperty.TYPE))
										{
											// if the selected sample is the same as the sampleEntry, do not allow
											if (!node.equals(sampleEntry)) {
												List<Entry> children = node.getChildren();
												for (Iterator<Entry> iterator = children
														.iterator(); iterator
														.hasNext();) {
													Entry entry = (Entry) iterator
															.next();
													if (entry.getProperty().getType().equals(ExperimentProperty.TYPE)) {
														match = true;
														selectedEntry = entry;
														break;
													}
												}
											}
										}
									
										if(match)
										{
											//then enables OK button
											setEnabledOKbutton(true);
										}
										else
										{
											setEnabledOKbutton(false);
										}
									}
									else
									{
										setEnabledOKbutton(false);
									}
								}	
								
							}
						);
						return parent;
					}
					
					public void setEnabledOKbutton (boolean enabled) {
						getButton(Dialog.OK).setEnabled(enabled);
					}
					
				};
				// Change the title bar text
				dlg.setTitle("Experiment Design Selection");
				// Customizable message displayed in the dialog
				dlg.setMessage("Choose an experiment design");
				// Calling open() will open and run the dialog.
				dlg.open();
				if (experimentSelected != null) {
					setReturnCode(NewExperimentHandler.COPY);
					existingDesignText.setText(experimentSelected.getDisplayName());
					OKbutton.setEnabled(true);
				}
			}
		});
		
				
		existingDesignText = new Text(parent, SWT.SEARCH | SWT.FILL);
		existingDesignText.setEditable(false);
		existingDesignText.setText("");
		gd = new GridData();
        gd.horizontalSpan = 1;
        gd.widthHint=100;
        existingDesignText.setLayoutData(gd);
        
        Label designLabel = new Label(parent, SWT.FILL);
		designLabel.setText("");
		gd = new GridData();
        gd.horizontalSpan = 1;
		designLabel.setLayoutData(gd);

		
		if (sampleEntry == null) {
			copyFromButton.setEnabled(false);
		}
		
		/*
		 * Last buttons area
		 */
		/*createLoadButton (parent);
		
		if (sampleEntry == null) {
			// disable the button
			loadButton.setEnabled(false);
		}*/
		
		new Label(parent, SWT.NONE);
		new Label(parent, SWT.NONE);
		createCANCELButton(parent);
		createOKButton(parent);
		
		return parent;
	}

	protected Button createCANCELButton(final Composite parent2) {
		//create a gridData for CANCEL button
		GridData cancelData = new GridData();
		cancelData.horizontalAlignment = SWT.END;
		cancelData.grabExcessHorizontalSpace = true;
		cancelData.horizontalSpan = 1;
		Button CancelButton = new Button(parent2, SWT.PUSH);
		CancelButton.setText("Cancel");
		CancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setReturnCode(CANCEL);
				close();
			}
		});
		CancelButton.setLayoutData(cancelData);
		return CancelButton;
	}
	
	protected Button createOKButton(final Composite parent2) {
		//create a gridData for OKButton
		GridData okData = new GridData();
		//okData.grabExcessHorizontalSpace = true;
		okData.horizontalAlignment = SWT.END;
		okData.horizontalSpan = 1;
		OKbutton = new Button(parent2, SWT.PUSH);
		OKbutton.setText("   OK   ");
		OKbutton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent event)
			{
				if (isValidInput()) {
					okPressed();
					close();
				}
			}
		});
		OKbutton.setLayoutData(okData);
		return OKbutton;
	}
	
	@Override
	public Entry createEntry() {
		return null;
	}

	public Entry getSampleEntry() {
		return this.sampleEntry;
	}

	public void setSampleEntry(Entry sampleEntry) {
		if(sampleEntry != null)
		{
			this.sampleEntry = sampleEntry;
		}
	}

	@Override
	protected boolean isValidInput() {
		//need to check if sampleNameLabel is empty or not
		if(!checkBasicLengthCheck(sampleNameLabel, sampleNameText, 0, PropertyHandler.LABEL_TEXT_LIMIT))
		{
			return false;
		}
		
		/*if(!checkBasicLengthCheck(descriptionLabel, descriptionText, 0, Integer.parseInt(PropertyHandler.getVariable("descriptionLength"))))
		{
			return false;
		}*/
		
		return true;
	}
	
	public Entry getToBeRemoved() {
		return toBeRemoved;
	}

	@Override
	protected void okPressed() {
		if (sampleEntry == null) 
			return;	
		// we have to check whether there is an experiment design for the sample already
		// if so, ask the user if they want to overwrite it
        List<Entry> children = sampleEntry.getChildren();
        toBeRemoved = null;
        for (Iterator<Entry> iter = children.iterator(); iter.hasNext();) {
			Entry entry = (Entry) iter.next();
			if (entry.getProperty().getType() == ExperimentProperty.TYPE) {
				// experiment design already exists
				// ask if the user wants to create it anyways and overwrite it
				String[] dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL,
	                    IDialogConstants.NO_LABEL,
	                    "Open Existing Design" };
				MessageDialog questionWithViewOption = new MessageDialog (this.getShell(), "Existing Experiment Design",
			            null, "Experiment design already exists! Do you want to overwrite it?", MessageDialog.QUESTION_WITH_CANCEL,
			            dialogButtonLabels, 0);
				
				int returnCode = questionWithViewOption.open();
				if (returnCode == 0) { // YES
					// delete the existing entry
					toBeRemoved = entry;
					this.close();
					questionWithViewOption.close();
					if (getReturnCode() == NewExperimentHandler.COPY)
						setReturnCode(NewExperimentHandler.COPY_OVERRIDE);
					else if (getReturnCode() == NewExperimentHandler.TEMPLATE)
						setReturnCode(NewExperimentHandler.TEMPLATE_OVERRIDE);
					else setReturnCode(NewExperimentHandler.OVERRIDE);
				}
				else if (returnCode == 1) { // NO
					questionWithViewOption.close();
					this.open();  // I don't know why but I have to do this to stay on the newexperimentdialog after questiondialog gets closed
				} else if (returnCode == 2) { // Open existing
					this.close();
					questionWithViewOption.close();
					setReturnCode(Window.OK);
				}
				break;   //should have only one experiment entry
			}
        }
	}

	/**
	 * 
	 * @return experiment selected, if any
	 */
	public Entry getSelectedExperiment() {
		return experimentSelected;
	}

	public ExperimentTemplateEntry getTemplateFile() {
		return templateFile;
	}
} 