package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.val;
import lombok.var;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DefaultContextMenu extends JPopupMenu
{
	private JMenuItem copy;
	private JMenuItem selectAll;

	private JTextComponent textComponent;

	public DefaultContextMenu()
	{
		addPopupMenuItems();
	}

	private void addPopupMenuItems()
	{
		copy = new JMenuItem("Copy");
		copy.setEnabled(false);
		copy.setAccelerator(KeyStroke.getKeyStroke("control C"));
		copy.addActionListener(event -> textComponent.copy());
		add(copy);

		selectAll = new JMenuItem("Select All");
		selectAll.setEnabled(false);
		selectAll.setAccelerator(KeyStroke.getKeyStroke("control A"));
		selectAll.addActionListener(event -> textComponent.selectAll());
		add(selectAll);
	}

	private void addTo(JTextComponent textComponent)
	{
		textComponent.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent releasedEvent)
			{
				handleContextMenu(releasedEvent);
			}

			@Override
			public void mouseReleased(MouseEvent releasedEvent)
			{
				handleContextMenu(releasedEvent);
			}
		});
	}

	private void handleContextMenu(MouseEvent releasedEvent)
	{
		if (releasedEvent.getButton() == MouseEvent.BUTTON3)
		{
			processClick(releasedEvent);
		}
	}

	private void processClick(MouseEvent event)
	{
		textComponent = (JTextComponent) event.getSource();
		textComponent.requestFocus();

		var enableCopy = false;
		var enableSelectAll = false;

		val selectedText = textComponent.getSelectedText();
		val text = textComponent.getText();

		if (text != null)
		{
			if (!text.isEmpty())
			{
				enableSelectAll = true;
			}
		}

		if (selectedText != null)
		{
			if (!selectedText.isEmpty())
			{
				enableCopy = true;
			}
		}

		copy.setEnabled(enableCopy);
		selectAll.setEnabled(enableSelectAll);

		// Shows the popup menu
		show(textComponent, event.getX(), event.getY());
	}

	public static void addDefaultContextMenu(JTextComponent component)
	{
		val defaultContextMenu = new DefaultContextMenu();
		defaultContextMenu.addTo(component);
	}
}
