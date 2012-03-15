package com.splunk.shep.archiver.archive;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.DefaultHttpClient;

import com.splunk.shep.archiver.archive.recovery.BucketLocker;
import com.splunk.shep.archiver.archive.recovery.BucketMover;
import com.splunk.shep.archiver.archive.recovery.FailedBucketsArchiver;
import com.splunk.shep.archiver.model.Bucket;
import com.splunk.shep.archiver.model.FileNotDirectoryException;

/**
 * Takes a bucket that froze and archives it. <br/>
 * The {@link BucketFreezer} also recovers any failed archiving attempts by
 * other {@link BucketFreezer}s.
 */
public class BucketFreezer {

    public static final int EXIT_OK = 0;
    public static final int EXIT_INCORRECT_ARGUMENTS = -1;
    public static final int EXIT_FILE_NOT_A_DIRECTORY = -2;
    public static final int EXIT_FILE_NOT_FOUND = -3;

    // CONFIG get this value from the config.
    public static final String DEFAULT_SAFE_LOCATION = FileUtils
	    .getUserDirectoryPath()
	    + File.separator
	    + BucketFreezer.class.getName() + "-safe-buckets";

    // CONFIG
    public static final String DEFAULT_FAIL_LOCATION = FileUtils
	    .getUserDirectoryPath()
	    + File.separator
	    + BucketFreezer.class.getName() + "-failed-buckets";

    private final String safeLocationForBuckets;
    private final FailedBucketsArchiver failedBucketsArchiver;
    private final ArchiveRestHandler archiveRestHandler;

    protected BucketFreezer(String safeLocationForBuckets,
	    ArchiveRestHandler archiveRestHandler,
	    FailedBucketsArchiver failedBucketsArchiver) {
	this.safeLocationForBuckets = safeLocationForBuckets;
	this.archiveRestHandler = archiveRestHandler;
	this.failedBucketsArchiver = failedBucketsArchiver;
    }

    /**
     * Freezes the bucket on the specified path and belonging to specified
     * index.
     * 
     * @param indexName
     *            The name of the index that this bucket belongs to
     * 
     * @param path
     *            The path of the bucket on the local file system
     * 
     * @return An exit code depending on the outcome.
     */
    public int freezeBucket(String indexName, String path) {
	try {
	    moveAndArchiveBucket(indexName, path);
	    return EXIT_OK;
	} catch (FileNotDirectoryException e) {
	    return EXIT_FILE_NOT_A_DIRECTORY;
	} catch (FileNotFoundException e) {
	    return EXIT_FILE_NOT_FOUND;
	}
    }

    private void moveAndArchiveBucket(String indexName, String path)
	    throws FileNotFoundException, FileNotDirectoryException {
	Bucket bucket = new Bucket(indexName, path);
	bucket = bucket.moveBucketToDir(getSafeLocationForBucket(bucket));
	archiveRestHandler.callRestToArchiveBucket(bucket);
	failedBucketsArchiver.archiveFailedBuckets(archiveRestHandler);
    }

    private File getSafeLocationForBucket(Bucket bucket) {
	File safeBucketLocation = new File(getSafeLocationRoot(),
		bucket.getIndex());
	safeBucketLocation.mkdirs();
	return safeBucketLocation;
    }

    private File getSafeLocationRoot() {
	return new File(safeLocationForBuckets);
    }

    /**
     * The construction logic for creating a {@link BucketFreezer}
     */
    public static BucketFreezer createWithDefaultHttpClientAndDefaultSafeAndFailLocations() {
	BucketMover bucketMover = new BucketMover(DEFAULT_FAIL_LOCATION);
	FailedBucketsArchiver failedBucketsArchiver = new FailedBucketsArchiver(
		bucketMover, new BucketLocker());
	ArchiveRestHandler archiveRestHandler = new ArchiveRestHandler(
		new DefaultHttpClient(), bucketMover);
	return new BucketFreezer(DEFAULT_SAFE_LOCATION, archiveRestHandler,
		failedBucketsArchiver);
    }

    /**
     * This method is used by the real main and only exists so that it can be
     * tested using test doubles.
     */
    /* package-private */static void runMainWithDepentencies(Runtime runtime,
	    BucketFreezer bucketFreezer, String... args) {
	if (args.length != 2) {
	    runtime.exit(EXIT_INCORRECT_ARGUMENTS);
	} else {
	    runtime.exit(bucketFreezer.freezeBucket(args[0], args[1]));
	}
    }

    public static void main(String... args) {
	runMainWithDepentencies(
		Runtime.getRuntime(),
		BucketFreezer
			.createWithDefaultHttpClientAndDefaultSafeAndFailLocations(),
		args);
    }

}
