package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class State {
	private List<Integer> footmen = new ArrayList<Integer>();
	private List<Integer> enemyFootmen = new ArrayList<Integer>();
	private Map<Integer, Integer> unitHealth = new HashMap<Integer, Integer>();
	private Map<Integer, Pair<Integer, Integer>> unitLocations = new HashMap<Integer, Pair<Integer, Integer>>();
	public int footmenDeadCount = 0;
	
	public State(List<Integer> footmen, List<Integer> enemyFootmen, Map<Integer, Integer> unitHealth, Map<Integer, Pair<Integer, Integer>> unitLocations) {
		this.footmen = new ArrayList<Integer>(footmen);
		this.enemyFootmen = new ArrayList<Integer>(enemyFootmen);
		this.unitHealth = new HashMap<Integer, Integer>(unitHealth);
		this.unitLocations = new HashMap<Integer, Pair<Integer, Integer>>(unitLocations);
	}

	public State(State state) {
		this.footmen = new ArrayList<Integer>(state.getFootmen());
		this.enemyFootmen = new ArrayList<Integer>(state.getEnemyFootmen());
		this.unitHealth = new HashMap<Integer, Integer>(state.getUnitHealth());
		this.unitLocations = new HashMap<Integer, Pair<Integer, Integer>>(state.getUnitLocations());
	}

	public List<Integer> getFootmen() {
		return footmen;
	}

	public List<Integer> getEnemyFootmen() {
		return enemyFootmen;
	}

	public Map<Integer, Integer> getUnitHealth() {
		return unitHealth;
	}

	public Map<Integer, Pair<Integer, Integer>> getUnitLocations() {
		return unitLocations;
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
	public static boolean isClosest(Integer footman, Integer enemy, List<Integer> enemyFootmen, Map<Integer, Pair<Integer, Integer>> unitLocations) {
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
	public static int chebyshevDistance(Pair<Integer, Integer> p, Pair<Integer, Integer> q) {
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
	public static int getAdjacentEnemyCount(Integer footman, List<Integer> enemyFootmen, Map<Integer, Pair<Integer, Integer>> unitLocations) {
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
	public static boolean isAdjacent(Pair<Integer, Integer> p, Pair<Integer, Integer> q) {
		
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
	public String toString() {
		String str = "";
		str += "==State==\n";
		
		str += "Friendlies:\n";
		for (Integer footman : footmen) {
			str += "\tFootman " + footman + ", HP: " + unitHealth.get(footman);
		}
		str += "\n";
		
		str += "Enemies:\n";
		for (Integer footman : enemyFootmen) {
			str += "\tFootman " + footman + ", HP: " + unitHealth.get(footman);
		}
		str += "\n";
		
		return str;
	}
}
