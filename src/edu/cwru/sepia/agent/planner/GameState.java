package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.ActionFactory;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
	
	public enum Resource { WOOD, GOLD, NONE }	// Types of resources the peasant can be holding
	private Map<String, Object[]> goldSources;	// A map of all gold resource positions, keyed by their coordinates
	private Map<String, Object[]> woodSources;	// A map of all wood resource positions, keyed by their coordinates
	private Resource holdType;					// The type of resource the peasant is currently holding

	private double cost;						// The cost of the plan thus far;
	private GameState parentState;				// The predecessor state before the last action
	private List<StripsAction> actionList;		// The collection of actions that make up the plan thus far
	private List<StripsAction> lastActions;		// The actions taken to result in this state
	
	private Position townHallPosition;			// The position of the town hall unit
	private Position peasantPosition;			// The position of the peasant unit
	private int peasantID;						// The id of the peasant unit
	
	private int xExtent;						// The horizontal extent of the map
	private int yExtent;						// The vertical extent of the map
	private int requiredGold;					// The required amount of gold
	private int requiredWood;					// The required amount of wood
	private int currentGold;					// The current total of gold
	private int currentWood;					// The current total of wood
	private int holdingCount;					// The amount the peasant is holding

	private static final int POS_INDEX = 1;
	private static final int AMT_INDEX = 2;
	
    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
    	
    	// Empty parent state
    	this.parentState = null;
    	this.cost = 0;
    	
    	// Get resource positions from state view
    	this.goldSources = new HashMap<String, Object[]>();
    	this.woodSources = new HashMap<String, Object[]>();
    	List<ResourceView> resourceViews = state.getAllResourceNodes();
    	for (int i = 0; i < resourceViews.size(); i++) {
    		ResourceView resource = resourceViews.get(i);
			Position position = new Position(resource.getXPosition(), resource.getYPosition());
    		if (resource.getType() == Type.GOLD_MINE) {
    			Object[] resourceInfo = {resource.getID(), position, resource.getAmountRemaining()};
    			goldSources.put(position.keyString(), resourceInfo);
    		}
    		else if (resource.getType() == Type.TREE) {
    			Object[] resourceInfo = {resource.getID(), position, resource.getAmountRemaining()};
    			woodSources.put(position.keyString(), resourceInfo);
    		}
    	}
    	
    	// The peasant initially is empty-handed
    	this.holdType = Resource.NONE;
    	this.holdingCount = 0;
    	
    	// The plan is initially empty
    	this.actionList = new ArrayList<StripsAction>();
    	this.lastActions = new ArrayList<StripsAction>();
    	
    	// Get the initial position of the town hall and the peasant
    	List<UnitView> units = state.getUnits(playernum);
    	for (UnitView unit : units) {
    		Position position = new Position(unit.getXPosition(), unit.getYPosition());
    		if (unit.getTemplateView().getName().equals("Peasant")) {
    			// Add an entry into the peasant info array about the initial peasant
    	    	this.peasantPosition = position;
    	    	this.peasantID = unit.getID();
    		}
    		else {
    			this.townHallPosition = position;
    		}
    	}
    	
    	// Get the extent of the map from the state view
    	this.xExtent = state.getXExtent();
    	this.yExtent = state.getYExtent();
    	
    	// Set the initial resource counts/goals
    	this.requiredGold = requiredGold;
    	this.requiredWood = requiredWood;
    	this.currentGold = 0;
    	this.currentWood = 0;
    }
    
    public GameState(GameState state, List<StripsAction> actions) {
    	// Set parent state
    	this.parentState = state;
    	
    	// Copy state resulting from action
    	this.holdType = parentState.holdType;
    	this.holdingCount = parentState.holdingCount;
    	this.goldSources = new HashMap<String, Object[]>(parentState.goldSources);
    	this.woodSources = new HashMap<String, Object[]>(parentState.woodSources);
    	this.xExtent = parentState.getXExtent();
    	this.yExtent = parentState.getYExtent();
    	this.requiredGold = parentState.requiredGold;
    	this.requiredWood = parentState.requiredWood;
    	this.currentGold = parentState.currentGold;
    	this.currentWood = parentState.currentWood;
    	this.peasantID = parentState.peasantID;
    	this.peasantPosition = new Position(parentState.peasantPosition.x, parentState.peasantPosition.y);
    	this.townHallPosition = new Position(parentState.townHallPosition.x, parentState.townHallPosition.y);
    	
    	// Plan/cost is previous state + last actions
    	this.lastActions = actions;
    	this.cost = parentState.cost;
    	this.actionList = new ArrayList<StripsAction>(parentState.getActionList());
    	for (int i = 0; i < lastActions.size(); i++) {
        	this.cost += lastActions.get(i).getCost(parentState);
        	this.actionList.add(lastActions.get(i));
        	this.actionList.get(i).apply(this);
    	}
    }
    
    public List<StripsAction> getActionList() {
    	return actionList;
    }
    
    public String getResourceLevels() {
    	return "Wood: " + (currentWood) + 
    			" Gold: " + (currentGold) + 
    			" Holding: " + holdingCount;
    }
    
    public Position getTownHallPosition() {
    	return townHallPosition;
    }
    
    public Position getPeasantPosition() {
    	return peasantPosition;
    }
    
    public void setPeasantPosition(Position newPosition) {
    	peasantPosition = newPosition;
    }
    
    public int getPeasantID() {
    	return peasantID;
    }
    
    public int getXExtent() {
    	return xExtent;
    }
    
    public int getYExtent() {
    	return yExtent;
    }
    
    public int getCurrentGold() {
    	return currentGold;
    }
    
    
    /**
     * 
     * @return The stack of StripsAction objects that make the plan to get to this state.
     */
    public Stack<StripsAction> getPlan() {
    	// Push them in reverse order, first action ends up on top
    	Stack<StripsAction> plan = new Stack<>();
    	for (int i = actionList.size() - 1; i >= 0; i--) {
    		plan.push(actionList.get(i));
    		
    	}
    	return plan;
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return currentGold >= requiredGold && currentWood >= requiredWood;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
    	List<GameState> children = new ArrayList<>();
		List<StripsAction> actions = new ArrayList<>();
		
    	// if (peasants.size == 1) 
    	if (currentGold < requiredGold) {
	    	// Add move actions to gold mines the peasant is not adjacent to already
	    	for (Object[] goldMine : goldSources.values()) {
    			Position minePosition = (Position)goldMine[POS_INDEX];
	    		if ((Integer)goldMine[AMT_INDEX] > 0) {
	    			StripsAction moveToMine = ActionFactory.makeMoveAction(minePosition, getPeasantID());
	    			if (moveToMine.preconditionsMet(this)) {
	    				actions.add(moveToMine);
	    				//GameState nearMine = new GameState(this, moveToMine);
	    				//children.add(nearMine);
	    			}
	    		}
	    	}

	    	// Add HarvestGold action if available
	    	StripsAction harvestGold = ActionFactory.makeHarvestAction(Resource.GOLD);
	    	if (harvestGold.preconditionsMet(this)) {
	    		GameState harvestGoldState = new GameState(this, harvestGold);
	    		children.add(harvestGoldState);
	    	}
    	}
		
    	if (currentWood < requiredWood) {
	    	// Add move actions to trees the peasant is not adjacent to already
	    	for (Object[] tree : woodSources.values()) {
	    		if ((Integer)tree[AMT_INDEX] > 0) {
		    		Position treePosition = (Position)tree[POS_INDEX];
		    		StripsAction moveToTree = ActionFactory.makeMoveAction(treePosition, getPeasantID());
		    		if (moveToTree.preconditionsMet(this)) {
		    			GameState nearTree = new GameState(this, moveToTree);
		    			children.add(nearTree);
		    		}
	    		}
	    	}
	    	
	    	// Add HarvestWood action if available
	    	StripsAction harvestWood = ActionFactory.makeHarvestAction(Resource.WOOD);
	    	if (harvestWood.preconditionsMet(this)) {
	    		GameState harvestWoodState = new GameState(this, harvestWood);
	    		children.add(harvestWoodState);
	    	}
    	}
		
    	// Add a move action to the town hall if available
    	StripsAction moveToTownHall = ActionFactory.makeMoveAction(townHallPosition, getPeasantID());
    	if (moveToTownHall.preconditionsMet(this)) {
    		GameState nearTownHall = new GameState(this, moveToTownHall);
    		children.add(nearTownHall);
    	}
    	
    	// Add Deposit action if available
    	StripsAction deposit = ActionFactory.makeDepositAction();
    	if (deposit.preconditionsMet(this)) {
    		GameState depositState = new GameState(this, deposit);
    		children.add(depositState);
    	}
    	
    	
    	for (int i = 0; i < actions.size(); i++) {
    		StripsAction build = ActionFactory.makeBuildAction();
    		GameState noBuild = new GameState(this, actions.get(0));
    		GameState withBuild = new GameState(this, actions.get(0));
    	}
  
        return children;
    }

    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {
    	// The priority 
    	double heuristic = 0.0;
    	
    	// If not holding
    	if (holdType == Resource.NONE) {
    		// - proximity to nearest and lowest resource is good
    		if (currentGold > currentWood) {
    			heuristic += nearestTreeToPeasant().euclideanDistance(getPeasantPosition());
    		}
    		else {
    			heuristic += nearestMineToPeasant().euclideanDistance(getPeasantPosition());
    		}

    	}
    	else { // If holding, minimize distance to town hall
    		heuristic += getPeasantPosition().euclideanDistance(townHallPosition);
    	}
    	
    	heuristic += requiredGold - currentGold;
    	heuristic += requiredWood - currentWood;
    	heuristic -= holdingCount/10;
    	
        return heuristic;
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        return cost;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
    	double g1 = heuristic() + getCost();
		double g2 = o.heuristic() + o.getCost();
		return (g1 > g2) ? 1 : (g1 < g2) ? -1 : 0;
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        // TODO: Implement me!
        return false;
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        // TODO: Implement me!
        return 0;
    }
    
    public boolean isResourceAtPosition(Position position) {
    	return goldSources.containsKey(position.keyString()) || 
    			woodSources.containsKey(position.keyString());
    }
    
    public Position goldMineNextToPosition(Position position) {
    	// Check every gold mine for adjacency
    	for (Object[] goldMine : goldSources.values()) {
    		Position minePosition = (Position)goldMine[POS_INDEX];
    		if (minePosition.isAdjacent(position)) {
    			return minePosition;
    		}
    	}
    	return null;
    }
    
    public Position treeNextToPosition(Position position) {
    	// Check every tree for adjacency
    	for (Object[] tree : woodSources.values()) {
    		Position treePosition = (Position)tree[POS_INDEX];
    		if (treePosition.isAdjacent(position)) {
    			return treePosition;
    		}
    	}
    	return null;
    }
    
    public Position nearestMineToPeasant() {
    	Position nearestMine = null;
    	double minDist = Double.MAX_VALUE;
    	for (Object[] goldMine : goldSources.values()) {
    		Position minePosition = (Position)goldMine[POS_INDEX];
    		double dist = minePosition.euclideanDistance(getPeasantPosition());
    		if (dist < minDist) {
    			minDist = dist;
    			nearestMine = minePosition;
    		}
    	}
    	return nearestMine;
    }
    
    public Position nearestTreeToPeasant() {
    	Position nearestTree = null;
    	double minDist = Double.MAX_VALUE;
    	for (Object[] tree : woodSources.values()) {
    		Position treePosition = (Position)tree[POS_INDEX];
    		double dist = treePosition.euclideanDistance(getPeasantPosition());
    		if (dist < minDist) {
    			minDist = dist;
    			nearestTree = treePosition;
    		}
    	}
    	return nearestTree;
    }
    
    public boolean isResourceEmpty(Resource type, String key) {
    	if (type == Resource.GOLD) {
    		Object[] goldMine = goldSources.get(key);
    		return (Integer)goldMine[AMT_INDEX] >= 0;
    	}
    	else if (type == Resource.WOOD) {
    		Object[] tree = woodSources.get(key);
    		return (Integer)tree[AMT_INDEX] >= 0;
    	}
    	return true;
    }
    
    public boolean isPeasantHolding() {
    	return holdType != Resource.NONE;
    }
    
    public void depositResource() {
    	if (holdType == Resource.GOLD) {
    		currentGold += 100;
    	}
    	else if (holdType == Resource.WOOD){
    		currentWood += 100;
    	}
    	holdType = Resource.NONE;
    	holdingCount = 0;
    }
    
    public void harvestResource(String key, Resource type) {
    	if (type == Resource.GOLD) {
    		Object[] goldMine = goldSources.remove(key);
    		int goldCount = (Integer)goldMine[AMT_INDEX];
    		if (goldCount >= 100) {
    			holdingCount = 100;
    			goldCount -= holdingCount;
    		}
    		else if (goldCount < 100) {
    			holdingCount = goldCount;
    			goldCount = 0;
    		}
    		goldMine[AMT_INDEX] = goldCount;
    		goldSources.put(key, goldMine);
    	}
    	else if (type == Resource.WOOD) {
    		Object[] tree = woodSources.remove(key);
    		int woodCount = (Integer)tree[AMT_INDEX];
    		if (woodCount >= 100) {
    			holdingCount = 100;
    			woodCount -= holdingCount;
    		}
    		else if (woodCount < 100) {
    			holdingCount = woodCount;
    			woodCount = 0;
    		}
    		tree[AMT_INDEX] = woodCount;
    		woodSources.put(key, tree);
    	}
    	holdType = type;
    }
    
    public void buildPeasant() {
    	currentGold -= 400;
    	
    	//currentFood -= 1;
    	
    	System.out.println("BUILT PEASANT");
    	
    }
}
