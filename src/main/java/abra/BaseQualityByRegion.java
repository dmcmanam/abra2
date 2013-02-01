package abra;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;

public class BaseQualityByRegion {

	public void run(String input, String regionsGtf) throws IOException {
		
		SAMFileReader reader = new SAMFileReader(new File(input));
		
		GtfLoader loader = new GtfLoader();
		List<Feature> regions = loader.load(regionsGtf);
		
		regions = ReAligner.splitRegions(regions);

		for (Feature region : regions) {
			
			long numReads = 0;
			long numBases = 0;
			long totalQuality = 0;
			long numBasesLt5 = 0;
			long numBasesLt10 = 0;
			long numBasesLt20 = 0;
			long numReadsLt5 = 0;
			long numReadsLt10 = 0;
			long numReadsLt20 = 0;
			long numReads5X10 = 0;
			long numInternalReadsLt20 = 0;
			long numReadsWithAmbiguousBases = 0;
			long numReadsIntersectLt20Ambiguous = 0;
			
			CloseableIterator<SAMRecord> iter = reader.queryOverlapping(region.getSeqname(), (int) region.getStart(), (int) region.getEnd());
			
			while (iter.hasNext()) {
				SAMRecord read = iter.next();
				String qualStr = read.getBaseQualityString();
				
				boolean readLt5 = false;
				boolean readLt10 = false;
				boolean readLt20 = false;
				boolean internalReadLt20 = false;
				
				numReads++;
				numBases += qualStr.length();
				int readBasesLt5 = 0;

				for (int i=0; i<qualStr.length(); i++) {
					
					// Assuming phred33
					int qual = qualStr.charAt(i) - '!';
					totalQuality += qual;
					
					if (qual < 5) {
						numBasesLt5++;
						readBasesLt5++;
						readLt5 = true;
					}
					
					if (qual < 10) {
						numBasesLt10++;
						readLt10 = true;
					}
					
					if (qual < 20) {
						numBasesLt20++;
						readLt20 = true;
						
						if ((i>=10) && (i<90)) {
							internalReadLt20 = true;
						}
					}
				}
				
				if (readLt5) {
					numReadsLt5++;
				}
				
				if (readLt10) {
					numReadsLt10++;
				}
				
				if (readLt20) {
					numReadsLt20++;
				}
				
				if (readBasesLt5 >= 10) {
					numReads5X10++;
				}
				
				if (internalReadLt20 == true) {
					numInternalReadsLt20++;
				}
				
				if (read.getReadString().contains("N")) {
					numReadsWithAmbiguousBases++;
				}
				
				if (readLt20 || read.getReadString().contains("N")) {
					numReadsIntersectLt20Ambiguous++;
				}
			}
			
			iter.close();
			
			System.out.println(region.getDescriptor() + "\t" +
					numBases + "\t" +
					numReads + "\t" +
					avg(numBasesLt5, numBases) + "\t" +
					avg(numBasesLt10, numBases) + "\t" +
					avg(numBasesLt20, numBases) + "\t" +
					avg(totalQuality, numBases) + "\t" +
					avg(numReadsLt5, numReads) + "\t" +
					avg(numReadsLt10, numReads) + "\t" +
					avg(numReadsLt20, numReads) + "\t" +
					avg(numReads5X10, numReads) + "\t" +
					avg(numInternalReadsLt20, numReads) + "\t" +
					avg(numReadsWithAmbiguousBases, numReads) + "\t" +
					avg(numReadsIntersectLt20Ambiguous, numReads));
		}

		reader.close();
	}
	
	private double avg(long num, long dem) {
		return (double) num / (double) dem;
	}
	
	public static void main(String[] args) throws Exception {
		BaseQualityByRegion bq = new BaseQualityByRegion();
		bq.run(args[0],  args[1]);
	}
}
