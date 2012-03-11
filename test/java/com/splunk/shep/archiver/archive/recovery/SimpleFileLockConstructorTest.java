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
package com.splunk.shep.archiver.archive.recovery;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shep.archiver.archive.recovery.SimpleFileLock;
import com.splunk.shep.testutil.UtilsFile;
import com.splunk.shep.testutil.UtilsTestNG;

/**
 * Fixture: Created with constructor that takes {@link FileChannel} to make sure
 * that it is closed properly.
 */
@Test(groups = { "fast" })
public class SimpleFileLockConstructorTest {

    SimpleFileLock simpleFileLock;
    FileChannel fileChannel;

    @BeforeMethod(groups = { "fast" })
    public void setUp() {
	fileChannel = getFileChannel();
	simpleFileLock = new SimpleFileLock(fileChannel);
    }

    @AfterMethod(groups = { "fast" })
    public void tearDown() {
	IOUtils.closeQuietly(fileChannel);
    }

    /**
     * @return
     */
    private FileChannel getFileChannel() {
	return getOutputStreamToTempFile().getChannel();
    }

    private FileOutputStream getOutputStreamToTempFile() {
	File file = UtilsFile.createTestFile();
	try {
	    return new FileOutputStream(file);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    UtilsTestNG.failForException("Could not open InputStream"
		    + " for file: " + file, e);
	    return null;
	}
    }

    public void closeLock_givenFileChannelToFailedBucketsLock_doesntCloseChannelWhenFileChannelIsNotOpen()
	    throws IOException {
	simpleFileLock.closeLock();
	assertTrue(!fileChannel.isOpen());
    }

    public void closeLock_givenClosedFileChannel_doesNotThrowAnything() {
	IOUtils.closeQuietly(fileChannel);
	assertTrue(!fileChannel.isOpen());
	simpleFileLock.closeLock();
    }
}