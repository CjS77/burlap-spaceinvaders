# Reinforcement Learning code for a Space Invaders bot
 
## Abstract

 This project contains code for building an AI bot for controlling a Space Invaders ship.
 
  * It uses a fork of the [BURLAP](http://burlap.cs.brown.edu/) machine learning library
  * The rules for the Space Invaders game are described [here](http://challenge.entelect.co.za/rules).
  * The current implementation does not work very well, but there is a decent foundation. You are encouraged to fork this
    project and play around with it. Let me know if you come up with something awesome.
    
## Quick start

  * Clone this repository
  * run the `build` gradle task
  
### Interactive version

There is an implementation of the Burlap Visualiser in this project. 
  
  * run the gradle task `runVisualise` to run an interactive visualisation of the game, where you control your ship.
    * The enemy strategy is specified on the command line, or you can edit the `build.gradle` file.
    * The key controls are 
          * A - Move Left
          * S - Nothing
          * D - Move Right
          * W - Fire
          * C - Build shields
          * X - Build Missile Controller
          * Z - Build Alien Factory

### Bot training

Run the gradle task `trainAgent` to carry out training using SARSA-λ. The adjustable parameters from the commandline are

* eps - the number of training episodes
* opp - the opponent strategy (see below)
* pNum - the player perspective (0 or 1)
* epsilon - the ε-value for the ε-greedy policy
* lrate - the learning rate for SARSA-λ

## Description

### Training methodology

 The current implementation uses SARSA-λ learning to fit parameters from a FeaturesDatabase to maximise the reward 
 against a specified opponent. The idea is then to use a Sparse Sampling strategy against a "live" opponent using the
  fitted parameters in a Value Function Approximation for the leaf nodes in the SS algorithm.

 The current implementation is only moderately successful. It does learn somewhat, but (after ~10,000 games) occasionally 
 does silly things, like moving into a missile. Improving the bot is a matter of
 
 * Additional learning time
 * Tweaking the learning parameters
 * Improving the feature database
 * Using different learning / planning algorithms
 * A combination of all of the above
 
 *Note that in the original competition, the bot had a maximum of 3 seconds to make a decision.*
 
### Opponent strategies 

Opponent strategies are implemented via the `OpponentStrategy` interface and added to the `OpponentStrategyFactory` class
 if they should be discoverable by the training routine. The current list of opponent strategies include:
 
 * SittingDuck - Does nothing every move
 * RunAndHide - moves under the shields, then sits there waiting.
 * Foxhole - moves under the shields, build a missile controller and then tries to fire every turn.
 * Feynman - uses parameters learnt from SARSA-λ for th `Heuristics3` Feature Database
 * Marx - Uses a set of heuristics to play reasonably (very) well. This is the best opponent of the current set. The 
   full strategy for Marx is documented in the `SpaceInvaderHeuristicsBasic` class.

## Package overview

### za.co.nimbus.game.constants

Common static constants used throughout the entire codebase

### za.co.nimbus.game.helpers

The `Location` class is a useful utility class for converting X and Y attributes to a single object.

`StateFlipper` makes player 2's state look like it would if he were player 1. Allows a single opponent class play both
sides of the screen

### za.co.nimbus.game.heuristics

Some burlap extension classes, specifically, the heuristics classes used as a basis for a Features Database

### za.co.nimbus.game.rules

Defines the mechanics of the Space Invader game as set out in the official rules.
 
The important classes here are

  * `SpaceInvaderMechanics` - implements all the rules for Space Invaders for the /single agent/ domain. Methods are
   all static so that they can be used from anywhere in the codebase without nasty side effects or state maintenance.
  * `Collision` - Propositional function for detecting collisions between entities
  * `GameOver` - the terminal function

  There are some other propositional functions in here as well that should be self-explanatory
  
### za.co.nimbus.game.saDomain

The single agent domain implementation. Contains a single-agent version of the reward function, the SA version of the 
`Action` subclass, and a DomainFactory. The opponent strategies are also implemented here.

### za.co.nimbus.game.visualiser

Classes used to create a visual playback and interactive version of the game

### za.co.nimbus.stochasticgame
Contains classes for implementing a stochastic game version of Space Invaders. These classes are included for
completeness, but were abandoned during development and are not guaranteed to work. 

# BURLAP

This code uses a fork of the Burlap library (git@github.com:CjS77/burlap.git), but the changes are minor and could 
easily be merged into the main repo, at which point you may want to switch to using the main repo instead.

