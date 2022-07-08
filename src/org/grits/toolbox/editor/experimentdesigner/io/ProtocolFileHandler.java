package org.grits.toolbox.editor.experimentdesigner.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;

public class ProtocolFileHandler {
	
	private static final Logger logger = Logger.getLogger(ProtocolFileHandler.class);

	/**
	 * copy the file with given name from the workspace/projectName/files folder into the configuration directory
	 * @param filename
	 * @param projectName
	 * @return
	 */
	public static String copyFromWorkspaceToConfig (String filename, String projectName) {
		// template files are in the configuration directory
		// copy from that directory into the workspace
		String configFolderLocation = PropertyHandler.getVariable("configuration_location");
		
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator + projectName;
        String uploadFolderLocation = projectFolderLocation
                + File.separator
                + "files";
        
        String experimentSubFolderName = configFolderLocation +
                    File.separator + "org.grits.toolbox.editor.experimentdesigner";
        String fileFolderName = experimentSubFolderName + File.separator + "files";
        
        File fileFolder = new File(fileFolderName);
        if (!fileFolder.exists())
        	fileFolder.mkdirs();
        
        File original = new File (uploadFolderLocation + File.separator + filename);
        if (original.exists()) {
        	FileOutputStream out;
			try {
				File destinationFile = new File (fileFolderName + File.separator + filename);
				while (destinationFile.exists()) {
					filename = generateUniqueFileName(filename, fileFolder.list());
					destinationFile = new File(fileFolderName + File.separator + filename);
				}
				out = new FileOutputStream(destinationFile);
				Files.copy (original.toPath(), out );
	        	out.close();
	        	return destinationFile.getName();
			} catch (FileNotFoundException e) {
				logger.error("Could not find the file " + filename, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not locate the file associated with the protocol template");
			} catch (IOException e) {
				logger.error("Could not copy the file " + filename, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not copy the file associated with the protocol template into the workspace");
			}
        }
		return null;
	}
	
	/**
	 * copy the file with the given name from configuration directory into the workspace/projectName/files folder
	 * @param filename
	 * @param projectName
	 * @return
	 */
	public static String copyFromConfigToWorkspace (String filename, String projectName) {
		// copy the file from workspace into the configuration folder
		String configFolderLocation = PropertyHandler.getVariable("configuration_location");
		
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator + projectName;
        String uploadFolderLocation = projectFolderLocation
                + File.separator
                + "files";
        
        String experimentSubFolderName = configFolderLocation +
                    File.separator + "org.grits.toolbox.editor.experimentdesigner";
        String fileFolderName = experimentSubFolderName + File.separator + "files";
        
        File uploadFolder = new File(uploadFolderLocation);
        if (!uploadFolder.exists())
        	uploadFolder.mkdirs();
        
        File original = new File (fileFolderName + File.separator + filename);
        if (original.exists()) {
        	FileOutputStream out;
			try {
				File destinationFile = new File(uploadFolderLocation + File.separator + filename);
				while (destinationFile.exists()) {
					filename = generateUniqueFileName(filename, uploadFolder.list());
					destinationFile = new File(uploadFolderLocation + File.separator + filename);
				}
				out = new FileOutputStream(destinationFile);
				Files.copy (original.toPath(), out );
	        	out.close();
	        	return destinationFile.getName();
			} catch (FileNotFoundException e) {
				logger.error("Could not find the file " + filename, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not locate the file associated with the protocol template");
			} catch (IOException e) {
				logger.error("Could not copy the file " + filename, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not copy the file associated with the protocol template into the workspace");
			}
        }
		return null;
	}
	
	/**
	 * copy the given file from one project's folder into the other's. This is used when a protocol is created by copying another
	 * 
	 * @param filename
	 * @param fromProject
	 * @param toProject
	 * @return
	 */
	public static String copyFromProjectToAnother (String filename, String fromProject, String toProject) {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator;
        String originalLocation=null;
        String destinationLocation=null;
        if (fromProject != null) 
        	originalLocation = projectFolderLocation + fromProject;
        
        if (toProject != null) 
        	destinationLocation = projectFolderLocation + toProject;
        
        originalLocation += File.separator + "files";
        destinationLocation += File.separator + "files";
        
        File destination = new File(destinationLocation);
        if (!destination.exists())
        	destination.mkdirs();
        
        File original = new File (originalLocation + File.separator + filename);
        if (original.exists()) {
        	FileOutputStream out;
			try {
				File destinationFile = new File(destinationLocation + File.separator + filename);
				while (destinationFile.exists()) {
					filename = generateUniqueFileName(filename, destination.list());
					destinationFile = new File(destinationLocation + File.separator + filename);
				}
				out = new FileOutputStream(destinationFile);
				Files.copy (original.toPath(), out );
	        	out.close();
	        	return destinationFile.getName();
			} catch (FileNotFoundException e) {
				logger.error("Could not find the file " + filename, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not locate the file associated with the protocol template");
			} catch (IOException e) {
				logger.error("Could not copy the file " + filename, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not copy the file associated with the protocol template into the workspace");
			}
        }
		return null;
		
	}

	/**
	 * copy the file with the given name from the jar into the workspace/projectName
	 * @param file
	 * @param projectName
	 * @return
	 */
	public static String copyFromJarToWorkspace(String file, String projectName) {
		String workspaceLocation = PropertyHandler.getVariable("workspace_location");
        String projectFolderLocation = workspaceLocation.substring(0, workspaceLocation.length()-1) 
                + File.separator;
        if (projectName != null) 
        	projectFolderLocation += projectName;
        String uploadFolderLocation = projectFolderLocation
                + File.separator
                + "files";
        
        URL url = ExperimentConfig.FILE_RESOURCE_URL;
		if (url != null) {
    		URL resourceFileUrl;
			try {
				resourceFileUrl = FileLocator.toFileURL(url);
	    		File dir = new File(resourceFileUrl.toURI());
	    		String fileLocation = dir.getAbsolutePath() + File.separator + file;
	    		File originalJarFile = new File(fileLocation);
	    		File destinationFile = new File (uploadFolderLocation + File.separator + file);
	    		while (destinationFile.exists()) {
	    			file = generateUniqueFileName(file, new File(uploadFolderLocation).list());
	    			destinationFile = new File (uploadFolderLocation + File.separator +  file);
	    		}
    			FileOutputStream configFile = new FileOutputStream(destinationFile);
    			Files.copy(originalJarFile.toPath(), configFile);
    			configFile.close();
    			return destinationFile.getName();
	    		
			} catch (IOException e) {
				logger.warn("Could not copy the file for the protocol template from the jar file: " + file, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not copy the file associated with the protocol template from the jar");
			} catch (URISyntaxException e) {
				logger.warn("Could not find the file for the protocol template in the jar file: " + file, e);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "File Error", "Could not find the file associated with the protocol template in the jar");	
			}
		}
        
        return null;
	}
	
	public static String generateUniqueFileName (String filename, String[] existingNames) {
		if (filename != null) {
			String name = filename;
			String extension = "";
			if (filename.contains(".")) {
				extension = filename.substring(filename.lastIndexOf("."));
				name = filename.substring(0, filename.lastIndexOf("."));
			}
			int i;
			if ((i = name.indexOf ("(")) != -1) {
				String counter = name.substring(i+1, name.indexOf(")"));
				int count = Integer.parseInt(counter);
				count ++;
				
				String newFilename = name.substring(0, name.indexOf("("));
				newFilename += "(" + count + ")";
				newFilename += extension;
				return newFilename;
			} else {
				String newFilename = "";
				int count = 1;
				do {
					// there is no "(version)", generate the first version
					newFilename = name + "(" + count + ")";
					newFilename += extension;
					count++;
				} while (Arrays.asList(existingNames).contains(newFilename));
				return newFilename;
			}
		}
		
		return null;
	}
}
