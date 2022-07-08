package org.grits.toolbox.editor.experimentdesigner.pdfgeneration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.encoding.Encoding;
import org.apache.pdfbox.encoding.EncodingManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

public class PDFGenerator {
	
	public static final float ROW_HEIGHT = 15;
	private static float ROW_OFFSET = ROW_HEIGHT + 5;
	
	public static float calculateRowHeight(String text, int columnWidth, PDFont font, float fontSize, float margin) throws IOException {
		int numberOfLines = 1;
		if (text != null) {
			if (text.contains("\n")) {
				 String[] lines = text.split("\n");
				 numberOfLines = lines.length;
				 for (int k = 0; k < lines.length; k++) {
					 // each line can also exceed the columnWidth
					 PDRectangle cell = new PDRectangle(columnWidth, ROW_HEIGHT);
					 List<String> additionalLines = PDFGenerator.divideTextIntoLines(cell, font, fontSize, lines[k], margin);
					 numberOfLines += additionalLines.size()-1;
				 }
			}
			else {
				PDRectangle cell = new PDRectangle(columnWidth, ROW_HEIGHT);
				List<String> additionalLines = PDFGenerator.divideTextIntoLines(cell, font, fontSize, text, margin);
				numberOfLines = additionalLines.size(); 
			}
		}
		
		return numberOfLines * ROW_HEIGHT;
	}
	
	/**
	 * 
	 * @param doc
	 * @param page
	 * @param text
	 * @param font
	 * @param fontSize
	 * @param margin starting x position
	 * @param y starting y position
	 * @param underline
	 * @return the ending x position
	 */
	public static float writeTextOnTheSameLine (PDDocument doc, PDPage page, String text, PDFont font, float fontSize, float margin, float y, boolean underline) throws IOException {
		PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, true);
		contentStream.beginText();
	    contentStream.setFont( font, fontSize );
	    contentStream.moveTextPositionByAmount( margin, y);
	    contentStream.drawString(text);
	    contentStream.endText();
	    
	    float endX = margin + font.getStringWidth(text) / 1000 * fontSize;
	    
	    if (underline) {    
		    //begin to draw our line
	        contentStream.drawLine(margin, y - 1f, endX, y - 1f);
	    }
	    
	    contentStream.close();
	    return endX + font.getStringWidth(" ") / 1000 * fontSize;   // return ending position plus a space
	}

	/**
	 * 
	 * @param doc
	 * @param page
	 * @param text
	 * @param font
	 * @param fontSize
	 * @param margin
	 * @param y
	 * @return
	 * @throws IOException
	 */
	public static float writeSimpleText (PDDocument doc, PDPage page, String text, PDFont font, float fontSize, float margin, float y) throws IOException {
		PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, true);
		contentStream.beginText();
	    contentStream.setFont( font, fontSize );
	    contentStream.moveTextPositionByAmount( margin, y);
	    contentStream.drawString(text);
	    contentStream.endText();
	    contentStream.close();
	    return y - ROW_OFFSET;
	}
	
	/**
	 * 
	 * @param doc
	 * @param page
	 * @param font
	 * @param fontSize
	 * @param text
	 * @param offset
	 * @return the final offset of the y index
	 * @throws IOException
	 */
	public static float drawMultilineString (PDDocument doc, PDPage page, PDFont font, float fontSize, String text, float y, float MARGIN, float LENGTH) throws IOException {
		
		PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, true);
		
		float leading = 1.5f * fontSize;

	    PDRectangle mediabox = page.findMediaBox();
	    float startX = mediabox.getLowerLeftX() + MARGIN;
	    float startY = y;
	    
	    if (text.contains("\n")) {
	    	contentStream.beginText();
			contentStream.moveTextPositionByAmount(startX,startY);
			contentStream.setFont(font, fontSize);
			String[] lines = text.split("\n");
			contentStream.appendRawCommands(leading + " TL\n");
			for (int k = 0; k < lines.length; k++) {
				 
				 List<String> additionalLines = PDFGenerator.divideTextIntoLines(mediabox, font, fontSize, lines[k], MARGIN);
				 if (additionalLines.size() > 1) {
					 for (Iterator iterator = additionalLines.iterator(); iterator
							.hasNext();) {
						String line = (String) iterator.next();
						contentStream.drawString(fixEncoding(line));
						startY -= leading;
						contentStream.appendRawCommands("T*\n");
						
						if (startY < MARGIN) {
							// need to start a new page
							contentStream.endText(); 
						    contentStream.close(); 
							
						    PDPage newPage = new PDPage (PDPage.PAGE_SIZE_LETTER);
						    doc.addPage(newPage);
						    contentStream = new PDPageContentStream(doc, newPage, true, true);
						    startY = LENGTH;
						    contentStream.beginText();
						    contentStream.setFont(font, fontSize);
						    contentStream.moveTextPositionByAmount(startX, startY);   
						    contentStream.appendRawCommands(leading + " TL\n");
						}
						
					 }
				 }
				 else {
					 if (lines[k] != null && lines[k].length() > 1) {
						 contentStream.drawString(fixEncoding(lines[k]));
						 startY -= leading;
						 if (k < lines.length - 1) {
							 contentStream.appendRawCommands("T*\n");
					 	 }
					 }
				 }
			 
				 if (startY < MARGIN) {
					// need to start a new page
					contentStream.endText(); 
				    contentStream.close(); 
					
				    PDPage newPage = new PDPage (PDPage.PAGE_SIZE_LETTER);
				    doc.addPage(newPage);
				    contentStream = new PDPageContentStream(doc, newPage, true, true);
				    startY = LENGTH;
				    contentStream.beginText();
				    contentStream.setFont(font, fontSize);
				    contentStream.moveTextPositionByAmount(startX, startY);   
				    contentStream.appendRawCommands(leading + " TL\n");
				}
			}
		} else {
 
		    List<String> lines = PDFGenerator.divideTextIntoLines (mediabox, font, fontSize, text, MARGIN);
	
		    contentStream.beginText();
		    contentStream.setFont(font, fontSize);
		    contentStream.moveTextPositionByAmount(startX, startY);            
		    for (String line: lines)
		    {
		        contentStream.drawString(fixEncoding(line));
		        contentStream.moveTextPositionByAmount(0, -leading);
		        
		        startY -= leading;
		        if (startY < MARGIN) {
		        	// need to start a new page
		        	contentStream.endText(); 
		    	    contentStream.close();
		    	    PDPage newPage = new PDPage (PDPage.PAGE_SIZE_LETTER);
		    	    doc.addPage(newPage);
		    	    contentStream = new PDPageContentStream(doc, newPage, true, true);
		    	    startY = LENGTH;
		    	    contentStream.beginText();
		    	    contentStream.setFont(font, fontSize);
		    	    contentStream.moveTextPositionByAmount(startX, startY);    
		        }
		    }
		}
	    contentStream.endText(); 
	    contentStream.close();
	    
	    return startY;
	}
	
	private static String fixEncoding (String text) {
		char[] tc = text.toCharArray();
        StringBuilder te = new StringBuilder();
        try {
	        Encoding e =
	                EncodingManager.INSTANCE.getEncoding(COSName.WIN_ANSI_ENCODING);           
	        for (int i = 0; i < tc.length; i++) {
	            Character c = tc[i];
	            int code = 0;
	            if(Character.isWhitespace(c)){
	                code = e.getCode("space");
	            }else{
	                code = e.getCode(e.getNameFromCharacter(c));
	            }               
	            te.appendCodePoint(code);
	        }
        } catch (IOException e) {
        	// if we cannot fix the encoding, simply return the original string
        	return text;
        }
        
        return te.toString();
	}

    // Configures basic setup for the table and draws it page by page
    public float drawTable(PDDocument doc, PDPage currentPage, Table table, boolean drawGrid, float currentY) throws IOException {
    	
    	float rowHeight = Math.max(table.getRowHeight(), table.getMaxRowHeight());
        // Calculate pagination
    	Integer numberOfPages;
    	Integer rowsFirstPage;
    	Integer rowsPerPage;
    	if (currentPage != null) {
    		rowsFirstPage = new Double(Math.floor(table.getHeight() / rowHeight)).intValue() - 1;
    		Integer rowsPerOtherPages = new Double(Math.floor(table.getMaxHeight() / rowHeight)).intValue() - 1;
    		if (table.getNumberOfRows() == 0)
    			numberOfPages = 0;
    		else {
    			Integer remainingRows;
    			if ((remainingRows = table.getNumberOfRows() - rowsFirstPage) > 0)
    				numberOfPages = 1 + new Double(Math.ceil(remainingRows.floatValue() / rowsPerOtherPages)).intValue();
    			else 
    				numberOfPages = 1;
    		}
    		rowsPerPage = rowsPerOtherPages;
    		 
    	} else {
	        rowsPerPage = new Double(Math.floor(table.getMaxHeight() / rowHeight)).intValue() - 1; // subtract
	        numberOfPages = new Double(Math.ceil(table.getNumberOfRows().floatValue() / rowsPerPage)).intValue();
	        rowsFirstPage = rowsPerPage;
    	}

        float nextY = currentY == -1 ? table.getHeight() : currentY;   // initialize
        int startRange = 0;
        // Generate each page, get the content and draw it
        for (int pageCount = 0; pageCount < numberOfPages; pageCount++) {
        	PDPage page;
        	if (pageCount == 0 && currentPage != null) {
        		PDPageContentStream contentStream = generateContentStream(doc, currentPage, table);
        		String[][] currentPageContent = getContentForCurrentPage(table, startRange, rowsFirstPage, pageCount);
                nextY = drawCurrentPage(table, currentPageContent, contentStream, drawGrid, currentY);
                startRange = rowsFirstPage;
        	}
        	else {
        		page = generatePage(doc, table);
        		nextY = table.getMaxHeight();
        		PDPageContentStream contentStream = generateContentStream(doc, page, table);
        		String[][] currentPageContent = getContentForCurrentPage(table, startRange, rowsPerPage, pageCount);
        		startRange = rowsPerPage;
                nextY = drawCurrentPage(table, currentPageContent, contentStream, drawGrid, -1);
        	}
        } 
        return nextY;
    }

    // Draws current page table grid and border lines and content
    private float drawCurrentPage(Table table, String[][] currentPageContent, PDPageContentStream contentStream, boolean drawGrid, float currentY)
            throws IOException {
        float tableTopY = table.isLandscape() ? table.getPageSize().getWidth() - table.getMargin()*2 : table.getPageSize().getHeight() - table.getMargin()*2;

        // Draws grid and borders
        if (drawGrid) drawTableGrid(table, currentPageContent, contentStream, (currentY == -1 ? tableTopY : currentY));

        // Position cursor to start drawing content
        float nextTextX = table.getMargin() + table.getCellMargin();
        // Calculate center alignment for text in cell considering font height
        float nextTextY = (currentY == -1 ? tableTopY : currentY) - (table.getRowHeight() / 2)
                - ((table.getTextFont().getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * table.getFontSize()) / 4);

        // Write column headers
        writeContentLine(table.getColumnsNamesAsArray(), contentStream, nextTextX, nextTextY, table, table.getHeaderFont());
        nextTextY -= table.getRowHeight();
        nextTextX = table.getMargin() + table.getCellMargin();

        // Write content
        for (int i = 0; i < currentPageContent.length; i++) {
            writeContentLine(currentPageContent[i], contentStream, nextTextX, nextTextY, table, table.getTextFont());
            nextTextY -= table.getMaxRowHeight();
            nextTextX = table.getMargin() + table.getCellMargin();
        }

        contentStream.close();
        
        return nextTextY;
    }

    // Writes the content for one line
    private void writeContentLine(String[] lineContent, PDPageContentStream contentStream, float nextTextX, float nextTextY,
            Table table, PDFont font) throws IOException {
    	float leading = 1.2f * table.getFontSize();
    	
        for (int i = 0; i < table.getNumberOfColumns(); i++) {
            String text = lineContent[i];
            Column column = table.getColumns().get(i);
            contentStream.beginText();
            contentStream.setFont(font, table.getFontSize());
            contentStream.moveTextPositionByAmount(nextTextX, nextTextY);
            
            if (text != null && text.contains("\n")) {
				 String[] lines = text.split("\n");
				 contentStream.appendRawCommands(table.getRowHeight() + " TL\n");
				 for (int k = 0; k < lines.length; k++) {
					 // each line can also exceed the columnWidth
					 PDRectangle cell = new PDRectangle(column.getWidth(), table.getRowHeight());
					 List<String> additionalLines = PDFGenerator.divideTextIntoLines(cell, table.getTextFont(), table.getFontSize(), lines[k], table.getCellMargin());
					 if (additionalLines.size() > 1) {
						 for (Iterator iterator = additionalLines.iterator(); iterator
								.hasNext();) {
							String line = (String) iterator.next();
							contentStream.drawString(fixEncoding(line));
							contentStream.appendRawCommands("T*\n");
						}
					 }
					 else {
						 contentStream.drawString(fixEncoding(lines[k]));
						 if (k < lines.length - 1) {
							 contentStream.appendRawCommands("T*\n");
						 }
					 }
				 }
			} else {
				 PDRectangle cell = new PDRectangle(column.getWidth(), table.getRowHeight());
				 List<String> additionalLines = PDFGenerator.divideTextIntoLines(cell, table.getTextFont(), table.getFontSize(), text!= null ? text: "", table.getCellMargin());     
				 for (String line: additionalLines)
				 {
				        contentStream.drawString(fixEncoding(line));
				        contentStream.moveTextPositionByAmount(0, -leading);
				 }
			}
            
            contentStream.endText();
            nextTextX += column.getWidth();   
        }
    }

    private void drawTableGrid(Table table, String[][] currentPageContent, PDPageContentStream contentStream, float tableTopY)
            throws IOException {
        // Draw row lines
        float nextY = tableTopY;
        for (int i = 0; i <= currentPageContent.length + 1; i++) {
            contentStream.drawLine(table.getMargin(), nextY, table.getMargin() + table.getWidth(), nextY);
            if (i > 0) nextY -= table.getMaxRowHeight(); // for the rest of the rows
            else nextY -= table.getRowHeight();   // for the header only
        }

        // Draw column lines
        final float tableYLength = table.getRowHeight() + (table.getMaxRowHeight() * currentPageContent.length);
        final float tableBottomY = tableTopY - tableYLength;
        float nextX = table.getMargin();
        for (int i = 0; i < table.getNumberOfColumns(); i++) {
            contentStream.drawLine(nextX, tableTopY, nextX, tableBottomY);
            nextX += table.getColumns().get(i).getWidth();
        }
        contentStream.drawLine(nextX, tableTopY, nextX, tableBottomY);
    }

    private String[][] getContentForCurrentPage(Table table, int startRange, Integer rowsPerPage, int pageCount) {
        //int startRange = pageCount * rowsPerPage;
        int endRange = startRange + rowsPerPage;
        if (endRange > table.getNumberOfRows()) {
            endRange = table.getNumberOfRows();
        }
        return Arrays.copyOfRange(table.getContent(), startRange, endRange);
    }

    private PDPage generatePage(PDDocument doc, Table table) {
        PDPage page = new PDPage();
        page.setMediaBox(table.getPageSize());
        page.setRotation(table.isLandscape() ? 90 : 0);
        doc.addPage(page);
        return page;
    }

    private PDPageContentStream generateContentStream(PDDocument doc, PDPage page, Table table) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, true);
        // User transformation matrix to change the reference when drawing.
        // This is necessary for the landscape position to draw correctly
        if (table.isLandscape()) {
            contentStream.concatenate2CTM(0, 1, -1, 0, table.getPageSize().getWidth(), 0);
        }
        contentStream.setFont(table.getTextFont(), table.getFontSize());
        return contentStream;
    }
    
    public static List<String> divideTextIntoLines (PDRectangle mediabox, PDFont font, float fontSize, String text, float margin) throws IOException {
	    float width = mediabox.getWidth() - 2*margin;
	    
	    List<String> lines = new ArrayList<String>();
	    int lastSpace = -1;
	    while (text.length() > 0)
	    {
	        int spaceIndex = text.indexOf(' ', lastSpace + 1);
	        if (spaceIndex < 0)  // no more spaces
	        {
	        	float size = fontSize * font.getStringWidth(text) / 1000;
	        	if (size > width) { // the remaining text still longer than the line...
	        		if (lastSpace > 0) {
	        			// try to put it in the next line
	        			String subString = text.substring(0, lastSpace);
	        			lines.add(subString);
	        			text = text.substring(lastSpace).trim();
	        			lastSpace = -1;
	        		}
	        		else {
	        			// single word longer than the line
	        			// break it
	        			int dashIndex = text.indexOf('-');
	        			if (dashIndex != -1) {
	        				// break from the dash
	        				String subString = text.substring (0, dashIndex);
	        				lines.add(subString);
	        				text = text.substring(dashIndex).trim();
	        			} else {
	        				// no dashes - break from the line length
	        				float fontWidth = font.getAverageFontWidth();
	        				int length = (int) Math.ceil ((1000 * width / fontSize) /fontWidth / 0.865);
	        				if (length < text.length()) {
	        					String subString = text.substring(0, length);
	        					lines.add(subString);
	        					text = text.substring(length).trim();
	        				} else {
	        					// wrong calculation
	        					lines.add(text);
	        					text="";
	        				}
	        			}
	        		}
	        	} else {
	        		lines.add(text);
	        		text = "";
	        	}
	        }
	        else
	        {
	            String subString = text.substring(0, spaceIndex);
	            float size = fontSize * font.getStringWidth(subString) / 1000;
	            if (size > width)
	            {
	                if (lastSpace < 0) {// So we have a word longer than the line... draw it anyways
	                	// split from the length of the line and put a dash
	                    //lastSpace = spaceIndex;
	                	int dashIndex = subString.indexOf('-');
	                	if (dashIndex != -1)
	                		lastSpace = dashIndex;
	                	else {
	                		float fontWidth = font.getAverageFontWidth();
	        				int length = (int) Math.floor ((1000 * width / fontSize) / fontWidth / 0.865);
	                		if (length < text.length()) 
	                			lastSpace = length;
	                		else
	                			lastSpace = spaceIndex;
	                	}
	                } 
	                subString = text.substring(0, lastSpace);
	                lines.add(subString);
	                text = text.substring(lastSpace).trim();
	                lastSpace = -1;
	            }
	            else
	            {
	                lastSpace = spaceIndex;
	            }
	        }
	    }
	    
	    return lines;
	}
}
