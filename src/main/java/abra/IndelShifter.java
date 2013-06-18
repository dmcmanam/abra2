package abra;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

/**
 * Utility class for shifting Indels into leftmost position.
 * 
 * @author lmose
 */
public class IndelShifter {

	/*
	public SAMRecord shiftIndelsLeft(SAMRecord read, CompareToReference2 c2r) {
		try {
			
			if (containsIndel(read)) {
				int indelPos = firstIndelOffset(read);
				List<Integer> origMismatches = c2r.mismatchPositions(read);
				
				SAMRecord clone = cloneRead(read);
				for (int i=indelPos; i>0; i--) {
					Cigar newCigar = shiftCigarLeft(read.getCigar(), i);
					
//					System.out.println("cigar: " + newCigar.toString());
					clone.setCigar(newCigar);
					
					List<Integer> newMismatches = c2r.mismatchPositions(clone, origMismatches.size());
					
					if (origMismatches.equals(newMismatches)) {
						SAMRecord newRead = cloneRead(read);
						newRead.setCigar(newCigar);
						newRead.setAttribute("IS", read.getCigarString());
						return newRead;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error processing: " + read.getSAMString());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return read;
	}
	*/
	
	public SAMRecord shiftIndelsLeft(SAMRecord read, CompareToReference2 c2r) {
		try {
			
			if (containsIndel(read)) {
				int indelPos = firstIndelOffset(read);
				String origReadAltRef = c2r.getAlternateReference(read, read.getCigar());
				
//				System.out.println("o: " + origReadAltRef);
				
				if (origReadAltRef != null) {
					for (int i=indelPos; i>0; i--) {
						Cigar newCigar = shiftCigarLeft(read.getCigar(), i);
						
						String shiftedReadAltRef = c2r.getAlternateReference(read, newCigar);
//						System.out.println("s: " + shiftedReadAltRef);
						
	//					System.out.println("cigar: " + newCigar.toString());
						
						if (origReadAltRef.equals(shiftedReadAltRef)) {					
							SAMRecord newRead = cloneRead(read);
							newRead.setCigar(newCigar);
							newRead.setAttribute("IS", read.getCigarString());
							return newRead;
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error processing: " + read.getSAMString());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return read;
	}
	
	private SAMRecord cloneRead(SAMRecord read) {
		try {
			return (SAMRecord) read.clone();
		} catch (CloneNotSupportedException e) {
			// Infamous "this should never happen" comment here.
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	protected Cigar shiftCigarLeft(Cigar cigar, int positionsToShift) {
		Cigar newCigar = new Cigar();
		
		for (int i=0; i<cigar.getCigarElements().size(); i++) {
			CigarElement elem = cigar.getCigarElement(i);
			
			if (isFirstNonSoftClippedElem(i, cigar)) {
				int newLen = elem.getLength() - positionsToShift;
				
				if (newLen > 0) {
					CigarElement newElem = new CigarElement(newLen, elem.getOperator());
					newCigar.add(newElem);
				}
			} else if (isLastNonSoftClippedElem(i, cigar)) {
				if (elem.getOperator() == CigarOperator.M) {
					CigarElement newElem = new CigarElement(elem.getLength() + positionsToShift, CigarOperator.M);
					newCigar.add(newElem);
				} else {
					CigarElement newElem = new CigarElement(positionsToShift, CigarOperator.M);
					newCigar.add(elem);
					newCigar.add(newElem);
				}
			} else {
				newCigar.add(elem);
			}
		}
		
		return newCigar;
	}
	
	private boolean isFirstNonSoftClippedElem(int idx, Cigar cigar) {
		// First element, not soft clipped
		if ((idx == 0) && (cigar.getCigarElement(0).getOperator() != CigarOperator.S)) {
			return true;
		}
		
		// Second element, with first element soft clipped
		if ((idx == 1) && (cigar.getCigarElement(0).getOperator() == CigarOperator.S)) {
			return true;
		}
		
		return false;
	}
	
	private boolean isLastNonSoftClippedElem(int idx, Cigar cigar) {
		int numElems = cigar.getCigarElements().size();
		
		// Last element, not soft clipped.
		if ((idx == numElems-1) && (cigar.getCigarElement(idx).getOperator() != CigarOperator.S)) {
			return true;
		}
		
		// Second to last element, with last element soft clipped.
		if ((idx == numElems-2) && (cigar.getCigarElement(numElems-1).getOperator() == CigarOperator.S)) {
			return true;
		}

		return false;
	}
	
	private boolean containsIndel(SAMRecord read) {
		for (CigarElement elem : read.getCigar().getCigarElements()) {
			if ((elem.getOperator() == CigarOperator.D) || (elem.getOperator() == CigarOperator.I)) {
				return true;
			}
		}
		
		return false;
	}
	
	private int firstIndelOffset(SAMRecord read) {
		int pos = 0;
		
		for (CigarElement elem : read.getCigar().getCigarElements()) {
			if ((elem.getOperator() == CigarOperator.D) || (elem.getOperator() == CigarOperator.I)) {
				return pos;
			} else if (elem.getOperator() != CigarOperator.S) {
				pos += elem.getLength();
			}
		}
		
		throw new IllegalArgumentException("No indel for record: [" + read.getSAMString() + "]");
	}
	
	public static void main(String[] args) throws IOException {
		String in = args[0];
		String out = args[1];
		String ref = args[2];

//		String in = "/home/lmose/dev/abra/leftalign.sam";
//		String out = "/home/lmose/dev/abra/la.out.sam";
//		String ref = "/home/lmose/reference/chr1/chr1.fa";
		
		SAMFileReader reader = new SAMFileReader(new File(in));
		
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(
				reader.getFileHeader(), false, new File(out));
		
		CompareToReference2 c2r = new CompareToReference2();
		c2r.init(ref);
		
		IndelShifter indelShifter = new IndelShifter();

		for (SAMRecord read : reader) {
			SAMRecord shiftedRead = indelShifter.shiftIndelsLeft(read, c2r);
			writer.addAlignment(shiftedRead);
		}
		
		writer.close();
		reader.close();
	}
}