package csp;
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
 * List of constraints that all have a particular variable in them.  Used
 * by SolutionSet to help optimize the enumerating of solutions.
 *
 * @see CSPStrategy
 * @version March 2001
 * @author Chris Studholme
 */
final class ConstraintList implements Comparable {
    /**
     * A particular board position (variable).
     */
    public BoardPosition variable;
    /**
     * Array of constraints containing the variable.
     */
    public Constraint[] constraints;
    /**
     * Number of (non-null) constraints in array.
     */
    public int nconstraints;

    /**
     * Construct list with an initial constraint and common variable.
     * @param c constraint
     * @param b common variable
     */
    public ConstraintList(Constraint c, BoardPosition b) {
	constraints = new Constraint[2];
	constraints[0]=c;
	nconstraints=1;
	variable=b;
    }

    /**
     * Add a constraint.  The constraint must have variable in it.
     * @param c constraint
     */
    public void addConstraint(Constraint c) {
	if (constraints.length<=nconstraints) {
	    // reallocate constraints
	    Constraint[] newconstraints = new Constraint[nconstraints*2];
	    System.arraycopy(constraints,0,newconstraints,0,nconstraints);
	    constraints=newconstraints;
	}
	constraints[nconstraints++] = c;
    }

    /**
     * Update all constraints given that variable has a new test assignment.
     */
    public final void updateConstraints() {
	for (int i=0; i<nconstraints; ++i)
	    constraints[i].updateVariable(variable);
    }

    /**
     * Check if all constraints are satisfied (or at least think they are).
     * @return false if any constraint is not satisfied
     */
    public final boolean checkConstraints() {
	for (int i=0; i<nconstraints; ++i)
	    if (!constraints[i].isSatisfied())
		return false;
	return true;
    }

    /**
     * Compare this list to another based on the number of constraints in
     * each one's list.
     * @param o another ConstraintList instance
     * @return 1 if this has fewer constraints than o, -1 if more, 0 if equal
     */
    public int compareTo(Object o) {
	if (nconstraints<((ConstraintList)o).nconstraints)
	    return 1;
	else if (nconstraints>((ConstraintList)o).nconstraints)
	    return -1;
	return 0;
    }
}
