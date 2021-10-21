package csp;
import map.Map;
import java.util.*;

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
 * Instances of this class are used to enumerate all of the solutions
 * to coupled set of Constraint's.  The solutions are found using a
 * backtracking algorithm and statistics about the solutions are kept
 * and sorted into bins based on the number of mines (1's) required for
 * each solution.  Note that individual solutions are not stored.  Instead,
 * for each mine rank, the total number of solutions will be tallied along
 * with the number of instances of each variable equalling 1 (ie. mine).
 *
 * From the stats produced, cases where a variable must be 0 or must
 * be 1 can be easily found.  If none of these are found, the probability
 * of a particular variables being 0 or 1 can be calculated.
 *
 * If it is found that some solutions require either too many 1's or too
 * few 1's, those solutions can be removed from the solution set with ease.
 *
 * In static space, details about the largest CSP solved thus far are
 * maintained.  A CSP's size is considered to be proportional to the
 * number of variables (total number of distinct variables) minus the number
 * of constraints.  The size and number of solutions found for this CSP
 * are recorded for profiling uses. 
 *
 * @see CSPStrategy
 * @version March 2001
 * @author Chris Studholme
 */

final class SolutionSet {

    /**
     * Number of variables in largest CSP solved.
     */
    public static int largest_nvars=0;
    /**
     * Number of constraints in largest CSP solved.
     */
    public static int largest_neqns=0;
    /**
     * Number of solutions found in largest CSP solved.
     */
    public static int largest_nsols=0;

    /**
     * Should we print anything to System.out?
     */
    private static boolean VERBOSE = false;

    /**
     * Master list of constraints.
     */
    private Constraint[] constraints;
    /**
     * Number of non-null entries in constraints[].
     */
    private int nconstraints;

    /**
     * Master list of variables (sorted in decreasing order by number of
     * constraints of each variable).
     */
    private BoardPosition[] variables;
    /**
     * For each variable, a list of constraints constraining that variable
     * (sorted in decreasing order by number of constraints).
     */
    private ConstraintList[] nodes;
    /**
     * Number of non-null entries in variables[] and nodes[].
     */
    private int nvariables;

    /**
     * The number of solutions found for each possible number of mines.
     */
    private int[] solutions;
    /**
     * The number of instances of variable=1 for each variable and number
     * of mines required for solution.  The first index is number of mines,
     * while the second is the variable number.
     */
    private int[][] mines;
    /**
     * The minimum number of mines required by a solution.
     */
    private int min;
    /**
     * The maximum number of mines required by a solution.
     */
    private int max;

    /**
     * Best place to probe next (lowest probability of mine).
     */
    private BoardPosition bestProbe;

    /**
     * Construct SolutionSet object given an array of constraints.
     * Note: only called by constructors.
     * @param constraints array of constraints
     * @param nconstraints number of non-null entries in array
     */
    private void construct(Constraint[] constraints, int nconstraints) {
	this.constraints=constraints;
	this.nconstraints=nconstraints;
	VERBOSE=CSPStrategy.VERBOSE;

	variables=null;
	nodes = new ConstraintList[nconstraints*2];
	nvariables=0;

	min=0;
	max=0;
	bestProbe=null;
	
	// tally variables and count maximum mines
	for (int i=0; i<nconstraints; ++i) {
	    BoardPosition[] vararray = constraints[i].getVariables();
		for (BoardPosition boardPosition : vararray) {
			boolean found = false;
			for (int k = 0; k < nvariables; ++k)
				if (nodes[k].variable.equals(boardPosition)) {
					nodes[k].addConstraint(constraints[i]);
					found = true;
					break;
				}
			if (!found) {
				if (nvariables >= nodes.length) {
					// reallocate variables
					ConstraintList[] newnodes =
							new ConstraintList[nvariables * 2];
					System.arraycopy(nodes, 0, newnodes, 0, nvariables);
					nodes = newnodes;
				}
				nodes[nvariables++] =
						new ConstraintList(constraints[i], boardPosition);
			}
		}
	    min+=constraints[i].getConstant();
	}

	/* Note: we used min here to tally the absolute maximum number of mines
	 * expected because this number is a good initial value for the minimum
	 * mines variables.
	 */

	// sort variables in decending order by number of constraints
	Arrays.sort(nodes,0,nvariables);
	if (nodes[0].nconstraints<nodes[nvariables-1].nconstraints)
	    System.err.println("WRONG ORDER!!!");

	// create variables array
	variables = new BoardPosition[nvariables];
	for (int i=0; i<nvariables; ++i)
	    variables[i] = nodes[i].variable;

	// create needed arrays
	solutions = new int[min+1];
	mines = new int[min+1][];
	for (int i=0; i<=min; ++i)
	    mines[i] = new int[nvariables];
    }

    /**
     * Construct from a full array of constraints.
     * @param constraints full array of constraints
     */
    public SolutionSet(Constraint[] constraints) {
	construct(constraints,constraints.length);
    }    

    /**
     * Construct from subset of an array of constraints.
     * @param constraints array of constraints
     * @param startIndex first constraints to include
     * @param nconstraints total number of constraints to include
     */
    public SolutionSet(Constraint[] constraints, int startIndex, 
		       int nconstraints) {
	if (startIndex==0)
	    construct(constraints,nconstraints);
	else {
	    Constraint[] newarray = new Constraint[nconstraints];
	    System.arraycopy(constraints,startIndex,newarray,0,nconstraints);
	    construct(newarray,nconstraints);
	}
    }

    /**
     * Returns number of constraints, variables and statistics about
     * solutions (if avaiable).
     * @return descriptive string for instance
     */
    public String toString() {
	StringBuffer s = new StringBuffer();
	s.append("constraints: ").append(nconstraints);
	s.append("  vars: ").append(nvariables);
	if (max>0) {
	    int total=0;
	    for (int i=min; i<=max; ++i)
		total+=solutions[i];
	    s.append("  solutions: ").append(total);
	    s.append("  min: ").append(min);
	    s.append("  max: ").append(max);
	}
	for (int i=0; i<nconstraints; ++i)
	    s.append("\n    ").append(constraints[i].toString());
	return s.toString();
    }

    /**
     * Get total number of distinct variables.
     * @return number of variables
     */
    public int getVariableCount() {
	return nvariables;
    }

    /**
     * Get total number of constraints.
     * @return constraint count
     */
    public int getConstraintCount() {
	return nconstraints;
    }

    /**
     * Get minimum number of mines required by solutions.
     * @return number of mines
     */
    public int getMin() {
	return min;
    }

    /**
     * Get maximum number of mines required by solutions.
     * @return number of mines
     */
    public int getMax() {
	return max;
    }

    /**
     * Calculate the mean number of mines required by solutions.
     * @return expected number of mines
     */
    public float expectedMines() {
	float total=0;
	float count=0;
	for (int i=min; i<=max; ++i) {
	    total += i*solutions[i];
	    count += solutions[i];
	}
	return total/count;
    }

    /**
     * Shrink the range [min,max] to [newmin,mewmax].
     * Precondition: min<=newmin<=newmax<=max (not checked)
     * @param newmin new value for min
     * @param newmax new value for max
     */
    public void reduceMinMax(int newmin, int newmax) {
	if (newmin>min) {
	    for (int i=min; i<newmin; ++i)
		solutions[i]=0;
	    min=newmin;
	}
	if (newmax<max) {
	    for (int i=max; i>newmax; --i)
		solutions[i]=0;
	    max=newmax;
	}
	// NOTE: mines[][] has not been zeroed out (but that's ok)
    }

    /**
     * Find the best place to probe (lowest probability of mine).  
     * Note: the best position itself is stored interally so doBestProbe() 
     * does not require a recalculation of the best position.
     * @return probability of mine at best location
     */
    public float findBestProbe() {
	int total_solutions=0;
	for (int j=min; j<=max; ++j)
	    total_solutions+=solutions[j];
	int best=total_solutions;
	for (int i=0; i<nvariables; ++i) {
	    int total=0;
	    for (int j=min; j<=max; ++j)
		total += mines[j][i];
	    if (total<best) {
		best=total;
		bestProbe=variables[i];
	    }
	}
	return best/(float)total_solutions;
    }

    /**
     * Probe at the best position (lowest probability of mine).
     * @param map current game
     * @return a new constraint if the probe was successful, null otherwise
     */
    public Constraint doBestProbe(Map map) {
	if (bestProbe==null)
	    findBestProbe();
	int s = bestProbe.probe(map);
	return s>=0 ? bestProbe.newConstraint() : null;
    }
    
    /**
     * A craps shoot is required if the following conditions hold:
     *   all solutions of this CSP require exactly the same number of mines,
     *   and all neighbours of variables[] have a known value (mine or clear).
     *
     * This situation is called a craps shoot because there is no posibility
     * of new information changing our computed probabilities and in the
     * majority of cases the probability of a mine in each position is 50%.  
     *
     * If we are not in a craps shoot situation, this method does nothing 
     * and returns null, otherwise it probes an appropriate position.
     * @param map current game
     * @return a new constraint if a position was probed and the probe was successful, null otherwise
     */
    public Constraint doCrapsShoot(Map map) {
	if (min!=max)
	    return null;
	for (int i=0; i<nvariables; ++i)
	    if (!variables[i].neighboursKnownOrInSet(variables,nvariables))
		return null;
	// figure out best choice (and mark for sure mines when found)
	int best = -1;
	int bestcount = solutions[min];
	for (int i=0; i<nvariables; ++i) {
	    if (mines[min][i]<bestcount) {
		bestcount = mines[min][i];
		best=i;
	    }
	    else if (mines[min][i]==solutions[min])
		// for-sure mine
		variables[i].mark(map);
	}

	if (best<0)
	    // must be all mines
	    return null;

	if (bestcount==0)
	    // for-sure clear
	    variables[best].probe(map);
	
	else {
	    if (VERBOSE)
		System.out.print("GUESS: "+(100-100*bestcount/solutions[min])+
				 "% CRAPS ...");
	    int s = variables[best].probe(map);
	    if (s<0) {
		if (VERBOSE)
		    System.out.println(" FAILED!");
		return null;
	    }
	    if (VERBOSE)
		System.out.println(" YEAH!");
	}
	
	return variables[best].newConstraint();
    }

    /**
     * If any variables are 1 in every solution found, they are marked
     * as mines.
     * @param map current game
     */
    public void markMines(Map map) {
	int total_solutions=0;
	for (int j=min; j<=max; ++j)
	    total_solutions+=solutions[j];
	for (int i=0; i<nvariables; ++i) {
	    float total=0;
	    for (int j=min; j<=max; ++j)
		total += mines[j][i];
	    if (total==total_solutions)
		variables[i].mark(map);
	}
    }

    /**
     * Solve the Constraint Satisfaction Problem (CSP) using a backtracking
     * technique.  All solutions to the problem will be found and statistics
     * about the solutions (min mines, max mines, etc.) will be recorded.
     *
     * The following technique is used:
     *   - Variables are assigned values one by one in a particular order.
     *   - Variables that are known (from one of their constraints) to be
     *     restricted to one value or the other are choosen first (when
     *     available).
     *   - Otherwise, variables are chosen in order of their level of 
     *     constraint.  That is, variables that appear in a large number
     *     of constraints are chosen before those that appear in few.
     *   - Each variable is tested with 0 and 1.  After each test assignment
     *     to a variable, all constraints which have that variable in them
     *     are checked for possible violation.  If a constraint is found
     *     to be unsatisfied by an assignment, the other possible assignment
     *     is tested and if that also doesn't work, backtracking occurs
     *     immediately.
     *  
     * This method makes use of testAssignment in the BoardPosition instance
     * variables as well as some state in the constraint variables.  Therefore,
     * two threads should not call enumerateSolutions() simultaneously if there
     * is any overlap in positions or constraints.
     *
     * Note: no attempt is made to intelligently jump back several levels.
     * The current algorithm is sufficient for our needs, but further
     * optimization is possible.
     */
    public void enumerateSolutions() {
	// initialize counters
	for (int i=0; i<solutions.length; ++i) {
	    solutions[i]=0;
	    for (int j=0; j<nvariables; ++j)
		mines[i][j]=0;
	}

	// initialize all variables to unset
	for (int i=0; i<nvariables; ++i)
	    variables[i].testAssignment=-1;

	// index to variable used at each level
	int[] variableindex = new int[nvariables];
	for (int i=0; i<nvariables; ++i)
	    variableindex[i]=-1;

	// last choice of variable by constrainedness
	int lastchoice=-1;

	// initialize constraints
	for (int i=0; i<nconstraints; ++i)
	    constraints[i].updateVariable(null);
	
	// main loop
	int level=0;
	do {
	    if (level==nvariables) {
		// all variables assigned, enumerate solution
		int m=0;
		for (int j=0; j<nvariables; ++j)
		    m += variables[j].testAssignment;
		++solutions[m];
		if (m<min) min=m;
		if (m>max) max=m;
		for (int j=0; j<nvariables; ++j)
		    mines[m][j] += variables[j].testAssignment;
		// go up
		--level;
		continue;
	    }

	    if (variableindex[level]<0) {
		// pick next variable
		BoardPosition var = null;
		for (int i=0; (var==null)&&(i<nconstraints); ++i)
		    var = constraints[i].suggestUnassignedVariable();
		if (var!=null) {
		    // find suggested variable
		    variableindex[level]=nvariables;
		    while (!variables[--variableindex[level]].equals(var)) ;
		    --var.testAssignment; // we re-increment it below
		}
		else {
		    // find next most constrained variable
		    while (variables[++lastchoice].testAssignment>=0) ;
		    variableindex[level]=lastchoice;
		}
	    }
	    
	    if (variables[variableindex[level]].testAssignment>0) {
		// domain exhausted, reset assignment and go up
		if (variableindex[level]<=lastchoice)
		    lastchoice=variableindex[level]-1;
		variables[variableindex[level]].testAssignment=-1;
		nodes[variableindex[level]].updateConstraints();
		variableindex[level]=-1;
		--level;
	    }
	    
	    else {
		// try next value in domain
		++variables[variableindex[level]].testAssignment;

		// update all constraints that have this variables
		nodes[variableindex[level]].updateConstraints();
		
		// check constraints
		if (nodes[variableindex[level]].checkConstraints())
		    // go down if constraints are satisfied
		    ++level;
	    }

	} while (level>=0);

	// check if this was the largest system solved
	if (nvariables-nconstraints>largest_nvars-largest_neqns) {
	    largest_nvars=nvariables;
	    largest_neqns=nconstraints;
	    largest_nsols=0;
	    for (int i=min; i<=max; ++i)
		largest_nsols += solutions[i];
	}
    }
}
