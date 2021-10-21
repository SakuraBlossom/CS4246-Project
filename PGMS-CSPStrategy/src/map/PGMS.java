package map;

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

/* Copyright (C) 1995 and 1997 John D. Ramsdell

This file is part of Programmer's Minesweeper (PGMS).

PGMS is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

PGMS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with PGMS; see the file COPYING.  If not, write to
the Free Software Foundation, 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.
*/

/** 
 * The class PGMS creates and displays minesweeper games played by
 * Java programs.  A user contributes a Java class that conforms to
 * the interface called Strategy.  The strategy plays a game of 
 * minesweeper using the methods provided by the Map class. <p> 
 * This class provides both an applet for graphical presentations
 * using browsers, and a main routine for applications.
 * @see Strategy
 * @see map.Map
 * @version February 1997
 * @author John D. Ramsdell
 */ 
public class PGMS extends Applet {
    // The default strategy
    private static final String default_strategy_name = "sp.SinglePointStrategy";
    private static final String csp_strategy = "csp.CSPStrategy";
    Strategy s;			// The selected strategy
    int mines = 10;		// Beginner game
    int rows = 8;
    int columns = 8;
    boolean hinted = false;
    boolean realrules = false;
    DisplayMap m;			// Panel for map display 
  
    /**
     * Application entry point.
     * @param args        program arguments
     * <dl>
     * <dt> <code>-b</code>
     * <dd> play a beginner game
     * <dt> <code>-i</code>
     * <dd> play an intermediate game
     * <dt> <code>-e</code>
     * <dd> play an expert game
     * <dt> <code>-r</code> <var>number of rows</var>
     * <dd> custom number of rows
     * <dt> <code>-c</code> <var>number of columns</var>
     * <dd> custom number of columns
     * <dt> <code>-m</code> <var>number of mines</var>
     * <dd> custom number of mines
     * <dt> <code>-R</code>
     * <dd> use real rules (cannot loose on first probe)
     * <dt> <code>-H</code>
     * <dd> play hinted game
     * <dt> <code>-s</code> <var>strategy class name</var>
     * <dd> play with given strategy
     * <dt> <code>-n</code> <var>number of games</var>
     * <dd> play multiple games - graphics will be disabled 
     * with more than one game 
     * <dt> <code>-S</code> <var>number of sets</var>
     * <dd> play multiple sets of multiple games
     * </dl>
     */
    public static void main(String[] args) {
	String strategy_name = csp_strategy;
	String game_name = "beginner";
	int mines = 80;		// Beginner game
	int rows = 20;
	int columns = 20;
	boolean hinted=false;
	boolean realrules=false;
	int trys = 10000; // PARAMETER: Set how many runs here
	int sets = 1; // PARAMETER

	for (int i = 0; i < args.length; i++) // Process args
		switch (args[i]) {
			case "-H":
				hinted = true;
				break;
			case "-R":
				realrules = true;
				break;
			case "-i":
				game_name = "intermediate";
				mines = 40;            // Intermediate game

				rows = 13;
				columns = 15;
				break;
			case "-e":
				game_name = "expert";
				mines = 99;            // Expert game

				rows = 16;
				columns = 30;
				break;
			case "-b":
				game_name = "beginner";
				mines = 10;            // Beginner game

				rows = 8;
				columns = 8;
				break;
			case "-r":
				if (++i >= args.length) {       // custom number of rows
					usage();
					return;
				} else
					rows = Integer.parseInt(args[i]);
				break;
			case "-c":
				if (++i >= args.length) {       // custom number of columns
					usage();
					return;
				} else
					columns = Integer.parseInt(args[i]);
				break;
			case "-m":
				if (++i >= args.length) {       // custom number of mines
					usage();
					return;
				} else
					mines = Integer.parseInt(args[i]);
				break;
			case "-s":
				if (++i >= args.length) { // User supplied strategy
					usage();
					return;
				} else
					strategy_name = args[i];
				break;
			case "-n":
				if (++i >= args.length) { // Game count supplied
					usage();
					return;
				} else
					try {
						trys = Math.max(1, Integer.parseInt(args[i]));
					} catch (NumberFormatException e) {
						System.out.println("Bad number of games");
						usage();
						return;
					}
				break;
			case "-S":
				if (++i >= args.length) {    // number of sets
					usage();
					return;
				} else
					try {
						sets = Math.max(1, Integer.parseInt(args[i]));
					} catch (NumberFormatException e) {
						System.out.println("Bad number of sets");
						usage();
						return;
					}
				break;
			default:
				usage();
				return;
		}

	// double check parameters
	if (rows < 1 || columns < 1 || mines < 1) {
	    usage();
	    return;
	}
	
	Strategy s;

	try {
	    s = (Strategy) Class.forName(strategy_name).newInstance();
	} catch (Exception e) {
	    System.out.println("Cannot create strategy " + strategy_name);
	    usage();
	    return;
	}

	if (trys == 1) {
	    Frame f = new Frame("PGMS");
	    f.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			System.exit(0);
		    }
		});

	    PGMS p = new PGMS(s, mines, rows, columns, realrules, hinted);

	    MenuBar mb = new MenuBar();
	    f.setMenuBar(mb);
	    Menu m = new Menu("File");
	    mb.add(m);
	    MenuItem mi = new MenuItem("Exit", new MenuShortcut(KeyEvent.VK_X));
	    mi.addActionListener(e -> System.exit(0));
	    m.add(mi);

	    p.init_display(strategy_name); 

	    f.add(p);
	    f.pack();
	    f.show();
      
	    p.start();
	    return;
	}
    
	System.out.print("Playing " + trys*sets + " " + game_name + " games");
	System.out.println(" using strategy " + strategy_name);
    
	int sum=0;
	long sumsqr=0;
	double totaltime=0;
	double sqrtime=0;
	int game_count=0;
	for (int set = 1; set<=sets; ++set) {
	    System.out.println("** Set: "+set);
	    int wins = 0;
	    for (int n = 1; n <= trys; n++) {
		MineMap m = new MineMap(mines, rows, columns, realrules); // Create mine map
		long start_time = ProcessTime.clock();
		try {
		    if (!hinted)
			s.play(m);			// Play game
		    else {
			int[] hint = m.hint();
			s.play(m,hint[0],hint[1]);
		    }
		} catch (Exception e) {
		    //System.out.println(e.toString());
		    e.printStackTrace();
		}
		long end_time = ProcessTime.clock();
		if (end_time>=start_time) {
		    double play_time = (end_time-start_time)/
			(double)ProcessTime.CLOCKS_PER_SEC;
		    totaltime+=play_time;
		    sqrtime+=play_time*play_time;
		    ++game_count;
		}
		else {
		    System.err.println("ERROR: clock() wrap-around "+
				       start_time+" "+end_time);
		}
		if (m.won()) wins++;	// Record results
		System.out.println(wins
				   + " wins in "
				   + n
				   + " trys -- "
				   + percent(wins, n)
				   + "%.");
	    }
	    sum += wins;
	    sumsqr += (long) wins * wins;
	}
	System.out.println("****************");
	System.out.println("In "+sets+" sets of "+trys+" trys ("+sets*trys+
			   " games total):");
	double mean = sum/(double)sets;
	System.out.println("  Mean wins: "+mean+"/"+trys+" ("+
			   (int)(100.0*mean/trys+0.5)+"%)");
	double var = (sumsqr-sum*sum/(double)sets) / sets;
	System.out.println("  Standard Deviation: "+Math.sqrt(var));
	System.out.println("  Standard Error of the Mean: "+
			   Math.sqrt(var/sets));

	System.out.println("  Mean time: "+totaltime/sets/trys+" sec");
	var = (sqrtime-totaltime*totaltime/sets/trys) /sets/trys;
	System.out.println("  Standard Deviation: "+Math.sqrt(var));
	System.out.println("  Standard Error of the Mean: "+
			   Math.sqrt(var/sets/trys));
    }

    /**
     * Create a PGMS instance for an application
     */
    PGMS(Strategy s, int mines, int rows, int columns, boolean realrules, boolean hinted) {
	this.s = s;
	this.mines = mines;
	this.rows = rows;
	this.columns = columns;
	this.hinted = hinted;
	this.realrules = realrules;
    }

    private static int percent(int n, int d) {
	return (200 * n + d) / (2 * d);
    }
  
    private static void usage() {
	System.out.println("Usage: java PGMS [-b] [-i] [-e]"
			   + " [-r rows] [-c columns] [-m mines]"
			   + " [-H] [-R] [-S sets]"
			   + " [-s strategy_name] [-n number_of_games]");
	System.out.println("Beginner:     -b");
	System.out.println("Intermediate: -i");
	System.out.println("Expert:       -e");
	System.out.println("Hinted Game:  -H");
    }

    /**
     * Create applet PGMS
     */
    public PGMS() { }

    /**
     * Initialize applet by processing the attributes.
     * <dl>
     * <dt> <code>strategy</code>
     * <dd> class name of user supplied strategy
     * <dt> <code>game</code>
     * <dd> level of game, one of
     * <ul> 
     * <li> <code>beginner</code>
     * <li> <code>intemediate</code>
     * <li> <code>expert</code>
     * </ul>
     * </dl>
     * <p>
     * Sample:
     * <pre>
     * &lt;applet codebase="classes" code="map/PGMS.class" 
     *       width=302 height=262&gt;
     * &lt;param name="strategy" value="eqn.EqnStrategy"&gt;
     * &lt;param name="game" value="intermediate"&gt;
     * &lt;/applet&gt;
     * </pre>
     */
    public void init() {
	String strategy_name = getParameter("strategy");
	String game = getParameter("game");
    
	if (strategy_name == null)
	    strategy_name = default_strategy_name;
    
	// Create strategy
	try {
	    s = (Strategy)Class.forName(strategy_name).newInstance();
	} catch (Exception e) {
	    return;
	}
    
	if (game != null) {		// Set game level
	    if (game.equals("intermediate")) {
		mines = 40;			// Intermediate game
		rows = 13;
		columns = 15;
	    }
	    else if (game.equals("expert")) {
		mines = 99;			// Expert game
		rows = 16;
		columns = 30;
	    }
	}
	init_display(strategy_name);
    }

    /**
     * This routine creates the panels that make up the display.
     */
    private void init_display(String strategy_name) {
	final int gap = 5;
	setLayout(new BorderLayout());
	setBackground(Color.lightGray);
    
	/* The status panel displays status information  */
	final Label status = new Label("Strategy: " + strategy_name,
				       Label.CENTER);
	add(status, "South");

	/* The tally panel displays the number of
	   mines minus the number of marks. */
	Label tally = new Label("999", Label.CENTER);
	tally.setBackground(Color.white);

	/* This panel displays the map. */
	Panel map_panel = new Panel(new FlowLayout(FlowLayout.CENTER, gap, 0));
	final DisplayMap display = new DisplayMap(s, mines, rows, columns, realrules, hinted,
						  status, tally);
	display.setBackground(Color.white);
	display.init();
	m = display;		// Save DisplayMap for the start and stop method
	map_panel.add(display);

	/* This panel contains the tally and the control buttons. */
	Panel button_panel = new Panel(new FlowLayout(FlowLayout.LEFT, gap, 0));

	button_panel.add(tally);

	Button b = new Button("Start");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    display.start();
		}
	    });
	button_panel.add(b);

	b = new Button("Stop");
	b.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    display.stop();
		}
	    });
	button_panel.add(b);

	Panel p = new Panel(new ColumnLayout(0, gap));
	p.add(button_panel);
	p.add(map_panel);
	add(p, "Center");
    }

    public void start() {
	m.resume();
    }
  
    public void stop() {
	m.stop();
    }
  
}
