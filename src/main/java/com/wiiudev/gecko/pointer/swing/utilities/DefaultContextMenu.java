package com.wiiudev.gecko.pointer.swing.utilities;

import lombok.var;
import lombok.val;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.event.KeyEvent.*;
import static java.awt.event.MouseEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

public class DefaultContextMenu extends JPopupMenu
{
	private static final Toolkit DEFAULT_TOOLKIT = Toolkit.getDefaultToolkit();
	@SuppressWarnings("deprecation")
	private static final int CONTROL_MASK = DEFAULT_TOOLKIT.getMenuShortcutKeyMask();

	private final Clipboard clipboard;
	private final UndoManager undoManager;

	private JMenuItem undo;
	private JMenuItem redo;
	private JMenuItem cut;
	private JMenuItem copy;
	private JMenuItem paste;
	private JMenuItem delete;
	private JMenuItem selectAll;

	private JTextComponent textComponent;

	private DefaultContextMenu()
	{
		undoManager = new UndoManager();
		clipboard = DEFAULT_TOOLKIT.getSystemClipboard();

		addPopupMenuItems();
	}

	private void addPopupMenuItems()
	{
		undo = new JMenuItem("Undo");
		undo.setEnabled(false);
		undo.setAccelerator(getKeyStroke(VK_Z, CONTROL_MASK));
		undo.addActionListener(event -> undoManager.undo());
		add(undo);

		redo = new JMenuItem("Redo");
		redo.setEnabled(false);
		redo.setAccelerator(getKeyStroke(VK_Y, CONTROL_MASK));
		redo.addActionListener(event -> undoManager.redo());
		add(redo);

		add(new JSeparator());

		cut = new JMenuItem("Cut");
		cut.setEnabled(false);
		cut.setAccelerator(getKeyStroke(VK_X, CONTROL_MASK));
		cut.addActionListener(event -> textComponent.cut());
		add(cut);

		copy = new JMenuItem("Copy");
		copy.setEnabled(false);
		copy.setAccelerator(getKeyStroke(VK_C, CONTROL_MASK));
		copy.addActionListener(event -> textComponent.copy());
		add(copy);

		paste = new JMenuItem("Paste");
		paste.setEnabled(false);
		paste.setAccelerator(getKeyStroke(VK_V, CONTROL_MASK));
		paste.addActionListener(event -> textComponent.paste());
		add(paste);

		delete = new JMenuItem("Delete");
		delete.setEnabled(false);
		delete.setAccelerator(getKeyStroke(VK_DELETE, CONTROL_MASK));
		delete.addActionListener(event -> textComponent.replaceSelection(""));
		add(delete);

		add(new JSeparator());

		selectAll = new JMenuItem("Select All");
		selectAll.setEnabled(false);
		selectAll.setAccelerator(getKeyStroke(VK_A, CONTROL_MASK));
		selectAll.addActionListener(event -> textComponent.selectAll());
		add(selectAll);
	}

	private void addTo(JTextComponent textComponent)
	{
		textComponent.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent pressedEvent)
			{
				val keyCode = pressedEvent.getKeyCode();
				val modifiers = pressedEvent.getModifiersEx();

				if (keyCode == VK_Z && modifiers == CONTROL_MASK)
				{
					if (undoManager.canUndo() && textComponent.isEditable())
					{
						undoManager.undo();
					}
				}

				if (keyCode == VK_Y && modifiers == CONTROL_MASK)
				{
					if (undoManager.canRedo() && textComponent.isEditable())
					{
						undoManager.redo();
					}
				}
			}
		});

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

		textComponent.getDocument().addUndoableEditListener(event -> undoManager.addEdit(event.getEdit()));
	}

	private void handleContextMenu(MouseEvent releasedEvent)
	{
		if (releasedEvent.getButton() == BUTTON3)
		{
			processClick(releasedEvent);
		}
	}

	private void processClick(MouseEvent event)
	{
		textComponent = (JTextComponent) event.getSource();
		textComponent.requestFocus();

		val enableUndo = undoManager.canUndo() && textComponent.isEditable();
		val enableRedo = undoManager.canRedo() && textComponent.isEditable();

		var enableCut = false;
		var enableCopy = false;
		var enablePaste = false;
		var enableDelete = false;
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
				enableCut = textComponent.isEditable();
				enableCopy = true;
				enableDelete = textComponent.isEditable();
			}
		}

		if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
		    && textComponent.isEnabled() && textComponent.isEditable())
		{
			enablePaste = true;
		}

		undo.setEnabled(enableUndo);
		redo.setEnabled(enableRedo);
		cut.setEnabled(enableCut);
		copy.setEnabled(enableCopy);
		paste.setEnabled(enablePaste);
		delete.setEnabled(enableDelete);
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
