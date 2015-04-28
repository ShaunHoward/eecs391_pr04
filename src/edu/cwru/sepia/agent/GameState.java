package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A game state to track all necessary variables for Q-learning in the SEPIA game engine.
 * The state contains the footmen and enemies in the level, the health and location of those units, and the number of footmen dead.
 * 
 * This simply serves as a container to all of the SEPIA values necessary to implement learning to defeat enemy footmen.
 * 
 * @author Shaun Howard, Matt Swartwout
 */
public class GameState {
	
	//footmen and enemys from the state
	private List<Integer> footmen = new ArrayList<Integer>();
	private List<Integer> enemyFootmen = new ArrayList<Integer>();
	
	//health of all the units, indexed by id
	private Map<Integer, Integer> unitHealth = new HashMap<Integer, Integer>();
	
	//locations of all the units as coordinate pairs, indexed by id
	private Map<Integer, CoordPair<Integer, Integer>> unitLocations = new HashMap<Integer, CoordPair<Integer, Integer>>();
	
	//track the number of footmen dead
	public int footmenDeadCount = 0;
	
	//A basic game state constructor from units, health, and locations
	public GameState(List<Integer> footmen, List<Integer> enemyFootmen, Map<Integer, Integer> unitHealth, Map<Integer, CoordPair<Integer, Integer>> unitLocations) {
		this.footmen = new ArrayList<Integer>(footmen);
		this.enemyFootmen = new ArrayList<Integer>(enemyFootmen);
		this.unitHealth = new HashMap<Integer, Integer>(unitHealth);
		this.unitLocations = new HashMap<Integer, CoordPair<Integer, Integer>>(unitLocations);
	}

	//A copy constructor
	public GameState(GameState state) {
		this.footmen = new ArrayList<Integer>(state.getFootmen());
		this.enemyFootmen = new ArrayList<Integer>(state.getEnemyFootmen());
		this.unitHealth = new HashMap<Integer, Integer>(state.getUnitHealth());
		this.unitLocations = new HashMap<Integer, CoordPair<Integer, Integer>>(state.getUnitLocations());
	}

	//basic getters and setters
	
	public List<Integer> getFootmen() {
		return footmen;
	}

	public List<Integer> getEnemyFootmen() {
		return enemyFootmen;
	}

	public Map<Integer, Integer> getUnitHealth() {
		return unitHealth;
	}

	public Map<Integer, CoordPair<Integer, Integer>> getUnitLocations() {
		return unitLocations;
	}
	
	/**
	 * Determines if the given enemy is the closest enemy.
	 * This is useful for determining when to target new enemies if they are closer.
	 * 
	 * @param footman - the footman to use the reference point of
	 * @param enemy - the enemy to find the distance from the footman
	 * @param enemyFootmen - the list of enemy footmen
	 * @param unitLocations - the list of unit locations
	 * @return if the given enemy is the closest enemy in terms of the Chebyshev distance
	 */
	public static boolean isClosest(Integer footman, Integer enemy, List<Integer> enemyFootmen, Map<Integer, CoordPair<Integer, Integer>> unitLocations) {
		int enemyDist = chebyshevDistance(unitLocations.get(footman), unitLocations.get(enemy));
		
		for (Integer curEnemy : enemyFootmen) {
			if (chebyshevDistance(unitLocations.get(footman), unitLocations.get(curEnemy)) < enemyDist) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Calculates the Chebyshev distance between two points (pairs).
	 * 
	 * @param p - the first point to calculate distance from
	 * @param q - the second point to calculate distance to
	 * @return the distance between points p and q
	 */
	public static int chebyshevDistance(CoordPair<Integer, Integer> p, CoordPair<Integer, Integer> q) {
		int x = p.getX() - q.getX();
		int y = p.getY() - q.getY();
		return Math.max(x, y);
	}
	
	/**
	 * Determines the number of enemies adjacent to the given footman in the current state.
	 * 
	 * @param footman - the footman to use the reference point of
	 * @param enemyFootmen - the enemy footmen in the game state
	 * @param unitLocations - the unit locations in the game state
	 * @return the number of enemies adjacent to the given footman
	 */
	public static int getAdjacentEnemyCount(Integer footman, List<Integer> enemyFootmen, Map<Integer, CoordPair<Integer, Integer>> unitLocations) {
		int adjacent = 0;
		for (Integer enemy : enemyFootmen) {
			if (isAdjacent(unitLocations.get(footman), unitLocations.get(enemy))) {
				adjacent++;
			}
		}
		return adjacent;
	}
	
	/**
	 * Determines if two points are adjacent to each other.
	 * 
	 * @param p - the first point to check
	 * @param q - the second point to check
	 * @return whether p and q are adjacent to each other
	 */
	public static boolean isAdjacent(CoordPair<Integer, Integer> p, CoordPair<Integer, Integer> q) {
		
		//check x-direction
		for (int i = p.getX() - 1; i <= p.getX() + 1; i++) {
			//check y-direction
			for (int j = p.getY() - 1; j <= p.getY() + 1; j++) {
				//got an adjacent point
				if (q.getX() == i && q.getY() == j) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Simply returns a string including valuable details about this state.
	 * @return the string of footman, health, and enemies and their health
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("==State==\n");
		
		builder.append("Friendlies:\n");
		for (Integer footman : footmen) {
			builder.append("\tFootman " + footman + ", HP: " + unitHealth.get(footman));
		}
		builder.append("\n");
		
		builder.append("Enemies:\n");
		for (Integer footman : enemyFootmen) {
			builder.append("\tFootman " + footman + ", HP: " + unitHealth.get(footman));
		}
		builder.append("\n");
		
		return builder.toString();
	}
}
