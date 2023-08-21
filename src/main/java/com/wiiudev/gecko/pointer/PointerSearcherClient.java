package com.wiiudev.gecko.pointer;

import com.jidesoft.utils.ThreadCheckingRepaintManager;
import com.wiiudev.gecko.pointer.swing.UniversalPointerSearcherGUI;
import com.wiiudev.gecko.pointer.utilities.JVMArgumentEnforcer;
import lombok.val;

import static com.wiiudev.gecko.pointer.utilities.JVMArgumentEnforcer.assert64BitJavaInstallation;
import static javax.swing.RepaintManager.setCurrentManager;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.UIManager.getSystemLookAndFeelClassName;
import static javax.swing.UIManager.setLookAndFeel;

public class PointerSearcherClient
{
	// TODO storeMemoryPointersFilePathField, storeMemoryPointerResultsBrowseButton etc. disable correctly while searching
	// TODO Implement ignored memory ranges
	// TODO File extensions without prepended dot "."
	// TODO Adding memory dumps/pointer maps by folder
	// TODO Move input file(s) up/down (+ context menu)
	// TODO Checkboxes for enabling/disabling input files: https://stackoverflow.com/questions/7391877
	// TODO When adding another pointer map, re-populate file type
	// TODO Check provided addresses against address size
	private static void startGUI()
	{
		val universalPointerSearcherGUI = UniversalPointerSearcherGUI.getInstance();
		universalPointerSearcherGUI.setVisible(true);
	}

	private static void forceMaximumMemoryLimit() throws Exception
	{
		assert64BitJavaInstallation();

		// Make sure that enough memory is assigned to the process
		val jvmArgumentEnforcer = new JVMArgumentEnforcer("-Xmx100g");
		jvmArgumentEnforcer.forceArgument();
	}

	private static void setupThreadViolationsDetector()
	{
		val repaintManager = new ThreadCheckingRepaintManager(true);
		setCurrentManager(repaintManager);
	}

	public static void main(final String[] arguments) throws Exception
	{
		val systemLookAndFeelClassName = getSystemLookAndFeelClassName();
		setLookAndFeel(systemLookAndFeelClassName);
		setupThreadViolationsDetector();
		// forceMaximumMemoryLimit();

		invokeLater(PointerSearcherClient::startGUI);
	}
}
