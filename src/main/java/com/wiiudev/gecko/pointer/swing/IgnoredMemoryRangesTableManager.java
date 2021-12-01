package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryRange;
import com.wiiudev.gecko.pointer.swing.utilities.JTableUtilities;
import lombok.val;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.swing.utilities.JTableUtilities.*;

public class IgnoredMemoryRangesTableManager
{
	private final JTable table;
	private final List<MemoryRange> ignoredMemoryRanges;

	public IgnoredMemoryRangesTableManager(final JTable table)
	{
		this.table = table;
		ignoredMemoryRanges = new ArrayList<>();
	}

	void configure()
	{
		val columns = new String[]{"Starting Address", "End Address"};
		configureTable(table, columns);
		setCellsAlignment(table, SwingConstants.CENTER);
	}

	public void addMemoryRange(final MemoryRange memoryRange)
	{
		val objects = new Object[]{Long.toHexString(memoryRange.getStartingOffset()).toUpperCase(),
				Long.toHexString(memoryRange.getEndOffset()).toUpperCase()};
		val tableModel = (DefaultTableModel) table.getModel();
		tableModel.addRow(objects);
		ignoredMemoryRanges.add(memoryRange);
	}

	public void clearAll()
	{
		ignoredMemoryRanges.clear();
		deleteAllRows(table);
	}

	public void deleteSelectedRows()
	{
		JTableUtilities.deleteSelectedRows(table);
	}
}
