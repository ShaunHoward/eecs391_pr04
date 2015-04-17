import java.util.List;
import java.util.Map;

/**
 * Stores the feature vector for a given state and action.
 * Has functions to perform various operations such as determining nearest enemies and
 * Q function values.
 * 
 * @author Shaun Howard (smh150), Matthew Swartwout (mws85)
 */
public class Features {
	
	//The number of features to store
	private static final int NUM_FEATURES = 9;
	
	//The weights of each feature
	private double[] featureWeights;
	
	/**
	 * Construct new features, with a constant number of features (9).
	 * Each feature weight will be random at the start.
	 */
	public Features() {
		
		featureWeights = new double[NUM_FEATURES];
		
		// Initialize random weights in (-1,1)
		for (int i = 0; i < featureWeights.length; i++) {
			featureWeights[i] = Math.random() * 2 - 1;
		}
	}
	
	/**
	 * Determines the Q weight value by using the given feature vector.
	 * 
	 * @param featureVector - the vectore of feature values to use in calculation
	 * @return the Q weight value of the given feature vector
	 */
	public double qFunction(double[] featureVector) {
		double qWeight = 0;
		
		for (int i = 0; i < featureWeights.length; i++) {
			qWeight += featureWeights[i] * featureVector[i];
		}
		
		return qWeight;
	}
	
	/**
	 * Gets the feature vector of a given state using the footman id, the enemy id, and an attack action map.
	 * 
	 * @param state - the game state to get the features of
	 * @param footman - the footman id to get the feature vector in reference to
	 * @param enemy - the enemy id targeted by the given footman
	 * @param action - an attack action map to keep track of the current attack actions
	 * @return the feature vector in reference to the given values
	 */
	public static double[] getFeatures(State state, Integer footman, Integer enemy, AttackAction action) {
		return getFeatures(footman, enemy, state.getFootmen(),
				state.getEnemyFootmen(), state.getUnitHealth(), state.getUnitLocations(), action.getAttack());
	}
	
	/**
	 * Gets the feature vector of a given state using the footman id, the enemy id, a list of footmen ids, a list of enemy ids,
	 * the health of all units, a map of the unit locations, and a map of attack actions in reference to unit ids.
	 * 
	 * @param footman - the footman id in reference to
	 * @param enemy - the enemy targeted by the given footman
	 * @param footmen - a list of the footmen currently in the game state
	 * @param enemyFootmen - a list of the enemy footmen currently in the game state
	 * @param unitHealth - a list of the health of all units
	 * @param unitLocations - a map of the locations of all units
	 * @param attack - the attack action map in refernce to unit ids
	 * @return the feature vector in reference to the given values
	 */
	public static double[] getFeatures(Integer footman, Integer enemy, List<Integer> footmen, List<Integer> enemyFootmen,
			Map<Integer, Integer> unitHealth, Map<Integer, Pair<Integer, Integer>> unitLocations, Map<Integer, Integer> attack) {
		
		double[] featureVector = new double[NUM_FEATURES];
		
		//first feature value is always 1
		featureVector[0] = 1;
		
		//footman health
		featureVector[1] = unitHealth.get(footman);
		
		//enemy health
		featureVector[2] = unitHealth.get(enemy);
		
		//the count of additional team mates attacking enemy at this moment
		featureVector[3] = 0;
		for (Integer attacker : attack.keySet()) {
			if (attacker == footman) {
				continue;
			}
			featureVector[3] -= attack.get(attacker) == enemy ? 1 : 0;
		}
		
		//determine if the footman currently targets the given enemy
		if (attack.get(footman) == null) {
			featureVector[4] -= .01;
		} else if (attack.get(footman) == enemy) {
			featureVector[4] += .02;
		}
		
		//determine the ratio of hit-points from enemy to those of footman
		featureVector[5] = unitHealth.get(footman) / Math.max(unitHealth.get(enemy), .001);
		
		//determine whether the enemy is the closest possible enemy
		if (isClosest(footman, enemy, enemyFootmen, unitLocations)){
			featureVector[6] += .3;
		} else {
			featureVector[6] -= .4;
		}
		
		//determine if the enemy can be attacked based on range from current footman
		if(isAdjacent(unitLocations.get(footman), unitLocations.get(enemy))){
			featureVector[7] += .03;
		}
		int adjEnemyCount = getAdjacentEnemyCount(footman, enemyFootmen, unitLocations);
		//determine how many enemies can currently attack the given footman
		if (adjEnemyCount <= 2){
			featureVector[8] += ((0.02 * adjEnemyCount) / Math.random());
		} else {
			featureVector[8] -= ((0.1 * adjEnemyCount) / Math.random());
		}
				
		return featureVector;
	}

	/**
	 * Updates the feature weights vector given a feature vector, a calculated loss function, and an alpha.
	 * 
	 * @param f - the feature vector to update the feature weights set with
	 * @param calcLoss - the calculated loss function
	 * @param alpha - a variable alpha value
	 */
	public void updateWeights(double[] f, double calcLoss, double alpha) {
		for (int i = 0; i < featureWeights.length; i++) {
			featureWeights[i] += (alpha * calcLoss * f[i]);
		}
	}

	/**
	 * Determines if the given enemy is the closest enemy
	 * 
	 * @param footman - the footman to use the reference point of
	 * @param enemy - the enemy to find the distance from the footman
	 * @param enemyFootmen - the list of enemy footmen
	 * @param unitLocations - the list of unit locations
	 * @return if the given enemy is the closest enemy in terms of the Chebyshev distance
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
	
	/**
	 * Calculates the c. distance between two points (pairs).
	 * 
	 * @param p - the first point to calc distance from
	 * @param q - the second point to calc distance to
	 * @return the distance between points p and q
	 */
	private static int chebyshevDistance(Pair<Integer, Integer> p, Pair<Integer, Integer> q) {
		int x = p.getX() - q.getX();
		int y = p.getY() - q.getY();
		return Math.max(x, y);
	}
	
	/**
	 * Determines the number of adjacent enemies given a footman, list of footmen, and a map of unit locations.
	 * 
	 * @param footman - the footman to use the reference point of
	 * @param enemyFootmen - the enemy footmen in the game state
	 * @param unitLocations - the unit locations in the game state
	 * @return the number of enemies adjacent to the given footman
	 */
	private static int getAdjacentEnemyCount(Integer footman, List<Integer> enemyFootmen, Map<Integer, Pair<Integer, Integer>> unitLocations) {
		int adjacent = 0;
		for (Integer enemy : enemyFootmen) {
			if (isAdjacent(unitLocations.get(footman), unitLocations.get(enemy))) {
				adjacent++;
			}
		}
		return adjacent;
	}
	
	/**
	 * Helper function to determine if two points are adjacent to each other.
	 * 
	 * @param p - the first point to check
	 * @param q - the second point to check
	 * @return whether p and q are adjacent to eachother
	 */
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
