package csp;
import map.Map;

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
 * Class to represent a single constraint.  A constraint consists of a bunch
 * of boolean variables (0 or 1) summed together on one side of an equal
 * sign with an integer constant on the other side.  The number of variables
 * will usually be in the range 1 to 8 and thus the constant should also be 
 * in this range.  
 *
 * Two special cases: if the constant is 0, all variables must be 0; and if 
 * the constant equals the number of variables, all variables must be 1.
 *
 * Every clear board position will have one constraint associated with it.  
 * The method BoardPosition.newConstraint() should be used to automatically
 * construct these constraints.  A constraint is usually "thrown away" once
 * all its variables are known (either 0 or 1).
 *
 * @see CSPStrategy
 * @version March 2001
 * @author Chris Studholme
 */
final class Constraint {

    /**
     * Initial size of variables[] array.
     */
    private static final int INIT_ARRAY_SIZE = 8;

    /**
     * Array of variables on RHS.
     */
    private BoardPosition[] variables;
    /**
     * Number of (non-null) variables in variables[].
     */
    private int nvariables;

    /**
     * Constant on LHS.
     */
    private int constant;

    /**
     * Number of unassigned variables.  
     * Used as optimization when SolutionSet is enumerating solutions.
     */
    private int unassigned;
    /**
     * constant minus number of variables assigned 1.
     * Used as optimization when SolutionSet is enumerating solutions.
     */
    private int current_constant;
    /**
     * An unassigned variable.
     * Used as optimization when SolutionSet is enumerating solutions.
     */
    private BoardPosition next_unassigned;
    
    /**
     * Construct empty constraint.
     */
    public Constraint() {
	nvariables=0;
	variables = new BoardPosition[INIT_ARRAY_SIZE];
	constant=0;
    }

    /**
     * Construct empty constraint with particular constant.
     * @param constant constant to set
     */
    public Constraint(int constant) {
	nvariables=0;
	variables = new BoardPosition[Math.max(constant, INIT_ARRAY_SIZE)];
	this.constant=constant;
    }

    /**
     * Creates descriptive string for constraint of the form:
     *   "constant=var1+var2+...+varn"
     * or "[EMPTY CONSTRAINT]" if there are no variables.
     * @return descriptive string for instance
     */
    public String toString() {
	if (nvariables<=0)
	    return "[EMPTY CONSTRAINT]";
	StringBuffer s = new StringBuffer(constant+"=");
	s.append(variables[0].toString());
	for (int i=1; i<nvariables; ++i)
	    s.append("+"+variables[i].toString());
	return s.toString();
    }

    /**
     * Add a variable to the constraint.
     * @param c variable to add
     */
    public void add(BoardPosition c) {
	if (variables.length<=nvariables) {
	    // reallocate variables
	    BoardPosition[] newvars = new BoardPosition[variables.length*2];
	    System.arraycopy(variables,0,newvars,0,nvariables);
	    variables=newvars;
	}
	variables[nvariables++]=c;
    }

    /**
     * Get array of variables.  The length of the array is equal to the
     * number of (non-null) variables in the array.
     * @return array of variables
     */
    public BoardPosition[] getVariables() {
	if (variables.length==nvariables)
	    return variables;
	BoardPosition[] result = new BoardPosition[nvariables];
	System.arraycopy(variables,0,result,0,nvariables);
	return result;
    }

    /**
     * Get count of variables.
     * @return variable count
     */
    public int getVariableCount() {
	return nvariables;
    }

    /**
     * Set constant.
     * @param c new constant
     */
    public void setConstant(int c) {
	constant=c;
    }

    /**
     * Get constant.  Also checks for bad constants (<0) and writes to
     * stderr if found.
     * @return constant
     */
    public int getConstant() {
	if (constant<0)
	    System.err.println("BAD CONSTANT "+toString());
	return constant;
    }

    /**
     * Check if constraint is empty.
     * @return true if no variables
     */
    public boolean isEmpty() {
	return nvariables<=0;
    }

    /**
     * Updates internal state to reflect a new test assignment to a particular
     * variable.  This method is used by SolutionSet when enumerating
     * solutions.
     * @param var hint of which variable changed
     */
    public final void updateVariable(BoardPosition var) {
	// doesn't use var for the moment
	current_constant=0;
	unassigned=0;
	next_unassigned=null;
	for (int i=0; i<nvariables; ++i) {
	    if (variables[i].testAssignment<0) {
		next_unassigned=variables[i];
		++unassigned;
	    }
	    else if (variables[i].testAssignment>=1)
		++current_constant;
	}
    }

    /**
     * Check if constraint is satisfied (for the moment).  A return of false
     * indicates that there is definately something wrong with the current
     * test assignments to the variables, but a return of true only
     * indicates that those variables that have test assignments are fine.
     * Test assignments to the remaining unassigned variables may still
     * cause the constraint to become unsatisfied.
     * @return true if there are unassigned variables
     */
    public final boolean isSatisfied() {
	if (current_constant>constant)
	    return false;
	if (unassigned>0)
	    return true;
	return (current_constant==constant);
    }
    
    /**
     * If there are currently variables without test assignments and it is
     * clear (given the variables that do have test assignments) that either 
     * all of the unassigned variables must be 0 or all of the unassigned
     * variables must be 1, then one of the unassigned variables will be
     * returned.  In this case, the testAssignment instance variable will be 
     * set to either 0 or 1 depending on what value the variable must have.
     * @return a suggested unassigned variable or null if none
     */
    public final BoardPosition suggestUnassignedVariable() {
	if (next_unassigned==null)
	    return null;
	if (current_constant==constant) { 
	    // all mines accounted for (only 0's left)
	    next_unassigned.testAssignment=0;
	    return next_unassigned;
	}
	if (constant-current_constant==unassigned) { 
	    // all remaining vars are mines (1's)
	    next_unassigned.testAssignment=1;
	    return next_unassigned;
	}
	return null;
    }

    /**
     * There are three cases handled by this method:
     *   1. if any variables have known values (either 0 or 1), the
     *      variables is removed and constant is updated as needed,
     *   2. if constant==0, all variables must be 0 so they are all
     *      probed and an array of new constraints is returned,
     *   3. if constant==number of variables, all variables must be 1
     *      so they are all marked as mines.  In this case an array
     *      of zero length is returned.
     *
     * Case 1 is always handled before 2 and 3.  If neither 2 or 3 apply,
     * null is returned (regardless of whether something was done for
     * case 1 or not).
     *
     * This instance may be an empty constraint at the return from this
     * method.
     * @param map current game
     * @return array of new constraints or null if no new MARKS or CLEARS
     */
    public Constraint[] updateAndRemoveKnownVariables(Map map) {
	// first check for previously known values
	for (int i=nvariables-1; i>=0; --i) {
	    int s = variables[i].getState();
	    if (s>=0)
		// clear (remove variable)
		variables[i]=variables[--nvariables];
	    else if (s==BoardPosition.MARKED) {
		// marked (remove variable and decrement constant)
		variables[i]=variables[--nvariables];
		--constant;
	    }
	}
	
	// if no variables left, return
	if (nvariables<=0)
	    return null;

	// check for all clear or all marked
	Constraint[] result;
	if (constant==0) {
	    // all variables are 0 (no mines)
	    result = new Constraint[nvariables];
	    for (int i=0; i<nvariables; ++i) {
		variables[i].probe(map);
		result[i] = variables[i].newConstraint();
	    }
	}
	else if (constant==nvariables) {
	    // all variables are 1 (are mines)
	    result = new Constraint[0];
	    for (int i=0; i<nvariables; ++i)
		variables[i].mark(map);
	}
	else 
	    return null;

	// empty constraint
	nvariables=0;
	constant=0;
	return result;
    }

    /**
     * Attempt to simplify either this constraint or other constraint.  If
     * this constraint's variables are a subset of other's, or vice versa,
     * this constraint's variables will be removed from other's, or vice
     * versa, and the constants will be updated as needed.
     * 
     * Example: given 2=a+b+c+d and 1=b+c, the former constraint will be
     * simplified to 1=a+d, while 1=b+c is left alone.  This method doesn't
     * care which constraint is which.  If the two constraints are the same
     * (same variables, same constant), "this" will become empty.
     * @param other another constraint
     * @return true if this constraint or other constraint was simplified
     */
    public boolean simplify(Constraint other) {
	if (nvariables<other.nvariables)
	    // Are we a subset of other?  Let other figure it out.
	    return other.simplify(this);

	// Is other a subset of us?
	for (int i=0; i<other.nvariables; ++i)
	    for (int j=0; j<nvariables; ++j)
		if (variables[j].equals(other.variables[i]))
		    break;
		else if (j>=nvariables-1)
		    return false;

	// remove other's variables from this
	for (int i=0; i<other.nvariables; ++i)
	    for (int j=0; j<nvariables; ++j)
		if (variables[j].equals(other.variables[i])) {
		    variables[j]=variables[--nvariables];
		    break;
		}
	constant -= other.constant;
	return true;
    }

    /**
     * Determine if this constraint has any variables in common with
     * another constraint.
     * @param other another constraint
     * @return true if the constraints are coupled
     */
    public boolean coupledWith(Constraint other) {
	for (int i=0; i<other.nvariables; ++i)
	    for (int j=0; j<nvariables; ++j)
		if (variables[j].equals(other.variables[i]))
		    return true;
	return false;
    }
}
