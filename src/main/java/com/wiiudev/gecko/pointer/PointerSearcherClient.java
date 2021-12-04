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
	// TODO Move input file(s) up/down
	// TODO Checkboxes for enabling/disabling input files: https://stackoverflow.com/questions/7391877
	// TODO When adding another pointer map, re-populate file type
	// TODO Implement minimum pointer address
	// TODO Check provided addresses against address size
	// TODO Starting address cursor flashing bug
	// TODO Save/Load pointer search configuration via menu bar
	// TODO Implement ignored memory ranges
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

	public static void main(String[] arguments) throws Exception
	{
		val systemLookAndFeelClassName = getSystemLookAndFeelClassName();
		setLookAndFeel(systemLookAndFeelClassName);
		setupThreadViolationsDetector();
		// forceMaximumMemoryLimit();

		invokeLater(PointerSearcherClient::startGUI);
	}
}
