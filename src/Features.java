import java.util.List;
import java.util.Map;


public class Features {
	private static final int NUM_FEATURES = 9;
	
	private double[] w;
	
	public Features() {
		
		w = new double[NUM_FEATURES];
		
		// Initialize random weights in (-1,1)
		for (int i = 0; i < w.length; i++) {
			w[i] = Math.random() * 2 - 1;
		}
	}
	
	/**
	 * Calculates Qw given a feature vector
	 * @param f
	 * @return Qw
	 */
	public double qFunction(double[] f) {
		double q = 0;
		
		for (int i = 0; i < w.length; i++) {
			q += w[i] * f[i];
		}
		
		return q;
	}
	
	/**
	 * Returns the values of the feature vector for a given state and action
	 * @param state
	 * @param footman
	 * @param enemy
	 * @param action
	 * @return the feature vector
	 */
	public static double[] getFeatures(State state, Integer footman, Integer enemy, AttackAction action) {
		return getFeatures(footman, enemy, state.getFootmen(),
				state.getEnemyFootmen(), state.getUnitHealth(), state.getUnitLocations(), action.getAttack());
	}
	
	/**
	 * Returns the values of the feature vector for a given state and action
	 * @param footman
	 * @param enemy
	 * @param footmen
	 * @param enemyFootmen
	 * @param unitHealth
	 * @param unitLocations
	 * @param attack
	 * @return the feature vector
	 */
	public static double[] getFeatures(Integer footman, Integer enemy, List<Integer> footmen, List<Integer> enemyFootmen,
			Map<Integer, Integer> unitHealth, Map<Integer, Pair<Integer, Integer>> unitLocations, Map<Integer, Integer> attack) {
		double[] f = new double[NUM_FEATURES];
		// constant
		f[0] = 1;
		
		// health of footman
		f[1] = unitHealth.get(footman);
		
		// health of enemy
		f[2] = unitHealth.get(enemy);
		
		// # of other friendly units currently attacking enemy
		f[3] = 0;
		for (Integer attacker : attack.keySet()) {
			if (attacker == footman) {
				continue;
			}
			f[3] += attack.get(attacker) == enemy ? 1 : 0;
		}
		
		// is enemy the current target of footman?
		if (attack.get(footman) == null) {
			f[4] = 0;
		} else {
			f[4] = attack.get(footman) == enemy ? 1 : 0;
		}
		
		// what is the ratio of hitpoints of enemy to footman
		f[5] = unitHealth.get(footman) / Math.max(unitHealth.get(enemy), 0.001);
		
		// is enemy my closest enemy?
		f[6] = isClosest(footman, enemy, enemyFootmen, unitLocations) ? 1 : 0;
		
		// is enemy within attacking range?
		f[7] = isAdjacent(unitLocations.get(footman), unitLocations.get(enemy)) ? 1 : 0;
		
		// how many enemies can attack footman?
		f[8] = numAdjacentEnemies(footman, enemyFootmen, unitLocations);
				
		return f;
	}

	/**
	 * Updates the weights vector given a feature vector and the calculated loss function
	 * @param f
	 * @param delLoss
	 * @param alpha
	 */
	public void updateWeights(double[] f, double delLoss, double alpha) {
		for (int i = 0; i < w.length; i++) {
			w[i] = w[i] + (alpha * delLoss * f[i]);
		}
	}

	/**
	 * 
	 * @param footman
	 * @param enemy
	 * @param enemyFootmen
	 * @param unitLocations
	 * @return True, if the given enemy is the closest enemy in terms of Chebyshev dist
	 */
	private static boolean isClosest(Integer footman, Integer enemy, List<Integer> enemyFootmen, Map<Integer, Pair<Integer, Integer>> unitLocations) {
		int enemyDist = chebyshevDistance(unitLocations.get(footman), unitLocations.get(enemy));
		
		for (Integer curEnemy : enemyFootmen) {
			if (chebyshevDistance(unitLocations.get(footman), unitLocations.get(curEnemy)) < enemyDist) {
				return false;
			}
		}
		
		return true;
	}
	
	private static int chebyshevDistance(Pair<Integer, Integer> p, Pair<Integer, Integer> q) {
		int x = p.getX() - q.getX();
		int y = p.getY() - q.getY();
		return Math.max(x, y);
	}
	
	private static int numAdjacentEnemies(Integer footman, List<Integer> enemyFootmen, Map<Integer, Pair<Integer, Integer>> unitLocations) {
		int adjacent = 0;
		for (Integer enemy : enemyFootmen) {
			if (isAdjacent(unitLocations.get(footman), unitLocations.get(enemy))) {
				adjacent++;
			}
		}
		return adjacent;
	}
	
	private static boolean isAdjacent(Pair<Integer, Integer> p, Pair<Integer, Integer> q) {
		for (int i = p.getX() - 1; i <= p.getX() + 1; i++) {
			for (int j = p.getY() - 1; j <= p.getY() + 1; j++) {
				if (q.getX() == i && q.getY() == j) {
					return true;
				}
			}
		}
		
		return false;
	}

}
