/**
 * 
 */
package org.grits.toolbox.editor.experimentdesigner.wordgeneration;

import java.math.BigInteger;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.io.ProjectDetailsHandler;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.property.project.ProjectCollaborator;
import org.grits.toolbox.core.datamodel.property.project.ProjectDetails;
import org.grits.toolbox.core.datamodel.property.project.ProjectEvent;
import org.grits.toolbox.core.datamodel.property.project.ProjectTasklist;
import org.grits.toolbox.core.preference.project.CollaboratorTableColumn;
import org.grits.toolbox.editor.experimentdesigner.pdfgeneration.ProjectReportPage;

/**
 * 
 *
 */
public class ProjectReportWordPage
{
	private static final Logger logger = Logger.getLogger(ProjectReportWordPage.class);

	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	public static void addProjectPage(Entry projectEntry, 
			ProjectProperty projectProperty, XWPFDocument document)
	{
		if(document != null)
		{
			try
			{
				XWPFParagraph p1 = writeHeadline(document, "Project Information", 14);
				p1.createRun().addBreak();
				addLine(p1, "Name", projectEntry.getDisplayName(), 3);
				addLine(p1, "Date Created", projectEntry.getCreationDate().toString(), 2);

				ProjectDetails projectDetails = 
						ProjectDetailsHandler.getProjectDetails(projectEntry);
				if(projectDetails != null)
				{
					addLine(p1, "Description", projectDetails.getDescription(), 2);

					addCollaboratorPart(document, projectDetails);
					addTasklistPart(document, projectDetails);
					addEventPart(document, projectDetails);
					addKeywordsPart(document, projectDetails);
				}
			} catch (Exception ex)
			{
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	private static void addCollaboratorPart(XWPFDocument document, ProjectDetails projectDetails)
	{
		try
		{
			writeUnderlined(document, "Project Collaborators", 12);
			XWPFParagraph p1 = null;
			int i = 1;
			for(ProjectCollaborator collaborator : projectDetails.getCollaborators())
			{
				try
				{
					p1 = writeHeadline(document, i + ". " + CollaboratorTableColumn.getColumnValue(
							collaborator, CollaboratorTableColumn.COLUMNS[0]), 12);
					for(int  j = 0; j < CollaboratorTableColumn.COLUMNS.length; j++)
					{
						addLine(p1, CollaboratorTableColumn.COLUMNS[j],
								CollaboratorTableColumn.getColumnValue(collaborator,
										CollaboratorTableColumn.COLUMNS[j]));
					}
					i++;
				} catch (Exception ex)
				{
					logger.error(ex.getMessage(), ex);
				}
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addTasklistPart(XWPFDocument document, ProjectDetails projectDetails)
	{
		try
		{
			writeUnderlined(document, "Tasklist", 12);
			XWPFTable table = document.createTable();
			XWPFTableRow row = table.getRow(0);
			addTableHeadlineCell(row.getCell(0), "Task", 8000);
			addTableHeadlineCell(row.addNewTableCell(),"Person", 8000);
			addTableHeadlineCell(row.addNewTableCell(),"Status", 8000);
			addTableHeadlineCell(row.addNewTableCell(),"Due Date", 8000);
			addTableHeadlineCell(row.addNewTableCell(),"Role", 7000);
			addTableHeadlineCell(row.addNewTableCell(),"Group/P.I.", 8000);
			addTableHeadlineCell(row.addNewTableCell(),"Description", 8000);
			for(ProjectTasklist task : projectDetails.getTasklists())
			{
				addTaskRow(table, task);
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addTaskRow(XWPFTable table, ProjectTasklist task)
	{
		XWPFTableRow row = table.createRow();
		row.getCell(0).setText(task.getTask());
		row.getCell(1).setText(task.getPerson());
		row.getCell(2).setText(task.getStatus());
		String dueDate = task.getDueDate() == null 
				? "" : dateFormat.format(task.getDueDate());
		row.getCell(3).setText(dueDate);
		row.getCell(4).setText(task.getRole());
		row.getCell(5).setText(task.getGroupOrPIName());
		row.getCell(6).setText(task.getDescription());
	}

	private static void addEventPart(XWPFDocument document, ProjectDetails projectDetails)
	{
		try
		{
			document.createParagraph().createRun().addBreak();
			writeUnderlined(document, "Events", 12);
			XWPFTable table = document.createTable();
			XWPFTableRow row = table.getRow(0);
			addTableHeadlineCell(row.getCell(0),"Action", 20000);
			addTableHeadlineCell(row.addNewTableCell(),"Date", 10000);
			addTableHeadlineCell(row.addNewTableCell(),"Description", 25000);
			for(ProjectEvent event : projectDetails.getEvents())
			{
				addEventRow(table, event);
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addEventRow(XWPFTable table, ProjectEvent event)
	{
		XWPFTableRow thisRow = table.createRow();
		String value = event.getProjectAction() == null
				|| event.getProjectAction().getAction() == null
				? "" : event.getProjectAction().getAction();
		thisRow.getCell(0).setText(value);
		value = event.getEventDate() == null 
				? "" : dateFormat.format(event.getEventDate());
		thisRow.getCell(1).setText(value);
		thisRow.getCell(2).setText(event.getDescription());
	}

	private static void addKeywordsPart(XWPFDocument document, ProjectDetails projectDetails)
	{
		try
		{
			document.createParagraph().createRun().addBreak();
			XWPFParagraph p1 = writeUnderlined(document, "Keywords", 12);
			String keywords = ProjectReportPage.getCSKeywords(projectDetails.getKeywords());
			if(!keywords.isEmpty())
			{
				addLine(p1, "", keywords, 0);
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addTableHeadlineCell(XWPFTableCell cell, String value, int width)
	{
		cell.setColor("C8C8C8");
		cell.getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(width));
		if(cell.getParagraphs().iterator().hasNext())
		{
			XWPFParagraph paragraph = cell.getParagraphs().iterator().next();
			paragraph.setAlignment(ParagraphAlignment.CENTER);
			XWPFRun runCell = paragraph.createRun();
			runCell.setBold(true);
			runCell.setText(value);
		}
	}

	private static void addLine(XWPFParagraph p1, String label, String value)
	{
		int numberOfTabs = 
				(int) Math.ceil(((double) 18 - label.length()) / 6);
		addLine(p1, label, value, numberOfTabs);
	}

	private static void addLine(XWPFParagraph p1, String label, String value,
			int numberOfTabs) {
		XWPFRun r1 = p1.createRun();
		r1.setBold(true);
		r1.setText(label);
		logger.debug("label.length" + label.length());
		logger.debug("numberOfTabs" + numberOfTabs);
		XWPFRun r2 = p1.createRun();
		for(int i = 0; i < numberOfTabs; i++)
			r2.addTab();
		r2.setText(value);
		r2.addCarriageReturn();
	}

	private static XWPFParagraph writeUnderlined(XWPFDocument document,
			String value, int fontSize)
	{
		XWPFParagraph paragraphOne = document.createParagraph();
		paragraphOne.setAlignment(ParagraphAlignment.LEFT);
		XWPFRun paragraphOneRunOne = paragraphOne.createRun();
		paragraphOneRunOne.setBold(true);
		paragraphOneRunOne.setFontSize(fontSize);
		paragraphOneRunOne.setText(value);
		paragraphOneRunOne.setUnderline(UnderlinePatterns.SINGLE);
		paragraphOneRunOne.addCarriageReturn();
		return paragraphOne;
	}
	


	private static XWPFParagraph writeHeadline(XWPFDocument document,
			String value, int fontSize)
	{
		XWPFParagraph paragraphOne = document.createParagraph();
		paragraphOne.setAlignment(ParagraphAlignment.LEFT);
		XWPFRun paragraphOneRunOne = paragraphOne.createRun();
		paragraphOneRunOne.setBold(true);
		paragraphOneRunOne.setFontSize(fontSize);
		paragraphOneRunOne.setText(value);
		paragraphOneRunOne.addCarriageReturn();
		return paragraphOne;
	}
}
