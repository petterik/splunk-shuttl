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
package com.splunk.shuttl.archiver.usecases;

import static com.splunk.shuttl.testutil.TUtilsFile.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shuttl.archiver.archive.ArchiveConfiguration;
import com.splunk.shuttl.archiver.archive.BucketArchiver;
import com.splunk.shuttl.archiver.archive.BucketArchiverFactory;
import com.splunk.shuttl.archiver.bucketsize.ArchivedBucketsSize;
import com.splunk.shuttl.archiver.listers.ArchiveBucketsLister;
import com.splunk.shuttl.archiver.listers.ArchiveBucketsListerFactory;
import com.splunk.shuttl.archiver.model.Bucket;
import com.splunk.shuttl.archiver.model.IllegalIndexException;
import com.splunk.shuttl.archiver.thaw.BucketThawer;
import com.splunk.shuttl.archiver.thaw.BucketThawerFactory;
import com.splunk.shuttl.archiver.thaw.SplunkSettings;
import com.splunk.shuttl.testutil.TUtilsBucket;
import com.splunk.shuttl.testutil.TUtilsFunctional;

@Test(groups = { "functional" }, enabled = false)
public class BucketSizeFunctionalTest {

	private BucketArchiver bucketArchiver;
	private BucketThawer bucketThawer;
	private ArchiveConfiguration config;
	private File thawLocation;
	private ArchivedBucketsSize archivedBucketsSize;

	@BeforeMethod
	public void setUp() throws IllegalIndexException {
		config = TUtilsFunctional.getLocalFileSystemConfiguration();
		bucketArchiver = BucketArchiverFactory.createWithConfiguration(config);
		SplunkSettings splunkSettings = mock(SplunkSettings.class);
		thawLocation = createDirectory();
		when(splunkSettings.getThawLocation(anyString())).thenReturn(thawLocation);
		bucketThawer = BucketThawerFactory.createWithSplunkSettingsAndConfig(
				splunkSettings, config);

		// archivedBucketsSize = new ArchivedBucketsSize();
	}

	@AfterMethod
	public void tearDown() {
		TUtilsFunctional.tearDownLocalConfig(config);
		FileUtils.deleteQuietly(thawLocation);
	}

	public void BucketSize_archiveBucket_bucketHasSameSizeAsBeforeArchiving() {
		Bucket bucket = TUtilsBucket.createRealBucket();
		long bucketSize = bucket.getSize();

		TUtilsFunctional.archiveBucket(bucket, bucketArchiver);
		ArchiveBucketsLister archiveBucketsLister = ArchiveBucketsListerFactory
				.create(config);
		List<Bucket> listBucketsInIndex = archiveBucketsLister
				.listBucketsInIndex(bucket.getIndex());

		assertEquals(1, listBucketsInIndex.size());
		Bucket bucketInArchive = listBucketsInIndex.get(0);
		assertEquals(bucketSize, archivedBucketsSize.getSize(bucketInArchive));
	}

	public void BucketSize_bucketRoundTrip_bucketGetSizeShouldBeTheSameBeforeArchiveAndAfterThaw() {
		Bucket bucket = TUtilsBucket.createRealBucket();

		TUtilsFunctional.archiveBucket(bucket, bucketArchiver);
		bucketThawer.thawBuckets(bucket.getIndex(), bucket.getEarliest(),
				bucket.getLatest());

		List<Bucket> thawedBuckets = bucketThawer.getThawedBuckets();
		assertEquals(1, thawedBuckets.size());
		Bucket thawedBucket = thawedBuckets.get(0);
		assertEquals(bucket.getSize(), thawedBucket.getSize());
	}
}
