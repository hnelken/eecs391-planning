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
	private Map<String, Object[]> goldSources;	// A map of gold resource info, keyed by their coordinates
	private Map<String, Object[]> woodSources;	// A map of wood resource info, keyed by their coordinates
	private Map<Integer, Object[]> peasants;	// A map of peasant unit info, keyed by their IDs
	//private Resource holdType;					// The type of resource the peasant is currently holding

	private double cost;						// The cost of the plan thus far;
	private GameState parentState;				// The predecessor state before the last action
	private List<StripsAction> actionList;		// The collection of actions that make up the plan thus far
	private List<StripsAction> lastActions;		// The actions taken to result in this state
	
	private Position townHallPosition;			// The position of the town hall unit
	//private Position peasantPosition;			// The position of the peasant unit
	//private int peasantID;						// The id of the peasant unit
	private int townHallID;						// The id of the town hall
	private int currentFood;					// The count of food available
	
	private boolean buildPeasant;				// The build peasant condition
	
	private int xExtent;						// The horizontal extent of the map
	private int yExtent;						// The vertical extent of the map
	private int requiredGold;					// The required amount of gold
	private int requiredWood;					// The required amount of wood
	private int currentGold;					// The current total of gold
	private int currentWood;					// The current total of wood
	//private int holdingCount;					// The amount the peasant is holding

	private static final int ID_INDEX = 0;
	private static final int POS_INDEX = 1;
	private static final int AMT_INDEX = 2;
	private static final int TYPE_INDEX = 3;
	
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
    	
    	// Copy build peasants
    	this.buildPeasant = buildPeasants;
    	
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
			
			// Determine if the resource is gold or wood
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
    	//this.holdType = Resource.NONE;
    	//this.holdingCount = 0;
    	
    	// The plan is initially empty
    	this.actionList = new ArrayList<StripsAction>();
    	this.lastActions = new ArrayList<StripsAction>();
    	
    	// Get info about the units in the world
    	peasants = new HashMap<Integer, Object[]>();
    	List<UnitView> units = state.getUnits(playernum);
    	for (UnitView unit : units) {
    		Position position = new Position(unit.getXPosition(), unit.getYPosition());
    		if (unit.getTemplateView().getName().equals("Peasant")) {
    			// Assemble peasant info array
    	    	//this.peasantID = unit.getID();
    	    	//this.peasantPosition = position;
    	    	Object[] info = new Object[]{ unit.getID(), position, 0, Resource.NONE };
    	    	peasants.put(unit.getID(), info);
    		}
    		else {	// This unit is the town hall
    			this.townHallPosition = position;
    			this.townHallID = unit.getID();
    			this.currentFood = unit.getTemplateView().getFoodProvided() - 1;
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
    	//this.holdType = parentState.holdType;
    	//this.holdingCount = parentState.holdingCount;
    	this.goldSources = new HashMap<String, Object[]>(parentState.goldSources);
    	this.woodSources = new HashMap<String, Object[]>(parentState.woodSources);
    	this.peasants = new HashMap<Integer, Object[]>();
    	
    	// Copy values from peasant info arrays to avoid lingering references
    	for (Object[] parent : parentState.peasants.values()) {
    		// Get id
    		int id = (Integer)parent[ID_INDEX];
    		
    		// Get position
    		Position pos = (Position)parent[POS_INDEX];
    		pos = new Position(pos.x, pos.y);
    		
    		// Get holding count and type
    		int holdingCount = (Integer)parent[AMT_INDEX];
    		Resource holdType = (Resource)parent[TYPE_INDEX];
    		
    		// Assemble and store
    		Object[] peasant = new Object[] { id, pos, holdingCount, holdType };
    		this.peasants.put(id, peasant);
    	}
    	
    	this.xExtent = parentState.getXExtent();
    	this.yExtent = parentState.getYExtent();
    	this.requiredGold = parentState.requiredGold;
    	this.requiredWood = parentState.requiredWood;
    	this.currentGold = parentState.currentGold;
    	this.currentWood = parentState.currentWood;
    	this.currentFood = parentState.currentFood;
    	
    	//this.peasantID = parentState.peasantID;
    	this.townHallID = parentState.townHallID;
    	//this.peasantPosition = new Position(parentState.peasantPosition.x, parentState.peasantPosition.y);
    	this.townHallPosition = new Position(parentState.townHallPosition.x, parentState.townHallPosition.y);
    	
    	// Plan/cost is previous state + last actions
    	this.lastActions = actions;
    	this.cost = parentState.cost;
    	this.actionList = new ArrayList<StripsAction>(parentState.getActionList());
    	for (int i = 0; i < lastActions.size(); i++) {
        	this.cost += lastActions.get(i).getCost(parentState);
        	this.actionList.add(lastActions.get(i));
        	this.lastActions.get(i).apply(this);
    	}
    }
    
    public List<StripsAction> getActionList() {
    	return actionList;
    }
    
    public String getResourceLevels() {
    	Object[] peasant = peasants.get(1);
    	return "Wood: " + (currentWood) + 
    			" Gold: " + (currentGold) + 
    			" Holding: " + peasant[AMT_INDEX];//holdingCount;
    }
    
    public Position getTownHallPosition() {
    	return townHallPosition;
    }

    public int getTownHallID() {
    	return townHallID;
    }
    
    public Position getPeasantPosition(int id) {
    	Object[] peasant = peasants.get(id);
    	return (Position)peasant[POS_INDEX];
    }
    
//    public Position getPeasantPosition() {
//    	return peasantPosition;
//    }
    
    public void setPeasantPosition(int id, Position newPosition) {
    	Object[] peasant = peasants.remove(id);
    	peasant[POS_INDEX] = newPosition;
    	peasants.put(id, peasant);
    }
    
//    public void setPeasantPosition(Position newPosition) {
//    	peasantPosition = newPosition;
//    }
    
//    public int getPeasantID() {
//    	return peasantID;
//    }
    
    public int getXExtent() {
    	return xExtent;
    }
    
    public int getYExtent() {
    	return yExtent;
    }
    
    public int getCurrentGold() {
    	return currentGold;
    }
    
    public int getCurrentFood() {
    	return currentFood;
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
    	List<List<StripsAction>> peasantActions = new ArrayList<>();

    	for (Object[] peasant : peasants.values()) {

    		int peasantID = (Integer)peasant[ID_INDEX];
    		List<StripsAction> actions = new ArrayList<>();
    		
    		if (!isPeasantHolding(peasantID)) {
		    	if (currentGold < requiredGold) {
			    	// Add move actions to gold mines the peasant is not adjacent to already
			    	for (Object[] goldMine : goldSources.values()) {
		    			Position minePosition = (Position)goldMine[POS_INDEX];
			    		if ((Integer)goldMine[AMT_INDEX] > 0) {
			    			StripsAction moveToMine = ActionFactory.makeMoveAction(peasantID, minePosition);
			    			if (moveToMine.preconditionsMet(this)) {
			    				actions.add(moveToMine);
			    			}
			    		}
			    	}
		
			    	// Add HarvestGold action if available
			    	StripsAction harvestGold = ActionFactory.makeHarvestAction(peasantID, Resource.GOLD);
			    	if (harvestGold.preconditionsMet(this)) {
			    		actions.add(harvestGold);
			    	}
		    	}
				
		    	if (currentWood < requiredWood) {
			    	// Add move actions to trees the peasant is not adjacent to already
			    	for (Object[] tree : woodSources.values()) {
			    		if ((Integer)tree[AMT_INDEX] > 0) {
				    		Position treePosition = (Position)tree[POS_INDEX];
				    		StripsAction moveToTree = ActionFactory.makeMoveAction(peasantID, treePosition);
				    		if (moveToTree.preconditionsMet(this)) {
				    			actions.add(moveToTree);
				    		}
			    		}
			    	}
			    	
			    	// Add HarvestWood action if available
			    	StripsAction harvestWood = ActionFactory.makeHarvestAction(peasantID, Resource.WOOD);
			    	if (harvestWood.preconditionsMet(this)) {
			    		actions.add(harvestWood);
			    	}
		    	}
    		}
    		else {	// peasant is holding
		    	// Add a move action to the town hall if available
		    	StripsAction moveToTownHall = ActionFactory.makeMoveAction(peasantID, townHallPosition);
		    	if (moveToTownHall.preconditionsMet(this)) {
		    		actions.add(moveToTownHall);
		    	}
		    	
		    	// Add Deposit action if available
		    	StripsAction deposit = ActionFactory.makeDepositAction(peasantID);
		    	if (deposit.preconditionsMet(this)) {
		    		actions.add(deposit);
		    	}	
    		}
    		
	    	peasantActions.add(actions);
    	}

    	List<StripsAction> stateActions = null;
		List<StripsAction> actions = peasantActions.get(0);
    	for (int i = 0; i < actions.size(); i++) {
    		stateActions = new ArrayList<StripsAction>();
    		
    		// Add action and make state
    		stateActions.add(actions.get(i));
    		GameState noBuild = new GameState(this, stateActions);
    		children.add(noBuild);
    		
    		// Consider build option
    		StripsAction build = ActionFactory.makeBuildAction();
    		if (buildPeasant && build.preconditionsMet(this)) {
    			stateActions = new ArrayList<StripsAction>(stateActions);
    			stateActions.add(build);
    			GameState withBuild = new GameState(this, stateActions);
    			children.add(withBuild);
    		}
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
    	
    	// TODO: BUILD PEASANT?
    	
    	// If not holding
    	for (Object[] peasant : peasants.values()) {
    		int id = (Integer)peasant[ID_INDEX];
    		Position position = (Position)peasant[POS_INDEX];
    		Resource holdType = (Resource)peasant[TYPE_INDEX];
    		int holdingCount = (Integer)peasant[AMT_INDEX];
	    	if (holdType == Resource.NONE) {
	    		// - proximity to nearest and lowest resource is good
	    		if (currentGold < currentWood) {
	    			heuristic += nearestMineToPosition(position).euclideanDistance(position);
	    		}
	    		else {
	    			heuristic += nearestTreeToPosition(position).euclideanDistance(position);
	    		}
	    	}
	    	else { // If holding, minimize distance to town hall
	    		if (parentState.parentState.isPeasantHolding(id)) {
	    			heuristic += 100;
	    		}
	    		heuristic += position.euclideanDistance(townHallPosition);
	    	}
	    	
	    	heuristic -= holdingCount/10;
    	}

    	heuristic += requiredGold - currentGold;
    	heuristic += requiredWood - currentWood;
    	
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
    
    public Position nearestMineToPosition(Position position) {
    	Position nearestMine = null;
    	double minDist = Double.MAX_VALUE;
    	for (Object[] goldMine : goldSources.values()) {
    		Position minePosition = (Position)goldMine[POS_INDEX];
    		double dist = minePosition.euclideanDistance(position);
    		if (dist < minDist) {
    			minDist = dist;
    			nearestMine = minePosition;
    		}
    	}
    	return nearestMine;
    }
    
    public Position nearestTreeToPosition(Position position) {
    	Position nearestTree = null;
    	double minDist = Double.MAX_VALUE;
    	for (Object[] tree : woodSources.values()) {
    		Position treePosition = (Position)tree[POS_INDEX];
    		double dist = treePosition.euclideanDistance(position);
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
    
    public boolean isPeasantHolding(int id) {
    	Object[] peasant = peasants.get(id);
    	Resource holdType = (Resource)peasant[TYPE_INDEX];
    	return holdType != Resource.NONE;
    }
    
    public void depositResource(int id) {
    	System.out.println("DEPOSIT\n - pre");
    	System.out.println(getResourceLevels());
    	// Get depositing peasant info
    	Object[] peasant = peasants.remove(id);
    	Resource holdType = (Resource)peasant[TYPE_INDEX];
    	int holdingCount = (Integer)peasant[AMT_INDEX];
    	
    	// Make deposit
    	if (holdType == Resource.GOLD) {
    		currentGold += holdingCount;
    	}
    	else if (holdType == Resource.WOOD){
    		currentWood += holdingCount;
    	}
    	
    	// Change peasant info
    	peasant[AMT_INDEX] = 0;
    	peasant[TYPE_INDEX] = Resource.NONE;
    	
    	// Save edited peasant info
    	peasants.put(id, peasant);
    	System.out.println(getResourceLevels());
    }
    
    public void harvestResource(int id, String key, Resource type) {
    	Object[] peasant = peasants.remove(id);
    	int holdingCount = 0;
    	
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
    	
    	// Edit peasant info
    	peasant[AMT_INDEX] = holdingCount;
    	peasant[TYPE_INDEX] = type;
    	
    	// Save edited peasant info
    	peasants.put(id, peasant);
    }
    
    public void buildPeasant() {
    	currentGold -= 400;
    	currentFood -= 1;
    	
    	// build peasant info [] and add it
    	
    	System.out.println("BUILT PEASANT");
    }
}
