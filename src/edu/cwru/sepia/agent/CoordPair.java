package edu.cwru.sepia.agent;

public class CoordPair<T, U> {
    private T x; //first member of pair
    private U y; //second member of pair

    public CoordPair(T x, U y) {
        this.x = x;
        this.y = y;
    }

    public void setX(T x) {
        this.x = x;
    }

    public void setY(U y) {
        this.y = y;
    }

    public T getX() {
        return x;
    }

    public U getY() {
        return y;
    }
    
	/**
	 * Checks if two coordinates are adjacent
	 * 
	 * @param p The first coordinate pair
	 * @param q The second coordinate pair
	 * @return True, if p and q are adjacent
	 */
	public static boolean areAdjacent(CoordPair<Integer, Integer> p,
			CoordPair<Integer, Integer> q) {
		for (int i = p.getX() - 1; i <= p.getX() + 1; i++) {
			for (int j = p.getY() - 1; j <= p.getY() + 1; j++) {
				if (q.getX() == i && q.getY() == j) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
            return false;
		}
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CoordPair)) {
            return false;
        }
        
        CoordPair<?, ?> pair = (CoordPair<?,?>) obj;
        return x.equals(pair.getX()) && y.equals(pair.getY());
	}

}