package com.wiiudev.gecko.pointer.preprocessed_search;

import lombok.val;

import javax.swing.*;

import static javax.swing.SwingUtilities.invokeLater;

class ProgressBarHelper
{
	private static final int MAXIMUM_PROGRESS = 100;
	private static final int PROGRESS_STEP_SIZE = 1000;
	private static final int MINIMUM_PROGRESS_STEP_SIZE = 10000;

	static boolean setProgress(int value, int maximum, JProgressBar progressBar)
	{
		val progress = (value * MAXIMUM_PROGRESS) / maximum;

		if (maximum > MINIMUM_PROGRESS_STEP_SIZE)
		{
			var progressStage = maximum / PROGRESS_STEP_SIZE;
			if (progressStage < MINIMUM_PROGRESS_STEP_SIZE)
			{
				progressStage = MINIMUM_PROGRESS_STEP_SIZE;
			}

			val shouldSetProgress = value % progressStage == 0;
			if (progressBar != null && (shouldSetProgress || progress == MAXIMUM_PROGRESS))
			{
				invokeLater(() -> progressBar.setValue(progress));
				return true;
			}

			return false;
		}

		if (progressBar != null)
		{
			invokeLater(() -> progressBar.setValue(progress));
		}

		return true;
	}
}
