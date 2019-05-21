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
	// TODO Save/Load pointer search configuration
	// TODO Last pointer offsets for Java engine
	// TODO Java pointer searcher does not find e.g. [[0x39520908] + 0x368] - 0x1AC in "dumps\No Track Music\39CEB148.bin"
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
