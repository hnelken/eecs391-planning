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
	public static StripsAction makeMoveAction(Position pos, int unitid) {
		ActionFactory factory = new ActionFactory();
		return factory.new MoveAction(pos, unitid);
	}
	
	/**
	 * Creates a deposit action of any resource type
	 * @return A DepositAction object for any resource type
	 */
	public static StripsAction makeDepositAction() {
		ActionFactory factory = new ActionFactory();
		return factory.new DepositAction();
	}
	
	/**
	 * Creates a harvest action of a given resource type
	 * @param type The type of resource 
	 * @return A HarvestAction object for the given resource type
	 */
	public static StripsAction makeHarvestAction(GameState.Resource type) {
		ActionFactory factory = new ActionFactory();
		return factory.new HarvestAction(type);
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
			state.setPeasantPosition(position);
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
			if (pos.isAdjacent(state.getPeasantPosition())) { 
				return false; 
			}
			
			// If the desired position is a resource or town hall,
			// set the destination to be the nearest position adjacent to it.
			if (state.isResourceAtPosition(pos) || 
					pos.equals(state.getTownHallPosition())) {
				position = pos.getNearestAdjacentPosition(state.getPeasantPosition());
			}
			return true;
		}

		@Override
		public double getCost(GameState state) {
			// Estimated to be the distance between the desired position and the peasant
			return state.getPeasantPosition().chebyshevDistance(position);
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
		
		public int getUnitID() {
			return unitID;
		}
		
		public Direction getDirection() {
			return direction;
		}
		
		@Override
		public boolean preconditionsMet(GameState state) {
			unitID = state.getPeasantID();
			
			// Peasant must be holding a resource and adjacent to the town hall
			Position peasant = state.getPeasantPosition();
			boolean nextToTownHall = peasant.isAdjacent(state.getTownHallPosition());
			if (nextToTownHall) {
				direction = peasant.getDirection(state.getTownHallPosition());
				return state.isPeasantHolding();
			}
			return false;
		}

		@Override
		public GameState apply(GameState state) {
			// Sets peasant to empty handed and adds to totals
			state.depositResource();
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
		
		public HarvestAction(GameState.Resource type) {
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
			unitID = state.getPeasantID();
			// Peasant must be empty handed
			if (state.isPeasantHolding()) { return false; }
			// Must be next to non-empty resource of given type
			Position resource = null;
			if (type == GameState.Resource.GOLD) {
				resource = state.goldMineNextToPosition(state.getPeasantPosition());
				if (resource != null) {
					direction = state.getPeasantPosition().getDirection(resource);
					resourceKey = resource.keyString();
					return state.isResourceEmpty(type, resourceKey);
				}
			}
			else if (type == GameState.Resource.WOOD) {
				resource = state.treeNextToPosition(state.getPeasantPosition());
				if (resource != null) {
					direction = state.getPeasantPosition().getDirection(resource);
					resourceKey = resource.keyString();
					return state.isResourceEmpty(type, resourceKey);
				}
			}
			return false;
		}

		@Override
		public GameState apply(GameState state) {
			// Sets peasant to holding 100 or less, removes from resource count
			state.harvestResource(resourceKey, type);
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
			
			// enough dough
			met = state.getCurrentGold() >= 400;
			
			// enough food
			// met = met && state.getFood > 0
			// townHallID = state.getTownHallID();
			
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
