package com.wiiudev.gecko.pointer.utilities;

import com.wiiudev.gecko.pointer.NativePointerSearcherManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.var;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import static com.wiiudev.gecko.pointer.NativePointerSearcherManager.giveAllPosixFilePermissions;
import static java.util.logging.Logger.getLogger;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;

@Getter
@AllArgsConstructor
public class GitHubUtils
{
	private final String repositoryOwner;

	private final String repositoryName;

	private final Path outputFilePath;

	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private static final Logger LOGGER = getLogger(GitHubUtils.class.getName());

	private static void downloadFile(final String downloadUrl,
	                                 final String destinationPath) throws IOException
	{
		val url = new URL(downloadUrl);
		try (val inputStream = url.openStream();
		     val fileOutputStream = new FileOutputStream(destinationPath))
		{
			IOUtils.copy(inputStream, fileOutputStream);
		}
	}

	private static void extractFile(final String archiveFilePath,
	                                final String destinationPath) throws IOException, ArchiveException
	{
		try (val fileInputStream = new FileInputStream(archiveFilePath);
		     val archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, fileInputStream))
		{
			ArchiveEntry entry;
			while ((entry = archiveInputStream.getNextEntry()) != null)
			{
				if (!archiveInputStream.canReadEntryData(entry))
				{
					continue;
				}

				val file = new File(destinationPath, entry.getName());
				if (entry.isDirectory())
				{
					Files.createDirectories(file.toPath());
				} else
				{
					try (val bufferedOutputStream = new BufferedOutputStream(Files.newOutputStream(file.toPath())))
					{
						IOUtils.copy(archiveInputStream, bufferedOutputStream);
					}
				}
			}
		}
	}

	private static String getFileNameFromUrl(final String urlString) throws MalformedURLException
	{
		val url = new URL(urlString);
		val path = url.getPath();
		return new File(path).getName();
	}

	public boolean isAssetUpToDate() throws IOException
	{
		LOGGER.info("Checking if assets are up-to-date...");

		val assetCreationDateFilePath = getAssetCreationDateFilePath();

		if (!Files.isRegularFile(assetCreationDateFilePath))
		{
			return false;
		}

		val assets = getGitHubAssets();
		for (val asset : assets)
		{
			LOGGER.info("Comparing asset creation date with the existing one...");
			val assetCreationDate = asset.getCreatedAt();
			val formattedDate = SIMPLE_DATE_FORMAT.format(assetCreationDate);
			val assetCreationDateBytes = Files.readAllBytes(assetCreationDateFilePath);
			val writtenDate = new String(assetCreationDateBytes, StandardCharsets.UTF_8);

			return formattedDate.equals(writtenDate);
		}

		return false;
	}

	private PagedIterable<GHAsset> getGitHubAssets() throws IOException
	{
		LOGGER.info("Reading GitHub assets...");

		val github = new GitHubBuilder().build();
		val repository = github.getRepository(repositoryOwner + "/" + repositoryName);
		val latestRelease = repository.getLatestRelease();

		return latestRelease.listAssets();
	}

	public static String getExtension()
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			return "exe";
		}

		if (SystemUtils.IS_OS_MAC)
		{
			return "macho";
		}

		return "elf";
	}

	public Path getUniversalPointerSearcherFilePath()
	{
		var fileName = NativePointerSearcherManager.BINARY_NAME;
		fileName += "." + getExtension();

		return outputFilePath.resolve(fileName);
	}

	public void downloadGitHubRelease() throws IOException, ArchiveException
	{
		LOGGER.info("Downloading GitHub release...");

		Files.createDirectories(outputFilePath);

		val assets = getGitHubAssets();
		for (val asset : assets)
		{
			val browserDownloadUrl = asset.getBrowserDownloadUrl();
			LOGGER.info("Download url: " + browserDownloadUrl);

			val downloadArchiveFileName = getFileNameFromUrl(browserDownloadUrl);
			val archiveFilePath = outputFilePath.resolve(downloadArchiveFileName);
			downloadFile(browserDownloadUrl, archiveFilePath.toString());

			LOGGER.info("Extracting...");
			extractFile(archiveFilePath.toString(), archiveFilePath.getParent().toString());

			if (IS_OS_UNIX)
			{
				LOGGER.info("Setting UniversalPointerSearcher engine file permission(s)...");
				val universalPointerSearcherFilePath = getUniversalPointerSearcherFilePath();
				giveAllPosixFilePermissions(universalPointerSearcherFilePath);
			}

			LOGGER.info("Writing asset creation date...");
			val assetCreationDate = asset.getCreatedAt();
			val formattedDate = SIMPLE_DATE_FORMAT.format(assetCreationDate);
			val assetCreationDateFilePath = getAssetCreationDateFilePath();
			Files.write(assetCreationDateFilePath, formattedDate.getBytes(StandardCharsets.UTF_8));

			Files.delete(archiveFilePath);

			LOGGER.info("GitHub assets ready to be used...");

			break;
		}
	}

	private Path getAssetCreationDateFilePath()
	{
		return outputFilePath.resolve("Release-Creation-Date.txt");
	}
}
