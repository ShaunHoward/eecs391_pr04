package edu.cwru.sepia.agent;

/**
 * A simple coordinate pair representing a point location in SEPIA.
 * 
 * @author Shaun Howard, Matt Swartwout
 *
 * @param <T> - Any number type (best would be Double)
 * @param <U> - Any number type (best would be Double)
 */
public class CoordPair<T, U> {
	//first value in pair
    private T x; 
    
    //second value in pair
    private U y; 

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

	/**
	 * Check if two pairs are equal based on x and y values.
	 * @return true if state.x=obj.x and state.y=obj.y
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof CoordPair)) {
            return false;
		}
//        if (obj == this) {
//            return true;
//        }
        
        CoordPair<?, ?> pair = (CoordPair<?,?>) obj;
        return x.equals(pair.getX()) && y.equals(pair.getY());
	}

}