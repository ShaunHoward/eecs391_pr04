# eecs391_pr04
Reinforcement learning agent to move multipler archers in SEPIA game engine.

Q-Learning Agent with linear approximation for the final project of EECS391 at Case Western Reserve University, Spring 2015.

Team: Shaun Howard (smh150) and Matt Swartwout (mws85)

The agent controls multiple footmen in the SEPIA game engine in order to attack and defeat the enemy footmen who have a
very intelligent attack system. The agent must be very smart to out-wit its enemies and defeat them. When it harms or defeats them successfully, it will get a good reward. When it loses allies or any of them are harmed, it will receive negative reward.

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

After testing on thousands of episodes with both 5v5 and 10v10 (6500 initially, then loading weights to do another 6500
-> 13000), we found that the agent plays more consistenly over time. The agent won the most in the first 6500 iterations, but it was only marginal from the number of wins attained in the second iteration of 6500 based on the good weights. These weights are included in the given zip file. They are labeled according to number of agents and number of iterations.

Overall we do believe our agent to be learning, even if at a slow rate. 
The number of wins attained on average per 6500 games was ~2500 on 10v10 and ~2000 on 5v5, sometimes higher and sometimes lower. This means that the agent is winning from 30 to 40 percent of the time, which is acceptable for 
a naive intelligent agent using Q-learning and linear approximation. 
