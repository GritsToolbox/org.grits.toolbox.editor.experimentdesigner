/**
 * 
 */
package org.grits.toolbox.editor.experimentdesigner.pdfgeneration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.io.ProjectDetailsHandler;
import org.grits.toolbox.core.datamodel.property.ProjectProperty;
import org.grits.toolbox.core.datamodel.property.project.ProjectCollaborator;
import org.grits.toolbox.core.datamodel.property.project.ProjectDetails;
import org.grits.toolbox.core.datamodel.property.project.ProjectEvent;
import org.grits.toolbox.core.datamodel.property.project.ProjectTasklist;
import org.grits.toolbox.core.preference.project.CollaboratorTableColumn;

/**
 * 
 *
 */
public class ProjectReportPage
{
	private static final Logger logger = Logger.getLogger(ProjectReportPage.class);

	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	private static final PDRectangle PAGE_SIZE = ExperimentDesignRDFReport.PAGE_SIZE;
	private static final PDFont BOLD_FONT = ExperimentDesignRDFReport.BOLD_FONT;
	private static final float FONT_SIZE = ExperimentDesignRDFReport.FONT_SIZE;
	private static final float MARGIN = ExperimentDesignRDFReport.MARGIN;
	private static final PDFont TEXT_FONT = ExperimentDesignRDFReport.TEXT_FONT;
	private static final float LENGTH = ExperimentDesignRDFReport.LENGTH;

	private static final float CELL_MARGIN = ExperimentDesignRDFReport.CELL_MARGIN;

	public static void addProjectPage(Entry projectEntry, 
			ProjectProperty projectProperty, PDDocument document, float yIndex)
	{
		if(document != null)
		{
			try
			{
				PDPage page = new PDPage(PDPage.PAGE_SIZE_LETTER);

				document.addPage(page);
				float x;

				yIndex = PDFGenerator.writeSimpleText(document, page, 
						"Project Information", BOLD_FONT, FONT_SIZE, MARGIN, yIndex);

				x = PDFGenerator.writeTextOnTheSameLine (document, page, "Name",
						TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
				yIndex = PDFGenerator.writeSimpleText(document, page, 
						": " + projectEntry.getDisplayName(), TEXT_FONT, FONT_SIZE, x, yIndex);

				x = PDFGenerator.writeTextOnTheSameLine (document, page, "Date Created",
						TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
				yIndex = PDFGenerator.writeSimpleText(document, page, 
						": " + projectEntry.getCreationDate().toString(),
						TEXT_FONT, FONT_SIZE, x, yIndex);

				ProjectDetails projectDetails = 
						ProjectDetailsHandler.getProjectDetails(projectEntry);
				if(projectDetails != null)
				{
					if (projectDetails.getDescription() != null)
					{
						x = PDFGenerator.writeTextOnTheSameLine (document, page, 
								"Description", TEXT_FONT, FONT_SIZE, MARGIN, yIndex, true);
						yIndex = PDFGenerator.drawMultilineString(document, page, 
								TEXT_FONT, FONT_SIZE, ": " + projectDetails.getDescription(),
								yIndex, x, LENGTH);
					}

					Object[] pageAndYIndex = new Object[2];
					pageAndYIndex[0] = page;
					pageAndYIndex[1] = yIndex;

					addExtraLine(pageAndYIndex);
					addProjectCollaboratorsPart(projectDetails, document, pageAndYIndex);

					addExtraLine(pageAndYIndex);
					addProjectTasklistPart(projectDetails, document, pageAndYIndex);

					addExtraLine(pageAndYIndex);
					addProjectEventsPart(projectDetails, document, pageAndYIndex);

					addExtraLine(pageAndYIndex);
					addProjectKeywordsPart(projectDetails, document, pageAndYIndex);
				}
			} catch (Exception ex)
			{
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	private static void addProjectCollaboratorsPart(
			ProjectDetails projectDetails, PDDocument document, Object[] pageAndYIndex)
	{
		int i = 1;
		try
		{
			checkMargin(document, pageAndYIndex);
			pageAndYIndex[1] = PDFGenerator.writeSimpleText(document, (PDPage) pageAndYIndex[0], 
					"Project Collaborators", BOLD_FONT, FONT_SIZE, MARGIN,
					(float) pageAndYIndex[1]);

			for(ProjectCollaborator collaborator : projectDetails.getCollaborators())
			{
				try
				{
					checkMargin(document, pageAndYIndex);
					addExtraLine(pageAndYIndex);
					pageAndYIndex[1] = PDFGenerator.writeSimpleText(document,
							(PDPage) pageAndYIndex[0], i + ". " + CollaboratorTableColumn.getColumnValue(
									collaborator, CollaboratorTableColumn.COLUMNS[0]), 
									BOLD_FONT, FONT_SIZE, MARGIN, (float) pageAndYIndex[1]);
					for(int  j = 0; j < CollaboratorTableColumn.COLUMNS.length; j++)
					{
						if(j == 5) 		// j = 5 implies the address of the collaborator
						{
							addMultiLine(document, pageAndYIndex, CollaboratorTableColumn.COLUMNS[j],
									": " + CollaboratorTableColumn.getColumnValue(collaborator,
											CollaboratorTableColumn.COLUMNS[j]));
						}
						else
						{
							addLine(document, pageAndYIndex, CollaboratorTableColumn.COLUMNS[j], 
									": " + CollaboratorTableColumn.getColumnValue(collaborator,
											CollaboratorTableColumn.COLUMNS[j]));
						}
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

	private static void addProjectTasklistPart(ProjectDetails projectDetails,
			PDDocument document, Object[] pageAndYIndex)
	{
		try
		{
			if(!projectDetails.getTasklists().isEmpty())
			{
				List<Column> columns = new ArrayList<Column>();
				columns.add(new Column("Task", 90));
				columns.add(new Column("Person", 80));
				columns.add(new Column("Status", 60));
				columns.add(new Column("Due Date", 70));
				columns.add(new Column("Role", 80));
				columns.add(new Column("Group/P.I.", 80));
				columns.add(new Column("Description", 90));

				int tableSize = projectDetails.getTasklists().size();

				String[][] content = new String[tableSize][7];
				int row = 0;
				float maxRowHeight = PDFGenerator.ROW_HEIGHT;
				for (ProjectTasklist task : projectDetails.getTasklists()) 
				{
					maxRowHeight = writeTaskRow(task, content, row, maxRowHeight);
					row++;
				}
				addTable(document, pageAndYIndex, "Tasklist", columns, content, maxRowHeight);
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addProjectEventsPart(ProjectDetails projectDetails,
			PDDocument document, Object[] pageAndYIndex)
	{
		try
		{
			if(!projectDetails.getEvents().isEmpty())
			{
				List<Column> columns = new ArrayList<Column>();
				columns.add(new Column("Action", 125));
				columns.add(new Column("Date", 125));
				columns.add(new Column("Description", 300));

				int tableSize = projectDetails.getEvents().size();

				String[][] content = new String[tableSize][4];
				int row = 0;
				float maxRowHeight = PDFGenerator.ROW_HEIGHT;
				for (ProjectEvent event : projectDetails.getEvents()) 
				{
					maxRowHeight = writeEventRow(event, content, row, maxRowHeight);
					row++;
				}
				addTable(document, pageAndYIndex, "Events", columns, content, maxRowHeight);
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addTable(PDDocument document, Object[] pageAndYIndex,
			String title, List<Column> columns, String[][] content, float maxRowHeight)
	{
		try
		{
			float tableStretchHeight = (float) pageAndYIndex[1] == LENGTH
					? (float) pageAndYIndex[1] - 2*MARGIN : (float) pageAndYIndex[1] - MARGIN;

			float actualTableHeight = maxRowHeight*(content.length) + PDFGenerator.ROW_HEIGHT;
			float tableMaxHeight = PAGE_SIZE.getHeight() - (2 * MARGIN);
			logger.debug("actualTableHeight : " + actualTableHeight);
			Table table = ExperimentDesignRDFReport.createTable(columns, content, 
					tableStretchHeight, tableMaxHeight, maxRowHeight);

			if(actualTableHeight > tableStretchHeight
					&& actualTableHeight < PAGE_SIZE.getHeight() - (2*MARGIN))
			{
				pageAndYIndex[0] = new PDPage(PDPage.PAGE_SIZE_LETTER);
				document.addPage((PDPage) pageAndYIndex[0]);
				pageAndYIndex[1] = LENGTH;
				table.setHeight((float) pageAndYIndex[1] - 2*MARGIN);
			}
			pageAndYIndex[1] = PDFGenerator.writeSimpleText(document, (PDPage) pageAndYIndex[0], 
					title, BOLD_FONT, FONT_SIZE, MARGIN, (float) pageAndYIndex[1]);
			pageAndYIndex[1] = (new PDFGenerator()).drawTable(document, (PDPage) pageAndYIndex[0], 
					table, true, (float) pageAndYIndex[1]);
			pageAndYIndex[0] = (PDPage) document.getDocumentCatalog().getAllPages().get(
					document.getDocumentCatalog().getAllPages().size()-1);
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static float writeTaskRow(ProjectTasklist task, String[][] content,
			int row, float maxRowHeight)
	{
		maxRowHeight = assignValue(task.getTask(), content, row, 0, 90, maxRowHeight);
		maxRowHeight = assignValue(task.getPerson(), content, row, 1, 80, maxRowHeight);
		maxRowHeight = assignValue(task.getStatus(), content, row, 2, 60, maxRowHeight);
		String dueDate = task.getDueDate() == null ? "" : dateFormat.format(task.getDueDate());
		maxRowHeight = assignValue(dueDate, content, row, 3, 70, maxRowHeight);
		maxRowHeight = assignValue(task.getRole(), content, row, 4, 80, maxRowHeight);
		maxRowHeight = assignValue(task.getGroupOrPIName(), content, row, 5, 80, maxRowHeight);
		maxRowHeight = assignValue(task.getDescription(), content, row, 6, 90, maxRowHeight);

		return maxRowHeight;
	}

	private static float assignValue(String value, String[][] content, int row,
			int column, int columnSize, float maxRowHeight)
	{
		try
		{
			content[row][column] = value == null ? "" : value;
			maxRowHeight = Math.max(PDFGenerator.calculateRowHeight(
					content[row][column], columnSize, TEXT_FONT, FONT_SIZE, CELL_MARGIN), maxRowHeight);
		} catch (IOException e)
		{
			logger.error(e.getMessage(), e);
		}
		return maxRowHeight;
	}

	private static float writeEventRow(ProjectEvent event, String[][] content,
			int row, float maxRowHeight)
	{
		String value = event.getProjectAction() == null
				|| event.getProjectAction().getAction() == null
				? "" : event.getProjectAction().getAction();
		maxRowHeight = assignValue(value, content, row, 0, 125, maxRowHeight);

		value = event.getEventDate() == null ? "" : dateFormat.format(event.getEventDate());
		logger.debug("date  is " + value);
		maxRowHeight = assignValue(value, content, row, 1, 100, maxRowHeight);

		maxRowHeight = assignValue(event.getDescription(), content, row, 2, 250, maxRowHeight);

		return maxRowHeight;
	}

	private static void addProjectKeywordsPart(ProjectDetails projectDetails,
			PDDocument document, Object[] pageAndYIndex)
	{
		try
		{
			checkMargin(document, pageAndYIndex);
			addExtraLine(pageAndYIndex);
			pageAndYIndex[1] = PDFGenerator.writeSimpleText(document, (PDPage) pageAndYIndex[0], 
					"Keywords", BOLD_FONT, FONT_SIZE, MARGIN, (float) pageAndYIndex[1]);

			String keywords = getCSKeywords(projectDetails.getKeywords());
			if(keywords.isEmpty() && (float) pageAndYIndex[1] != LENGTH)
			{
				pageAndYIndex[1] = (float) pageAndYIndex[1] - 20;
			}
			else
			{
				pageAndYIndex[1] = PDFGenerator.drawMultilineString(document, 
						(PDPage) pageAndYIndex[0], TEXT_FONT, FONT_SIZE, 
						keywords, (float) pageAndYIndex[1], (float) MARGIN, LENGTH);
			}
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	public static String getCSKeywords(Set<String> keywordSet)
	{
		String keywords = "";
		String separator = ", ";
		for(String keyword : keywordSet)
		{
			keywords += keyword + separator;
		}
		if(!keywords.isEmpty())
			keywords = keywords.substring(0, keywords.length() - separator.length());
		return keywords;
	}

	private static void checkMargin(PDDocument document, Object[] pageAndYIndex)
	{
		if ((float) pageAndYIndex[1] < MARGIN)
		{
			document.addPage((PDPage) (pageAndYIndex[0] = new PDPage(PDPage.PAGE_SIZE_LETTER)));
			pageAndYIndex[1] = LENGTH;
		}
	}

	private static void addExtraLine(Object[] pageAndYIndex)
	{
		if ((float) pageAndYIndex[1] != LENGTH) 
			pageAndYIndex[1] = (float) pageAndYIndex[1] - 20;
	}

	private static void addMultiLine(PDDocument document,
			Object[] pageAndYIndex, String label, String value)
	{
		try
		{
			checkMargin(document, pageAndYIndex);
			float x = PDFGenerator.writeTextOnTheSameLine(document,
					(PDPage) pageAndYIndex[0], label,
					TEXT_FONT, FONT_SIZE, MARGIN, (float) pageAndYIndex[1], true);
			value = value == null ? "" : value;
			if(value.isEmpty())
			{
				pageAndYIndex[1] = (float) pageAndYIndex[1] - 20;
			}
			else
			{
				pageAndYIndex[1] = PDFGenerator.drawMultilineString(document, 
						(PDPage) pageAndYIndex[0], TEXT_FONT, FONT_SIZE, 
						value, (float) pageAndYIndex[1], x, LENGTH);
			}
			pageAndYIndex[0] = (PDPage) document.getDocumentCatalog().getAllPages().get(
					document.getDocumentCatalog().getAllPages().size()-1);
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

	private static void addLine(PDDocument document, Object[] pageAndYIndex,
			String label, String value)
	{
		try
		{
			checkMargin(document, pageAndYIndex);
			float x = PDFGenerator.writeTextOnTheSameLine (document, (PDPage) pageAndYIndex[0], 
					label, TEXT_FONT, FONT_SIZE, MARGIN, (float) pageAndYIndex[1], true);
			value = value == null ? "" : value;
			pageAndYIndex[1] = PDFGenerator.writeSimpleText(document, (PDPage) pageAndYIndex[0], 
					value, TEXT_FONT, FONT_SIZE, x, (float) pageAndYIndex[1]);
		} catch (Exception ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}
}
