package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class JTableUtilities
{
	public static void setCellsAlignment(JTable table, int alignment)
	{
		val rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(alignment);

		val tableModel = table.getModel();

		for (int columnIndex = 0; columnIndex < tableModel.getColumnCount(); columnIndex++)
		{
			table.getColumnModel().getColumn(columnIndex).setCellRenderer(rightRenderer);
		}
	}

	private static void setHeaderAlignment(JTable table)
	{
		val header = table.getTableHeader();
		val headerRenderer = new HeaderRenderer(table);
		header.setDefaultRenderer(headerRenderer);
	}

	public static void deleteSelectedRows(JTable table)
	{
		val model = (DefaultTableModel) table.getModel();
		val selectedRows = table.getSelectedRows();

		for (var rowIndex = 0; rowIndex < selectedRows.length; rowIndex++)
		{
			model.removeRow(selectedRows[rowIndex] - rowIndex);
		}

		// Select the previous row now
		val previousRow = selectedRows[0] - 1;
		if (previousRow >= 0)
		{
			table.setRowSelectionInterval(previousRow, previousRow);
		}
	}

	public static void deleteAllRows(JTable table)
	{
		val defaultTableModel = (DefaultTableModel) table.getModel();
		defaultTableModel.setRowCount(0);
	}

	public static void configureTable(JTable table, String[] columnHeaderNames)
	{
		val tableModel = (DefaultTableModel) table.getModel();
		tableModel.setColumnCount(columnHeaderNames.length);
		tableModel.setColumnIdentifiers(columnHeaderNames);
		setHeaderAlignment(table);

		table.setModel(tableModel);
		val tableHeader = table.getTableHeader();
		tableHeader.setReorderingAllowed(false);
		tableHeader.setResizingAllowed(false);
		tableHeader.setVisible(true);
		setCellsAlignment(table, SwingConstants.CENTER);
	}

	public static DefaultTableModel getDefaultTableModel()
	{
		return new DefaultTableModel()
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
	}

	public static void setSingleSelection(JTable table)
	{
		table.setSelectionModel(new ForcedListSelectionModel());
	}

	/*public static void removeAllKeyListeners(JComponent component)
	{
		val keyListeners = component.getKeyListeners();

		for (val keyListener : keyListeners)
		{
			component.removeKeyListener(keyListener);
		}
	}*/

	private static class ForcedListSelectionModel extends DefaultListSelectionModel
	{
		ForcedListSelectionModel()
		{
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		@Override
		public void clearSelection()
		{
		}

		@Override
		public void removeSelectionInterval(int start, int end)
		{
		}
	}

	private static class HeaderRenderer implements TableCellRenderer
	{
		private DefaultTableCellRenderer renderer;

		HeaderRenderer(JTable table)
		{
			renderer = (DefaultTableCellRenderer)
					table.getTableHeader().getDefaultRenderer();
			renderer.setHorizontalAlignment(JLabel.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int col)
		{
			return renderer.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, col);
		}
	}

	/*public static void setSelectedRow(JTable table, int rowIndex, int columnIndex)
	{
		table.setRowSelectionInterval(rowIndex, columnIndex);
		scrollToSelectedRow(table);
	}

	private static void scrollToSelectedRow(JTable table)
	{
		val viewport = (JViewport) table.getParent();
		val cellRectangle = table.getCellRect(table.getSelectedRow(), 0, true);
		val visibleRectangle = viewport.getVisibleRect();
		invokeLater(() -> table.scrollRectToVisible(new Rectangle(cellRectangle.x, cellRectangle.y,
				(int) visibleRectangle.getWidth(), (int) visibleRectangle.getHeight())));
	}*/
}
