package com.splunk.shep.mapreduce.lib.rest;


import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.splunk.shep.testutil.FileSystemUtils;
import com.splunk.shep.testutil.HadoopFileSystemPutter;

public class SplunkEventsInputFormatTest {

    private static final String TEST_INPUT_FILENAME_1 = "sdata1";
    private static final String TEST_INPUT_FILENAME_2 = "sdata2";
    private HadoopFileSystemPutter putter;
    private FileSystem fileSystem;

    @BeforeMethod(groups = { "slow-unit" })
    public void setUp() {
	fileSystem = FileSystemUtils.getLocalFileSystem();
	putter = HadoopFileSystemPutter.create(fileSystem);
    }

    @AfterMethod(groups = { "slow-unit" })
    public void tearDown() {
	putter.deleteMyFiles();
    }

    @Test(groups = { "slow-unit" })
    public void should_getExpectedOutput_when_havingSplunkEventsInputFormat_as_inputFormat()
	    throws IOException, InterruptedException, ClassNotFoundException {
	putFilesOnHadoop();

	runMapReduceJob();

	verifyOutput();
    }

    private void putFilesOnHadoop() {
	File file1 = getFirstFile();
	File file2 = getSecondFile();
	putter.putFile(file1);
	putter.putFile(file2);
    }

    private File getFirstFile() {
	return getFileForFileName(TEST_INPUT_FILENAME_1);
    }

    private File getSecondFile() {
	return getFileForFileName(TEST_INPUT_FILENAME_2);
    }

    private File getFileForFileName(String fileName) {
	return new File(MapRedRestTestConstants.TEST_RESOURCES_PATH + "/"
		+ fileName);
    }

    private void runMapReduceJob() throws IOException, InterruptedException,
	    ClassNotFoundException {
	Job job = new Job();
	Configuration conf = job.getConfiguration();
	job.setJobName(this.getClass().getSimpleName());
	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(NullWritable.class);

	job.setMapperClass(Map.class);

	job.setInputFormatClass(SplunkEventsInputFormat.class);
	job.setOutputFormatClass(TextOutputFormat.class);

	Path pathForFirst = putter.getPathForFile(getFirstFile());
	Path pathForSecondFile = putter.getPathForFile(getSecondFile());
	FileInputFormat.setInputPaths(job, pathForFirst, pathForSecondFile);
	FileOutputFormat.setOutputPath(job, getOutputPath());

	job.waitForCompletion(true);
    }

    private Path getOutputPath() {
	return new Path(putter.getPathOfMyFiles(), "output");
    }

    private void verifyOutput() throws IOException {
	List<String> expectedLines = getExpectedLines();
	FSDataInputStream inputStream = fileSystem.open(new Path(
		getOutputPath(), "part-r-00000"));
	List<String> readLines = IOUtils.readLines(inputStream);
	assertEquals(readLines, expectedLines);
    }

    private List<String> getExpectedLines() {
	List<String> expectedLines = new ArrayList<String>();
	expectedLines.add("<105>Jan 19 15:37:41 2012 datagen-host63"
		+ " postfix/qmgr[27513]: 1G9XWOG6QI: removed ");
	expectedLines.add("<115>Jan 19 15:37:41 2012 datagen-host99"
		+ " postfix/qmgr[15450]: VLNUTPSGNB: removed ");
	expectedLines
		.add("<130>Jan 19 18:08:54 2012 datagen-host93"
			+ " postfix/smtpd[13396]: disconnect from unknown[44.254.53.22] ");
	expectedLines.add("<136>Jan 19 18:08:54 2012 datagen-host2"
		+ " snmpd[18798]: truncating integer value > 32 bits ");
	expectedLines.add("<13>Jan 19 15:37:41 2012 datagen-host76"
		+ " postfix/qmgr[19920]: JQ4FZ93VTE: removed ");
	expectedLines.add("<166>Jan 19 15:37:41 2012 datagen-host25"
		+ " status=101 DHCPACK=ASCII from host=22.71.238.208 ");
	expectedLines
		.add("<168>Jan 19 15:37:41 2012 datagen-host1"
			+ " sshd(pam_unix)[3269]: session on port=950 closed for user cgreene ASCII");
	expectedLines.add("<53>Jan 19 18:08:55 2012 datagen-host54"
		+ " crond[31203]: (root) CMD (run-parts /etc/cron.hourly) ");
	expectedLines
		.add("<98>Jan 19 18:08:55 2012 datagen-host63"
			+ " postfix/smtpd[32053]: disconnect from unknown[22.121.157.180] ");
	return expectedLines;
    }

    public static class Map extends
	    Mapper<LongWritable, Text, Text, NullWritable> {

	@Override
	public void map(LongWritable key, Text value, Context context)
		throws IOException {
	    NullWritable nullwr = NullWritable.get();
	    try {
		FEvent event = getEventObject(value.toString());
		value.set(event.getBody());
		context.write(value, nullwr);
	    } catch (Exception e) {
		throw new IOException(e);
	    }
	}

	private FEvent getEventObject(String item) throws Exception {
	    JsonFactory f = new JsonFactory();
	    JsonParser jp = f.createJsonParser(item);
	    FEvent event = new FEvent();
	    jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
	    while (jp.nextToken() != JsonToken.END_OBJECT) {
		String fieldname = jp.getCurrentName();
		jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
		if ("fields".equals(fieldname)) {
		    FEvent.Fields field = new FEvent.Fields();
		    while (jp.nextToken() != JsonToken.END_OBJECT) {
			String namefield = jp.getCurrentName();
			jp.nextToken(); // move to value
			if ("source".equals(namefield)) {
			    field.setSource(jp.getText());
			} else if ("sourceType".equals(namefield)) {
			    field.setSourceType(jp.getText());
			} else {
			    throw new IllegalStateException(
				    "Unrecognized field '" + fieldname + "'!");
			}
		    }
		    event.setFields(field);
		} else if ("body".equals(fieldname)) {
		    event.setBody(jp.getText());
		} else if ("host".equals(fieldname)) {
		    event.setHost(jp.getText());
		} else if ("timestamp".equals(fieldname)) {
		    event.setTimestamp(jp.getText());
		}
	    }
	    return event;
	}
    }

    /**
     * What is FEvent?
     */
    public static class FEvent {
	private String body;
	private String timestamp;
	private String host;
	private Fields fields;

	public Fields getFields() {
	    return this.fields;
	}

	public void setFields(Fields fields) {
	    this.fields = fields;
	}

	public String getBody() {
	    return body;
	}

	public void setBody(String body) {
	    this.body = body;
	}

	public String getTimestamp() {
	    return timestamp;
	}

	public void setTimestamp(String timestamp) {
	    this.timestamp = timestamp;
	}

	public String getHost() {
	    return host;
	}

	public void setHost(String host) {
	    this.host = host;
	}

	public static class Fields {
	    private String sourceType;
	    private String source;

	    public String getSourceType() {
		return sourceType;
	    }

	    public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	    }

	    public String getSource() {
		return source;
	    }

	    public void setSource(String source) {
		this.source = source;
	    }

	}

    }

}
