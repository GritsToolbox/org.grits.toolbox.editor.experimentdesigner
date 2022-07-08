package org.grits.toolbox.editor.experimentdesigner.pdfgeneration;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.util.DataModelSearch;
import org.grits.toolbox.core.editor.ViewInput;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.handler.ExperimentDesignReportHandler.Options;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.entry.sample.model.Category;
import org.grits.toolbox.entry.sample.model.Component;
import org.grits.toolbox.entry.sample.model.Descriptor;
import org.grits.toolbox.entry.sample.model.DescriptorGroup;
import org.grits.toolbox.entry.sample.model.Sample;
import org.grits.toolbox.entry.sample.ontologymanager.SampleOntologyManager;
import org.grits.toolbox.entry.sample.property.SampleProperty;

public class ExperimentDesignRDFReport {

    private static final Logger logger = Logger.getLogger(ExperimentDesignRDFReport.class);

    // Page configuration
    public static final PDRectangle PAGE_SIZE = PDPage.PAGE_SIZE_LETTER;
    public static final float MARGIN = 40;
    public static final boolean IS_LANDSCAPE = false;

    // Font configuration
    public static final PDFont TEXT_FONT = PDType1Font.HELVETICA;
    public static final PDFont BOLD_FONT = PDType1Font.HELVETICA_BOLD;
    public static final float FONT_SIZE = 12;

    // Table configuration
    public static final float CELL_MARGIN = 2;

    // length of the writing area considering header and footer
    public static float LENGTH = PDPage.PAGE_SIZE_LETTER.getHeight() - 2* MARGIN;

    PDDocument document;
    float yIndex = LENGTH;

    public ExperimentDesignRDFReport () {	
    }

    public void generateDocument(Options options, GraphEditor editor) {
       // Create a new empty document
       this.document = new PDDocument();

        try {
            IEditorInput input = editor.getEditorInput();
            if (input instanceof ViewInput) {
            	Entry entry = ((ViewInput) input).getEntry();
            	// get the project information
            	Entry projectEntry = DataModelSearch.findParentByType(entry, ProjectProperty.TYPE);

            	if (options != null && options.getProject())
            		ProjectReportPage.addProjectPage(projectEntry, 
            				(ProjectProperty) projectEntry.getProperty(), document, yIndex);

            	// create a page with sample information
            	Sample sample = SampleProperty.loadAnalyte(
            			DataModelSearch.findParentByType(entry, SampleProperty.TYPE));

                if (options != null && options.getSample())
                	addSampleInformation (sample);
                
                ExperimentGraph model = ((GraphEditor)editor).getModel();
                
                if (options != null && options.getExpDesign())
                	addExperimentDesignerInformation((GraphEditor)editor, model);
            
                // add header / footer
              //  addHeader (document, model);
                addFooter();

                // ask the user where to save the document
                FileDialog fileDialog = new FileDialog(editor.getSite().getShell(), SWT.SAVE);
                // Set the text
                fileDialog.setText("Select File");
                // Set filter on .pdf files
                fileDialog.setFilterExtensions(new String[] { "*.pdf" });
                // Put in a readable name for the filter
                fileDialog.setFilterNames(new String[] { "PDF (*.pdf)" });
                fileDialog.setFileName(model.getName() + "-experimentdesign.pdf");
                fileDialog.setOverwrite(true);
                // Open Dialog and save result of selection
                String selected = fileDialog.open();
                if (selected != null) {
                    document.save(selected);
                    //MessageDialog.openInformation(editor.getSite().getShell(), "Information", "PDF Report generated");
                }

                // finally make sure that the document is properly
                // closed.
                document.close();
            }
        } catch (COSVisitorException | IOException  | JAXBException e) {
            logger.error (Activator.PLUGIN_ID + " Cannot generate report!", e);
            MessageDialog.openError(editor.getSite().getShell(), "Error", "PDF Report cannot be generated! Please check the log file");
        }
    }
    
    @SuppressWarnings("rawtypes")
	private void addExperimentDesignerInformation (GraphEditor editor, ExperimentGraph model) throws IOException {
        BufferedImage imageForPDF = editor.getBufferedImage();

        PDXObjectImage ximage = new PDPixelMap(document, imageForPDF);

        // Create a new page and add it to the document
        PDPage page = new PDPage(PDPage.PAGE_SIZE_LETTER);

        PDRectangle mediabox = page.findMediaBox();
        float pageWidth = mediabox.getWidth();

        document.addPage( page );

        yIndex = LENGTH;
        float x;

        yIndex = PDFGenerator.writeSimpleText(document, page, "Experiment Graph", BOLD_FONT, FONT_SIZE, MARGIN, yIndex);

        x = PDFGenerator.writeTextOnTheSameLine (document, page, "Date Created:", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
        yIndex = PDFGenerator.writeSimpleText(document, page, model.getDateCreated().toString(), TEXT_FONT, FONT_SIZE, x, yIndex);

        if (model.getDescription() != null && model.getDescription().length() > 0) {
        	x = PDFGenerator.writeTextOnTheSameLine (document, page, "Description:", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
            yIndex = PDFGenerator.drawMultilineString(document, page, TEXT_FONT, FONT_SIZE, model.getDescription(), yIndex, x, LENGTH);
            // go to the last page
            page = (PDPage) document.getDocumentCatalog().getAllPages().get(document.getNumberOfPages()-1);
        }

        Dimension size = model.calculateSize();
        float xscale = (pageWidth-2*MARGIN)/size.width;
        float yscale = (LENGTH-2*MARGIN)/size.height;
        float scale = Math.min(xscale, yscale);

        if (yIndex - 2* MARGIN < size.height*scale) { // cannot fit into this page, need to create a new page

            page = new PDPage(PDPage.PAGE_SIZE_LETTER);
            document.addPage(page);

            yIndex = LENGTH;
            PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true);
            contentStream.drawXObject(ximage, MARGIN, MARGIN, size.width*scale, size.height*scale);

            // Make sure that the content stream is closed:
            contentStream.close();				
        }
        else {
            PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true);
            contentStream.drawXObject(ximage, MARGIN, LENGTH-yIndex+MARGIN, size.width*scale, size.height*scale);

            // Make sure that the content stream is closed:
            contentStream.close();					
        }

        // add details of protocols
        if (model.getChildren() != null) {
            for (Iterator iterator = model.getChildren().iterator(); iterator
                    .hasNext();) {
                GraphNode node = (GraphNode) iterator.next();
                if (node instanceof ProtocolNode) {
                    // one page for each protocol
                    PDPage protocolPage = new PDPage(PDPage.PAGE_SIZE_LETTER);
                    document.addPage(protocolPage);

                    yIndex = LENGTH;

                    x = PDFGenerator.writeTextOnTheSameLine (document, protocolPage, "Protocol", BOLD_FONT, FONT_SIZE, MARGIN, yIndex, false);
                    yIndex = PDFGenerator.writeSimpleText(document, protocolPage, ((ProtocolNode)node).getLabel(), BOLD_FONT, FONT_SIZE, x, yIndex);

                    if (((ProtocolNode) node).getCreator() != null) {
                    	x = PDFGenerator.writeTextOnTheSameLine (document, protocolPage, "Created By:", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                        yIndex = PDFGenerator.writeSimpleText(document, protocolPage, ((ProtocolNode)node).getCreator(), TEXT_FONT, FONT_SIZE, x, yIndex);						    
                    }

                    if (node.getDescription() != null && node.getDescription().length() > 0) {
                    	x = PDFGenerator.writeTextOnTheSameLine (document, protocolPage, "Description:", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                        yIndex =  PDFGenerator.drawMultilineString(document, protocolPage, TEXT_FONT, FONT_SIZE, node.getDescription(), yIndex, x, LENGTH);
                        // go to the last page
                        protocolPage = (PDPage) document.getDocumentCatalog().getAllPages().get(document.getNumberOfPages()-1);
                    }

                    // add a table for ParameterGroups and Parameters
                    addParameterTable ((ProtocolNode)node);
                   
                    addPapers((ProtocolNode)node);
                    
                    // go to the last page
                    protocolPage = (PDPage) document.getDocumentCatalog().getAllPages().get(document.getNumberOfPages()-1); 
                }
            }
        }
    }
    
    private void addParameterTable ( ProtocolNode node) throws IOException {
    	
    	// go to the last page
        PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(document.getNumberOfPages()-1);
        
    	List<Column> columns = new ArrayList<Column>();
        columns.add(new Column("Parameter/Parameter Group", 200));
        columns.add(new Column("Value", 200));
        columns.add(new Column("Unit", 100));

        String [][] content;
        // calculate the size of the table
        int tableSize=0;
        if ( node.getParameterGroups() != null) {
            tableSize += node.getParameterGroups().size();
            for (ParameterGroup group : node.getParameterGroups()) {
                tableSize += group.getParameters().size();
            }
        }
        if ( node.getParameters() != null) {
            tableSize +=  node.getParameters().size();
        }

        content = new String[tableSize][3];

        // fill the contents
        int row = 0;
        float maxRowHeight = PDFGenerator.ROW_HEIGHT;
        if ( node.getParameterGroups() != null) {
        	for (ParameterGroup group : node.getParameterGroups()) {
                content [row][0] = group.getLabel();
                content [row][1] = "";
                content [row++][2] = "";
                List<Parameter> parameters =  group.getParameters();
                for (Parameter parameter : parameters) {
                    content[row][0] = "     " + parameter.getName();
                    if (parameter.getValue() != null)  {
                        content[row][1] = parameter.getValue();
                        maxRowHeight = Math.max(PDFGenerator.calculateRowHeight(parameter.getValue(), 200, TEXT_FONT, FONT_SIZE, CELL_MARGIN ), maxRowHeight);
                    }
                    else
                        content[row][1] = "";
                    if (parameter.getUnit() != null)
                        content[row][2] = parameter.getUnit().getLabel();
                    else
                        content[row][2]= "";
                    row++;
                }
            }
        }

        if ( node.getParameters() != null) {
        	for (Parameter parameter : node.getParameters()) {
                content[row][0] = parameter.getName();
                if (parameter.getValue() != null) {
                    content[row][1] = parameter.getValue();
                    maxRowHeight = Math.max(PDFGenerator.calculateRowHeight(parameter.getValue(), 200, TEXT_FONT, FONT_SIZE, CELL_MARGIN), maxRowHeight);
                }
                else
                    content[row][1] = "";
                if (parameter.getUnit() != null)
                    content[row][2] = parameter.getUnit().getLabel();
                else
                    content[row][2]= "";
                row++;
            }
        }

        float tableHeight = IS_LANDSCAPE ? PAGE_SIZE.getWidth() - (2 * MARGIN) : yIndex - (2 * MARGIN);
        float tableMaxHeight = IS_LANDSCAPE ? PAGE_SIZE.getWidth() - (2 * MARGIN) : PAGE_SIZE.getHeight() - (2 * MARGIN);

        Table table = new TableBuilder()
        .setCellMargin(CELL_MARGIN)
        .setColumns(columns)
        .setContent(content)
        .setHeight(tableHeight)
        .setMaxHeight(tableMaxHeight)
        .setNumberOfRows(content.length)
        .setRowHeight(PDFGenerator.ROW_HEIGHT)
        .setMargin(MARGIN)
        .setPageSize(PAGE_SIZE)
        .setLandscape(IS_LANDSCAPE)
        .setTextFont(TEXT_FONT)
        .setHeaderFont(BOLD_FONT)
        .setFontSize(FONT_SIZE)
        .setMaxRowHeight(maxRowHeight)
        .build();

        PDFGenerator tableGenerator = new PDFGenerator();

        yIndex = tableGenerator.drawTable(this.document, page, table, true, yIndex);   
        
    }
    
    public void addPapers (ProtocolNode node) throws IOException {
    	// add the papers
        if ( node.getPapers() != null && node.getPapers().size() > 0) {
        	PDPage page = (PDPage) this.document.getDocumentCatalog().getAllPages().get(this.document.getNumberOfPages()-1);
        	yIndex = PDFGenerator.writeSimpleText(this.document, page, "Papers", BOLD_FONT, FONT_SIZE, MARGIN, yIndex-10);
        	for (Paper paper: node.getPapers()) {
				yIndex = PDFGenerator.drawMultilineString(this.document, page, TEXT_FONT, FONT_SIZE, paper.toString(), yIndex, MARGIN, LENGTH);
				if (yIndex != LENGTH) yIndex -= 20; // put a space between each paper, if it is not the beginning of a new page
				
				// go to the last page
                page = (PDPage) this.document.getDocumentCatalog().getAllPages().get(this.document.getNumberOfPages()-1);	
			}
        }
    }



    private void addSampleInformation(Sample sample) 
    {
        try
        {
            PDPage page = new PDPage(PDPage.PAGE_SIZE_LETTER);
            document.addPage(page);
            yIndex = LENGTH;
            
            yIndex = PDFGenerator.writeSimpleText(document, page, 
                    "Analyte",BOLD_FONT, FONT_SIZE, MARGIN, yIndex);

            float x = PDFGenerator.writeTextOnTheSameLine (document, page, "Name:",
            		TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
            yIndex = PDFGenerator.writeSimpleText(document, page, 
                    sample.getName(), TEXT_FONT, FONT_SIZE, x, yIndex);

            if (sample.getDescription() != null && !sample.getDescription().isEmpty())
            {
            	x = PDFGenerator.writeTextOnTheSameLine (document, page, "Description:",
            			TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                yIndex = PDFGenerator.drawMultilineString(
                        document, page, TEXT_FONT, FONT_SIZE, 
                        sample.getDescription(), yIndex, x, LENGTH);
            }

            yIndex = PDFGenerator.writeSimpleText(document, page, 
                    "List of Components: ", BOLD_FONT, FONT_SIZE, MARGIN, yIndex);
            int componentNumber = 1;
            for(Component comp : sample.getComponents())
            {
            	x = PDFGenerator.writeTextOnTheSameLine (document, page, componentNumber++ + ". ",
            			TEXT_FONT, FONT_SIZE, MARGIN, yIndex, false);
                yIndex = PDFGenerator.writeSimpleText(document, page, 
                         comp.getLabel(), TEXT_FONT, FONT_SIZE, x, yIndex);
            }

            componentNumber = 1;
            for(Component comp : sample.getComponents())
            {
                PDPage componentPage = new PDPage(PDPage.PAGE_SIZE_LETTER);
                document.addPage(componentPage);
                yIndex = LENGTH;
                
                yIndex = PDFGenerator.writeSimpleText(document, componentPage, 
                        "Component" + componentNumber++, BOLD_FONT, FONT_SIZE, MARGIN, yIndex);

                x = PDFGenerator.writeTextOnTheSameLine (document, componentPage, "Name:",
                		TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                yIndex = PDFGenerator.writeSimpleText(document, componentPage, 
                        comp.getLabel(), TEXT_FONT, FONT_SIZE, x, yIndex);

                if (comp.getDescription() != null && !comp.getDescription().isEmpty())
                {
                	x = PDFGenerator.writeTextOnTheSameLine (document, componentPage, "Description:",
                			TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                    yIndex = PDFGenerator.drawMultilineString(document, componentPage,
                    		TEXT_FONT, FONT_SIZE, comp.getDescription(), yIndex, x, LENGTH);
                }

                if(comp.getTemplateUri() != null)
                {
                    x = PDFGenerator.writeTextOnTheSameLine (document, componentPage, "Template: ",
                    		TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                    yIndex = PDFGenerator.writeSimpleText(document, componentPage, comp.getTemplateUri(),
                    		TEXT_FONT, FONT_SIZE, x, yIndex);
                }

                Category sampleInformation = comp.getSampleInformation();
                yIndex = writeCategoryInComponentPage(sampleInformation, document, componentPage, yIndex);

                Category tracking = comp.getTracking();
                yIndex = writeCategoryInComponentPage(tracking, document, componentPage, yIndex);

                Category amount = comp.getAmount();
                yIndex = writeCategoryInComponentPage(amount, document, componentPage, yIndex);

                Category purityQC = comp.getPurityQC();
                yIndex = writeCategoryInComponentPage(purityQC, document, componentPage, yIndex);

            }

        } catch (IOException e)
        {
            logger.error("Sample Information could not be added to the document successfully.");
        }
    }

    private float writeCategoryInComponentPage(Category category,
            PDDocument document, PDPage componentPage, float yIndex) throws IOException
            {
        try
        {
            if(!category.getDescriptors().isEmpty() || 
                    !category.getDescriptorGroups().isEmpty())
            {

                List<Column> columns = new ArrayList<Column>();
                columns.add(new Column("Descriptor/Descriptor Group", 250));
                columns.add(new Column("Value", 150));
                columns.add(new Column("Unit", 150));

                int tableSize = category.getDescriptors().size();
                for(DescriptorGroup dg : category.getDescriptorGroups())
                {
                    int sum = (1 + dg.getMandatoryDescriptors().size() 
                            + dg.getOptionalDescriptors().size());
                    tableSize += sum;
                }

                String[][] content = new String[tableSize][3];
                int row = 0;
                float maxRowHeight = PDFGenerator.ROW_HEIGHT;
                for (Descriptor desc : category.getDescriptors()) 
                {
                    maxRowHeight = writeDescriptorRow(desc, content, row, maxRowHeight);
                    row++;
                }
                for(DescriptorGroup dg : category.getDescriptorGroups())
                {
                    content [row][0] = dg.getLabel();
                    content [row][1] = "";
                    content [row][2] = "";
                    row++;
                    for(Descriptor desc : dg.getMandatoryDescriptors())
                    {
                        maxRowHeight = writeDescriptorRow(desc, content, row, maxRowHeight);
                        row++;
                    }
                    for(Descriptor desc : dg.getOptionalDescriptors())
                    {
                        maxRowHeight = writeDescriptorRow(desc, content, row, maxRowHeight);
                        row++;
                    }
                }

                float tableHeight = IS_LANDSCAPE ? PAGE_SIZE.getWidth() - (2 * MARGIN) : yIndex - (2 * MARGIN);
                float tableMaxHeight = IS_LANDSCAPE ? PAGE_SIZE.getWidth() - (2 * MARGIN) : PAGE_SIZE.getHeight() - (2 * MARGIN);

                Table table = createTable(columns, content, tableHeight, tableMaxHeight, maxRowHeight);

                PDFGenerator tableGenerator = new PDFGenerator();
                float x;
                if((yIndex - MARGIN - (PDFGenerator.ROW_HEIGHT*(content.length +1))) > 0)
                {
                    yIndex -= 10f;
                    x = PDFGenerator.writeTextOnTheSameLine (document, componentPage, "Category:", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                    yIndex = PDFGenerator.writeSimpleText(document, componentPage, 
                            getCategoryLabel(category.getUri()), BOLD_FONT, FONT_SIZE, x, yIndex);
                    yIndex = tableGenerator.drawTable(document, componentPage, table, true, yIndex);
                }
                else
                {
                    PDPage componentPage2 = new PDPage(PDPage.PAGE_SIZE_LETTER);
                    document.addPage(componentPage2);
                    yIndex = LENGTH;
                    
//                    yIndex -= 10f;
                    x = PDFGenerator.writeTextOnTheSameLine (document, componentPage2, "Category:", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
                    yIndex = PDFGenerator.writeSimpleText(document, componentPage2, 
                            getCategoryLabel(category.getUri()), BOLD_FONT, FONT_SIZE, x, yIndex);

                    tableHeight = IS_LANDSCAPE ? PAGE_SIZE.getWidth() - (2 * MARGIN) : yIndex - (2 * MARGIN);
                    tableMaxHeight = IS_LANDSCAPE ? PAGE_SIZE.getWidth() - (2 * MARGIN) : PAGE_SIZE.getHeight() - (2 * MARGIN);

                    table.setHeight(tableHeight);
                    table.setMaxHeight(tableMaxHeight);
                    yIndex = tableGenerator.drawTable(document, componentPage2, table, true, yIndex);
                }

            }
            return yIndex;
        } catch (IOException e)
        {
            logger.error("Error Generating category Information for Sample : " + category.getUri());
            throw e;
        }
    }

    /**
     * returns a table with the following set parameters
     * @param columns
     * @param content
     * @param tableHeight
     * @param tableMaxHeight
     * @param maxRowHeight
     * @return
     */
    public static Table createTable(List<Column> columns, String[][] content, 
    		float tableHeight, float tableMaxHeight, float maxRowHeight)
	{
    	return new TableBuilder()
        .setCellMargin(CELL_MARGIN)
        .setColumns(columns)
        .setContent(content)
        .setHeight(tableHeight)
        .setMaxHeight(tableMaxHeight)
        .setNumberOfRows(content.length)
        .setRowHeight(PDFGenerator.ROW_HEIGHT)
        .setMargin(MARGIN)
        .setPageSize(PAGE_SIZE)
        .setLandscape(IS_LANDSCAPE)
        .setTextFont(TEXT_FONT)
        .setHeaderFont(BOLD_FONT)
        .setFontSize(FONT_SIZE)
        .setMaxRowHeight(maxRowHeight)
        .build();
	}

	private String getCategoryLabel(String uri)
    {
        switch(uri)
        {
        case SampleOntologyManager.CATEGORY_SAMPLE_INFO_CLASS_URI :
            return "Sample Information";
        case SampleOntologyManager.CATEGORY_TRACKING_INFO_CLASS_URI :
            return "Sample Tracking";
        case SampleOntologyManager.CATEGORY_AMOUNT_CLASS_URI :
            return "Amount";
        case SampleOntologyManager.CATEGORY_PURITY_QC_CLASS_URI :
            return "Purity Q.C.";
        }
        return null;
    }

	/**
	 * returns the new maxRowHeight after adding the content 
	 * inside fixed width columns of the table
	 * @param desc
	 * @param content
	 * @param row
	 * @param maxRowHeight
	 * @return
	 * @throws IOException
	 */
    private float writeDescriptorRow(Descriptor desc, String[][] content,
            int row, float maxRowHeight) throws IOException
            {
        try
        {
            content [row][0] = desc.getLabel();
            if(desc.getValue() != null)
            {
                content [row][1] = desc.getValue();
                maxRowHeight = Math.max(PDFGenerator.calculateRowHeight(
                        desc.getValue(), 150, TEXT_FONT, FONT_SIZE, CELL_MARGIN), maxRowHeight);
                if(desc.getSelectedMeasurementUnit() != null)
                {
                    String selectedUnitLabel = desc.getUnitLabelFromUri(desc.getSelectedMeasurementUnit());
                    content [row][2] = selectedUnitLabel;
                    maxRowHeight = Math.max(PDFGenerator.calculateRowHeight(
                            selectedUnitLabel, 150, TEXT_FONT, FONT_SIZE, CELL_MARGIN), maxRowHeight);
                }
            }
            return maxRowHeight;
        } catch (IOException e)
        {
            logger.error("Error Creating Row for Descriptor : " + desc.getLabel());
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public void addHeader (ExperimentGraph model) throws IOException {	
        List pages = this.document.getDocumentCatalog().getAllPages();
        for (Iterator iterator = pages.iterator(); iterator.hasNext();) {
            PDPage page = (PDPage) iterator.next();
            PDPageContentStream contentStream = new PDPageContentStream(this.document, page, true, true);

            contentStream.beginText();
            contentStream.setFont( TEXT_FONT, FONT_SIZE - 2 );
            contentStream.moveTextPositionByAmount( MARGIN, 750 );
            contentStream.drawString(model.getName());
            contentStream.endText();

            contentStream.close();

            if (model.getCreatedBy() != null) {
                contentStream = new PDPageContentStream(this.document, page, true, true);
                contentStream.beginText();
                contentStream.setFont( TEXT_FONT, FONT_SIZE - 2 );
                contentStream.moveTextPositionByAmount( 400, 750);
                contentStream.drawString(model.getCreatedBy());
                contentStream.endText();
                contentStream.close();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void addFooter () throws IOException {

        List pages = this.document.getDocumentCatalog().getAllPages();
        int i=1;
        for (Iterator iterator = pages.iterator(); iterator.hasNext();) {
            PDPage page = (PDPage) iterator.next();
            PDPageContentStream contentStream = new PDPageContentStream(this.document, page, true, true);
            PDRectangle mediabox = page.findMediaBox();
            float rightCorner = mediabox.getUpperRightX();
            contentStream.beginText();
            contentStream.setFont( TEXT_FONT, FONT_SIZE - 2 );
            contentStream.moveTextPositionByAmount(rightCorner - MARGIN, 10 );
            contentStream.drawString(String.valueOf(i));
            contentStream.endText();

            contentStream.close();

            float leftCorner = mediabox.getLowerLeftX();
            contentStream = new PDPageContentStream( this.document, page, true, true);
            contentStream.beginText();
            contentStream.setFont( TEXT_FONT, FONT_SIZE - 2 );
            contentStream.moveTextPositionByAmount( leftCorner + MARGIN, 10);
            contentStream.drawString(new Date().toString());
            contentStream.endText();
            contentStream.close();
            i++;
        }		
    }



    public static BufferedImage SWTimageToAWTImage(ImageData data) {
        ColorModel colorModel = null;
        PaletteData palette = data.palette;
        if (palette.isDirect) {
            colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
            BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int pixel = data.getPixel(x, y);
                    RGB rgb = palette.getRGB(pixel);
                    pixelArray[0] = rgb.red;
                    pixelArray[1] = rgb.green;
                    pixelArray[2] = rgb.blue;
                    raster.setPixels(x, y, 1, 1, pixelArray);
                }
            }
            return bufferedImage;
        } else {
            RGB[] rgbs = palette.getRGBs();
            byte[] red = new byte[rgbs.length];
            byte[] green = new byte[rgbs.length];
            byte[] blue = new byte[rgbs.length];
            for (int i = 0; i < rgbs.length; i++) {
                RGB rgb = rgbs[i];
                red[i] = (byte) rgb.red;
                green[i] = (byte) rgb.green;
                blue[i] = (byte) rgb.blue;
            }
            if (data.transparentPixel != -1) {
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
            } else {
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
            }
            BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int pixel = data.getPixel(x, y);
                    pixelArray[0] = pixel;
                    raster.setPixel(x, y, pixelArray);
                }
            }
            return bufferedImage;
        }
    }
}
