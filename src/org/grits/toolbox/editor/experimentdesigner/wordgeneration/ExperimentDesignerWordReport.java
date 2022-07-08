package org.grits.toolbox.editor.experimentdesigner.wordgeneration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlException;
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
import org.grits.toolbox.editor.experimentdesigner.pdfgeneration.ExperimentDesignRDFReport;
import org.grits.toolbox.entry.sample.model.Category;
import org.grits.toolbox.entry.sample.model.Component;
import org.grits.toolbox.entry.sample.model.Descriptor;
import org.grits.toolbox.entry.sample.model.DescriptorGroup;
import org.grits.toolbox.entry.sample.model.Sample;
import org.grits.toolbox.entry.sample.ontologymanager.SampleOntologyManager;
import org.grits.toolbox.entry.sample.property.SampleProperty;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;

public class ExperimentDesignerWordReport
{
	
	private static final Logger	logger		= Logger.getLogger(ExperimentDesignerWordReport.class);
	
	private static final float	PAGEWIDTH	= 452;													// Letter
																									// size
																									// -
																									// 612
																									// minus
																									// margins
	private static final float	PAGEHEIGHT	= 622;													// Letter
																									// size
																									// -
																									// 792
																									// minus
																									// margins

	private XWPFDocument		document	= null;

	public ExperimentDesignerWordReport()
	{
		
	}

	public void generateReport(Options options, GraphEditor editor)
	{
		
		this.document = new XWPFDocument();
		
		try
		{
			IEditorInput input = editor.getEditorInput();
			if (input instanceof ViewInput)
			{
				
				Entry entry = ((ViewInput) input).getEntry();
				// get the project information
				Entry projectEntry = DataModelSearch.findParentByType(entry, ProjectProperty.TYPE);
				
				if (options != null && options.getProject())
				{
					ProjectReportWordPage.addProjectPage(projectEntry, (ProjectProperty) projectEntry.getProperty(),
							document);
					if (options.getExpDesign() || options.getSample())
						pageBreak();
				}
				
				// create a page with sample information
				Sample sample = SampleProperty
						.loadAnalyte(DataModelSearch.findParentByType(entry, SampleProperty.TYPE));
				
				if (options != null && options.getSample())
				{
					addSampleInformation(sample);
					if (options.getExpDesign())
						pageBreak();
				}
				
				ExperimentGraph model = editor.getModel();
				
				if (options != null && options.getExpDesign())
					addExperimentDesignInformation(editor, model);
				
				addHeader();
				addFooter();
				
				// ask the user where to save the document
				FileDialog fileDialog = new FileDialog(editor.getSite().getShell(), SWT.SAVE);
				// Set the text
				fileDialog.setText("Select File");
				// Set filter on word files
				fileDialog.setFilterExtensions(new String[] { "*.docx" });
				// Put in a readable name for the filter
				fileDialog.setFilterNames(new String[] { "Word (*.docx)" });
				fileDialog.setFileName(model.getName() + "-experimentdesign.docx");
				fileDialog.setOverwrite(true);
				// Open Dialog and save result of selection
				String selected = fileDialog.open();
				if (selected != null)
				{
					FileOutputStream outStream = new FileOutputStream(selected);
					this.document.write(outStream);
					outStream.close();
				}
			}
		}
		catch (IOException | JAXBException e)
		{
			logger.error(Activator.PLUGIN_ID + " Cannot generate report!", e);
			MessageDialog.openError(editor.getSite().getShell(), "Error",
					"Word Document cannot be generated! Please check the log file");
		}
		catch (InvalidFormatException e)
		{
			logger.error(Activator.PLUGIN_ID + " Cannot generate report!", e);
			MessageDialog.openError(editor.getSite().getShell(), "Error",
					"Word Document cannot be generated! Please check the log file");
		}
		catch (XmlException e)
		{
			logger.error(Activator.PLUGIN_ID + " Cannot generate report!", e);
			MessageDialog.openError(editor.getSite().getShell(), "Error",
					"Word Document cannot be generated! Please check the log file");
		}
	}

	private void pageBreak()
	{
		// make sure to start from a new page
		XWPFParagraph pEmpty = document.createParagraph();
		pEmpty.createRun().addBreak(BreakType.PAGE);
	}
	
	@SuppressWarnings("rawtypes")
	private void addExperimentDesignInformation(GraphEditor editor, ExperimentGraph model)
			throws IOException, InvalidFormatException
	{
		this.writeHeadlineLevel("Experiment Graph", 1);
		
		XWPFParagraph p2 = document.createParagraph();
		p2.setAlignment(ParagraphAlignment.LEFT);

		XWPFRun r1 = p2.createRun();
		r1.setUnderline(UnderlinePatterns.SINGLE);
		r1.setText("Name: ");

		XWPFRun r2 = p2.createRun();
		r2.addTab();
		r2.addTab();
		r2.addTab();
		r2.setText(model.getName());
		r2.addCarriageReturn();

		XWPFRun r3 = p2.createRun();
		r3.setUnderline(UnderlinePatterns.SINGLE);
		r3.setText("Date Created: ");

		XWPFRun r4 = p2.createRun();
		r4.addTab();
		r4.setText(model.getDateCreated().toString());
		r4.addCarriageReturn();

		if (model.getDescription() != null && model.getDescription().length() > 0)
		{
			XWPFRun r5 = p2.createRun();
			r5.setUnderline(UnderlinePatterns.SINGLE);
			r5.setText("Description: ");
			r5.addTab();

			XWPFRun r6 = p2.createRun();
			r6.setText(model.getDescription());
			r6.addCarriageReturn();
		}

		XWPFRun r7 = p2.createRun();
		r7.addCarriageReturn();

		
		
		// Dimension size=f.getPreferredSize();
		Dimension size = model.calculateSize();
		float xscale = PAGEWIDTH / size.width;
		float yscale = PAGEHEIGHT / size.height;
		float scale = Math.min(xscale, yscale);
		if (scale > 1)
			scale = 1;
		BufferedImage imageForPDF = editor.getBufferedImage();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(imageForPDF, "gif", os);
		InputStream is = new ByteArrayInputStream(os.toByteArray());
		r7.addPicture(is, XWPFDocument.PICTURE_TYPE_GIF, null, Units.toEMU(scale * size.width),
				Units.toEMU(scale * size.height));
		
		// add details of protocols
		if (model.getChildren() != null)
		{
			for (Iterator iterator = model.getChildren().iterator(); iterator.hasNext();)
			{
				GraphNode node = (GraphNode) iterator.next();
				if (node instanceof ProtocolNode)
				{
					// one page for each protocol
					XWPFParagraph protocolP = document.createParagraph();
					protocolP.setAlignment(ParagraphAlignment.LEFT);
					
					XWPFRun r = protocolP.createRun();
					r.addBreak(BreakType.PAGE);
					r.setBold(true);
					r.setText("Protocol - ");
					r.setText(((ProtocolNode) node).getLabel());
					r.addCarriageReturn();
					
					if (((ProtocolNode) node).getCreator() != null)
					{
						XWPFRun r8 = protocolP.createRun();
						r8.setUnderline(UnderlinePatterns.SINGLE);
						r8.setText("Created By: ");
						
						XWPFRun r9 = protocolP.createRun();
						r9.addTab();
						r9.setText(((ProtocolNode) node).getCreator());
						r9.addCarriageReturn();
					}
					
					if (node.getDescription() != null && node.getDescription().length() > 0)
					{
						XWPFRun r8 = protocolP.createRun();
						r8.setUnderline(UnderlinePatterns.SINGLE);
						r8.setText("Description: ");
						
						XWPFRun r9 = protocolP.createRun();
						r9.addTab();
						r9.setText(node.getDescription());
						r9.addCarriageReturn();
					}
					
					// add a table for ParameterGroups and Parameters
					addParameterTable((ProtocolNode) node);
					
					addPapers((ProtocolNode) node);
					
				}
			}
		}
	}

	private void addHeader() throws IOException, XmlException
	{
		
		CTP ctp = CTP.Factory.newInstance();
		CTR ctr = ctp.addNewR();
		ctr.addNewRPr();
		CTText textt = ctr.addNewT();
		textt.setStringValue(new Date().toString());
		XWPFParagraph codePara = new XWPFParagraph(ctp, document);
		XWPFParagraph[] newparagraphs = new XWPFParagraph[1];
		newparagraphs[0] = codePara;
		CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
		XWPFHeaderFooterPolicy headerFooterPolicy = new XWPFHeaderFooterPolicy(document, sectPr);
		headerFooterPolicy.createHeader(STHdrFtr.DEFAULT, newparagraphs);
	}
	
	private void addFooter() throws IOException, XmlException
	{
		
		CTP ctp = CTP.Factory.newInstance();

		// add style (s.th.)
		CTPPr ctppr = ctp.addNewPPr();
		CTString pst = ctppr.addNewPStyle();
		pst.setVal("style21");
		CTJc ctjc = ctppr.addNewJc();
		ctjc.setVal(STJc.RIGHT);
		ctppr.addNewRPr();
		
		/*
		 * CTR ctr = ctp.addNewR(); ctr.addNewRPr();
		 * ctp.addNewR().addNewT().setStringValue(new Date().toString());
		 * 
		 * ctppr = ctp.addNewPPr(); pst = ctppr.addNewPStyle();
		 * pst.setVal("style21"); ctjc = ctppr.addNewJc();
		 * ctjc.setVal(STJc.RIGHT); ctppr.addNewRPr();
		 */

		// add everything from the footerXXX.xml you need
		CTR ctr = ctp.addNewR();
		ctr.addNewRPr();
		CTFldChar fch = ctr.addNewFldChar();
		fch.setFldCharType(STFldCharType.BEGIN);

		ctr = ctp.addNewR();
		ctr.addNewInstrText().setStringValue(" PAGE ");
		ctp.addNewR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
		ctp.addNewR().addNewT().setStringValue("1");
		ctp.addNewR().addNewFldChar().setFldCharType(STFldCharType.END);

		XWPFParagraph codePara = new XWPFParagraph(ctp, document);
		XWPFParagraph[] newparagraphs = new XWPFParagraph[1];
		newparagraphs[0] = codePara;
		CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
		XWPFHeaderFooterPolicy headerFooterPolicy = new XWPFHeaderFooterPolicy(document, sectPr);
		headerFooterPolicy.createFooter(STHdrFtr.DEFAULT, newparagraphs);

	}
	
	private void addPapers(ProtocolNode node)
	{
		if (node.getPapers() != null && node.getPapers().size() > 0)
		{
			XWPFParagraph pageP = document.createParagraph();
			pageP.setAlignment(ParagraphAlignment.LEFT);

			XWPFRun r = pageP.createRun();
			r.setBold(true);
			r.addCarriageReturn();
			r.setText("Papers");
			r.addCarriageReturn();
			for (Paper paper : node.getPapers())
			{
				XWPFRun r1 = pageP.createRun();
				r1.setText(paper.toString());
				r1.addCarriageReturn();
				r1.addCarriageReturn();
			}
		}
	}
	
	private void addParameterTable(ProtocolNode node)
	{
		if ((node.getParameters() == null || node.getParameters().size() == 0)
				&& (node.getParameterGroups() == null || node.getParameterGroups().size() == 0))
			return;
		
		XWPFTable t_table = this.createTable();
		XWPFTableRow t_row = t_table.getRow(0);
		this.tableHeadlineCell(t_row.getCell(0), "Parameter/Parameter Group");
		this.tableHeadlineCell(t_row.addNewTableCell(), "Value");
		this.tableHeadlineCell(t_row.addNewTableCell(), "Unit");
		
		if (node.getParameterGroups() != null)
		{
			for (ParameterGroup group : node.getParameterGroups())
			{
				t_row = t_table.createRow();
				this.tableCell(t_row.getCell(0), group.getLabel());
				this.tableCell(t_row.getCell(1), "");
				this.tableCell(t_row.getCell(2), "");
				group.getParameters();
				for (Parameter parameter : group.getParameters())
				{
					t_row = t_table.createRow();
					this.tableCell(t_row.getCell(0), "     " + parameter.getName());
					if (parameter.getValue() != null)
					{
						this.tableCell(t_row.getCell(1), parameter.getValue());
					}
					else
						this.tableCell(t_row.getCell(1), "");
					if (parameter.getUnit() != null)
						this.tableCell(t_row.getCell(2), parameter.getUnit().getLabel());
					else
						this.tableCell(t_row.getCell(2), "");
				}
			}
		}
		
		if (node.getParameters() != null)
		{
			for (Parameter parameter : node.getParameters())
			{
				t_row = t_table.createRow();
				this.tableCell(t_row.getCell(0), parameter.getName());
				if (parameter.getValue() != null)
				{
					this.tableCell(t_row.getCell(1), parameter.getValue());
				}
				else
					this.tableCell(t_row.getCell(1), "");
				if (parameter.getUnit() != null)
					this.tableCell(t_row.getCell(2), parameter.getUnit().getLabel());
				else
					this.tableCell(t_row.getCell(2), "");
			}
		}

	}
	
	private void addSampleInformation(Sample sample)
	{
		
		this.writeHeadlineLevel("Analyte", 1);

		XWPFParagraph p = document.createParagraph();
		p.setAlignment(ParagraphAlignment.LEFT);
		XWPFRun r1 = p.createRun();
		r1.setUnderline(UnderlinePatterns.SINGLE);
		r1.setText("Name: ");
		
		XWPFRun r2 = p.createRun();
		r2.addTab();
		r2.addTab();
		r2.setText(sample.getName());
		r2.addCarriageReturn();
		
		if (sample.getDescription() != null && !sample.getDescription().isEmpty())
		{
			XWPFRun r3 = p.createRun();
			r3.setUnderline(UnderlinePatterns.SINGLE);
			r3.setText("Description: ");
			
			XWPFRun r4 = p.createRun();
			r4.addTab();
			r4.setText(sample.getDescription());
			r4.addCarriageReturn();
		}
		
		XWPFRun rr3 = p.createRun();
		rr3.setBold(true);
		rr3.addCarriageReturn();
		rr3.setText("List of Components:");
		rr3.addCarriageReturn();
		
		int componentNumber = 1;
		for (Component comp : sample.getComponents())
		{
			XWPFRun r4 = p.createRun();
			r4.setText(componentNumber++ + ". " + comp.getLabel());
			r4.addCarriageReturn();
		}
		p.createRun().addBreak(BreakType.PAGE);
		componentNumber = 1;
		for (Component comp : sample.getComponents())
		{
			this.writeHeadlineLevel("Component" + componentNumber++, 2);
			XWPFParagraph p2 = document.createParagraph();
			p2.setAlignment(ParagraphAlignment.LEFT);
			XWPFRun rr4 = p2.createRun();
			rr4.setUnderline(UnderlinePatterns.SINGLE);
			rr4.setText("Name: ");
			
			XWPFRun rr5 = p2.createRun();
			rr5.addTab();
			rr5.addTab();
			rr5.addTab();
			rr5.setText(comp.getLabel());
			rr5.addCarriageReturn();
			if (comp.getDescription() != null && !comp.getDescription().isEmpty())
			{
				XWPFRun r6 = p2.createRun();
				r6.setUnderline(UnderlinePatterns.SINGLE);
				r6.setText("Description: ");
				
				XWPFRun r7 = p2.createRun();
				r7.addTab();
				r7.setText(comp.getDescription());
			}
			
			if (comp.getTemplateUri() != null)
			{
				XWPFRun r6 = p2.createRun();
				r6.setUnderline(UnderlinePatterns.SINGLE);
				r6.setText("Template: ");
				
				XWPFRun r7 = p2.createRun();
				r7.addTab();
				r7.setText(comp.getTemplateUri());
			}
			
			Category sampleInformation = comp.getSampleInformation();
			writeCategoryInComponentPage(sampleInformation);
			
			Category tracking = comp.getTracking();
			writeCategoryInComponentPage(tracking);
			
			Category amount = comp.getAmount();
			writeCategoryInComponentPage(amount);
			
			Category purityQC = comp.getPurityQC();
			writeCategoryInComponentPage(purityQC);
		}
	}

	private String getCategoryLabel(String uri)
	{
		switch (uri)
		{
			case SampleOntologyManager.CATEGORY_SAMPLE_INFO_CLASS_URI:
				return "Sample Information";
			case SampleOntologyManager.CATEGORY_TRACKING_INFO_CLASS_URI:
				return "Sample Tracking";
			case SampleOntologyManager.CATEGORY_AMOUNT_CLASS_URI:
				return "Amount";
			case SampleOntologyManager.CATEGORY_PURITY_QC_CLASS_URI:
				return "Purity Q.C.";
		}
		return null;
	}
	
	private void writeCategoryInComponentPage(Category category)
	{
		if ((category.getDescriptors() == null || category.getDescriptors().size() == 0)
				&& (category.getDescriptorGroups() == null || category.getDescriptorGroups().size() == 0))
			return;
		
		XWPFParagraph p3 = document.createParagraph();
		p3.setAlignment(ParagraphAlignment.LEFT);
		XWPFRun r3 = p3.createRun();
		r3.setText("Category: " + getCategoryLabel(category.getUri()));
		r3.addCarriageReturn();
		
		XWPFTable t_table = this.createTable();
		XWPFTableRow t_row = t_table.getRow(0);
		this.tableHeadlineCell(t_row.getCell(0), "Descriptor/Descriptor Group");
		this.tableHeadlineCell(t_row.addNewTableCell(), "Value");
		this.tableHeadlineCell(t_row.addNewTableCell(), "Unit");
		
		for (Descriptor desc : category.getDescriptors())
		{
			t_row = t_table.createRow();
			writeDescriptorRow(desc, t_row);
		}
		for (DescriptorGroup dg : category.getDescriptorGroups())
		{
			t_row = t_table.createRow();
			this.tableCell(t_row.getCell(0), dg.getLabel());
			this.tableCell(t_row.getCell(1), "");
			this.tableCell(t_row.getCell(2), "");
			for (Descriptor desc : dg.getMandatoryDescriptors())
			{
				t_row = t_table.createRow();
				writeDescriptorRow(desc, t_row);
			}
			for (Descriptor desc : dg.getOptionalDescriptors())
			{
				t_row = t_table.createRow();
				writeDescriptorRow(desc, t_row);
			}
		}
	}

	private void writeDescriptorRow(Descriptor desc, XWPFTableRow t_row)
	{
		this.tableCell(t_row.getCell(0), desc.getLabel());
		if (desc.getValue() != null)
		{
			this.tableCell(t_row.getCell(1), desc.getValue());
		}
		if (desc.getSelectedMeasurementUnit() != null)
		{
			String selectedUnitLabel = desc.getUnitLabelFromUri(desc.getSelectedMeasurementUnit());
			this.tableCell(t_row.getCell(2), selectedUnitLabel);
		}
	}

	private void writeHeadlineLevel(String a_class, Integer a_level)
	{
		this.document.createParagraph().createRun().addBreak();
		XWPFParagraph t_paragraphOne = this.document.createParagraph();
		t_paragraphOne.setAlignment(ParagraphAlignment.LEFT);
		// t_paragraphOne.setStyle("Heading" + a_level.toString()); // need a
		// template.dotx to be able to do this!
		XWPFRun paragraphOneRunOne = t_paragraphOne.createRun();
		switch (a_level)
		{
			case 1:
				paragraphOneRunOne.setBold(true);
				paragraphOneRunOne.setFontSize(14);
				break;
			case 2:
				paragraphOneRunOne.setBold(true);
				paragraphOneRunOne.setFontSize(12);
				break;
		}
		
		paragraphOneRunOne.setText(a_class);
	}

	private void tableCell(XWPFTableCell a_cell, String a_text)
	{
		List<XWPFParagraph> t_paragraphCell = a_cell.getParagraphs();
		for (XWPFParagraph t_xwpfParagraph : t_paragraphCell)
		{
			t_xwpfParagraph.setAlignment(ParagraphAlignment.LEFT);
			XWPFRun runCell = t_xwpfParagraph.createRun();
			runCell.setText(a_text);
		}
	}
	
	private void tableHeadlineCell(XWPFTableCell a_cell, String a_text)
	{
		a_cell.setColor("C8C8C8");
		List<XWPFParagraph> t_paragraphCell = a_cell.getParagraphs();
		for (XWPFParagraph t_xwpfParagraph : t_paragraphCell)
		{
			t_xwpfParagraph.setAlignment(ParagraphAlignment.CENTER);
			XWPFRun runCell = t_xwpfParagraph.createRun();
			runCell.setBold(true);
			runCell.setText(a_text);
		}
	}
	
	private XWPFTable createTable()
	{
		return this.document.createTable();
	}
}
