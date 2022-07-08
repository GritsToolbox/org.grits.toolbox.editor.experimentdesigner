package org.grits.toolbox.editor.experimentdesigner.parts;

import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.ParagraphTextLayout;
import org.eclipse.draw2d.text.TextFlow;

public class InputOutputFigure extends Figure {
	
	static int maxLength = 200;
	static int maxHeight = 150;
	
	private TextFlow label;
	private Ellipse ellipse;

	private FlowPage flowPage;
	
	public InputOutputFigure() {
		setLayoutManager(new XYLayout());
		ellipse = new Ellipse();
		add(ellipse);
		flowPage = new FlowPage();
		label = new TextFlow();
		ParagraphTextLayout layout = new ParagraphTextLayout(
				label, ParagraphTextLayout.WORD_WRAP_SOFT);
		label.setLayoutManager(layout);
		flowPage.add(label);
		flowPage.setHorizontalAligment(PositionConstants.CENTER);
		add(flowPage);
	}
	
	public TextFlow getLabel() {
		return label;
	}
	
	@Override
	public Rectangle getBounds() {
		Rectangle original = super.getBounds();
		if (label.getFont() != null) {   // if font is not set yet, the following throws NullPointerException
			Label testLabel = new Label();
			testLabel.setText(label.getText());
			Rectangle labelBounds = testLabel.getTextBounds();
			//Rectangle labelBounds = label.getBounds();
			
			if (labelBounds.width > original.width ) {
				if (labelBounds.width < maxLength)
					original.setWidth(labelBounds.width + 10);  // extra padding
				else
					original.setWidth(maxLength);
			}
			
			if (labelBounds.height > original.height) {
				if (labelBounds.height < maxHeight) 
					original.setHeight(labelBounds.height + 10);
				else
					original.setHeight(maxHeight);
			}
		}
		
		return original;
	}
	
	@Override 
	protected void paintFigure(Graphics graphics) {
	    Rectangle r = getBounds().getCopy();
	    setConstraint(ellipse, new Rectangle(0, 0, r.width, r.height));
	    setConstraint(flowPage, new Rectangle(0, r.height/2 - 7, r.width, r.height));
	    flowPage.setConstraint(label, new Rectangle(0, 0, r.width, r.height));
	    ellipse.invalidate();
	    flowPage.invalidate();
	    label.invalidate();
	  }
}
