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
 * A Q-learning reinforcement learning agent designed to learn how to use the good footmen
 * to defeat the enemy footmen in the SEPIA game engine.
 * 
 * @author Shaun Howard (smh150) and Matt Swartwout (mws85)
 */
public class RLAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	
	/**
	 * Convenience variable specifying enemy agent number. Use this whenever
	 * referring to the enemy agent. We will make sure it is set to the proper
	 * number when testing your code.
	 */
	public static final int ENEMY_PLAYERNUM = 1;

	// feature vector size of this agent
	public static final int NUM_FEATURES = 9;

	// the discount factor, a constant gamma
	private static final double GAMMA = 0.9;

	// the learning rate of the agent
	private static final double ALPHA = 0.001;

	// the GLIE exploration value
	private static final double EPSILON = 0.02;

	// state before the current state
	private GameState priorState = null;

	// the random instance for use of generating random events
	private static Random random;

	// track the rewards received
	private List<Double> rewards;

	// actions before the current actions
	private AttackAction priorAction = new AttackAction(
			new HashMap<Integer, Integer>());

	// values used when in evaluation mode,
	// this will make the agent freeze its Q function and play 5 evaluation
	// games
	private boolean evaluationMode = false;
	private int gameNumber = 1;
	private int evalGameNumber = 1;
	private int gamesWon = 0;

	// limit the number of episodes to 100 if not specified in constructor args
	private int episodes = 100;

	// Q-learning weights
	public Double[] featureWeights;

	// Boolean to record whether or not the agent is in its first round
	private boolean firstRound = true;

	//Values useful for the Q-function and feature vectors
	private double currentEpsilon;
	private double currentGameReward = 0.0;
	private double avgGameReward = 0.0;
	public int footmenDeadCount = 0;

	//The final string to output
	private StringBuilder finalOutput;

	//The current game state
	private StateView currentState;

	public RLAgent(int playernum, String[] args) {
		super(playernum);
		
		//assign something to the weights vector
		featureWeights = new Double[10];
		
		finalOutput = new StringBuilder();
		
		//Initializes the current epsilon
		currentEpsilon = EPSILON;

		//make a random with PRNG seed of 12345
		random = new Random(12345);

		//read in number of episodes or default to 100
		if (args.length > 0) {
			episodes = Integer.parseInt(args[0]);
		} else {
			System.out.println("The number of episodes was not specified. The game will default to 100 episodes.");
		}

		//determine whether to load weights from a file
		boolean loadWeights = false;
		if (args.length > 1) {
			loadWeights = Boolean.parseBoolean(args[1]);
		} else {
			System.out.println("Loading weights was not specified. The game will default to not loading them.");
		}

		//loads the weights from a file or makes new random ones
		if (loadWeights) {
			featureWeights = loadWeights();
		} 
		
		if(featureWeights == null || !loadWeights) {
			// initialize weights to random values between -1 and 1
			featureWeights = new Double[NUM_FEATURES];
			for (int i = 0; i < featureWeights.length; i++) {
				featureWeights[i] = random.nextDouble() * 2 - 1;
			}
		}

		//accumulate rewards from the start
		rewards = new ArrayList<>();
		rewards.add(avgGameReward);
	}

	/**
	 * Initializes the current state, the current game reward,
	 * determines whether the game is to be played in evaluation mode, and
	 * deals with setting up any other variables necessary for playing the game.
	 * 
	 * @param stateView - the current state
	 * @param historyView - the game's history
	 * @return a map of unit-action pairs
	 */
	@Override
	public Map<Integer, Action> initialStep(StateView stateView,
			History.HistoryView historyView) {

		// initialize first values for the game state, game reward, and for the
		// mode to operate in
		currentState = stateView;

		// initializes the current game reward
		currentGameReward = 0.0;

		// checks if agent needs to enter evaluation mode
		evaluationMode = (gameNumber - 1) % 15 > 9;

		// code for testing mode
		if (!evaluationMode) {
			avgGameReward = 0.0;
			evalGameNumber = 1;
		}

		//must be the first round if initial step is called
		firstRound = true;
		
		//there is no previous state to this state
		priorState = null;
		
		//make a new attack action map 
		priorAction = new AttackAction(new HashMap<Integer, Integer>());

		return middleStep(stateView, historyView);
	}

	/**
	 * Determines the current state of the game.
	 * 
	 * Tracks the health and locations of all units.
     *
	 * Determines if a significant event has occurred such as unit death.
	 * 
	 * When such an event has occurred,	the reward will be calculated for all of our footmen
	 * and the totals will be updated. Then the weights for the Q-function will be updated if 
	 * the game is not in evaluation mode. Subsequently, a new action is selected for our footman team.
	 * 
	 * @param stateView - the current state
	 * @param historyView - the history of the game
	 * @return New actions to execute or nothing if an event has not occurred.
	 */
	@Override
	public Map<Integer, Action> middleStep(StateView stateView, History.HistoryView historyView) {
		Map<Integer, Action> builder = new HashMap<Integer, Action>();
		currentState = stateView;

		//Lists for current state's own and enemy footmen, health, and locations
		List<Integer> curFootmen = new ArrayList<Integer>();
		List<Integer> curEnemyFootmen = new ArrayList<Integer>();
		Map<Integer, Integer> curUnitHealth = new HashMap<Integer, Integer>();
		Map<Integer, CoordPair<Integer, Integer>> curUnitLocations = new HashMap<Integer, CoordPair<Integer, Integer>>();
        
		//fetch all the footman and track their locations and health
		for (UnitView unit : currentState.getAllUnits()) {
			String unitTypeName = unit.getTemplateView().getName();
			
			//we only want to select footmen
			if (unitTypeName.equals("Footman")) {
				
				//Stores location of each footman as a coordinate pair in the locations list
				curUnitLocations.put(unit.getID(), new CoordPair<Integer, Integer>(
						unit.getXPosition(), unit.getYPosition()));
				
				//Stores until health in the health list
				curUnitHealth.put(unit.getID(), unit.getHP());
				
				//Adds footman to the own or enemy list
				if (currentState.getUnits(playernum).contains(unit)) {
					curFootmen.add(unit.getID());
				} else {
					curEnemyFootmen.add(unit.getID());
				}
			}
		}

		//create the new state from all units, health, and locations
		GameState currentState = new GameState(curFootmen, curEnemyFootmen,
				curUnitHealth, curUnitLocations);
		
		//Calculate the overall reward from all footmen on our team if not the first round
		if (!firstRound) {
			
			//check if any units have died, if not, keep executing the same actions 
			if (!eventHasHappened(currentState, priorState)) {
				return builder;
			}
			
			//calculate the reward for each footman and add it to the current game reward
			for (Integer footman : priorState.getFootmen()) {

				//the reward of executing the previous action for the given footman
				double reward = calculateReward(currentState, priorState,
						priorAction, footman);
				currentGameReward += reward;

				//only update weights if not in evaluation mode
				if (!evaluationMode) {
					updateWeights(reward, currentState, priorState, priorAction, footman);
				}
			}
		} else {
			//no reward obtained yet if on the first round
			firstRound = false;
		}

		//Recognize that the current state will now be the previous state
		priorState = currentState;

		//Select a new action to execute based on the current state and the previous action
		priorAction = selectAction(priorState, priorAction);

		//Create compound attacks between each footman and an enemy
		for (Integer footmanID : priorAction.getAttack().keySet()) {
			Action b = TargetedAction.createCompoundAttack(footmanID,
					priorAction.getAttack().get(footmanID));
			builder.put(footmanID, b);
		}

		return builder;
	}

	/**
	 * Calculates the cumulative average rewards for testing episodes and prints
	 * them.
	 * 
	 * Increments the game number count.
	 * 
	 * Determines the number of games won, and prints value at the end of all episodes.
	 *
	 * Weights are saved with the saveWeights function.
	 * 
	 * Handles exiting the program when finished.
	 * 
	 * @param stateView - the current state
	 * @param historyView - the history of states
	 */
	@Override
	public void terminalStep(StateView stateView,
			History.HistoryView historyView) {

		//the builder to output any strings necessary
		StringBuilder builder = new StringBuilder();

		// Save feature weights to file for future reference
		saveWeights(featureWeights);

		boolean won = false;
		
		//Checks if any of our footmen are still alive at the end
		//if they are, that means we won!
		for (UnitView unit : currentState.getUnits(playernum)) {
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Footman")) {
				won = true;
			}
		}
		
		//increment the number of games won not in eval mode
		if (won == true && !evaluationMode)
			gamesWon += 1;

		// Calculate the average cumulative reward during tests
		if (evaluationMode) {

			// find the current average game reward averaged over the current eval game number
			avgGameReward = ((avgGameReward * evalGameNumber) + currentGameReward)
					/ ++evalGameNumber;

			// one more eval game was played
			evalGameNumber++;
		}

		// update epsilon value and print the test reward data when complete with eval mode
		if (gameNumber % 15 == 0) {
			
			//update epsilon by -0.002f to explore less as we play
			currentEpsilon = currentEpsilon < 0 ? 0 : currentEpsilon - 0.002f;

			// print the test reward data
			rewards.add(avgGameReward);
			printTestData(rewards);
		}

		// the game is now complete, must print all relevant episode data from entire game
		if (((gameNumber / 15) * 10) >= episodes) {
			System.out.println(finalOutput.toString());
			System.out.print("Games won: " + gamesWon);
			System.exit(0);
		}

		//print any useful info to determine learning updates
		System.out.print(builder.toString());

		// one more game was played
		gameNumber++;
	}

	/**
	 * Determines the Q-function value by using the given feature vector.
	 * 
	 * @param featureVector - the vector of feature values to use for Q-function
	 * calculation
	 * @return the Q-function value of the given feature vector
	 */
	public double calculateQValue(double[] featureVector) {
		double qWeight = 0;

		//take dot product of feature vector and feature weights
		for (int i = 0; i < featureWeights.length; i++) {
			qWeight += featureWeights[i] * featureVector[i];
		}

		return qWeight;
	}

	/**
	 * Gets the feature vector of a given state using the footman id, the enemy
	 * id, and an attack action map.
	 * 
	 * @param state - the game state to get the features of
	 * @param footman - the footman id to get the feature vector in reference to
	 * @param enemy - the enemy id targeted by the given footman
	 * @param action - an attack action map to keep track of the current attack
	 *            actions
	 * @return the feature vector in reference to the given values
	 */
	public static double[] calculateFeatureVector(GameState state, Integer footman,
			Integer enemy, AttackAction action) {
		return getFeatureVector(footman, enemy, state.getFootmen(),
				state.getEnemyFootmen(), state.getUnitHealth(),
				state.getUnitLocations(), action.getAttack());
	}

	/**
	 * Gets the feature vector of a given state using the footman id, the enemy
	 * id, a list of footmen ids, a list of enemy ids, the health of all units,
	 * a map of the unit locations, and a map of attack actions in reference to
	 * unit ids. The features are the following:
	 * 
	 *first feature value is always 1 to remain non-zero
	 *second feature value is the health of the given footman, but negative
	 *third feature value is the health of the given footman's enemy target
	 *fourth feature is valued from this footman attacking the closest enemy footman
	 *fifth feature is a multiple of how many enemies are attacking the given footman
	 *sixth feature values determining the ratio of hit-points of footman to target enemy
	 *seventh feature values footmen staying alive
	 *eighth feature is based on whether the target is adjacent for attacking
	 *ninth feature values how many enemies can currently attack the given footman
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
	public static double[] getFeatureVector(Integer footman, Integer enemy,
			List<Integer> footmen, List<Integer> enemyFootmen,
			Map<Integer, Integer> unitHealth,
			Map<Integer, CoordPair<Integer, Integer>> unitLocations,
			Map<Integer, Integer> attack) {

		double[] featureVector = new double[NUM_FEATURES];

		// first feature value is always 1 to remain non-zero
		featureVector[0] = 1;

		// second feature value is the health of the given footman
		featureVector[1] = unitHealth.get(footman);

		// third feature value is the health of the given footman's enemy target, but negative
		featureVector[2] = -unitHealth.get(enemy);

		// fourth feature is valued from this footman attacking the closest enemy footman
		if (GameState.isClosest(footman, enemy, enemyFootmen, unitLocations)){//attack.get(footman) == enemy) {
			//positively weigh attacking the closest footman
			featureVector[3] += 100;
			
		} else if (attack.get(footman) == null) {
			featureVector[3] -= 100;
		} else {
			featureVector[3] += 50;
		}

		
		// fifth feature is a multiple of how many enemies are attacking the given footman
		featureVector[4] = 0;
		for (Integer attacker : attack.keySet()) {
			//we cannot attack ourselves
			if (attacker == footman) {
				continue;
			}
			
			//greatly weigh attacking the enemy over not
			featureVector[4] += attack.get(attacker) == enemy ? 10 : 0.1;
		}


		// sixth feature values determining the ratio of hit-points of footman to target enemy
		featureVector[5] = unitHealth.get(footman) / Math.max(unitHealth.get(enemy), 1);
		
		// seventh feature values footmen staying alive
        for (Integer man : footmen){
        	//greatly weight having hp
        	if (unitHealth.get(man) > 0){
        		featureVector[6] += 10;
        	} else {
        		//otherwise, add values for still having any footmen alive
        		featureVector[6] += 0.1;
        	}
        }
        
        // eighth feature is based on whether the target is adjacent for attacking
		if (GameState.isAdjacent(unitLocations.get(footman),
				unitLocations.get(enemy))) {
			featureVector[7] += 10;
		} else {
			featureVector[7] -= 10;
		}
		
		int adjEnemyCount = GameState.getAdjacentEnemyCount(footman, enemyFootmen,
				unitLocations);
		
		// ninth feature values how many enemies can currently attack the given footman
		if (adjEnemyCount <= 2) {
			featureVector[8] += adjEnemyCount * 10;
		} else {
			featureVector[8] -= adjEnemyCount * 10;
		}

		return featureVector;
	}

	/**
	 * Updates the feature weights vector given the feature vector, a calculated
	 * loss function, and the learning rate of the agent.
	 * 
	 * @param featureVector - the feature vector to update the feature weights set with
	 * @param calcLoss - the calculated loss function
	 * @param alpha - the agent learning rate
	 */
	public void updateWeights(double[] featureVector, double calcLoss, double alpha) {
		for (int i = 0; i < featureWeights.length; i++) {
			featureWeights[i] += (alpha * calcLoss);
		}
	}

	/**
	 * Determines if a significant event has happened.
	 * Such an event is whether a unit, good or bad, has been harmed or has died.
	 * 
	 * @param currentState - the current state of the game
	 * @param priorState - the prior state of the game
	 * @return True, if such a described event has happened
	 */
	private boolean eventHasHappened(GameState currentState, GameState priorState) {
		//determine if a unit has died since the prior state
		if (currentState.getEnemyFootmen().size() < priorState.getEnemyFootmen().size() 
				|| currentState.footmenDeadCount > priorState.footmenDeadCount) {
			return true;
		}

		//was a unit hurt since the prior state?
		boolean unitHurt = false;

		//gather all footmen from the current state
		List<Integer> footmen = new ArrayList<Integer>();
		footmen.addAll(currentState.getFootmen());
		footmen.addAll(currentState.getEnemyFootmen());

		//Determine if any footmen were injured in the event
		for (Integer footman : footmen) {
			unitHurt |= (currentState.getUnitHealth().get(footman) < priorState
					.getUnitHealth().get(footman));
		}

		return unitHurt;
	}

	/**
	 * Uses the Q Function to determine which actions maximize Q.
	 * The only real actions are to attack the enemy team with the footman team.
	 * This method selects who attacks who.
	 * 
	 * @param state - the current state of the game
	 * @param priorAction - the prior action in prior state of the game
	 * @return An attack plan which assigns a target to each footman
	 */
	private AttackAction selectAction(GameState state, AttackAction priorAction) {
		Map<Integer, Integer> attack = new HashMap<Integer, Integer>();
		for (Integer footman : state.getFootmen()) {
			
			/*
			 *Implementation of the epsilon-greedy strategy
			 *Selects a random action when a random value is less than epsilon
			 *This causes less random events to occur over time when epsilon is decreased
			 */
			if (!evaluationMode && (currentEpsilon > random.nextDouble())) {
				//choose random enemy
				int randEnemy = randInt(0, state.getEnemyFootmen().size() - 1);
				
				//plan to attack them
				attack.put(footman, state.getEnemyFootmen().get(randEnemy));
			} else {
				double maxQ = Double.NEGATIVE_INFINITY;
				int currentTarget = state.getEnemyFootmen().get(0);

				// Find the enemy that gives the maximum Q function
				for (Integer enemy : state.getEnemyFootmen()) {
					double[] f = calculateFeatureVector(state, footman, enemy,
							priorAction);
					double curQ = calculateQValue(f);
					if (curQ > maxQ) {
						maxQ = curQ;
						currentTarget = enemy;
					}
				}
				
				//choose to attack that enemy
				attack.put(footman, currentTarget);
			}
		}

		return new AttackAction(attack);
	}

	/**
	 * Updates the weights associated with the Q function features based on the
	 * previous and current states.
	 * 
	 * @param reward - the current reward
	 * @param currState - the current state
	 * @param priorState - the prior state
	 * @param priorAction - the action taken in the prior state
	 * @param footman - the footman who created the given values
	 */
	private void updateWeights(double reward, GameState currState, GameState priorState,
			AttackAction priorAction, Integer footman) {
		//Determine prior features and Q value
		double[] priorFeatures = calculateFeatureVector(priorState, footman,
				priorAction.getAttack().get(footman), priorAction);
		double priorQValue = calculateQValue(priorFeatures);
		
		GameState currentState = new GameState(currState);

		// Any footman can be dead now, absent from currState
		// We track that he has existed by adding him with a health of 0
		if (!currentState.getFootmen().contains(footman)) {
			currentState.footmenDeadCount++;
			currentState.getFootmen().add(footman);
			currentState.getUnitHealth().put(footman, 0);
			currentState.getUnitLocations().put(footman,
					priorState.getUnitLocations().get(footman));
		}

		//Determine an action that maximizes the Q value at the state of the game
		AttackAction curAction = selectAction(currentState, priorAction);

		// Find Q(s',a') by getting the feature vector for the current state and the max Q value
		double[] currFeatureVector = calculateFeatureVector(currentState,
				footman, curAction.getAttack().get(footman), curAction);
		double maxCurrQ = calculateQValue(currFeatureVector);

		//Get the loss value from the loss function of the Q-function
		double lossCalculated = (reward + (GAMMA * maxCurrQ) - priorQValue);

		// update the weight vector with the loss value and prior features at the given learning rate
		updateWeights(priorFeatures, lossCalculated, ALPHA);
	}

	/**
	 * Calculates the reward for the given state and action.
	 * The reward is based on the following values:
	 * 
	 * Ally getting killed = -100
	 * 
	 * Any footman to be injured = -healthLost 
	 * 
	 * Enemy getting killed = +100
	 * 
	 * Any enemy to be injured = +healthLost
	 * 
	 * **Always lose 0.1 for time step
	 * 
	 * @param currState - the current state of the game
	 * @param priorState - the prior state of the game
	 * @param priorAction - the prior action taken in the prior state
	 * @param footman - the footman to get the reward of
	 * @return The reward of the given footman for the new state Q value
	 */
	private double calculateReward(GameState currState, GameState priorState,
			AttackAction priorAction, Integer footman) {
		//Reward = -0.1 - footmanHP - footmanKilled + enemyHP + enemyKilled
		double reward = -0.1;

		//Update the attacking footman's location.
		currState.getUnitLocations().put(
				priorAction.getAttack().get(footman),
				priorState.getUnitLocations().get(
						priorAction.getAttack().get(footman)));

		//Update reward based on footman's health.
		if (!currState.getFootmen().contains(footman)) {
			//Ally killed
			reward -= 100.0;
		} else {
			//Ally injured
			int healthLost = priorState.getUnitHealth().get(footman)
					- currState.getUnitHealth().get(footman);
			reward -= healthLost;
		}

		//Gather the target of the given footman
		Integer target = priorAction.getAttack().get(footman);

		//Get the locations of the footman and their target
		if (!currState.getFootmen().contains(footman)) {
			currState.getUnitLocations().put(footman,
					priorState.getUnitLocations().get(footman));
		}
		if (!currState.getEnemyFootmen().contains(target)) {
			currState.getUnitLocations().put(target,
					priorState.getUnitLocations().get(target));
		}

		//Update reward based on enemy's health.
		if (CoordPair.areAdjacent(currState.getUnitLocations().get(footman), currState
				.getUnitLocations().get(target))) {
			if (!currState.getEnemyFootmen().contains(target)) {
				//Enemy killed
				reward += 100;
			} else {
				//Enemy injured
				int healthLost = priorState.getUnitHealth().get(target) 
						- currState.getUnitHealth().get(target);
				reward += healthLost;
			}
		}

		return reward;
	}
	
	/**
	 * Gets a pseudo-random number inclusively between the given min and max integers.
	 * The maximum difference between min and max should be Integer.MAX_VALUE - 1.
	 *
	 * @param min - the minimum range value
	 * @param max - the maximum range value, which needs to be > min.
	 * @return an integer inclusively between min and max
	 * @see java.util.Random#nextInt(int)
	 */
	public static int randInt(int min, int max) {

		// add 1 to nextInt to make it inclusive because it's usually exclusive
		int randomNum = random.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	/**
	 * DO NOT CHANGE THIS!
	 *
	 * Prints the learning rate data described in the assignment. Do not modify
	 * this method.
	 *
	 * @param averageRewards
	 *            List of cumulative average rewards from test episodes.
	 */
	public void printTestData(List<Double> averageRewards) {
		System.out.println("");
		System.out.println("Games Played      Average Cumulative Reward");
		System.out.println("-------------     -------------------------");
		for (int i = 0; i < averageRewards.size(); i++) {
			String gamesPlayed = Integer.toString(10 * i);
			String averageReward = String.format("%.2f", averageRewards.get(i));

			int numSpaces = "-------------     ".length()
					- gamesPlayed.length();
			StringBuffer spaceBuffer = new StringBuffer(numSpaces);
			for (int j = 0; j < numSpaces; j++) {
				spaceBuffer.append(" ");
			}
			System.out.println(gamesPlayed + spaceBuffer.toString()
					+ averageReward);
		}
		System.out.println("");
	}

	/**
	 * DO NOT CHANGE THIS!
	 *
	 * This function will take your set of weights and save them to a file.
	 * Overwriting whatever file is currently there. You will use this when
	 * training your agents. You will include the output of this function from
	 * your trained agent with your submission.
	 *
	 * Look in the agent_weights folder for the output.
	 *
	 * @param weights
	 *            Array of weights
	 */
	public void saveWeights(Double[] weights) {
		File path = new File("agent_weights/weights.txt");
		// create the directories if they do not already exist
		path.getAbsoluteFile().getParentFile().mkdirs();

		try {
			// open a new file writer. Set append to false
			BufferedWriter writer = new BufferedWriter(new FileWriter(path,
					false));

			for (double weight : weights) {
				writer.write(String.format("%f\n", weight));
			}
			writer.flush();
			writer.close();
		} catch (IOException ex) {
			System.err.println("Failed to write weights to file. Reason: "
					+ ex.getMessage());
		}
	}

	/**
	 * DO NOT CHANGE THIS!
	 *
	 * This function will load the weights stored at agent_weights/weights.txt.
	 * The contents of this file can be created using the saveWeights function.
	 * You will use this function if the load weights argument of the agent is
	 * set to 1.
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
			while ((line = reader.readLine()) != null) {
				weights.add(Double.parseDouble(line));
			}
			reader.close();

			return weights.toArray(new Double[weights.size()]);
		} catch (IOException ex) {
			System.err.println("Failed to load weights from file. Reason: "
					+ ex.getMessage());
		}
		return null;
	}

	@Override
	public void savePlayerData(OutputStream os) {
		// nothing to persist.

	}

	@Override
	public void loadPlayerData(InputStream is) {
		// nothing to persist.
	}
}
