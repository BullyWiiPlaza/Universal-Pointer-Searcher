package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryRange;
import lombok.val;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.swing.utilities.JTableUtilities.*;

public class IgnoredMemoryRangesTableManager
{
	private final JTable table;

	public IgnoredMemoryRangesTableManager(final JTable table)
	{
		this.table = table;
		List<MemoryRange> ignoredMemoryRanges = new ArrayList<>();
	}

	void configure()
	{
		val columns = new String[]{"Starting Address", "End Address"};
		configureTable(table, columns);
		setCellsAlignment(table, SwingConstants.CENTER);
	}
}
