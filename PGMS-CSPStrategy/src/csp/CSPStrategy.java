package csp;
import map.*;
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
 * Public class CSPStrategy implements minesweeper strategy 
 * using CSP techniques.
 *
 * @version March 2001
 * @author Chris Studholme
 */
public final class CSPStrategy implements Strategy {

    /**
     * Should we print anything to System.out?
     */
    public static final boolean VERBOSE = true;
    /**
     * Threshold for display "solving..." message.  The message is displayed
     * if the number of variables minus the number of constraints exceeds
     * this threshold.
     */
    public static final int SOLVE_THRESHOLD = 20;

    /**
     * Private copy of the current game.
     */
    private Map map;

    /**
     * Master list of outstanding constraints.
     */
    private Constraint constraints[];
    /**
     * Number of non-null entries in the constraints array.
     */
    private int nconstraints;

    /**
     * Default constructor initializes the constraints array.
     */
    public CSPStrategy() {
	constraints = new Constraint[8];
	nconstraints=0;
    }

    /**
     * Play a non-hinted game.  Starting in a corner seems to be the best
     * strategy and since we assume a random map (as opposed to one
     * designed to fool us), we always start in the lower left corner.
     * @param m game to play
     */
    public void play(Map m) {
	//play(m,m.columns()/2,m.rows()/2);
	//play(m,m.pick(m.columns()),m.pick(m.rows()));
	play(m,0,0);
    }

    /**
     * Play a hinted game.
     * @param m game to play
     * @param hint_column x coordinate of hint
     * @param hint_row y coordinate of hint 
     */
    public void play(Map m, int hint_column, int hint_row) {
	map=m;

	// initialize SolutionSet statics
	SolutionSet.largest_neqns=0;
	SolutionSet.largest_nvars=0;
	SolutionSet.largest_nsols=0;
	
	// initialize board
	BoardPosition.CreateBoard(map);

	// use hint
	if (BoardPosition.board[hint_column][hint_row].probe(map)==Map.BOOM)
	    return;

	if (VERBOSE)
	    System.out.println("================ NEW GAME ================");
	
	// initialize constraints
	nconstraints=0;
	for (int x=0; x<map.columns(); ++x)
	    for (int y=0; y<map.rows(); ++y)
		addConstraint(BoardPosition.board[x][y].newConstraint());
		
	// main loop
	while (!map.done()) {

	    /* Simplify constraints by combining with each other and
	     * marking or probing _obvious_ mines and cleared areas.
	     */
	    simplifyConstraints();
	    if (map.done())
		break;

	    /* At this point the constraints are as simple as possible and
	     * the choice of next move is _not_ obvious.  All solutions to
	     * the CSP must be found to determine if there are any _safe_
	     * moves.
	     */
	    
	    /* Seperate the constraints into coupled subsets, each represented
	     * by a SolutionSet object.
	     */
	    SolutionSet[] subsets = seperateConstraints();
	    int nsubsets = subsets.length;

	    if (nsubsets<=0) {
		/* This happens when all remaining (unknown) clear positions 
		 * are seperated (by mines) from the known clear positions.
		 */
		if (VERBOSE)
		    System.out.println("No problems to solve!");
	    }

	    else {
		boolean solving_msg=false;
 		if (VERBOSE) {
		    // determine number of variables in largest subproblem
		    int nvars=subsets[0].getVariableCount();
		    int ncnts=subsets[0].getConstraintCount();
		    for (int i=1; i<nsubsets; ++i)
			if (subsets[i].getVariableCount()-
			    subsets[i].getConstraintCount()>nvars-ncnts) {
			    nvars = subsets[i].getVariableCount();
			    ncnts = subsets[i].getConstraintCount();
			}
		    
		    if (nvars-ncnts>=SOLVE_THRESHOLD) {
			solving_msg=true;
			if (nsubsets==1)
			    System.out.print("Solving "+ncnts+" constraint "+
					     nvars+" variable system...");
			else
			    System.out.print("Solving "+nsubsets+
					     " systems (largest is "+
					     +ncnts+" constraints "+nvars+
					     " variables)...");
			System.out.flush();
		    }
		}
		
		/* Solve each of the sub-problems by enumerating all solutions
		 * to the constraint satisfaction problem.
		 */
		for (int i=0; i<nsubsets; ++i)
		    subsets[i].enumerateSolutions();
		
 		if (solving_msg)
		    System.out.println(" done.");
	    }

	    /* Account for all remaining mines.  It may be found that some
	     * sub-problems have solutions that require too many or too few
	     * mines.  In these cases, some solutions will be deleted from
	     * the SolutionSet.  
	     *
	     * The number of mines expected to be found in the unknown
	     * positions is also calculated.
	     */
	    int remaining = map.mines_minus_marks();
	    int far = BoardPosition.nonConstrainedCount();
	    int far_max = remaining;
	    float far_expected = remaining;
	    for (int i=0; i<nsubsets; ++i) {
		int min=0;
		int max=far;
		for (int j=0; j<nsubsets; ++j)
		    if (i!=j) {
			min+=subsets[j].getMin();
			max+=subsets[j].getMax();
		    }
		subsets[i].reduceMinMax(remaining-max,remaining-min);
		far_expected -= subsets[i].expectedMines();
		far_max -= subsets[i].getMin();
	    }

	    /* Using far_expected here seems to work better, but sometimes
	     * yeilds negative probabilities.  far_max doesn't have this
	     * problem, but doesn't work as well.
	     */
	    //float far_prob = far>0 ? far_max/(float)far : 1;
	    float far_prob = far>0 ? far_expected/far : 1;
	    if (far_prob<0.01) far_prob=(float)0.01;

	    /* Do any craps shoots.  Even if we survive these, we are no
	     * better off.
	     */
	    boolean crapshoot=false;
	    for (int i=nsubsets-1; i>=0; --i) {
		Constraint c = subsets[i].doCrapsShoot(map);
		if (c!=null) {
		    addConstraint(c);
		    // throw away subset so we don't do anything with it
		    // again until the constraints are next simplified
		    subsets[i]=subsets[--nsubsets];
		    crapshoot=true;
		}
		else if (map.done())
		    break;
	    }
	    if (map.done())
		break;
	    if ((nsubsets<=0)&&crapshoot)
		continue;
	    
	    /* Mark for-sure mines.  These don't make us any better off 
	     * either.
	     */
	    for (int i=0; i<nsubsets; ++i)
		subsets[i].markMines(map);

	    /* If no mines are left in the unknown positions, probe them all.
	     * This is very good for us and we go back to simplification
	     * immediately afterwards.
	     */
	    if ((far_max<=0)&&(far>0)) {
		BoardPosition[] positions = BoardPosition.enumerateUnknown();
			for (BoardPosition position : positions) {
				position.probe(map);
				addConstraint(position.newConstraint());
			}
		continue;
	    }

	    /* Determine best position to make a probe (a guess).
	     */
	    int best_subset=-1;
	    float best_prob=far_prob;
	    boolean surething=false;
	    for (int i=0; i<nsubsets; ++i) {
		float prob = subsets[i].findBestProbe();
		if (prob<=0) {
		    surething=true;
		    addConstraint(subsets[i].doBestProbe(map));
		}
		else if (prob<=best_prob) {
		    best_prob=prob;
		    best_subset=i;
		}
	    }
	    if (surething)
		continue;

	    /* If best guess is a constrained position, probe it.
	     */
	    if (best_subset>=0) {
		if (VERBOSE)
		    System.out.print("GUESS: "+(int)((1-best_prob)*100)+
				     "% educated ...");
		Constraint c = subsets[best_subset].doBestProbe(map);
		if (c!=null) {
		    addConstraint(c);
		    if (VERBOSE)
			System.out.println(" good.");
		}
		else if (VERBOSE)
		    System.out.println(" FAILED!");
	    }

	    /* Otherwise, we probe one of the unknown positions.
	     */
	    else {
		// first check the corners
		BoardPosition[] positions = BoardPosition.enumerateCorners();
		String type = "corner";
		if (positions==null) {
		    // next check for edges
		    positions = BoardPosition.enumerateEdges();
		    type = "edge";
		}
		if (positions==null) {
		    // next check for a boundary position
		    positions = BoardPosition.enumerateMaxBoundary();
		    type = "boundary";
		}
		if (positions==null) {
		    // finally, if all else fails, probe some random position
		    positions = BoardPosition.enumerateUnknown();
		    type = "far";
		}
		if (positions==null)
		    System.err.println("WHAT!  No boundary or unknown?");
		if (VERBOSE)
		    System.out.print("GUESS: "+(int)((1-best_prob)*100)+"% "+
				     type+" ...");
		int i=map.pick(positions.length);
		int s=positions[i].probe(map);
		if (s>=0) {
		    addConstraint(positions[i].newConstraint());
		    if (VERBOSE)
			System.out.println(" ok.");
		}
		else if (VERBOSE)
		    System.out.println(" FAILED!");
	    }

	    /*
	    if (VERBOSE) {
	    System.out.println("Subproblems: "+nsubsets);
	    for (int i=0; i<nsubsets; ++i)
		System.out.println("  "+subsets[i].toString());
            }
	    */
	}

	// miscellaneous stats
	if ((VERBOSE)&&(SolutionSet.largest_nvars>0)) {
	    System.out.println("Largest System Solved:  "+
			       SolutionSet.largest_neqns+" equations  "+
			       SolutionSet.largest_nvars+" variables  "+
			       SolutionSet.largest_nsols+" solutions");
	}
    }

    /**
     * Add a constraint to the master list.  If the constraint is null,
     * nothing is done.
     * @param c a constraint
     */
    private void addConstraint(Constraint c) {
	if (c==null)
	    return;
	if (constraints.length<=nconstraints) {
	    // reallocate constraints
	    Constraint[] newc = new Constraint[constraints.length*2];
	    System.arraycopy(constraints,0,newc,0,nconstraints);
	    constraints=newc;
	}
	constraints[nconstraints++] = c;
    }

    /**
     * Seperate the constraints into coupled subsets and create a new
     * SolutionSet object for each one.
     * @return full array of SolutionSet objects
     */
    private SolutionSet[] seperateConstraints() {
	Vector sets = new Vector();
	int start=0;
	for (int end=1; end<=nconstraints; ++end) {
	    // search for constraints that are coupled with ones in [start,end)
	    boolean found=false;
	    for (int i=end; (i<nconstraints)&&!found; ++i)
		for (int j=start; j<end; ++j)
		    if (constraints[i].coupledWith(constraints[j])) {
			found=true;
			if (i!=end) {
			    // swap i and end
			    Constraint tmp = constraints[i];
			    constraints[i] = constraints[end];
			    constraints[end] = tmp;
			}
			break;
		    }
	    // if none were found, we have a coupled set in [start,end)
	    if (!found) {
		sets.add(new SolutionSet(constraints,start,end-start));
		start=end;
	    }
	}
	SolutionSet[] result = new SolutionSet[sets.size()];
	return (SolutionSet[])sets.toArray(result);
    }

    /**
     * Repeatedly update and remove known variables from constraints and
     * simplify those constraints until no more work can be done.
     */
    private void simplifyConstraints() {
	boolean done;
	do {
	    done=true;
	    
	    // update state of varilables
	    for (int i=0; i<nconstraints; ++i) {
		Constraint[] newconstraints = 
		    constraints[i].updateAndRemoveKnownVariables(map);
		if (newconstraints!=null) {
		    done=false;
		    for (int j=0; j<newconstraints.length; ++j)
			addConstraint(newconstraints[j]);
		}
	    }

	    if (!done)
		continue;
	    
	    // check for empty or simplifiable constraints
	    for (int i=0; i<nconstraints; ++i) {
		// check for empty, eliminate if necessary
		while ((constraints[i].isEmpty())&&(i<nconstraints))
		    constraints[i] = constraints[--nconstraints];
		
		// attempt to simplify using all others
		if (i<nconstraints)
		    for (int j=i+1; j<nconstraints; ++j)
			if (constraints[i].simplify(constraints[j]))
			    done=false;
	    }
	} while (!done);
    }


}
