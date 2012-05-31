// Copyright (C) 2011 Splunk Inc.
//
// Splunk Inc. licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.splunk.shuttl.archiver.bucketsize;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import com.splunk.shuttl.archiver.archive.PathResolver;
import com.splunk.shuttl.archiver.fileSystem.ArchiveFileSystem;
import com.splunk.shuttl.archiver.fileSystem.FileOverwriteException;
import com.splunk.shuttl.archiver.model.Bucket;

/**
 * Gets a {@link Bucket}'s size. Both a remote bucket and a local bucket. <br/>
 * <br/>
 * This is needed because we want to know how big the {@link Bucket} will be on
 * the local file system and there's no guarantee that the size on the archive
 * file system is the same as on local disk. We therefore need to be able to put
 * and get the local file system size of the bucket, from the archive file
 * system.
 */
public class ArchivedBucketsSize {

	private final PathResolver pathResolver;
	private final BucketSizeFile bucketSizeFile;
	private final ArchiveFileSystem archiveFileSystem;
	private final BucketSizeFilePathResolver bucketSizeFilePathResolver;

	/**
	 * @see ArchivedBucketsSize
	 */
	public ArchivedBucketsSize(PathResolver pathResolver,
			BucketSizeFile bucketSizeFile, ArchiveFileSystem archiveFileSystem,
			BucketSizeFilePathResolver bucketSizeFilePathResolver) {
		this.pathResolver = pathResolver;
		this.bucketSizeFile = bucketSizeFile;
		this.archiveFileSystem = archiveFileSystem;
		this.bucketSizeFilePathResolver = bucketSizeFilePathResolver;
	}

	/**
	 * @return size of an archived bucket on the local file system.
	 */
	public long getSize(Bucket bucketInArchive) {
		return 0;
	}

	/**
	 * Put metadata on the {@link ArchiveFileSystem} about a bucket's size.
	 */
	public void putSize(Bucket bucket) {
		File fileWithBucketSize = bucketSizeFile.getFileWithBucketSize(bucket);
		URI metadataFolder = pathResolver.getMetadataFolderForBucket(bucket);
		URI bucketSizeFilePath = bucketSizeFilePathResolver
				.resolveBucketSizeFilePath(fileWithBucketSize, metadataFolder);
		try {
			archiveFileSystem.putFileAtomically(fileWithBucketSize,
					bucketSizeFilePath);
			// TODO: This is duplication between ArchiveBucketTransferer. There should
			// exist a ArchiveTransferer?
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (FileOverwriteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	/**
	 */
	public static ArchivedBucketsSize create(PathResolver pathResolver,
			ArchiveFileSystem archiveFileSystem) {
		return new ArchivedBucketsSize(pathResolver, new BucketSizeFile(),
				archiveFileSystem, new BucketSizeFilePathResolver());
	}

}