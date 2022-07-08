package org.grits.toolbox.editor.experimentdesigner.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PrinterGraphics;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.widgets.Display;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;

public class PrintExperimentGraph {
	
	private GC printerGC; // Note: Only one GC instance should be created per
	// print job
	private Insets printMargin = new Insets(0, 0, 0, 0);
	private Printer printer;
	private PrinterGraphics printerGraphics;
	private SWTGraphics g;
	
	public static final int TILE = 1;
	/**
	 * A print mode that scales the printer graphics so that the entire printed
	 * image fits on one page.
	 */
	public static final int FIT_PAGE = 2;
	/**
	 * A print mode that scales the printer graphics so that the width of the
	 * printed image fits on one page and tiles vertically, if necessary.
	 */
	public static final int FIT_WIDTH = 3;
	/**
	 * A print mode that scales the printer graphics so that the height of the
	 * printed image fits on one page and tiles horizontally, if necessary.
	 */
	public static final int FIT_HEIGHT = 4;

	ExperimentGraph model;
	GraphicalViewer viewer;
	private List selectedEditParts;
	
	private IFigure printSource;
	private Color oldBGColor;
	private int printMode = TILE;
	
	int lineHeight = 0;
	int tabWidth = 0;
	int leftMargin, rightMargin, topMargin, bottomMargin;
	int x, y;
	int index, end;
	//String textToPrint;
	String tabs;
	StringBuffer wordBuffer;
	
	public PrintExperimentGraph(Printer p, GraphicalViewer g, ExperimentGraph model) {
		setPrinter(p);
		this.viewer = g;
		this.model = model;
		
		LayerManager lm = (LayerManager) viewer.getEditPartRegistry().get(
				LayerManager.ID);
		IFigure f = lm.getLayer(LayerConstants.PRINTABLE_LAYERS);
		setPrintSource(f);
	}

	protected void cleanup() {
		if (g != null) {
			printerGraphics.dispose();
			g.dispose();
		}
		if (printerGC != null)
			printerGC.dispose();
	}

	/**
	* Returns a new PrinterGraphics setup for the Printer associated with this
	* PrintOperation.
	* 
	* @return PrinterGraphics The new PrinterGraphics
	*/
	protected PrinterGraphics getFreshPrinterGraphics() {
		if (printerGraphics != null) {
			printerGraphics.dispose();
			g.dispose();
			printerGraphics = null;
			g = null;
		}
		g = new SWTGraphics(printerGC);
		printerGraphics = new PrinterGraphics(g, printer);
		setupGraphicsForPage(printerGraphics);
		return printerGraphics;
	}
	
	int getGraphicsOrientation() {
		return getPrintSource().isMirrored() ? SWT.RIGHT_TO_LEFT
				: SWT.LEFT_TO_RIGHT;
	}

	/**
	 * Returns the current print mode. The print mode is one of:
	 * {@link #FIT_HEIGHT}, {@link #FIT_PAGE}, or {@link #FIT_WIDTH}.
	 * 
	 * @return the print mode
	 */
	protected int getPrintMode() {
		return printMode;
	}

	/**
	* Returns the printer.
	* 
	* @return Printer
	*/
	public Printer getPrinter() {
		return printer;
	}

	/**
	* Returns a Rectangle that represents the region that can be printed to.
	* The x, y, height, and width values are using the printers coordinates.
	* 
	* @return the print region
	*/
	public Rectangle getPrintRegion() {
		org.eclipse.swt.graphics.Rectangle trim = printer.computeTrim(0, 0, 0,
		0);
		org.eclipse.swt.graphics.Point printerDPI = printer.getDPI();
		Insets notAvailable = new Insets(-trim.y, -trim.x,
		trim.height + trim.y, trim.width + trim.x);
		Insets userPreferred = new Insets(
		(printMargin.top * printerDPI.x) / 72,
		(printMargin.left * printerDPI.x) / 72,
		(printMargin.bottom * printerDPI.x) / 72,
		(printMargin.right * printerDPI.x) / 72);
		Rectangle paperBounds = new Rectangle(printer.getBounds());
		Rectangle printRegion = paperBounds.getCropped(notAvailable);
		printRegion.intersect(paperBounds.getCropped(userPreferred));
		printRegion.translate(trim.x, trim.y);
		return printRegion;
	}

	/**
	* Sets the print job into motion.
	* 
	* @param jobName
	*            A String representing the name of the print job
	*/
	public void run(String jobName) {
		preparePrintSource();
		if (printer.startJob(jobName)) {
			printerGC = new GC(getPrinter(), getGraphicsOrientation());
			printPages();
			printer.endJob();
		}
		restorePrintSource();
		cleanup();
	}

	/**
	* Sets the printer.
	* 
	* @param printer
	*            The printer to set
	*/
	public void setPrinter(Printer printer) {
		this.printer = printer;
	}

	/**
	* Sets the page margin in pels (logical pixels) to the passed Insets.(72
	* pels == 1 inch)
	* 
	* @param margin
	*            The margin to set on the page
	*/
	public void setPrintMargin(Insets margin) {
		printMargin = margin;
	}
	
	/**
	 * Sets the print mode. Possible values are {@link #TILE},
	 * {@link #FIT_HEIGHT}, {@link #FIT_WIDTH} and {@link #FIT_PAGE}.
	 * 
	 * @param mode
	 *            the print mode
	 */
	public void setPrintMode(int mode) {
		printMode = mode;
	}

	/**
	 * Sets the printSource.
	 * 
	 * @param printSource
	 *            The printSource to set
	 */
	protected void setPrintSource(IFigure printSource) {
		this.printSource = printSource;
	}

	/**
	 * Sets up Graphics object for the given IFigure.
	 * 
	 * @param graphics
	 *            The Graphics to setup
	 * @param figure
	 *            The IFigure used to setup graphics
	 */
	protected void setupPrinterGraphicsFor(Graphics graphics, IFigure figure) {
		double dpiScale = (double) getPrinter().getDPI().x
				/ Display.getCurrent().getDPI().x;

		Rectangle printRegion = getPrintRegion();
		// put the print region in display coordinates
		printRegion.width /= dpiScale;
		printRegion.height /= dpiScale;

		Rectangle bounds = figure.getBounds();
		double xScale = (double) printRegion.width / bounds.width;
		double yScale = (double) printRegion.height / bounds.height;
		switch (getPrintMode()) {
		case FIT_PAGE:
			graphics.scale(Math.min(xScale, yScale) * dpiScale);
			break;
		case FIT_WIDTH:
			graphics.scale(xScale * dpiScale);
			break;
		case FIT_HEIGHT:
			graphics.scale(yScale * dpiScale);
			break;
		default:
			graphics.scale(dpiScale);
		}
		graphics.setForegroundColor(figure.getForegroundColor());
		graphics.setBackgroundColor(figure.getBackgroundColor());
		graphics.setFont(figure.getFont());
	}

	/**
	* Manipulates the PrinterGraphics to position it to paint in the desired
	* region of the page. (Default is the top left corner of the page).
	* 
	* @param pg
	*            The PrinterGraphics to setup
	*/
	protected void setupGraphicsForPage(PrinterGraphics pg) {
		Rectangle printRegion = getPrintRegion();
		pg.clipRect(printRegion);
		pg.translate(printRegion.getTopLeft());
	}
	
	protected IFigure getPrintSource() {
		return printSource;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void preparePrintSource() {
		oldBGColor = getPrintSource().getLocalBackgroundColor();
		getPrintSource().setBackgroundColor(ColorConstants.white);

		selectedEditParts = new ArrayList(viewer.getSelectedEditParts());
		viewer.deselectAll();
	}
	
	protected void restorePrintSource() {
		getPrintSource().setBackgroundColor(oldBGColor);
		oldBGColor = null;
		viewer.setSelection(new StructuredSelection(selectedEditParts));
	}
	
	
	void printGraphicalViewer() {
			Graphics graphics = getFreshPrinterGraphics();
			IFigure figure = getPrintSource();
			setupPrinterGraphicsFor(graphics, figure);
			Rectangle bounds = figure.getBounds();
			int x = bounds.x, y = bounds.y;
			Rectangle clipRect = new Rectangle();
			while (y < bounds.y + bounds.height) {
				while (x < bounds.x + bounds.width) {
					graphics.pushState();
					getPrinter().startPage();
					graphics.translate(-x, -y);
					graphics.getClip(clipRect);
					clipRect.setLocation(x, y);
					graphics.clipRect(clipRect);
					figure.paint(graphics);
					getPrinter().endPage();
					graphics.popState();
					x += clipRect.width;
				}
				x = bounds.x;
				y += clipRect.height;
			}
	}
	
	protected void printPages() {
		printGraphicalViewer();
		// print the text portion
		print (printer);
	}
	
	void print(Printer printer) {
		org.eclipse.swt.graphics.Rectangle clientArea = printer.getClientArea();
		org.eclipse.swt.graphics.Rectangle trim = printer.computeTrim(0, 0, 0, 0);
		Point dpi = printer.getDPI();
		leftMargin = dpi.x + trim.x; // one inch from left side of paper
		rightMargin = clientArea.width - dpi.x + trim.x + trim.width; // one inch from right side of paper
		topMargin = dpi.y + trim.y; // one inch from top edge of paper
		bottomMargin = clientArea.height - dpi.y + trim.y + trim.height; // one inch from bottom edge of paper
		
		/* Create a buffer for computing tab width. */
		int tabSize = 4; // is tab width a user setting in your UI?
		StringBuffer tabBuffer = new StringBuffer(tabSize);
		for (int i = 0; i < tabSize; i++) tabBuffer.append(' ');
		tabs = tabBuffer.toString();
		
	//	FontData[] printerFontData = new FontData[0];
		RGB printerForeground, printerBackground;
		/* Get the font & foreground & background data. */
	//	printerFontData[0] = new FontData("Arial", 10, SWT.NORMAL);
		printerForeground = new RGB (0, 0, 0);
		printerBackground = new RGB (255, 255, 255);
		
		Font printerFont = new Font(printer, "Tahoma", 11, SWT.NORMAL);
		Font boldFont = new Font (printer, "Tahoma", 11, SWT.BOLD);
		Color printerForegroundColor = new Color(printer, printerForeground);
		Color printerBackgroundColor = new Color(printer, printerBackground); 
		
		printerGC.setForeground(printerForegroundColor);
		printerGC.setBackground(printerBackgroundColor);
		tabWidth = printerGC.stringExtent(tabs).x;
		lineHeight = printerGC.getFontMetrics().getHeight();
	
		/* Print text to current gc using word wrap */
		// print the header
		printerGC.setFont(boldFont);
		
		printer.startPage();
		x = leftMargin;
		y = topMargin;
		printText("Experiment Design " + model.getName() + "\n");
		printLine();
		printText("\nCreated By: ");
		if (model.getCreatedBy() != null) {
			printerGC.setFont(printerFont);
			printText (model.getCreatedBy());
		}
		printerGC.setFont(boldFont);
		printText("\nCreated: ");
		printerGC.setFont(printerFont);
		printText (model.getDateCreated().toString());
		if (model.getDescription() != null && model.getDescription().trim().length() > 0) {
			printText("\nDescription: ");
			printText(model.getDescription());
		}
		if (model.getUri() != null && model.getUri().trim().length() > 0) {
			printText ("\nURI: ");
			printText (model.getUri());
		}
		
		// print each protocol
		for (Iterator iterator = model.getChildren().iterator(); iterator.hasNext();) {
			GraphNode childNode = (GraphNode) iterator.next();
			if (childNode instanceof ProtocolNode) {
				printerGC.setFont(boldFont);
				printText("\n\nProtocol " + childNode.getLabel() + "\n");
				printLine();
				printText("\nDescription: ");
				if (childNode.getDescription() != null) {
					printerGC.setFont(printerFont);
					printText(childNode.getDescription());
				}
				printerGC.setFont(boldFont);
				printText("\nCreator: ");
				if (((ProtocolNode)childNode).getCreator() != null) {
					printerGC.setFont(printerFont);
					printText(((ProtocolNode)childNode).getCreator() );
				}
				printerGC.setFont(boldFont);
				printText ("\nTemplate: ");
				printerGC.setFont(printerFont);
				printText(((ProtocolNode)childNode).getTemplate());
				printerGC.setFont(boldFont);
				printText ("\nURI: ");
				printerGC.setFont(printerFont);
				printText (((ProtocolNode)childNode).getUri());
				
				List<Paper> papers = ((ProtocolNode)childNode).getPapers();
				printerGC.setFont(boldFont);
				printText("\n\nPapers");
				printerGC.setFont(printerFont);
				if (papers != null) {
					for (Iterator iterator2 = papers.iterator(); iterator2
							.hasNext();) {
						Paper paper = (Paper) iterator2.next();
						printText("\n\n" + paper.toString());
					}
				}
				printerGC.setFont(boldFont);
				printText ("\n\nParameters/Parameter Groups ");
				printText("\nName\t\t\tValue\t\t\tUnit ");
				List<Parameter> parameters = ((ProtocolNode)childNode).getParameters();
				if (parameters != null) {
					printerGC.setFont(printerFont);
					for (Iterator iterator2 = parameters.iterator(); iterator2
							.hasNext();) {
						Parameter parameter = (Parameter) iterator2.next();
						String text = "\n" + parameter.getName() + "\t\t" + parameter.getValue();
						if (parameter.getUnit() != null) {
							text += "\t\t" + parameter.getUnit().getLabel();
						}
						printText(text);
					}
				}
				List<ParameterGroup> parameterGroups = ((ProtocolNode)childNode).getParameterGroups();
				if (parameterGroups != null) {
					for (Iterator iterator2 = parameterGroups.iterator(); iterator2
							.hasNext();) {
						ParameterGroup parameterGroup = (ParameterGroup) iterator2
								.next();
						printText("\n" + parameterGroup.getLabel());
						List<Parameter> params = parameterGroup.getParameters();
						for (Iterator iterator3 = params.iterator(); iterator3
								.hasNext();) {
							Parameter parameter = (Parameter) iterator3.next();
							String text = "\n" + parameter.getName() + "\t\t" + parameter.getValue();
							if (parameter.getUnit() != null) {
								text += "\t\t" + parameter.getUnit().getLabel();
							}
							printText(text);
						}	
					}
				}
			}
		}
		
		printerFont.dispose();
		boldFont.dispose();
	}
	
	void printText(String textToPrint) {
		wordBuffer = new StringBuffer();
		//x = leftMargin;
		//y = topMargin;
		index = 0;
		end = textToPrint.length();
		while (index < end) {
			char c = textToPrint.charAt(index);
			index++;
			if (c != 0) {
				if (c == 0x0a || c == 0x0d) {
					if (c == 0x0d && index < end && textToPrint.charAt(index) == 0x0a) {
						index++; // if this is cr-lf, skip the lf
					}
					printWordBuffer();
					newline();
				} else {
					if (c != '\t') {
						wordBuffer.append(c);
					}
					if (Character.isWhitespace(c)) {
						printWordBuffer();
						if (c == '\t') {
							x += tabWidth;
						}
					}
				}
			}
		}
		if (wordBuffer.length() != 0) {
			// still data left in wordBuffer, happens when the text does not end with new line or tab or a white space character
			printWordBuffer();
		}
		if (y + lineHeight > bottomMargin) {
			printer.endPage();
			x = leftMargin;
			y = topMargin;
			printer.startPage();
		}
	}
	
	void printLine () {
		printerGC.drawLine(leftMargin+1, y, rightMargin-1, y);
		y += lineHeight;
	}

	void printWordBuffer() {
		if (wordBuffer.length() > 0) {
			String word = wordBuffer.toString();
			int wordWidth = printerGC.stringExtent(word).x;
			if (x + wordWidth > rightMargin) {
				/* word doesn't fit on current line, so wrap */
				newline();
			}
			printerGC.drawString(word, x, y, false);
			x += wordWidth;
			wordBuffer = new StringBuffer();
		}
	}

	void newline() {
		x = leftMargin;
		y += lineHeight;
		if (y + lineHeight > bottomMargin) {
			printer.endPage();
			if (index + 1 < end) {
				y = topMargin;
				printer.startPage();
			}
		}
	}

}
