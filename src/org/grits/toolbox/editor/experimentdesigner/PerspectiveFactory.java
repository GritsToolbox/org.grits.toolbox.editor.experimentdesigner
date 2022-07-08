package org.grits.toolbox.editor.experimentdesigner;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.grits.toolbox.editor.experimentdesigner.views.PaperView;
import org.grits.toolbox.editor.experimentdesigner.views.ParameterView;
import org.grits.toolbox.editor.experimentdesigner.views.ProtocolView;

public class PerspectiveFactory implements IPerspectiveFactory {

	public static String PERSPECTIVE_ID = "org.grits.toolbox.editor.experimentdesigner.designPerspective1";
	@Override
	public void createInitialLayout(IPageLayout layout) {
		addPerspectiveShortcuts(layout);
		layout.addView("org.grits.toolbox.core.part.projectexplorer", IPageLayout.LEFT, 0.22f, IPageLayout.ID_EDITOR_AREA);
		layout.addView(ProtocolView.VIEW_ID, IPageLayout.RIGHT, 0.64f, IPageLayout.ID_EDITOR_AREA);
		layout.addView(ParameterView.VIEW_ID, IPageLayout.BOTTOM, 0.5f, ProtocolView.VIEW_ID);
		layout.addView(PaperView.VIEW_ID, IPageLayout.BOTTOM, 0.5f, ProtocolView.VIEW_ID);
	}

	private void addPerspectiveShortcuts(IPageLayout layout) {
		layout.addPerspectiveShortcut(PERSPECTIVE_ID);
	}
}
