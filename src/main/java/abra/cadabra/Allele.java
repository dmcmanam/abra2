package abra.cadabra;

public class Allele {
	public enum Type {
		A, T, C, G, INS, DEL, UNK
	}

	private Type type;
	private int length;
	private String insertBases;
	
	public static final Allele UNK = new Allele(Type.UNK, 1);
	public static final Allele A = new Allele(Type.A, 1);
	public static final Allele T = new Allele(Type.T, 1);
	public static final Allele C = new Allele(Type.C, 1);
	public static final Allele G = new Allele(Type.G, 1);
	
	public Allele(Type type, int length) {
		this.type = type;
		this.length = length;
	}
	
	public Allele(Type type, int length, String insertBases) {
		this.type = type;
		this.length = length;
		this.insertBases = insertBases;
	}
	
	public static Allele getAllele (char base) {
		switch (base) {
			case 'A':
				return A;
			case 'T':
				return T;
			case 'C':
				return C;
			case 'G':
				return G;
			default:
				return UNK;
		}
	}
	
	public Type getType() {
		return type;
	}
	
	public int getLength() {
		return length;
	}
	
	public String getInsertBases() {
		return insertBases;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((insertBases == null) ? 0 : insertBases.hashCode());
		result = prime * result + length;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Allele other = (Allele) obj;
		if (insertBases == null) {
			if (other.insertBases != null)
				return false;
		} else if (!insertBases.equals(other.insertBases))
			return false;
		if (length != other.length)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

}
