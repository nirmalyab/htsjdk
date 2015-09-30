package htsjdk.samtools;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * The purpose is basically to show that unsigned ints work only with SILENT validation.
 * BAM and CRAM exhibit similar behaviour.
 * <p/>
 * Created by vadim on 28/10/2015.
 */
public class UnsignedIntTest {
    private static final File TEST_DATA_DIR = new File("testdata/htsjdk/samtools");
    private static final String unsignedBam_filename = "unsignedInt.bam";
    private static final long TOO_LARGE = 0xFFFFFFFFL + 1L;
    private static final long UNSIGNED_INT_TEST_VALUE = Integer.MAX_VALUE + 1L;
    private static final long ATTR_VALUE_IN_TEST_BAM = 4294967295L;

    private void test(final long value, final boolean bamOrCram) throws IOException {
        final SAMFileHeader header = new SAMFileHeader();
        final File outFile = File.createTempFile("unsignedIntTest", ".bam");
        outFile.deleteOnExit();
        final SAMFileWriter w;
        if (bamOrCram) {
            w = new SAMFileWriterFactory().makeBAMWriter(header, false, outFile);
        } else {
            w = new SAMFileWriterFactory().makeCRAMWriter(header, outFile, null);
        }

        final SAMRecord record = new SAMRecord(header);
        record.setReadName("1");
        record.setReadUnmappedFlag(true);
        record.setReadBases("A".getBytes());
        record.setBaseQualityString("!");
        record.setAttribute("UI", value);
        Assert.assertEquals(value, record.getAttribute("UI"));

        w.addAlignment(record);
        w.close();

        final SamReader reader = SamReaderFactory.make().validationStringency(ValidationStringency.SILENT).open(outFile);
        final SAMRecordIterator iterator = reader.iterator();
        Assert.assertTrue(iterator.hasNext());
        final SAMRecord record2 = iterator.next();
        final Number returnedValue = (Number) record2.getAttribute("UI");
        Assert.assertEquals(value, returnedValue.longValue());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_BAM_roundtrip_tooLarge() throws IOException {
        test(TOO_LARGE, true);
    }

    @Test
    public void test_BAM_roundtrip() throws IOException {
        test(UNSIGNED_INT_TEST_VALUE, true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_CRAM_roundtrip_tooLarge() throws IOException {
        test(TOO_LARGE, false);
    }

    @Test
    public void test_CRAM_roundtrip() throws IOException {
        test(UNSIGNED_INT_TEST_VALUE, false);
    }

    @Test
    public void test_BAM_fromFile_SILENT() {
        final SamReader reader = SamReaderFactory.make().validationStringency(ValidationStringency.SILENT).open(new File(TEST_DATA_DIR, unsignedBam_filename));
        final SAMRecord record = reader.iterator().next();
        Assert.assertEquals(ATTR_VALUE_IN_TEST_BAM, record.getAttribute("ia"));
    }

    @Test(expectedExceptions = SAMFormatException.class)
    public void test_BAM_fromFile() {
        final SamReader reader = SamReaderFactory.make().open(new File(TEST_DATA_DIR, unsignedBam_filename));
        reader.iterator().next();
    }

    @Test
    public void test_CRAM_fromFile_SILENT() throws IOException {
        final SamReader reader = SamReaderFactory.make().validationStringency(ValidationStringency.SILENT).open(new File(TEST_DATA_DIR, unsignedBam_filename));
        final SAMRecord record = reader.iterator().next();

        final File outFile = File.createTempFile("unsignedIntTest", ".cram");
        outFile.deleteOnExit();
        final SAMFileWriter writer = new SAMFileWriterFactory().makeCRAMWriter(reader.getFileHeader(), outFile, null);
        writer.addAlignment(record);
        writer.close();

        final SamReader reader2 = SamReaderFactory.make().validationStringency(ValidationStringency.SILENT).open(outFile);
        final SAMRecordIterator iterator = reader2.iterator();
        Assert.assertTrue(iterator.hasNext());
        final SAMRecord record2 = iterator.next();
        Assert.assertEquals(ATTR_VALUE_IN_TEST_BAM, record2.getAttribute("ia"));
    }

    @Test(expectedExceptions = SAMFormatException.class)
    public void test_CRAM_fromFile() throws IOException {
        SamReaderFactory.setDefaultValidationStringency(ValidationStringency.DEFAULT_STRINGENCY);
        final SamReader reader = SamReaderFactory.makeDefault().open(new File(TEST_DATA_DIR, unsignedBam_filename));
        reader.iterator().next();
    }


}
