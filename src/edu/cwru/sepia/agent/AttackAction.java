package edu.cwru.sepia.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Attack action class that simply stores the map of actions that deal with attacking
 * for the player's footmen.
 * 
 * @author Shaun Howard (smh150)
 *
 */
public class AttackAction {
	private Map<Integer, Integer> attack = new HashMap<Integer, Integer>();
	
	public AttackAction(Map<Integer, Integer> attack) {
		this.attack = new HashMap<Integer, Integer>(attack);
	}

	public Map<Integer, Integer> getAttack() {
		return attack;
	}
	
}
