package sdis_proj1;

public class Pair<A, B> {
	
	private final A lhs;
	private final B rhs;

	public Pair(final A first, final B second) {
		lhs = first;
		rhs = second;
	}

	public final int hashCode() {
		final int hashFirst = lhs != null ? lhs.hashCode() : 0;
		final int hashSecond = rhs != null ? rhs.hashCode() : 0;
		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	public final boolean equals(final Object other) {

		if (other instanceof Pair) {
			final Pair<?, ?> o = (Pair<?, ?>) other;
			return ((lhs == o.lhs || 
					(lhs != null && o.lhs != null && lhs.equals(o.lhs))) &&
					(rhs == o.rhs ||
					(rhs != null && o.rhs != null && rhs.equals(o.rhs))));
		}

		return false;
	}

	public final String toString() {
		return "(" + lhs + ", " + rhs + ")";
	}

	public final A first() {
		return lhs;
	}

	public final B second() {
		return rhs;
	}
}