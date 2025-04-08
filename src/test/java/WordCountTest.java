import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.test.MiniDFSCluster;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class WordCountTest {

    private MiniDFSCluster dfsCluster;
    private MiniYARNCluster yarnCluster;
    private Configuration conf;

    @Before
    public void setup() throws Exception {
        conf = new Configuration();

        // Start MiniDFSCluster
        dfsCluster = new MiniDFSCluster.Builder(conf).build();
        dfsCluster.waitActive();

        // Start MiniYARNCluster
        yarnCluster = new MiniYARNCluster("MiniYARNCluster", 1, 1, 1);
        yarnCluster.init(conf);
        yarnCluster.start();
    }

    @Test
    public void testWordCount() throws Exception {
        // Write input to HDFS
        Path inputPath = new Path("/input/test.txt");
        Path outputPath = new Path("/output/");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                dfsCluster.getFileSystem().create(inputPath, true)))) {
            writer.write("hello world\n");
            writer.write("hello hadoop\n");
            writer.write("hadoop world\n");
        }

        // Configure and run the job
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(WordCount.TokenizerMapper.class);
        job.setReducerClass(WordCount.IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        assertTrue("Job failed", job.waitForCompletion(true));

        // Validate output
        String output = new String(Files.readAllBytes(Paths.get("output/part-r-00000")));
        String expectedOutput = "hadoop\t2\nhello\t2\nworld\t2\n";
        assertTrue("Output mismatch", output.trim().equals(expectedOutput.trim()));
    }

    @After
    public void cleanup() throws Exception {
        if (yarnCluster != null) yarnCluster.stop();
        if (dfsCluster != null) dfsCluster.shutdown();
    }
}
