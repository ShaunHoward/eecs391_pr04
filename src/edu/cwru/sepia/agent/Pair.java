package edu.cwru.sepia.agent;

public class Pair<T, U> {
    private T x; //first member of pair
    private U y; //second member of pair

    public Pair(T x, U y) {
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

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
            return false;
		}
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Pair)) {
            return false;
        }
        
        Pair<?, ?> pair = (Pair<?,?>) obj;
        return x.equals(pair.getX()) && y.equals(pair.getY());
	}

}