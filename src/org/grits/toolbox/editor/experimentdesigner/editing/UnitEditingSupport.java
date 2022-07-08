package org.grits.toolbox.editor.experimentdesigner.editing;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.MeasurementUnit;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;

/**
 * This class provides editing support for the Parameter units.
 * 
 * @author sena
 *
 */
public class UnitEditingSupport extends EditingSupport {
	
	private static final Logger logger = Logger.getLogger(UnitEditingSupport.class);

	TreeViewer viewer;
	ComboBoxCellEditor editor;
	String[] unitLabels;
	
	GraphEditor graphEditor;
	
	public void setGraphEditor(GraphEditor graphEditor) {
		this.graphEditor = graphEditor;
	}
	
	public UnitEditingSupport(TreeViewer viewer) {
		super(viewer);
		this.viewer = viewer;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected CellEditor getCellEditor(Object element) {
		List<MeasurementUnit> units = ((Parameter)element).getAvailableUnits();
		unitLabels = new String[units.size()];
		int i=0;
		for (Iterator iterator = units.iterator(); iterator.hasNext();) {
			MeasurementUnit measurementUnit = (MeasurementUnit) iterator.next();
			unitLabels[i++] = measurementUnit.getLabel();
		}
		if (unitLabels.length == 0) {
			// no combobox
			return null;
		}
		return new ComboBoxCellEditor(viewer.getTree(), unitLabels, SWT.READ_ONLY);
	}

	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof Parameter) 	
			return true;
		return false;
	}

	@Override
	protected Object getValue(Object element) {
		Parameter parameter = (Parameter) element;
		for (int i=0; i < unitLabels.length; i++) {
			if (parameter.getUnit() != null)
				if (parameter.getUnit().getLabel().equals(unitLabels[i])) 
					return i;
		}
	    return 0;
	}

	@Override
	protected void setValue(Object element, Object value) {
		Parameter param = (Parameter) element;
	    for (Iterator<MeasurementUnit> iterator = param.getAvailableUnits().iterator(); iterator.hasNext();) {
			MeasurementUnit unit = (MeasurementUnit) iterator.next();
			if (unitLabels[(Integer)value].equals(unit.getLabel())) {
				param.setUnit(unit);	
				if (graphEditor == null) {
					logger.error("Cannot get the reference to Graph Editor");
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Update Error", "Cannot get the reference to Graph Editor");
					return;
				}
				graphEditor.refreshParameter( (Parameter) element);
			}
		}
	    viewer.update(element, null);
	}

}
