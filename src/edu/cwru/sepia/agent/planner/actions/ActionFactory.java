package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

public class ActionFactory {
	
	/**
	 * Creates a move action with the given direction
	 * @param dir The direction the move is in
	 * @return A MoveAction object in the given direction
	 */
	public static StripsAction makeMoveAction(int unitID, Position pos) {
		ActionFactory factory = new ActionFactory();
		return factory.new MoveAction(pos, unitID);
	}
	
	/**
	 * Creates a deposit action of any resource type
	 * @return A DepositAction object for any resource type
	 */
	public static StripsAction makeDepositAction(int unitID) {
		ActionFactory factory = new ActionFactory();
		return factory.new DepositAction(unitID);
	}
	
	/**
	 * Creates a harvest action of a given resource type
	 * @param type The type of resource 
	 * @return A HarvestAction object for the given resource type
	 */
	public static StripsAction makeHarvestAction(int unitID, GameState.Resource type) {
		ActionFactory factory = new ActionFactory();
		return factory.new HarvestAction(unitID, type);
	}
	
	public static StripsAction makeBuildAction() {
		ActionFactory factory = new ActionFactory();
		return factory.new BuildPeasantAction();
	}
	
	/**
	 * A strips action describing a directional move of the peasant
	 */
	public class MoveAction implements StripsAction {
		
		private Position position;	// The direction of the move
		private int unitID;
		
		// Pass the direction the move will be in
		public MoveAction(Position pos, int unitid) {
			this.position = pos;
			this.unitID = unitid;
		}
		
		public Position getActionPosition() {
			return position;
		}
		
		public int getUnitID() {
			return unitID;
		}
		
		@Override
		public boolean preconditionsMet(GameState state) {
			// The resultant position must be valid
			return processPosition(position, state);
		}

		@Override
		public GameState apply(GameState state) {
			// Set new peasant location
			state.setPeasantPosition(unitID, position);
			return state;
		}
		
		// Processes and cleans the destination position for the peasant
		private boolean processPosition(Position pos, GameState state) {
			
			// Must be in bounds
			if (!pos.inBounds(state.getXExtent(), state.getYExtent())) {
				return false;
			}
			
			// Since moves are between distinct positions, moves to a destination
			// that the peasant is already adjacent to are not allowed.
			if (pos.isAdjacent(state.getPeasantPosition(unitID))) { 
				return false; 
			}
			
			// If the desired position is a resource or town hall,
			// set the destination to be the nearest position adjacent to it.
			if (state.isResourceAtPosition(pos) || 
					pos.equals(state.getTownHallPosition())) {
				position = pos.getNearestAdjacentPosition(state.getPeasantPosition(unitID));
			}
			return true;
		}

		@Override
		public double getCost(GameState state) {
			// Estimated to be the distance between the desired position and the peasant
			return state.getPeasantPosition(unitID).chebyshevDistance(position);
		}
		
		@Override
		public String toString() {
			return "[Move - ID:" + unitID + ", Position:" + position + "]";
		}
	}
	
	/**
	 * A strips action describing a peasant depositing a resource
	 */
	public class DepositAction implements StripsAction {
		
		private int unitID;
		private Direction direction;
		
		public DepositAction(int unitID){
			this.unitID = unitID;
		}
		
		public int getUnitID() {
			return unitID;
		}
		
		public Direction getDirection() {
			return direction;
		}
		
		@Override
		public boolean preconditionsMet(GameState state) {
			
			// Peasant must be holding a resource and adjacent to the town hall
			Position peasant = state.getPeasantPosition(unitID);
			boolean nextToTownHall = peasant.isAdjacent(state.getTownHallPosition());
			if (nextToTownHall) {
				direction = peasant.getDirection(state.getTownHallPosition());
				return state.isPeasantHolding(unitID);
			}
			return false;
		}

		@Override
		public GameState apply(GameState state) {
			// Sets peasant to empty handed and adds to totals
			state.depositResource(unitID);
			return state;
		}

		@Override
		public double getCost(GameState state) {
			// Primitive action takes unit time
			return 1.0;
		}
		
		@Override
		public String toString() {
			return "[Deposit - ID:" + unitID + ", Direction: " + direction + "]";
		}
	}
	
	public class HarvestAction implements StripsAction {

		private String resourceKey;
		
		private int unitID;
		private Direction direction;
		private GameState.Resource type;
		
		public HarvestAction(int id, GameState.Resource type) {
			this.unitID = id;
			this.type = type;
		}
		
		public int getUnitID() {
			return unitID;
		}
		
		public Direction getDirection() {
			return direction;
		}
		
		@Override
		public boolean preconditionsMet(GameState state) {
			// Peasant must be empty handed
			if (state.isPeasantHolding(unitID)) { return false; }
			
			// Must be next to non-empty resource of given type
			Position peasant = state.getPeasantPosition(unitID);
			Position resource = null;
			if (type == GameState.Resource.GOLD) {
				resource = state.goldMineNextToPosition(peasant);
				if (resource != null) {
					direction = peasant.getDirection(resource);
					resourceKey = resource.keyString();
					return state.isResourceEmpty(type, resourceKey);
				}
			}
			else if (type == GameState.Resource.WOOD) {
				resource = state.treeNextToPosition(peasant);
				if (resource != null) {
					direction = peasant.getDirection(resource);
					resourceKey = resource.keyString();
					return state.isResourceEmpty(type, resourceKey);
				}
			}
			return false;
		}

		@Override
		public GameState apply(GameState state) {
			// Sets peasant to holding 100 or less, removes from resource count
			state.harvestResource(unitID, resourceKey, type);
			return state;
		}

		@Override
		public double getCost(GameState state) {
			// Primitive action takes unit time
			return 1.0;
		}
		
		@Override
		public String toString() {
			return "[Harvest - Type:" + type + ", ID:" + unitID + ", Direction:" + direction + "]";
		}
		
	}
	
	public class BuildPeasantAction implements StripsAction {

		private int townHallID;
		
		@Override
		public boolean preconditionsMet(GameState state) {
			boolean met = false;
			// Must have sufficient gold and food;
			met = state.getCurrentGold() >= 400;
			met = met && state.getCurrentFood() > 0;
			townHallID = state.getTownHallID();
			return met;
		}

		@Override
		public GameState apply(GameState state) {
			// Build peasant
			state.buildPeasant();
			return state;
		}

		@Override
		public double getCost(GameState state) {
			return 1.0;
		}

		@Override
		public int getUnitID() {
			return townHallID;
		}
		
	}
	
}
