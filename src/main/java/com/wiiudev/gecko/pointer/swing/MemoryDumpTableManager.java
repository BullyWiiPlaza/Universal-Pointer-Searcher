package com.wiiudev.gecko.pointer.swing;

import com.wiiudev.gecko.pointer.preprocessed_search.data_structures.MemoryDump;
import com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport;
import lombok.val;
import lombok.var;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.MEMORY_DUMP;
import static com.wiiudev.gecko.pointer.swing.preprocessed_search.FileTypeImport.POINTER_MAP;
import static com.wiiudev.gecko.pointer.swing.utilities.JTableUtilities.*;
import static java.lang.Long.toHexString;
import static java.nio.ByteOrder.BIG_ENDIAN;

public class MemoryDumpTableManager
{
	private JTable table;

	private List<MemoryDump> memoryDumps;

	MemoryDumpTableManager(JTable table)
	{
		this.table = table;
		memoryDumps = new ArrayList<>();

		val tableModel = getDefaultTableModel();
		table.setModel(tableModel);
		setSingleSelection(table);
		configure();
	}

	private void configure()
	{
		val columns = new String[]{"File Name", "Starting Address",
				"Target Address", "Byte Order", "File Type"};
		configureTable(table, columns);
		setCellsAlignment(table, SwingConstants.CENTER);
	}

	void addMemoryDump(MemoryDump memoryDump)
	{
		addMemoryDump(memoryDump, true);
	}

	private void addMemoryDump(MemoryDump memoryDump, boolean addToList)
	{
		val fileType = memoryDump.getFileType();
		val chosenByteOrder = memoryDump.getByteOrder();
		val byteOrder = fileType.equals(POINTER_MAP) ? "-" : byteOrderToString(chosenByteOrder);
		val startingAddress = memoryDump.getStartingAddress();
		val targetAddress = memoryDump.getTargetAddress();
		val row = new Object[]{memoryDump.getFilePath().toFile().getName(),
				startingAddress == null ? "" : toHexString(startingAddress).toUpperCase(),
				targetAddress == null ? "" : toHexString(targetAddress).toUpperCase(),
				byteOrder, memoryDump.getFileType()};
		val tableModel = (DefaultTableModel) table.getModel();
		tableModel.addRow(row);

		if (addToList)
		{
			memoryDumps.add(memoryDump);
		}
	}

	private String byteOrderToString(ByteOrder chosenByteOrder)
	{
		if (chosenByteOrder == null)
		{
			return "";
		}

		return BIG_ENDIAN.equals(chosenByteOrder) ? "Big Endian" : "Little Endian";
	}

	void removeMemoryDumps()
	{
		memoryDumps.clear();
		deleteAllRows(table);
	}

	boolean isMemoryDumpSelected()
	{
		return table.getSelectedRow() != -1;
	}

	MemoryDump getSelectedMemoryDump()
	{
		val selectedRow = table.getSelectedRow();
		return memoryDumps.get(selectedRow);
	}

	void replaceSelectedMemoryDumpWith(MemoryDump memoryDump)
	{
		val selectedRow = table.getSelectedRow();
		memoryDumps.set(selectedRow, memoryDump);
		refreshTable();
	}

	private void refreshTable()
	{
		val selectedRow = table.getSelectedRow();
		deleteAllRows(table);

		for (val currentMemoryDump : memoryDumps)
		{
			addMemoryDump(currentMemoryDump, false);
		}

		table.setRowSelectionInterval(selectedRow, selectedRow);
	}

	public List<MemoryDump> getMemoryDumps()
	{
		return getList(MEMORY_DUMP);
	}

	List<MemoryDump> getPointerMaps()
	{
		return getList(POINTER_MAP);
	}

	private List<MemoryDump> getList(FileTypeImport fileTypeImport)
	{
		val actualMemoryDumps = new ArrayList<MemoryDump>();
		for (val memoryDump : memoryDumps)
		{
			val fileType = memoryDump.getFileType();
			if (fileType.equals(fileTypeImport))
			{
				actualMemoryDumps.add(memoryDump);
			}
		}

		return actualMemoryDumps;
	}

	void removeSelectedMemoryDumps()
	{
		deleteSelectedMemoryDumps();
		deleteSelectedRows(table);
	}

	private void deleteSelectedMemoryDumps()
	{
		val selectedRows = table.getSelectedRows();
		for (var rowIndex = 0; rowIndex < selectedRows.length; rowIndex++)
		{
			val selectedRow = selectedRows[rowIndex];
			memoryDumps.remove(selectedRow - rowIndex);
		}
	}
}
