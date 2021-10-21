package csp;
import map.Map;
import java.util.Vector;

/* Copyright (C) 2001 Chris Studholme

This file is part of a Constraint Satisfaction Problem (CSP) strategy 
for Programmer's Minesweeper (PGMS).
 
CSPStrategy is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
CSPStrategy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with CSPStrategy; see the file COPYING.  If not, write to
the Free Software Foundation, 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.
*/

/** 
 * Class to represent a single minesweeper board position.  There will
 * by one and only one instance of this class for each board position.
 *
 * A board position is in one of 4 possible states: unknown, unknown but
 * constrained by a neighbouring clear position, marked as being a mine,
 * and clear (indicates the number of neighbouring mines).  
 *
 * For unknown but not constrained positions, a count of the number of 
 * neighbouring constrained positions is maintained.  Note that if one of 
 * these positions is later found to be a mine, the count will decrease.  
 *
 * In static space, a two dimensional array of the entire board is created
 * and maintained.  Counts of the number of unknown, contrained, mines,
 * and clear positions are maintained.  
 *
 * A public instance variable holding a test assignment for constrained 
 * positions is available for the use of SolutionSet and Constraint.
 *
 * @see CSPStrategy
 * @version March 2001
 * @author Chris Studholme
 */
final class BoardPosition {
    /**
     * Unknown state (no neighbouring clear positions).
     */
    public static final int UNKNOWN=-5;
    /**
     * Unknown but constrained state (some neighbour is clear).
     */
    public static final int CONSTRAINED=-4;
    /**
     * 100% certain of mine.
     */
    public static final int MARKED=-3;
    /**
     * Position is clear.  The actual state value will be in the range [0..8]
     * which represents the number of neighbouring mines.
     */
    public static final int CLEAR=0; // or greater

    /**
     * Count of number of unknown postions.
     */
    private static int unknown;
    /**
     * Count of number of unknown but constrained positions.
     */
    private static int constrained;
    /**
     * Count of number of known mines.
     */
    private static int mine;
    /**
     * Count of number of known clear positions.
     */
    private static int clear;

    /**
     * The board.  The first dimension is x in the range [0,map.columns()-1].
     * The second is y in the range [0,map.rows()-1].
     */
    public static BoardPosition[][] board;

    /**
     * x coordinate of position.
     */
    private int x;
    /**
     * y coordinate of position.
     */
    private int y;
    /**
     * All neighbours lie in nx1<=x<nx2 and ny1<=y<ny2.
     */
    private int nx1,nx2,ny1,ny2; // coords of neighbours

    /**
     * State of element (>=0 is number of mines).
     */
    private int state;
    /**
     * Number of constrained positions nearby.
     */
    private int boundary_level; 

    /**
     * For the exclusive use of SolutionSet and Constraint.
     * -1 means not set, 0 or 1 otherwise.
     */
    public int testAssignment;

    /**
     * Construct the board and all position instances to correspond to
     * given Map instance.
     * @param map the game we are going to play
     * @return true if any positions are already clear
     */
    public static boolean CreateBoard(Map map) {
	// these need to be reinitialized for each game
	unknown=0;
	constrained=0;
	mine=0;
	clear=0;

	// create board and all position instances
	board = new BoardPosition[map.columns()][map.rows()];
	for (int x=0; x<map.columns(); ++x)
	    for (int y=0; y<map.rows(); ++y)
		board[x][y] = new BoardPosition(x,y);

	// check if we have a head start
	boolean headstart=false;
	for (int x=0; x<map.columns(); ++x)
	    for (int y=0; y<map.rows(); ++y) {
		int s = map.look(x,y);
		if (s==Map.MARKED)
		    board[x][y].setState(MARKED);
		else if (s>=0) {
		    board[x][y].setState(s);
		    headstart=true;
		}
	    }
	return headstart;
    }

    /**
     * Count of unknown and non-constrained positions.
     * @return count
     */
    public static int nonConstrainedCount() {
	return unknown;
    }

    /**
     * Helper method to enumerate all unknown positions with a particular
     * boundary level.
     * @param level boundary level to search for
     * @return array of positions
     */
    private static BoardPosition[] enumerateBoundary(int level) {
	BoardPosition[] result = new BoardPosition[unknown];
	int count=0;
	for (int x=0; x<board.length; ++x)
	    for (int y=0; y<board[x].length; ++y)
		if ((board[x][y].state==UNKNOWN)&&
		    (board[x][y].boundary_level==level))
		    result[count++] = board[x][y];
	if (count==0)
	    return null;
	if (count<result.length) {
	    // reallocate
	    BoardPosition[] newresult = new BoardPosition[count];
	    System.arraycopy(result,0,newresult,0,count);
	    result=newresult;
	}
	return result;
    }

    /**
     * Determine the maximum (non-zero) value of boundary_level and return all
     * board positions with that level.
     * @return array of board positions or null if no non-zero boundary postions found
     */
    public static BoardPosition[] enumerateMaxBoundary() {
	// find maximum
	int max=0;
	for (int x=0; x<board.length; ++x)
	    for (int y=0; y<board[x].length; ++y)
		if ((board[x][y].state==UNKNOWN)&&
		    (board[x][y].boundary_level>max))
		    max=board[x][y].boundary_level;
	if (max==0)
	    return null;
	// get board positions
	BoardPosition[] result = enumerateBoundary(max);
	//System.err.println("Boundary: level="+max+" number="+result.length);
	if (result==null)
	    System.out.println("BoardPosition.enumerateBoundary() WE CAN'T COUNT!!!");
	return result;
    }

    /**
     * Determine the minimum (non-zero) value of boundary_level and return all
     * board positions with that level.
     * @return array of board positions or null if no non-zero boundary postions found
     */
    public static BoardPosition[] enumerateMinBoundary() {
	// find minimum
	int min=1000; //lots
	for (int x=0; x<board.length; ++x)
	    for (int y=0; y<board[x].length; ++y)
		if ((board[x][y].state==UNKNOWN)&&
		    (board[x][y].boundary_level>0)&&
		    (board[x][y].boundary_level<min))
		    min=board[x][y].boundary_level;
	if (min==1000)
	    return null;
	// get board positions
	BoardPosition[] result = enumerateBoundary(min);
	if (result==null)
	    System.out.println("BoardPosition.enumerateBoundary() WE CAN'T COUNT!!!");
	return result;
    }

    /**
     * Enumerate all unknown but not boundary (boundary_level==0) positions.  
     * If there are none, return the result of enumerateMinBoundary() instead.
     * @return array of unknown positions or null if there are none
     */
    public static BoardPosition[] enumerateFar() {
	BoardPosition[] result = enumerateBoundary(0);
	return result!=null ? result : enumerateMinBoundary();
    }

    /**
     * Enumerate all unknown positions regardless of the value of 
     * boundary_level.
     * @return array of unknown board positions or null if none
     */
    public static BoardPosition[] enumerateUnknown() {
	if (unknown==0)
	    return null;
	BoardPosition[] result = new BoardPosition[unknown];
	int count=0;
	for (int x=0; x<board.length; ++x)
	    for (int y=0; y<board[x].length; ++y)
		if (board[x][y].state==UNKNOWN)
		    result[count++] = board[x][y];
	if (count!=unknown)
	    System.err.println("BoardPosition.enumerateUnknown() WE CAN'T COUNT!!!");
	return result;
    }

    /**
     * Enumerate all unknown edge positions.
     * @return array of unknown board positions or null if none
     */
    public static BoardPosition[] enumerateEdges() {
	Vector v = new Vector();
	for (int x=1; x<board.length-1; ++x) {
	    if (board[x][0].state<CONSTRAINED)
		v.add(board[x][0]);
	    if (board[x][board[x].length-1].state<CONSTRAINED)
		v.add(board[x][board[x].length-1]);
	}
	for (int y=1; y<board[0].length-1; ++y) {
	    if (board[0][y].state<CONSTRAINED)
		v.add(board[0][y]);
	    if (board[board.length-1][y].state<CONSTRAINED)
		v.add(board[board.length-1][y]);
	}
	if (v.size()==0)
	    return null;
	BoardPosition[] result = new BoardPosition[v.size()];
	return (BoardPosition[])v.toArray(result);
    }

    /**
     * Enumerate all unknown corner positions.
     * @return array of unknown board positions or null if none
     */
    public static BoardPosition[] enumerateCorners() {
	BoardPosition[] result = new BoardPosition[4];
	int count=0;
	if (board[0][0].state<CONSTRAINED)
	    result[count++]=board[0][0];
	if (board[board.length-1][0].state<CONSTRAINED)
	    result[count++]=board[board.length-1][0];
	if (board[0][board[0].length-1].state<CONSTRAINED)
	    result[count++]=board[0][board[0].length-1];
	if (board[board.length-1][board[board.length-1].length-1].state<CONSTRAINED)
	    result[count++]=board[board.length-1][board[board.length-1].length-1];
	if (count==0)
	    return null;
	if (count==4)
	    return result;
	// reallocate
	BoardPosition[] newresult = new BoardPosition[count];
	System.arraycopy(result,0,newresult,0,count);
	return newresult;
    }

    /**
     * Default constructor.  This constructor is private because new instances
     * of BoardPosition should only be created by the static method 
     * CreateBoard().
     *
     * Initializes all instance variables.
     */
    private BoardPosition(int x, int y) {
	state=UNKNOWN;
	boundary_level=0;
	++unknown;
	this.x=x;
	this.y=y;
	nx1=x>0?x-1:0;
	nx2=x<board.length-1?x+2:x+1;
	ny1=y>0?y-1:0;
	ny2=y<board[0].length-1?y+2:y+1;
	testAssignment=-1;
    }

    /**
     * Returns "(?x,y)" where ? indicates state ('U','C','M', or ' ') and
     * x,y are the coordinates.
     * @return descriptive string for object
     */
    public String toString() {
	char c;
	switch (state) {
	case UNKNOWN:
	    c='U'; break;
	case CONSTRAINED:
	    c='C'; break;
	case MARKED:
	    c='M'; break;
	default:
	    c=' ';
	}
	return "("+c+x+","+y+")";
    }

    /**
     * Construct a new Constraint object for this position if the position
     * is known to be clear.  Neighbouring unknown positions will have their
     * states updated to CONSTRAINED.
     * @return new instance of Constraint
     */
    public Constraint newConstraint() {
	if (state<0)
	    return null;
	Constraint c = new Constraint();
	int constant=state;
	for (int i=nx1; i<nx2; ++i)
	    for (int j=ny1; j<ny2; ++j)
		if (board[i][j].state<0) {
		    if (board[i][j].state==MARKED)
			--constant;
		    else {
			c.add(board[i][j]);
			board[i][j].setState(CONSTRAINED);
		    }
		}
	c.setConstant(constant);
	return c;
    }

    /**
     * Determine if all neighbouring positions are either known (MARKED or
     * CLEAR) or are in the provided set of positions.
     * @param variables array of positions
     * @param nvariables number of positions to look at
     * @return true if neighbours are known or in set
     */
    public boolean neighboursKnownOrInSet(BoardPosition[] variables, 
					  int nvariables) {
	for (int i=nx1; i<nx2; ++i)
	    for (int j=ny1; j<ny2; ++j)
		if (board[i][j].state<MARKED) {
		    boolean found=false;
		    for (int k=0; k<nvariables; ++k)
			if (board[i][j].equals(variables[k])) {
			    found=true;
			    break;
			}
		    if (!found)
			return false;
		}
	return true;
    }

    /**
     * Mark map position as mine and update state of board.
     * @param map current game
     */
    public void mark(Map map) {
	map.mark(x,y);
	setState(MARKED);
    }

    /**
     * Probe map position and update state of board.
     * @param map current game
     * @return number of neighbouring mines if probe successful, negate otherwise
     */
    public int probe(Map map) {
	int result = map.probe(x,y);
	setState(result);
	return result;
    }

    /**
     * Get current state of position.
     * @return state
     */
    public int getState() {
	return state;
    }

    /**
     * Set state of position.  This method updates the static counters for
     * each state type.  Also, the states of neighbouring positions may be
     * changed as a result of this change.
     * @param state new state to change to
     */
    private void setState(int state) {
	if (state==this.state)
	    return; // do nothing

	// undo current state
	switch (this.state) {
	case UNKNOWN:
	    --unknown;
	    break;
	case CONSTRAINED:
	    --constrained;
	    break;
	case MARKED:
	    --mine;
	    break;
	default:
	    --clear;
	}

	// set new state
	this.state=state;
	switch (state) {
	case UNKNOWN:
	    ++unknown;
	    break;
	case CONSTRAINED:
	    ++constrained;
	    boundary_level=0;
	    // increase boundary_level of all neighbours
	    for (int i=nx1; i<nx2; ++i)
		for (int j=ny1; j<ny2; ++j)
		    if (board[i][j].state==UNKNOWN)
			++board[i][j].boundary_level;
	    break;
	case MARKED:
	    ++mine;
	    boundary_level=0;
	    // decrease boundary_level of all neighbours
	    for (int i=nx1; i<nx2; ++i)
		for (int j=ny1; j<ny2; ++j)
		    if (board[i][j].state==UNKNOWN)
			--board[i][j].boundary_level;
	    break;
	default:
	    boundary_level=0;
	    if (state>=0)
		++clear;
	    //else
	    //System.err.println("BoardPosition.setState() invalid state code, "+state);
	}
    }

}
