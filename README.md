# eecs391_pr04
Reinforcement learning agent to move multipler archers in SEPIA game engine.

Q-Learning Agent for the Fourth and Final project of EECS391 at Case Western Reserve University, Spring 2015.

Team: Shaun Howard (smh150) and Matt Swartwout (mws85)

The agent controls multiple footmen in the SEPIA game engine in order to attack and defeat the enemy footmen who have a
very intelligent attack system. The agent must be very smart to out-wit its enemies and defeat them. When it defeats them
successfully, it will get a good reward. When it loses allies or any of them are harmed, it will receive negative reward.

The agent has a Q-function that is based on the Russell and Norvig AI book.

It calculates the next Q value like so:

Q(t+1) = Q(t) + alpha * (reward + (discount * optimalNextQ) - currQ)

The feature vector for this learning agent is the same for each footman that is controlled.

It consists of the following features and their numerical values:

	 *first feature value is always 1 to remain non-zero
	 *second feature value is the health of the given footman
	 *third feature value is the health of the given footman's enemy target, but negative
	 *fourth feature is valued from this footman attacking the closest enemy footman
	 *fifth feature is a multiple of how many enemies are attacking the given footman
	 *sixth feature values determining the ratio of hit-points of footman to target enemy
	 *seventh feature values footmen staying alive
	 *eighth feature is based on whether the target is adjacent for attacking
	 *ninth feature values how many enemies can currently attack the given footman
	 
We figured valuing life and hating death would be a good idea for features. Basically, the footmen will try to attack
the closest enemies and remain alive, whilst dealing damage to those close enemies. If any die, they are punished, but
if enemies die they are rewarded.

