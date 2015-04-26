package edu.cwru.sepia.agent;

/**
 *  Strategy Engine for Programming Intelligent Agents (SEPIA)
    Copyright (C) 2012 Case Western Reserve University

    This file is part of SEPIA.

    SEPIA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SEPIA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SEPIA.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

/**
 * A reinforcement learning agent designed to learn how to use the good footmen to defeat the enemy footmen in SEPIA game engine.
 * 
 * @author Shaun Howard (smh150) and Matt Swartwout (mws85)
 */
public class RLAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	
	//the discount factor, a constant gamma
	private static final double GAMMA = 0.9;
	
	//the learning rate of the agent
	private static final double ALPHA = 0.0001;
	
	//the GLIE exploration value
	private static final double EPSILON = 0.1;
	
	//state before the current state
	private State prevState = null;
	
	//the random instance for use of generating random events
	private static Random random;
	
	//good and bad footmen
    private List<Integer> myFootmen;
    private List<Integer> enemyFootmen;
    
    //track the rewards received
    private List<Double> rewards;
	
	//actions before the current actions
	private AttackAction prevAction = new AttackAction(new HashMap<Integer, Integer>());
	
	//values used when in evaluation mode,
	//this will make the agent freeze its Q function and play 5 evaluation games
	private boolean evaluationMode = false;
	private int gameNumber = 1;
	private int evalGameNumber = 1;
	
	//limit the number of episodes to 100
	private int episodes = 100;
	
	//Q-learning weights
	public Double[] featureWeights;
	
	private boolean firstRound = true;
	
    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;
	
	//feature vector size of this agent
    public static final int NUM_FEATURES = 9;
	
	//some values useful for calculations along with above
	private double currEpsilon;
	private double currentGameReward = 0.0;
	private double avgGameReward = 0.0;
	public int footmenDeadCount = 0;
	
	private String finalOutput = "";
	
	private StateView currentState;
	
	public RLAgent(int playernum, String[] args) {
		super(playernum);
		
		currEpsilon = EPSILON;
		
		random = new Random();
		
		if (args.length > 0) {
			episodes = Integer.parseInt(args[0]);
		} else {
            episodes = 10;
            System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
        }
		
		boolean loadWeights = false;
        if (args.length > 1) {
            loadWeights = Boolean.parseBoolean(args[1]);
        } else {
            System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        }

        if (loadWeights) {
            featureWeights = loadWeights();
        } else {
            // initialize weights to random values between -1 and 1
            featureWeights = new Double[NUM_FEATURES];
            for (int i = 0; i < featureWeights.length; i++) {
                featureWeights[i] = random.nextDouble() * 2 - 1;
            }
        }
        
        rewards = new ArrayList<>();
        rewards.add(avgGameReward);
    }

	@Override
	public Map<Integer, Action> initialStep(StateView stateView, History.HistoryView statehistory) {

        // Find all of our units
        myFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                myFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        // Find all of the enemy units
        enemyFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                enemyFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }
		
        //intialize first values for the game state, game reward, and for the mode to operate in
		currentState = stateView;
		
		currentGameReward = 0.0;
		
		evaluationMode = (gameNumber - 1) % 15 > 9;
		
		//code for testing mode
		if (!evaluationMode) {
			avgGameReward = 0.0;
			evalGameNumber = 1;
		}
		
		firstRound = true;
		prevState = null;
		prevAction = new AttackAction(new HashMap<Integer, Integer>());
		
		return middleStep(stateView, statehistory);
	}

    /**
     * Calculates the reward at each step and updates the totals.
     * Checks if an event has occurred. If it has then you weights are updated and a new action is selected.
     *
     * Removes dead units from footmen vectors. remove killed units.
     *
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an even whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     *
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     *
     * @return New actions to execute or nothing if an event has not occurred.
     */
	@Override
	public Map<Integer,Action> middleStep(StateView newState, History.HistoryView statehistory) {
		Map<Integer,Action> builder = new HashMap<Integer,Action>();
		currentState = newState;

		List<Integer> curFootmen = new ArrayList<Integer>();
		List<Integer> curEnemyFootmen = new ArrayList<Integer>();
		Map<Integer, Integer> curUnitHealth = new HashMap<Integer, Integer>();
		Map<Integer, Pair<Integer, Integer>> curUnitLocations = new HashMap<Integer, Pair<Integer, Integer>>();
		
		for (UnitView unit : currentState.getAllUnits()) {
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Footman")) {
				curUnitLocations.put(unit.getID(), new Pair<Integer, Integer>(unit.getXPosition(), unit.getYPosition()));
				curUnitHealth.put(unit.getID(), unit.getHP());
				if (currentState.getUnits(playernum).contains(unit)) {
					curFootmen.add(unit.getID());
				} else {
					curEnemyFootmen.add(unit.getID());
				}
			}
		}
		
		State currentState = new State(curFootmen, curEnemyFootmen, curUnitHealth, curUnitLocations);
		
		if (!firstRound) {
			if (!eventHasHappened(currentState, prevState)) {
				return builder;
			}
			
			double currReward = 0;
			
			for (Integer footman : prevState.getFootmen()) {
				
				double reward = calculateReward(currentState, prevState, prevAction, footman);
				System.out.println("Reward = " + reward);
				currentGameReward += Math.max(currentGameReward, reward);
				
				if (!evaluationMode) {
					updateWeights(reward, currentState, prevState, prevAction, footman);
				}
			}
//			System.out.println("");
		} else {
			firstRound = false;
		}
		
		// Update previous state to current state
		prevState = currentState;
		
		prevAction = selectAction(prevState, prevAction);
		
		for (Integer footmanID : prevAction.getAttack().keySet()) {
			Action b = TargetedAction.createCompoundAttack(footmanID, prevAction.getAttack().get(footmanID));
			builder.put(footmanID, b);
		}
		
		return builder;
	}

    /**
     * Calculates the cumulative average rewards for testing episodes and prints them.
     *
     * Weights are saved with the saveWeights function.
     * 
     * @param stateView - the current state
     * @param historyView - the history of states
     */
	@Override
	public void terminalStep(StateView stateView, History.HistoryView historyView) {
		
        // MAKE SURE YOU CALL printTestData after you finish a test episode.
		StringBuilder builder = new StringBuilder();

        // Save your weights
        saveWeights(featureWeights);
		
		boolean won = false;
		for (UnitView unit : currentState.getUnits(playernum)) {
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Footman")) {
				won = true;
			}
		}
				
		String result = won ? "won" : "lost";
		
		//Calculate the average cumulative reward during tests
		if (evaluationMode) {
			
			//find the current average game reward
			avgGameReward = ((avgGameReward * evalGameNumber) + currentGameReward) / ++evalGameNumber;
			
			System.out.printf("Played evaluation game %d and %s (Cumulative reward: %.2f)\n", ((gameNumber - 1) % 15 - 9), result, currentGameReward);
			
			//one more eval game was played
			evalGameNumber++;
		} else {
			builder.append("Played game: " + ((gameNumber / 15) * 10 + (gameNumber % 15)) + " and " + result + "\n");
		}
		
		//update epsilon value and print the test reward data
		if (gameNumber % 15 == 0) {
			currEpsilon = currEpsilon < 0 ? 0 : currEpsilon - 0.002f;
			
			//print the test reward data
			rewards.add(avgGameReward);
			printTestData(rewards);
			
			builder.append("Games trained on: " + ((gameNumber / 15) * 10) + ", Average Reward: " + avgGameReward + "\n");
			String out = "Games trained on: " + ((gameNumber / 15) * 10) + ", Average Reward: " + avgGameReward + "\n";
			finalOutput += out;
		}
		
		//the game is completed, print episode tally
		if (((gameNumber / 15) * 10) >= episodes) {
			System.out.println();
			System.out.println(finalOutput);
			System.exit(0);
		}
		
		System.out.print(builder.toString());
		
		//one more game was played
		gameNumber++;
	}
	
	/**
	 * Determines the Q weight value by using the given feature vector.
	 * 
	 * @param featureVector - the vectore of feature values to use in calculation
	 * @return the Q weight value of the given feature vector
	 */
	public double calcQValue(double[] featureVector) {
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
	public static double[] calculateFeatureVector(State state, Integer footman, Integer enemy, AttackAction action) {
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
		if (State.isClosest(footman, enemy, enemyFootmen, unitLocations)){
			featureVector[6] += .3;
		} else {
			featureVector[6] -= .4;
		}
		
		//determine if the enemy can be attacked based on range from current footman
		if(State.isAdjacent(unitLocations.get(footman), unitLocations.get(enemy))){
			featureVector[7] += .03;
		}
		int adjEnemyCount = State.getAdjacentEnemyCount(footman, enemyFootmen, unitLocations);
		//determine how many enemies can currently attack the given footman
		if (adjEnemyCount <= 2){
			featureVector[8] += ((0.02 * adjEnemyCount) / random.nextDouble());
		} else {
			featureVector[8] -= ((0.1 * adjEnemyCount) / random.nextDouble());
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
			featureWeights[i] += (alpha * calcLoss);// * f[i]);
		}
	}
	
	/**
	 * Determines if a significant event has happened, which is someone being attacked or someone dying.
	 * 
	 * @param curState
	 * @param prevState
	 * @return True, if an event has happened
	 */
	private boolean eventHasHappened(State curState, State prevState) {
		if (curState.footmenDeadCount > prevState.footmenDeadCount
				|| curState.getEnemyFootmen().size() < prevState.getEnemyFootmen().size()) {
			// uh oh, they dead
			return true;
		}
		
		boolean someoneInjured = false;
		
		List<Integer> footmen = new ArrayList<Integer>();
		footmen.addAll(curState.getFootmen());
		footmen.addAll(curState.getEnemyFootmen());
		
		for (Integer footman : footmen) {
			someoneInjured |= (curState.getUnitHealth().get(footman) < prevState.getUnitHealth().get(footman));
		}
		
		return someoneInjured;
	}
	
	/**
	 * Uses the Q Function to determine which actions (Attack(Footman, Enemy)) maximize Q.
	 * 
	 * @param state
	 * @param prevAction
	 * @return A attack plan which assigns targets to each footman
	 */
	private AttackAction selectAction(State state, AttackAction prevAction) {
		Map<Integer, Integer> attack = new HashMap<Integer, Integer>();
		
		int i = 0;
		for (Integer footman : state.getFootmen()) {
			//epsilon-greedy strategy
			if (!evaluationMode && (1.0 - currEpsilon <= random.nextDouble())) {
				int randEnemy = randInt(0, state.getEnemyFootmen().size() - 1);
				attack.put(footman, state.getEnemyFootmen().get(randEnemy));
//				System.out.println("Hit random assignment");
				i++;
			} else {
				double maxQ = Double.NEGATIVE_INFINITY;
				int currentTarget = state.getEnemyFootmen().get(0);
				
				// Find the enemy that gives the highest Q function
				for (Integer enemy : state.getEnemyFootmen()) {
					double[] f = calculateFeatureVector(state, footman, enemy, prevAction);
					double curQ = calcQValue(f);
					if (curQ > maxQ) {
						maxQ = curQ;
						currentTarget = enemy;
					}
				}
				attack.put(footman, currentTarget);
			}
		}
		
		return new AttackAction(attack);
	}
	
	/**
	 * Returns a pseudo-random number between min and max, inclusive.
	 * The difference between min and max can be at most
	 * Integer.MAX_VALUE - 1.
	 *
	 * @param min - the minimum value
	 * @param max - the maximum value, which MUST be greater than min.
	 * @return an integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static int randInt(int min, int max) {

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = random.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
	/**
	 * Updates the weights associated with the Q function features based on the previous and current states
	 * @param reward
	 * @param curState
	 * @param prevState
	 * @param prevAction
	 * @param footman
	 */
	private void updateWeights(double reward, State curState, State prevState, AttackAction prevAction, Integer footman) {
		State currentState = new State(curState);
		double[] previousFeatures = calculateFeatureVector(prevState, footman, prevAction.getAttack().get(footman), prevAction);
		double previousQValue = calcQValue(previousFeatures);
		
		// footman can be dead at this point and therefore not in curState, so add him with health of 0
		if (!currentState.getFootmen().contains(footman)) {
			currentState.footmenDeadCount++;
			currentState.getFootmen().add(footman);
			currentState.getUnitHealth().put(footman, 0);
			currentState.getUnitLocations().put(footman, prevState.getUnitLocations().get(footman));
		}
		
		// Find the max Q function w.r.t. the possible actions
		AttackAction curAction = selectAction(currentState, prevAction);
		
		// Find Q(s',a')
		double[] currFeatureVector = calculateFeatureVector(currentState, footman, curAction.getAttack().get(footman), curAction);
		double maxCurrQ = calcQValue(currFeatureVector);
		
		double lossCalculated = (reward + (GAMMA * maxCurrQ - previousQValue));
		
		// updates the weight vector
		updateWeights(previousFeatures, lossCalculated, ALPHA);
	}
	
	/**
	 * Finds the reward for a state and action
	 * @param curState
	 * @param prevState
	 * @param prevAction
	 * @param footman
	 * @return The reward
	 */
	private double calculateReward(State curState, State prevState, AttackAction prevAction, Integer footman) {
		// Reward = -0.1 - FHP - FKILLED + EHP + EKILLED
		double reward = -0.1;
		
		// Update the attacking footman's location.
		curState.getUnitLocations().put(prevAction.getAttack().get(footman), prevState.getUnitLocations().get(prevAction.getAttack().get(footman)));
		
		// (FHP/FKILLED) Update reward based on footman's health.
		if (!curState.getFootmen().contains(footman)) {
			// Ally killed
			reward -= 100.0;
		} else {
			// Ally injured
			int healthLost = prevState.getUnitHealth().get(footman) - curState.getUnitHealth().get(footman);
			reward -= healthLost;
		}
		
		Integer target = prevAction.getAttack().get(footman);
		
		if (!curState.getFootmen().contains(footman)) {
			curState.getUnitLocations().put(footman, prevState.getUnitLocations().get(footman));
		}
		if (!curState.getEnemyFootmen().contains(target)) {
			curState.getUnitLocations().put(target, prevState.getUnitLocations().get(target));
		}

		// (EHP/EKILLED) Update reward based on enemy's health.
		if (areAdjacent(curState.getUnitLocations().get(footman), curState.getUnitLocations().get(target))) {
			if (!curState.getEnemyFootmen().contains(target)) {
				// Enemy killed
				reward += 100;
			} else {
				// Enemy injured
				int healthLost = prevState.getUnitHealth().get(target) - curState.getUnitHealth().get(target);
				reward += healthLost;
			}
		}
		
		return reward;
	}
	
	/**
	 * 
	 * @param p
	 * @param q
	 * @return True, if p and q are adjacent
	 */
	private boolean areAdjacent(Pair<Integer, Integer> p, Pair<Integer, Integer> q) {
		for (int i = p.getX() - 1; i <= p.getX() + 1; i++) {
			for (int j = p.getY() - 1; j <= p.getY() + 1; j++) {
				if (q.getX() == i && q.getY() == j) {
					return true;
				}
			}
		}
		
		return false;
	}

	public static String getUsage() {
		return "Uses Q learning to defeat enemies.";
	}

    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
//    public double calculateReward(StateView stateView, History.HistoryView historyView, int footmanId) {
//        return 0;
//    }

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
//    public double calcQValue(StateView stateView,
//                             History.HistoryView historyView,
//                             int attackerId,
//                             int defenderId) {
//        return 0;
//    }

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
//    public double[] calculateFeatureVector(StateView stateView,
//                                           History.HistoryView historyView,
//                                           int attackerId,
//                                           int defenderId) {
//        return null;
//    }

    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include th output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(Double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public Double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            List<Double> weights = new LinkedList<>();
            while((line = reader.readLine()) != null) {
                weights.add(Double.parseDouble(line));
            }
            reader.close();

            return weights.toArray(new Double[weights.size()]);
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        return null;
    }
	
	@Override
	public void savePlayerData(OutputStream os) {
		//this agent lacks learning and so has nothing to persist.
		
	}
	@Override
	public void loadPlayerData(InputStream is) {
		//this agent lacks learning and so has nothing to persist.
	}
}
