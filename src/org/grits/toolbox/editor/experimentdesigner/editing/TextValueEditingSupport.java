package org.grits.toolbox.editor.experimentdesigner.editing;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.core.typeahead.NamespaceHandler;
import org.grits.toolbox.core.typeahead.PatriciaTrieContentProposalProvider;
import org.grits.toolbox.core.utilShare.validator.BooleanValidator;
import org.grits.toolbox.core.utilShare.validator.IntegerValidator;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.namespace.ShortNamespacesHandler;
import org.grits.toolbox.editor.experimentdesigner.ontology.OntologyManager;
import org.grits.toolbox.entry.sample.utilities.TextCellEditorWithContentProposal;



/**
 * This class provides editing support for the Parameter values
 * 
 * @author sena
 *
 */

public class TextValueEditingSupport extends EditingSupport {

	static Logger logger = Logger.getLogger(TextValueEditingSupport.class);

	TreeViewer viewer;
	TextCellEditor textCellEditor;
	TextCellEditor doubleCellEditor;
	TextCellEditor integerCellEditor;
	TextCellEditor booleanCellEditor;
	ComboBoxViewerCellEditor comboBoxViewerCellEditor;

	private GraphEditor editor;

	public void setEditor(GraphEditor editor) {
		this.editor = editor;
	}

	public TextValueEditingSupport(TreeViewer treeViewer) {
		super(treeViewer);
		this.viewer = treeViewer;


		this.textCellEditor = new TextCellEditor(viewer.getTree());
		((Text)this.textCellEditor.getControl()).setTextLimit(PropertyHandler.URI_TEXT_LIMIT);
		
		this.doubleCellEditor = new TextCellEditor(viewer.getTree());
		((Text)this.doubleCellEditor.getControl()).setTextLimit(PropertyHandler.LABEL_TEXT_LIMIT);
		ControlDecoration controlDecoration = new ControlDecoration(doubleCellEditor.getControl(), SWT.CENTER);
		doubleCellEditor.setValidator(new DoubleValidator(controlDecoration));
		
		this.integerCellEditor = new TextCellEditor(viewer.getTree());
		((Text)this.integerCellEditor.getControl()).setTextLimit(PropertyHandler.LABEL_TEXT_LIMIT);
		controlDecoration = new ControlDecoration(integerCellEditor.getControl(), SWT.CENTER);
		integerCellEditor.setValidator(new IntegerValidator(controlDecoration));
		
		this.booleanCellEditor = new TextCellEditor(viewer.getTree());
		((Text)this.booleanCellEditor.getControl()).setTextLimit(PropertyHandler.LABEL_TEXT_LIMIT);
		controlDecoration = new ControlDecoration(booleanCellEditor.getControl(), SWT.CENTER);
		booleanCellEditor.setValidator(new BooleanValidator(controlDecoration));
		
		comboBoxViewerCellEditor = new ComboBoxViewerCellEditor(viewer.getTree(), SWT.READ_ONLY);
		comboBoxViewerCellEditor.setContentProvider(new ArrayContentProvider());

		if(ShortNamespacesHandler.NAMESPACE_VALUES_MAP == null)
		{
			ShortNamespacesHandler.loadShortNamespaceFiles();
		}
	}

	@Override
	protected CellEditor getCellEditor(Object element) {

		if (element instanceof Parameter) {
			String namespace = ((Parameter)element).getNamespace();
			String namespaceFile = ((Parameter) element).getNamespaceFile();
			Boolean shortNamespace = ((Parameter) element).getShortNamespace();
			String value = ((Parameter) element).getValue();
			if (namespace != null && namespace.equals(OntologyManager.baseURI+"double"))
				return this.doubleCellEditor;
			else if (namespace == null || namespace.equals(OntologyManager.baseURI + "string")) {
				return this.textCellEditor;
			}
			else if (namespace == null || namespace.equals(OntologyManager.baseURI + "integer")) {
				return this.integerCellEditor;
			}
			else if (namespace == null || namespace.equals(OntologyManager.baseURI + "boolean")) {
				return this.booleanCellEditor;
			}
			else {
				if(namespaceFile != null && shortNamespace)
				{
					try
					{
						if(!ShortNamespacesHandler.NAMESPACE_VALUES_MAP .containsKey(namespace)
								|| ShortNamespacesHandler.NAMESPACE_VALUES_MAP.get(namespace) == null)
						{
							throw new Exception("No namespace value found for : " + namespace);
						}
						Set<String> valueSet = ShortNamespacesHandler.NAMESPACE_VALUES_MAP.get(namespace);
						comboBoxViewerCellEditor.setInput(valueSet.toArray());
						if(value != null && !valueSet.contains(value))
						{
							valueSet.add(value);
							comboBoxViewerCellEditor.setValue(value);
						}
						return comboBoxViewerCellEditor;
					} catch (Exception ex)
					{
						logger.error(ex.getMessage(), ex);
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Namespace File Error", 
								"Could not find namespace for the parameter " + ((Parameter) element).getName()
								+ "\n" + ex.getMessage());
						return this.textCellEditor;
					}
				}
				else if(namespaceFile != null)
				{
					try
					{
						NamespaceHandler handler = new NamespaceHandler(
								namespace, null, 
								((Parameter)element).getNamespaceFile(), org.grits.toolbox.editor.experimentdesigner.Activator.PLUGIN_ID);
						PatriciaTrie<String> trie = handler.getTrieForNamespace();
						if (trie != null) 
						{
							IContentProposalProvider contentProposalProvider = new PatriciaTrieContentProposalProvider(trie);
							ColumnViewer viewer = getViewer();
							if(viewer != null)
							{
								return new TextCellEditorWithContentProposal(
										((Composite) viewer.getControl()), contentProposalProvider, null, null);
							}
						} 
					} catch (Exception ex)
					{
						logger.error(ex.getMessage(), ex);
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Type Ahead Error", 
								"Something went wrong for the namespace " + namespace
								+".Typeahead is not available!");
						return this.textCellEditor;
					}
				}
				else {
					// no namespace file
					return this.textCellEditor;
				}
			}
		}
		return null;
	}

	@Override
	protected boolean canEdit(Object element) {
		if(element instanceof Parameter)
			return true;
		else if (element instanceof ParameterGroup)
			return false;
		return false;
	}

	@Override
	protected Object getValue(Object element) {
		if (element == null) 
			return "";
		if (((Parameter)element).getValue() == null)
			return "";
		return ((Parameter)element).getValue();
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (element != null && element instanceof Parameter) {
			String oldValue = ((Parameter) element).getValue();
			String newValue = value == null ? null : String.valueOf(value).trim();
			((Parameter) element).setValue(newValue);
			
			if (!Objects.equals(oldValue, newValue)) {
				if (editor == null) {
					logger.error("Cannot get the reference to Graph Editor");
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Update Error", "Cannot get the reference to Graph Editor");
					return;
				}
				editor.refreshParameter((Parameter) element);
			}
			viewer.update(element, null);
		}
	}

}
