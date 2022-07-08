package org.grits.toolbox.editor.experimentdesigner.config;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.grits.toolbox.editor.experimentdesigner.Activator;

public class ImageRegistry
{
	private static Logger logger = Logger.getLogger(ImageRegistry.class);
	private static final String IMAGE_PATH = "icons" + File.separator;
	private static Map<ExperimentDesignerImage, ImageDescriptor> imageCache = new HashMap<ExperimentDesignerImage, ImageDescriptor>();

	public static ImageDescriptor getImageDescriptor(ExperimentDesignerImage sampleImage)
	{
		logger.debug("Get image from experiment designer plugin : " + sampleImage);

		ImageDescriptor imageDescriptor = null;
		if(sampleImage != null)
		{
			imageDescriptor = imageCache.get(sampleImage);
			if(imageDescriptor == null)
			{
				logger.debug("ImageDescriptor not found in cache");
				URL fullPathString = FileLocator.find(
						Platform.getBundle(Activator.PLUGIN_ID), new Path(IMAGE_PATH + sampleImage.iconName), null);

				logger.debug("Loading image from url : " + fullPathString);
				if(fullPathString != null)
				{
					imageDescriptor = ImageDescriptor.createFromURL(fullPathString);
					imageCache.put(sampleImage, imageDescriptor);
				}
			}
		}
		else
			logger.error("Cannot load image from experiment designer plugin (image name is null)");

		return imageDescriptor;
	}
	
	public static ImageDescriptor getSmallIcon (String iconName) {
		URL fullPathString = FileLocator.find(
				Platform.getBundle(Activator.PLUGIN_ID), new Path(IMAGE_PATH + "16" + File.separator + iconName), null);
		return ImageDescriptor.createFromURL(fullPathString);
	}
	
	public static ImageDescriptor getLargeIcon (String iconName) {
		URL fullPathString = FileLocator.find(
				Platform.getBundle(Activator.PLUGIN_ID), new Path(IMAGE_PATH + "24" + File.separator + iconName), null);
		return ImageDescriptor.createFromURL(fullPathString);
	}


	/**
	 ***********************************
	 *			Icons
	 ***********************************
	 */
	public enum ExperimentDesignerImage
	{
		EXPERIMENTDESIGNICON("experimentdesign.png"),
		SAVEASTEMPLATE ("saveas-template16.png"),
		PRINTICON("printer-icon.png"),
		SAVEASTEMPLATEBIG ("saveas-template.png"),
		SAVEASEMPLATEBIGDISABLED("saveas-template-disabled.png"),
		CONNECTION16("connection_s16.gif"),
		CONNECTION24("connection_s24.gif"),
		WORDICON("page-white-word-icon.png"),
		PDFICON("pdf-icon.png"),
		ELLIPSEICON("ellipse16.gif");

		private String iconName = null;
		private ExperimentDesignerImage(String iconName)
		{
			this.iconName  = iconName;
		}
	}


}
