package org.grits.toolbox.editor.experimentdesigner.editing;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.graphics.Image;

public class DoubleValidator implements ICellEditorValidator {
	private ControlDecoration controlDecoration;

	public DoubleValidator(ControlDecoration controlDecoration) {
		this.controlDecoration = controlDecoration;
	}

	@Override
	public String isValid(Object value) {
		String inValidMessage = null;
		if (value != null) {
			String stringValue = (String)value;
			if (!stringValue.isEmpty()) {
				try {
					Double.parseDouble((String)stringValue);
				} catch (NumberFormatException e) {
					inValidMessage = "not a double value";
				}
			} 
		}

        Image errorImage;
        if(inValidMessage != null)
        {
            errorImage = FieldDecorationRegistry.getDefault()
                    .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
                    .getImage();
            this.controlDecoration.setMarginWidth(2);
            this.controlDecoration.setImage(errorImage);
            this.controlDecoration.setDescriptionText(inValidMessage);
            this.controlDecoration.show();
        }
        else
        {
            this.controlDecoration.hide();
        }
        return inValidMessage;
	}

}
