package com.wiiudev.gecko.pointer;

import javax.swing.*;

import static com.wiiudev.gecko.pointer.swing.utilities.FrameUtilities.setWindowIconImage;

public class GitHubAssetsDownloaderDialog extends JDialog
{
	private JPanel contentPane;

	public GitHubAssetsDownloaderDialog()
	{
		setContentPane(contentPane);
		setWindowIconImage(this);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setModal(true);
		setSize(400, 100);
	}
}
