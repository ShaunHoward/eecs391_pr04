package edu.cwru.sepia.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * AttackAction class that simply stores the map of actions that deal with attacking
 * for the player's footmen.
 * 
 * @author Shaun Howard (smh150), Matthew Swartwout(mws85)
 *
 */
public class AttackAction {
	private Map<Integer, Integer> attack = new HashMap<Integer, Integer>();
	
	/**
	 * Stores the attacks that are input into the HashMap
	 * @param attack The attack to be stored in the HashMap
	 */
	public AttackAction(Map<Integer, Integer> attack) {
		this.attack = new HashMap<Integer, Integer>(attack);
	}
	
	/**
	 * Returns the HashMap of attacks
	 * @return The HashMap of attack actions
	 */
	public Map<Integer, Integer> getAttack() {
		return attack;
	}
	
}
