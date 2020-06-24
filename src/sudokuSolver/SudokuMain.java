package sudokuSolver;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import sudokuSolver.SudokuMain.ActionMgr.*;
import sudokuSolver.SudokuMain.NumMgr.*;

/**
 * SudokuMain.java
 * CSCI185.sudokuSolver
 * 
 * CSCI 185 M02
 * File ID: FP001
 * Feb 22, 2016 6:05:28 PM
 * @author Hasol Im
 * Instructor: Professor. Simon Ben-Avi
 * 
 * Auto-Generated Comment Format Ver 1.4
 */
// NOTE: Due Monday May 9th.
public class SudokuMain
{
	// directory of project folder.
	final static String dir = common.Const.PROJECT_DIR;
	
	// JFrame, button, label for display.
	static JFrame mainFrame;
	static Graphics mainGraphic;
	static JButton newBtn, resetBtn, fillBtn;
	static JButton stepBtn, solveBtn, checkBtn;
	static JButton saveBtn, infoBtn, optionBtn;
	static JButton markBtn, autoBtn, clearBtn;
	static JButton[] numBtn;
	static JLabel gridLbl;
	static JLabel[] lineLbl;
	final static int CellSizeX = 60, CellSizeY = 60;
	static int OffsetX, OffsetY; //final static int OffsetX = 12, OffsetY = 35;
	final static int BoxGap = 2;
	
	
	// array to store sudoku data.
	static int[][] puzzle; // puzzle itself.
	static int[][] original, solution, temporary; // copy of puzzle for storage.
	static int[][] possible; // possible combinations
	static boolean[] row, column, box; // true if solved.
	static boolean isGenerated;
	static int[] count; // number of filled, given, remaining items.
	final static int SIZE = 9, TOTAL = 81;
	static int[] select; // index of selected cells and modes.
	
	// randomizer.
	static Random rnd;
	
	/**
	 * Main Method
	 * @param args
	 */
	public static void main (String[] args)
	{
		select = new int[6];
		// initialize application.
		mainFrame = new JFrame();
		mainGraphic = mainFrame.getGraphics ();
		initVar();
		FrameMgr.initFrame();
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// initialization section.
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Initialize variables
	 */
	public static void initVar ()
	{
		// initialize all arrays.
		puzzle = new int[SIZE][SIZE];
		original = new int[SIZE][SIZE];
		solution = new int[SIZE][SIZE];
		possible = new int[SIZE][SIZE];
		temporary = new int[SIZE][SIZE];
		row = new boolean[SIZE];
		column = new boolean[SIZE];
		box = new boolean[SIZE];
		count = new int[3];
		select[0] = select[1] = -1;
		isGenerated = false;
		
		// set randomizer.
		rnd = new Random();
		
		SudokuMgr.countPuzzle ();
	}
	
	
	
	/**
	 * Class to generate and solve puzzle.
	 * @author hasol
	 */
	public static class SudokuMgr
	{
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		// puzzle mechanics section.
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		
		/**
		 * Initialize puzzle for new use.
		 * Number of elements generated depends on difficulty.
		 * @param diff difficulty of puzzle.
		 */
		public static void initPuzzle (int diff)
		{
			do
			{
				// initialize puzzle to 0.
				ArrayMgr.fill (puzzle, 0);
				// initialize possible solution to 0b111111111 (or 0d511).
				ArrayMgr.fill (possible, 0b111111111);
				// initialize boolean arrays.
				Arrays.fill (row, false);
				Arrays.fill (column, false);
				Arrays.fill (box, false);
				
				// do randomization. if fails, re-do.
			} while (!randomizePuzzle (diff));
			// save original copy.
			puzzle = ArrayMgr.clone (original);
			countPuzzle();
			updatePossible();
			isGenerated = true;
		}
		
		/**
		 * randomize the puzzle with given difficulty
		 * @param diff difficulty of puzzle.
		 */
		public static boolean randomizePuzzle (int diff)
		{
			// set number of given elements (count[1])
			switch (diff)
			{
				case 5: // very hard
					count[1] = 17 + rnd.nextInt (3);
					break;
				case 4: // hard
					count[1] = 20 + rnd.nextInt (5);
					break;
				case 3: // normal
					count[1] = 25 + rnd.nextInt (4);
					break;
				case 2: // easy
					count[1] = 29 + rnd.nextInt (5);
					break;
				case 1: // very easy
					count[1] = 34 + rnd.nextInt (3);
					break;
				case 0: // for training
					count[1] = 37 + rnd.nextInt (44);
					break;
				default: // completed
					count[1] = TOTAL;
					break;
			}
			
			countPuzzle();
			if (solvePuzzle (count[1]))
			{
				original = ArrayMgr.clone (puzzle);
				return solvePuzzle();
			}
			else
			{
				return false;
			}
		}
		
		/**
		 * counts number of items present in the puzzle.
		 */
		public static void countPuzzle ()
		{
			count[0] = 0;
			Runnable.repeatAll (new Runnable() {
				public boolean runB() {
					if (puzzle[set.i][set.j] > 0)
					{
						count[0] ++;
					}
					return false;
				}
			});
			count[2] = TOTAL - count[0];
		}
		
		/**
		 * solve puzzle for specified steps
		 * @param n number of steps to solve.
		 * @return true if successful, otherwise, false.
		 */
		public static boolean solvePuzzle (int n)
		{
			n += count[0];
			while (count[0] < n)
			{
				if (checkStatus())
				{
					// if condition is not met, but there's no possible option, terminate.
					return false;
				}
				
				if (!refreshPuzzle())
				{
					// if cannot be solved, pick a random number.
					NumberSet s = new NumberSet(SIZE);
					
					while (possible[s.i][s.j] <= 0)
					{
						s.generateLocation ();
					}
					s.compareWith (possible[s.i][s.j]);
					
					// then assign to array.
					puzzle[s.i][s.j] = s.n;
					// and subtract that element's bit from possible array.
					refreshPossible (s);
					checkPossible (s);
				}
				// if a cell can be filled, increment counter.
				count[0] ++;
				count[2] --;
			}
			return true;
		}
		
		/**
		 * solve puzzle to the end.
		 * @return true if successful, otherwise, false
		 */
		public static boolean solvePuzzle ()
		{
			updatePossible();
			countPuzzle();
			temporary = ArrayMgr.clone (puzzle);
			//return solvePuzzle (count[2]);
			/*
			if (solvePuzzle (count[2]))
			{
				// save completed copy.
				solution = ArrayMgr.clone (puzzle);
				return true;
			}
			else
			{
				return false;
			} */
			// TODO: use return above, assuming perfect solution.
			// Otherwise, try repeated check.
			final int LIMIT = 5;
			int COUNT = 0;
			while (COUNT < LIMIT)
			{
				if (solvePuzzle(count[2]))
				{
					// save completed copy.
					solution = ArrayMgr.clone (puzzle);
					return true;
				}
				else
				{
					// if test fails, revert to original state, and repeat.
					puzzle = ArrayMgr.clone (temporary);
					countPuzzle();
					updatePossible();
					COUNT ++;
				}
			}
			return false;
		}
		
		/**
		 * solve the puzzle only once. this method should be followed by completed instances of checkOnce()
		 * @return true if any element is filled. otherwise, false.
		 */
		public static boolean solveOnce ()
		{
			// use repeatAll() to break loop when true is found.
			if (Runnable.repeatAll (new Runnable() {
				// define Runnable for repeatAll()
				public boolean runB() {
					///////////////////////////////////////
					// Sole Candidate test.
					///////////////////////////////////////
					int b = NumMgr.getBitCount (possible[set.i][set.j]);
					// for each element of possible array, proceed if it has only one bit.
					// meaning only one possible answer can be taken.
					if (b == 1)
					{
						// return true and break loop.
						return true;
					}
					// place for more test.
					
					// if not found, continue to next element.
					return false;
				}
			}))
			{
				NumberSet set = new NumberSet ();
				do
				{
					set.generateLocation ();
				} while (NumMgr.getBitCount (possible[set.i][set.j]) != 1);
				
				// find the possible answer.
				set.n = NumMgr.getLogTwo (possible[set.i][set.j]) + 1;
				puzzle[set.i][set.j] = set.n;
				// save, and update.
				refreshPossible (set);
				checkPossible (set);
				
				return true;
			}
			// place for more test.
			return false;
		}
		
		/**
		 * check possible array one with given level. this method should be used in decrementing loop.
		 * @param l level of check.
		 * @return true if any new action is taken. otherwise, false.
		 */
		public static boolean checkOnce (int l)
		{
			// define Runnable for repeatAll()
			if (Runnable.repeatAll (new Runnable() {
				// variables to use.
				int t, s;
				boolean chk;
				Runnable body1, body2;
				public boolean runB() {
					// signature of selected possible array.
					s = possible[set.i][set.j];
					// proceed only if signature is equal to level of check.
					if (NumMgr.getBitCount (s) == l)
					{
						///////////////////////////////////////
						// Naked Subset test.
						///////////////////////////////////////
						
						// define second Runnable for repeated check.
						body1 = new Runnable() {
							public boolean runB() {
								// compare signature with that of newly selected element.
								if (s == possible[set.i][set.j])
								{
									// of they matches, increment counter.
									t++;
								}
								// always return false for complete loop.
								return false;
							}
						};
						// define third Runnable for repeated assignment.
						body2 = new Runnable() {
							public boolean runB() {
								// compare signature with that of newly selected element.
								if (s != possible[set.i][set.j] && (s & possible[set.i][set.j]) > 0)
								{
									// if they do not match partially (not equal, but with overlapping bits),
									// then remove overlapping bits.
									possible[set.i][set.j] &= NumMgr.getInverse(s);
									chk = true;
								}
								// always return false for complete loop.
								return false;
							}
						};
						// initialize, and perform row test.
						t = 0;
						chk = false;
						repeatRow (body1, set.j);
						if (t == l)
						{ // proceed only if counted element equals check level.
							repeatRow (body2, set.j);
							if (chk)
							{
								return true;
							}
						}
						// initialize, and perform column test.
						t = 0;
						chk = false;
						repeatColumn (body1, set.i);
						if (t == l)
						{ // proceed only if counted element equals check level.
							repeatColumn (body2, set.i);
							if (chk)
							{
								return true;
							}
						}
						// initialize, and perform box test.
						t = 0;
						chk = false;
						repeatBox (body1, set);
						if (t == l)
						{ // proceed only if counted element equals check level.
							repeatBox (body2, set);
							if (chk)
							{
								return true;
							}
						}
					}
					// place for more test.
					return false;
				}
			}))
			{
				return true;
			}
			if (l == 1)
			{
				// define Runnable for repeatAll()
				if (Runnable.repeatAll (new Runnable() {
					NumberSet setS;
					int t, s;
					Runnable body1, body2;
					public boolean runB() {
						setS = set;
						///////////////////////////////////////
						// Unique Candidate test.
						///////////////////////////////////////
						
						// signature of selected possible array.
						s = possible[setS.i][setS.j];
						// proceed only selected element is not filled.
						if (s > 0)
						{
							// define Runnable for repeated check.
							body1 = new Runnable() {
								public boolean runB() {
									// check if selected element is not newly selected element,
									if (setS.i != set.i || setS.j != set.j)
									{
										// if so, remove bits of new element from old element.
										t &= NumMgr.getInverse(possible[set.i][set.j]);
									}
									// always return false for complete loop.
									return false;
								}
							};
							// define third Runnable for repeated assignment.
							body2 = new Runnable() {
								public boolean runB() {
									// find the possible answer.
									setS.n = NumMgr.getLogTwo (t) + 1;
									// remove all occurrence of t
									boolean k = refreshPossible (setS);
									// then set possible to t.
									possible[setS.i][setS.j] = t;
									// match is found. always return true.
									return k;
								}
							};
							// initialize, and perform row test.
							t = s;
							repeatRow (body1, setS.j);
							if (NumMgr.getBitCount (t) == 1)
							{ // proceed only if result has exactly one bit.
								// find the possible answer.
								return body2.runB ();
							}
							// initialize, and perform column test.
							t = s;
							repeatColumn (body1, setS.i);
							if (NumMgr.getBitCount (t) == 1)
							{ // proceed only if result has exactly one bit.
								// find the possible answer.
								return body2.runB ();
							}
							// initialize, and perform box test.
							t = s;
							repeatBox (body1, setS);
							if (NumMgr.getBitCount (t) == 1)
							{ // proceed only if result has exactly one bit.
								// find the possible answer.
								return body2.runB ();
							}
						}
						return false;
					}
				}))
				{
					return true;
				}
			}
			// place for more test.
			return false;
		}
		
		/**
		 * check for all possible array, and solve once.
		 * @return true if one step can be solved.
		 */
		public static boolean refreshPuzzle()
		{
			// start from 9, stop at 0.
			for (int i = SIZE; i > 0; i--)
			{
				// for each level, do all possible elimination.
				while (checkOnce(i));
				// if no action can be taken, go to lower level.
			}
			// once possible array is reduced, solve once.
			return solveOnce();
		}
		
		/**
		 * check if the puzzle's boolean value.
		 * @return true if all boolean value are true.
		 */
		public static boolean checkStatus ()
		{
			checkPossible();
			// go through all boolean array,
			// find any false.
			for (int k = 0; k < SIZE; k++)
			{
				if (!row[k] || !column[k] || !box[k])
				{
					return false;
				}
			}
			return true;
		}
		
		/**
		 * updates three boolean arrays when (i,j) elements are updated.
		 * 
		 * @param i row number
		 * @param j column number
		 */
		public static void checkPossible (NumberSet s)
		{
			// define Runnable body for repeated use.
			Runnable body = new Runnable() {
				public boolean runB() {
					// for each element, check if any element is not solved/filled.
					if (possible[set.i][set.j] > 0)
					{
						// if so, break out of loop.
						return true;
					}
					return false;
				}
			};
			// repeat for rows and columns.
			// true means loop is interrupted by return true, meaning some elements are not solved/filled.
			// if true is returned save false. vice versa for false.
			row[s.i] = !Runnable.repeatRow (body, s.j);
			column[s.j] = !Runnable.repeatColumn (body, s.i);
			box[NumMgr.getBoxLow(s.i) + NumMgr.getBox(s.j)] = !Runnable.repeatBox (body, s);
		}
		
		/**
		 * updates three boolean arrays. check all elements.
		 * this is equal to running checkPossible(i, j) with 0 to 9.
		 */
		public static void checkPossible ()
		{
			// to minimize number of checks, use only one loop instead of nested two.
			// check: (0,0) -> (1,3) -> (2,6) -> (3,1) -> (4,4) -> (5,7) -> (6,2) -> (7,5) -> (8,8)
			int c = 0;
			// make r non-repeating number with increment 1.
			for (int r = 0; r < SIZE; r++)
			{
				checkPossible (new NumberSet(r, c));
				// make c non-repeating number with increment 3.
				c += 3;
				if (c >= SIZE)
				{
					c -= 8;
				}
			}
		}
		
		/**
		 * updates Possible array when (i,j) element is updated to n.
		 * 
		 * @param i row number
		 * @param j column number
		 * @param n entered number
		 * @return true if any elements are updated.
		 */
		public static boolean refreshPossible (NumberSet s)
		{
			// since number is entered, no possible for (i,j)
			possible[s.i][s.j] = 0;
			// get binary representation of entered number.
			int x = NumMgr.getInverse (NumMgr.getTwoPowN (s.n - 1));
			// invert for bitwise AND operation.
			
			// define Runnable body for repeated use.
			Runnable body = new Runnable() {
				public boolean runB() {
					// use binary mask to eliminate selected number's bit from possible array.
					if ((s.i != set.i || s.j != set.j) && (possible[set.i][set.j] | x) != x)
					{
						possible[set.i][set.j] &= x;
						set.b = true;
					}
					return false;
				}
			};
			body.set.b = false;
			// do for rows, columns, and boxes.
			Runnable.repeatRow (body, s.j);
			Runnable.repeatColumn (body, s.i);
			Runnable.repeatBox (body, s);
			return body.set.b;
		}
		
		/**
		 * updates possible array of given cell.
		 * @param i row number of cell.
		 * @param j column number of cell.
		 */
		public static void updatePossible (NumberSet s)
		{
			// check only if cell is not filled.
			if (puzzle[s.i][s.j] == 0)
			{
				possible[s.i][s.j] = NumMgr.getInverse(0);
				// define Runnable body for repeated use.
				Runnable body = new Runnable() {
					public boolean runB() {
						// refresh only if puzzle is filled
						if (puzzle[set.i][set.j] > 0)
						{
							possible[s.i][s.j] &= NumMgr.getInverse(NumMgr.getTwoPowN (puzzle[set.i][set.j] - 1));
						}
						return false;
					}
				};
				
				// do for rows, columns, and boxes.
				Runnable.repeatRow (body, s.j);
				Runnable.repeatColumn (body, s.i);
				Runnable.repeatBox (body, s);
			}
			else
			{
				possible[s.i][s.j] = 0;
			}
		}
		
		/**
		 * updates possible array of all cells.
		 */
		public static void updatePossible ()
		{
			Runnable.repeatAll (new Runnable () {
				public boolean runB() {
					updatePossible (set);
					return false;
				}
			});
		}
		
		/**
		 * check if the puzzle is solved or not.
		 * uses pre-solved solution array.
		 * @return true if puzzle is solved.
		 */
		public static boolean checkSolution ()
		{
			// compare all elements of puzzle array with all elements of solution array.
			return !Runnable.repeatAll (new Runnable() {
				public boolean runB() {
					return (puzzle[set.i][set.j] != solution[set.i][set.j]);
				}
			});
		}
		
		/**
		 * check if the puzzle is solved or not.
		 * uses pre-configured boolean array first,
		 * then compare all elements next.
		 * use checkPossible() to refresh boolean array.
		 * @return true if puzzle is solved.
		 */
		public static boolean checkPuzzle ()
		{
			// update once again.
			updatePossible();
			countPuzzle();
			if (!checkStatus() || count[0] != TOTAL)
			{ // bypass test if puzzle appears incomplete.
				return false;
			}
			else
			{
				// TODO: optimize
				return !Runnable.repeatAll (new Runnable () {
					NumberSet setS;
					public boolean runB() {
						// for all elements...
						setS = set;
						Runnable body = new Runnable () {
							public boolean runB() {
								// compare with every other elements that is not itself.
								if (setS.i != set.i || setS.j != set.j)
								{
									return !(puzzle[setS.i][setS.j] != puzzle[set.i][set.j]);
								}
								else
								{
									return false;
								}
							}
						};
						
						if (puzzle[set.i][set.j] == 0)
						{ // if selected element is not filled, test fails.
							return true;
						}
						else if (repeatRow (body, setS.j))
						{ // search for rows.
							return true;
						}
						else if (repeatColumn (body, setS.i))
						{ // search for columns.
							return true;
						}
						else if (repeatBox(body, setS))
						{ // search for boxes.
							return true;
						}
						else
						{ // true breaks the test, false continues the test.
							return false;
						}
					}
				});
			}
		}
		
		/**
		 * Shows one element from solution array.
		 * Solution array must be up to date.
		 * @return true if successful.
		 */
		public static boolean revealPuzzle ()
		{
			// update once again.
			updatePossible();
			countPuzzle();
			if (checkStatus ())
			{
				return false;
			}
			else
			{
				NumberSet set = new NumberSet ();
				do
				{
					set.generateLocation ();
				} while (puzzle[set.i][set.j] > 0);
				
				set.n = solution[set.i][set.j];
				puzzle[set.i][set.j] = set.n;
				// increment counter.
				count[0] ++;
				count[2] --;
				refreshPossible (set);
				checkPossible (set);
				
				// set focus.
				select[0] = set.i;
				select[1] = set.j;
				// set highlight as auto input.
				select[3] = 1;
				
				return true;
			}
		}
	}
	
	
	
	/**
	 * Class to convert numbers.
	 * @author hasol
	 */
	public static class NumMgr
	{
		/**
		 * get 9-bit binary inverse of given number.
		 * @param x number to find inverse.
		 * @return bitwise inverse of x.
		 */
		public static int getInverse (int x)
		{
			return 511 - x;
		}
		
		/**
		 * get power of 2 raised by x
		 * @param x number to get power of two.
		 * @return 2^x.
		 */
		public static int getTwoPowN (int x)
		{
			return (int) Math.pow (2, x);
		}
		
		/**
		 * get Integer.bitCount of given integer.
		 * @param x integer to count bit.
		 * @return total bits of X.
		 * @see java.lang.Integer.bitCount
		 */
		public static int getBitCount (int x)
		{
			return Integer.bitCount (x);
		}
		
		/**
		 * get logBASE 2 of integer x.
		 * @param x number to apply log.
		 * @return logBASE (x, 2)
		 */
		public static int getLogTwo (int x)
		{
			return (int) (Math.log (x) / Math.log (2));
		}
		
		/**
		 * returns row or column box number of entered row or column cell number.
		 * @param x row or column of cell.
		 * @return row or column of box.
		 */
		public static int getBox (int x)
		{ // (int)Math.floor((x+.5)/3)
			return (x - x % 3) / 3;
		}
		
		/**
		 * returns beginning row or column cell number of the box corresponds to entered row or column cell number.
		 * @param x row or column of cell.
		 * @return row or column of first cell of the box.
		 */
		public static int getBoxLow (int x)
		{ // (int)Math.floor((x+.5)/3) * 3
			return getBox(x) * 3;
		}
		
		/**
		 * returns end row or column cell number of the box corresponds to entered row or column cell number.
		 * @param x row or column of cell.
		 * @return row or column of first cell of next box.
		 */
		public static int getBoxHigh (int x)
		{ // (int)Math.ceil((x+.5)/3) * 3
			return (getBox(x) + 1) * 3;
		}
		
		/**
		 * class to hold location and number data.
		 * @author hasol
		 */
		public static class NumberSet
		{
			int i, j, n, x;
			boolean b;
			int[] p;
			
			/**
			 * Initialize with nothing.
			 */
			public NumberSet()
			{
				
			}
			
			public NumberSet (int s)
			{
				generateLocation();
				resetNumber (s);
			}
			
			/**
			 * randomize with given location.
			 * @param a row number.
			 * @param b column number.
			 */
			public NumberSet (int a, int b)
			{
				i = a;
				j = b;
			}
			
			/**
			 * generate new array, using one through nine only once.
			 * @param s size of new array.
			 * @return 1 by 9 array.
			 */
			public static int[] generateNumbers (int s)
			{
				// new array.
				int[] a = new int[s];
				int x = 0, k;
				while (x < s)
				{
					// pick a random number.
					k = rnd.nextInt (s) + 1;
					if (ArrayMgr.linearSearch (a, k) == -1)
					{ // save only if never used.
						a[x] = k;
						x ++;
					}
				}
				return a;
			}
			
			/**
			 * increment index to use for next number.
			 * @return new number.
			 */
			public int nextNumber ()
			{
				if (x < p.length - 1)
				{
					x++;
					n = p[x];
					return n;
				}
				else
				{
					return -1;
				}
			}
			
			/**
			 * resets number set.
			 * @param s size of new array.
			 */
			public void resetNumber (int s)
			{
				p = generateNumbers(s);
				x = 0;
				n = p[x];
			}
			
			/**
			 * resets row and column number.
			 */
			public void generateLocation ()
			{
				// randomly select row and column.
				i = rnd.nextInt(SIZE);
				j = rnd.nextInt(SIZE);
			}
			
			/**
			 * compare randomized array with 9-bit possible input.
			 * @param k
			 */
			public void compareWith (int k)
			{
				while ((k & NumMgr.getTwoPowN (n - 1)) == 0)
				{
					nextNumber ();
				}
				// repeat until it is a possible selection.
				// if selected number's corresponding bit is zero, then selection is not possible.
			}
			
			/**
			 * clones this object.
			 */
			public NumberSet clone ()
			{
				NumberSet s = new NumberSet();
				s.i = i;
				s.j = j;
				s.n = n;
				s.p = p.clone ();
				s.x = x;
				s.b = b;
				return s;
			}
		}
	}
	
	
	
	/**
	 * Class to draw objects
	 * @author hasol
	 */
	public static class FrameMgr
	{
		
		/**
		 * Initialize JFrame object.
		 */
		public static void initFrame ()
		{
			// gap between components
			final int GAP = 10;
			// set size
			mainFrame.setTitle ("Sudoku Solver");
			mainFrame.setSize (800, 600);
			mainFrame.setResizable (false);
			mainFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
			
			// initialize containers
			Container mainUI = mainFrame.getContentPane ();
			Container puzzleUI = new Container ();
			Container controlUI = new Container ();
			Container commandUI = new Container ();
			Container displayUI = new Container ();
			Container numberUI = new Container ();
			// and layouts
			mainUI.setLayout (new BorderLayout(GAP, GAP));
			puzzleUI.setLayout (new FlowLayout(GAP));
			controlUI.setLayout (new BoxLayout (controlUI, BoxLayout.Y_AXIS));
			commandUI.setLayout (new GridLayout (3, 3, GAP, GAP));
			displayUI.setLayout (new GridLayout (3, 1, GAP, GAP));
			numberUI.setLayout (new GridLayout (4, 3, GAP, GAP));
			
			// puzzle part.
			gridLbl = new JLabel (new ImageIcon(dir + "\\res\\image\\SudokuGrid.jpg"));
			puzzleUI.add (gridLbl);
			
			// command part
			newBtn = new JButton ("New");
			newBtn.addActionListener (new CommandAction ('N'));
			newBtn.setMnemonic (KeyEvent.VK_N);
			resetBtn = new JButton ("Reset");
			resetBtn.addActionListener (new CommandAction ('R'));
			resetBtn.setMnemonic (KeyEvent.VK_R);
			fillBtn = new JButton ("Fill");
			fillBtn.addActionListener (new CommandAction ('F'));
			fillBtn.setMnemonic (KeyEvent.VK_F);
			stepBtn = new JButton ("Step");
			stepBtn.addActionListener (new CommandAction ('T'));
			stepBtn.setMnemonic (KeyEvent.VK_T);
			solveBtn = new JButton ("Solve");
			solveBtn.addActionListener (new CommandAction ('S'));
			solveBtn.setMnemonic (KeyEvent.VK_S);
			checkBtn = new JButton ("Check");
			checkBtn.addActionListener (new CommandAction ('K'));
			checkBtn.setMnemonic (KeyEvent.VK_K);
			saveBtn = new JButton ("Save/Load");
			saveBtn.addActionListener (new CommandAction ('V'));
			saveBtn.setMnemonic (KeyEvent.VK_V);
			saveBtn.setMargin (new Insets(1, 1, 1, 1));
			infoBtn = new JButton ("Info");
			infoBtn.addActionListener (new CommandAction ('I'));
			infoBtn.setMnemonic (KeyEvent.VK_I);
			infoBtn.setMargin (new Insets (1, 1, 1, 1));
			optionBtn = new JButton ("Option");
			optionBtn.addActionListener (new CommandAction ('O'));
			optionBtn.setMnemonic (KeyEvent.VK_O);
			optionBtn.setMargin (new Insets (1, 1, 1, 1));
			
			commandUI.add (newBtn);
			commandUI.add (resetBtn);
			commandUI.add (fillBtn);
			commandUI.add (stepBtn);
			commandUI.add (solveBtn);
			commandUI.add (checkBtn);
			commandUI.add (saveBtn);
			commandUI.add (infoBtn);
			commandUI.add (optionBtn);
			
			// display part.
			lineLbl = new JLabel[3];
			lineLbl[0] = new JLabel ("");
			lineLbl[1] = new JLabel ("");
			lineLbl[2] = new JLabel ("");
			
			for (JLabel lbl: lineLbl)
			{
				displayUI.add (lbl);
			}
			
			// number part
			numBtn = new JButton[SIZE];
			Font numberFont = new Font (null, Font.PLAIN, 24);
			for (int n = 0; n < SIZE; n++)
			{
				numBtn[n] = new JButton (Integer.toString (n + 1));
				numBtn[n].setFont (numberFont);
				numBtn[n].addActionListener (new NumberAction (n + 1));
				numBtn[n].setMnemonic (KeyEvent.VK_1 + n);
				numBtn[n].setMnemonic (KeyEvent.VK_NUMPAD1 + n);
			}
			markBtn = new JButton ("Mark");
			markBtn.addActionListener (new CommandAction ('M'));
			markBtn.setMnemonic (KeyEvent.VK_M);
			autoBtn = new JButton ("Auto");
			autoBtn.addActionListener (new CommandAction ('A'));
			autoBtn.setMnemonic (KeyEvent.VK_A);
			clearBtn = new JButton ("Clear");
			clearBtn.addActionListener (new CommandAction ('C'));
			clearBtn.setMnemonic (KeyEvent.VK_C);
			
			for (JButton btn: numBtn)
			{
				numberUI.add (btn);
			}
			numberUI.add (markBtn);
			numberUI.add (autoBtn);
			numberUI.add (clearBtn);
			
			
			// add all UI to main layout.
			commandUI.setPreferredSize (new Dimension (230, 200));
			displayUI.setPreferredSize (new Dimension (230, 100));
			numberUI.setPreferredSize (new Dimension (230, 300));
			
			controlUI.add (commandUI);
			controlUI.add (displayUI);
			controlUI.add (numberUI);
			
			mainUI.add (puzzleUI, BorderLayout.WEST);
			mainUI.add (controlUI, BorderLayout.EAST);
			
			// last, make frame visible.
			mainFrame.setVisible (true);
			mainFrame.setFocusable (true);
			mainFrame.setLocation (50, 50);
			mainFrame.addKeyListener (new KeyboardAction ());
			mainFrame.addMouseListener (new MouseAction ());
			
			Point p = gridLbl.getLocationOnScreen();
			Point q = mainFrame.getLocationOnScreen ();
			OffsetX = (int) (p.getX () - q.getX () + 4);
			OffsetY = (int) (p.getY () - q.getY () + 4);
			
			mainFrame.setSize (OffsetX + 788, OffsetY + 556);
		}
		
		/**
		 * redraws numbers and borders of puzzle.
		 */
		public static void redrawFrame ()
		{
			// repaint items.
			mainFrame.paint (mainGraphic);
			// paint highlighted box.
			if (select[0] >= 0 && select[1] >= 0)
			{
				if (select[3] == 0)
				{
					if (select[2] == 0)
					{
						mainGraphic.setColor (Color.RED);
					}
					else
					{
						mainGraphic.setColor (Color.GREEN);
					}
				}
				else
				{
					mainGraphic.setColor (Color.CYAN);
				}
				mainGraphic.drawRect (
						OffsetX + CellSizeX * select[1] + BoxGap * NumMgr.getBox(select[1]),
						OffsetY + CellSizeY * select[0] + BoxGap * NumMgr.getBox(select[0]),
						CellSizeX - 1, CellSizeY - 1);
				mainGraphic.drawRect (
						OffsetX + CellSizeX * select[1] + BoxGap * NumMgr.getBox(select[1]) + 1,
						OffsetY + CellSizeY * select[0] + BoxGap * NumMgr.getBox(select[0]) + 1,
						CellSizeX - 3, CellSizeY - 3);
				mainGraphic.drawRect (
						OffsetX + CellSizeX * select[1] + BoxGap * NumMgr.getBox(select[1]) + 2,
						OffsetY + CellSizeY * select[0] + BoxGap * NumMgr.getBox(select[0]) + 2,
						CellSizeX - 5, CellSizeY - 5);
			}
			
			// paint numbers.
			Runnable.repeatAll ( new Runnable () {
				public boolean runB () {
					// if puzzle is blank,
					if (puzzle[set.i][set.j] == 0)
					{
						// bypass if possible array has nothing.
						if (possible[set.i][set.j] > 0)
						{
							// draw one to nine to each location.
							mainGraphic.setColor (Color.GRAY);
							mainGraphic.setFont (new Font (Font.SANS_SERIF, Font.BOLD, 16));
							for (int n = 0; n < SIZE; n++)
							{
								if ((possible[set.i][set.j] & NumMgr.getTwoPowN (n)) > 0)
								{
									mainGraphic.drawString (Integer.toString (n+1), 
											OffsetX + CellSizeX*set.j + BoxGap * NumMgr.getBox(set.j) + 16*(n%3) + 9,
											OffsetY + CellSizeY*set.i + BoxGap * NumMgr.getBox(set.i) + 16*(n/3) + 20);
								}
							}
						}
					}
					else
					{ // if puzzle has number,
						// draw single number, centered.
						if (puzzle[set.i][set.j] == original[set.i][set.j])
						{
							// if number is original,
							mainGraphic.setColor (Color.BLACK);
						}
						else if (select[5] != 0 && puzzle[set.i][set.j] != solution[set.i][set.j])
						{
							// if number is wrong
							mainGraphic.setColor (Color.MAGENTA);
						}
						else if (original [set.i][set.j] == 0)
						{ // if original is empty
							mainGraphic.setColor (Color.BLUE);
						}
						else
						{ // if number are not same.
							mainGraphic.setColor (Color.PINK);
						}
						mainGraphic.setFont (new Font (Font.SANS_SERIF, Font.PLAIN, 48));
						mainGraphic.drawString (Integer.toString (puzzle[set.i][set.j]), 
								OffsetX + CellSizeX*set.j + BoxGap * NumMgr.getBox(set.j) + 16,
								OffsetY + CellSizeY*(set.i+1) + BoxGap * NumMgr.getBox(set.i) - 12);
					}
					// always return false to continue loop.
					return false;
				}
			});
		}
		
		/**
		 * sets text of selected label with given string.
		 * @param s string of text
		 * @param i index of lineLbl
		 */
		public static void setLabelText (String s, int i)
		{
			lineLbl[i].setText (s);
		}
	}
	
	
	
	/**
	 * Class to print out array.
	 * @author hasol
	 */
	public static class ArrayMgr
	{
		/**
		 * print one-dimensional array into stream
		 * @param a array to convert
		 * @param s spacing string
		 * @param w wrapping string
		 */
		public static void printArray (int[] a, String s, String w)
		{
			// insert first wrapping string
			System.out.print (w.charAt (0));
			// insert all elements
			for (int n = 0; n < a.length; n++)
			{
				// for last element, just add it.
				// otherwise, add element with spacing.
				if (n == a.length - 1)
				{
					System.out.print (a[n]);
				}
				else
				{
					System.out.print (a[n] + s);
				}
			}
			// insert last wrapping string
			System.out.print (w.charAt (1));
			
		}
		
		/**
		 * print one-dimensional array into stream with default values
		 * @param a array to convert
		 */
		public static void printArray (int[] a)
		{
			printArray (a, ", ", "{}");
		}
		
		/**
		 * print two-dimensional array into stream
		 * @param a array to convert
		 * @param s spacing string
		 * @param w wrapping string
		 */
		public static void printArray (int[][] a, String s, String w)
		{
			// insert first wrapping string
			System.out.print (w.charAt (0));
			// insert all elements
			for (int n = 0; n < a.length; n++)
			{
				// for last element, just add it.
				// otherwise, add element with spacing.
				if (n == a.length - 1)
				{
					printArray (a[n], s, w);
				}
				else
				{
					printArray (a[n], s, w);
					System.out.println (s);
				}
			}
			// insert last wrapping string
			System.out.print (w.charAt (1));
		}
		
		/**
		 * print two-dimensional array into stream with default values
		 * @param a array to convert
		 */
		public static void printArray (int[][] a)
		{
			printArray (a, ", ", "{}");
		}
		
		
		/**
		 * clones two-dimensional array.
		 * @param a array to clone.
		 * @return new independent array with same integer value.
		 */
		public static int[][] clone (int[][] a)
		{
			int[][] array = new int[a.length][a[0].length];
			
			for (int n = 0; n < a.length; n++)
			{
				array[n] = a[n].clone ();
			}
			
			return array;
		}
		
		/**
		 * Search for an item in a given array.
		 * @param a array to look up
		 * @param s item to search
		 * @return index of item. if not found return -1;
		 */
		public static int linearSearch (String[] a, String s)
		{
			for (int n = 0; n < a.length; n++)
			{
				if (a[n].equals (s))
				{
					return n;
				}
			}
			return -1;
		}
		
		/**
		 * Search for an item in a given array.
		 * @param a array to look up
		 * @param s item to search
		 * @return index of item. if not found return -1;
		 */
		public static int linearSearch (int[] a, int s)
		{
			for (int n = 0; n < a.length; n++)
			{
				if (a[n] == s)
				{
					return n;
				}
			}
			return -1;
		}
		
		/**
		 * fill two-dimensional array with given value.
		 * @param a array to fill
		 * @param n number to fill
		 */
		public static void fill (int[][] a, int n)
		{
			for (int[] p: a)
			{
				Arrays.fill (p, n);
			}
		}
	}
	
	
	
	/**
	 * Class to read/write files
	 * @author hasol
	 */
	public static class IOMgr
	{
		
		/**
		 * converts file to puzzle array
		 * @param path location of file to read
		 * @param format formatting to separate numbers.
		 * @return true if success. false if fail.
		 */
		public static boolean fileToPuzzle (String path, String format)
		{
			// separate formatting.
			String[] str = splitFormat (format);
			// check path format
			path = pathFormat (path, "res\\text\\");
			// scanner object
			Scanner scan = null;
			// NumberSer to keep track of index.
			NumberSet set = new NumberSet();
			
			try
			{
				// make buffered scanner.
				scan = new Scanner (new BufferedReader (new FileReader (path)));
				// use given parameter.
				scan.useDelimiter (str[1]);
				
				// Repeat for all scanned elements.
				while (scan.hasNext ())
				{
					// save next string.
					String s = scan.next ();
					// remove new line character.
					if (s.startsWith ("\r"))
					{
						s = s.substring (1);
						if (s.startsWith ("\n"))
						{
							s = s.substring (1);
						}
					}
					// remove starting wrapping string.
					if (s.startsWith (str[0]))
					{
						s = s.substring (str[0].length ());
						set.j = 0;
						if (s.startsWith (str[0]))
						{
							s = s.substring (str[0].length ());
							set.i = 0;
						}
						else
						{
							set.i++;
						}
					}
					// remove ending wrapping string.
					if (s.endsWith (str[2]))
					{
						s = s.substring (0, s.length () - str[2].length ());
						if (s.endsWith (str[2]))
						{
							s = s.substring (0, s.length () - str[2].length ());
						}
					}
					
					try
					{
						// convert integer to string.
						puzzle[set.i][set.j] = Integer.parseInt (s);
						set.j++;
					}
					catch (NumberFormatException e)
					{
						scan.close ();
						return false;
					}
					finally
					{
						
					}
				}
				// always close.
				scan.close ();
				// redraw numbers.
				FrameMgr.redrawFrame ();
			}
			catch (IOException e)
			{
				return false;
			}
			finally 
			{
				
			}
			
			return true;
		}
		
		/**
		 * converts puzzle array to file
		 * @param path location of file to write
		 * @param format formatting to separate numbers
		 * @return true if success, false if fail.
		 */
		public static boolean puzzleToFile (String path, String format)
		{
			// separate formatting.
			String[] str = splitFormat (format);
			// check path format
			path = pathFormat (path, "out\\");
			PrintWriter writer = null;
			
			try
			{
				// make stream to write.
				writer = new PrintWriter (new BufferedWriter (new FileWriter (path)));
				
				// insert first wrapping string.
				writer.print (str[0]);
				for (int r = 0; r < SIZE; r++)
				{
					if (r != 0)
					{ // insert first separator before new input
						writer.println (str[1]);
					}
					// insert second wrapping string.
					writer.print (str[0]);
					for (int c = 0; c < SIZE; c++)
					{
						if (c != 0)
						{ // insert second separator before new input
							writer.print (str[1]);
						}
						writer.print (puzzle[r][c]);
					}
					// insert first wrapping string.
					writer.print (str[2]);
				}
				// insert first wrapping string.
				writer.print (str[2]);
				// always close.
				writer.close ();
			}
			catch (IOException e)
			{
				return false;
			}
			finally 
			{
				
			}
			return true;
		}
		
		/**
		 * separate format string to an array.
		 * @param format single string for formatting.
		 * @return array of string. with size 3.
		 */
		public static String[] splitFormat (String format)
		{
			String[] str = new String[3];
			str[0] = Character.toString (format.charAt (0));
			str[1] = format.substring (1, format.length () - 1);
			str[2] = Character.toString (format.charAt (format.length () - 1));
			return str;
		}
		
		/**
		 * convert formatting of string to complete directory.
		 * @param path
		 * @param loc
		 * @return
		 */
		public static String pathFormat (String path, String loc)
		{
			if (path.indexOf (':') == -1)
			{
				return dir + loc + path;
			}
			else
			{
				return path;
			}
		}
	}
	
	
	
	/**
	 * Runnable class that extends use of java.lang.Runnable.
	 * @author hasol
	 */
	public static class Runnable implements java.lang.Runnable
	{
		NumberSet set;
		
		public Runnable()
		{
			set = new NumberSet();
		}
		public Runnable(int x)
		{
			this();
			init(x);
		}
		public void init(int x)
		{
			
		}
		
		@Override
		public void run ()
		{
			
		}
		public boolean runB ()
		{
			return false;
		}
		public int runI ()
		{
			return 0;
		}
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		// Runnable loop methods section.
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		
		/**
		 * use nested loop on s.i and s.j; run given body.
		 * this method uses repeatRow() and repeatColumn()
		 * @param body Runnable to execute
		 * @param s NumberSet to use.
		 * @return true if any action returned true, otherwise false.
		 * @see #repeatRow
		 * @see #repeatColumn
		 */
		public static boolean repeatAll (Runnable body, NumberSet s)
		{
			// use repeatRow,
			return repeatRow (new Runnable() {
				@Override
				public boolean runB ()
				{
					// and repeatColumn.
					return repeatColumn (body, s);
				}
			}, s);
		}
		
		public static boolean repeatAll (Runnable body)
		{
			return repeatAll (body, body.set);
		}
		
		/**
		 * use loop on s.i; run given body.
		 * if any of body returns true, break loop and return true.
		 * @param body Runnable to execute
		 * @param s NumberSet to use.
		 * @return true if any action returned true, otherwise, false.
		 */
		public static boolean repeatRow (Runnable body, NumberSet s)
		{
			for (s.i = 0; s.i < SIZE; s.i++)
			{
				if (body.runB ())
				{
					return true;
				}
			}
			return false;
		}
		
		public static boolean repeatRow (Runnable body)
		{
			return repeatRow (body, body.set);
		}
		
		public static boolean repeatRow (Runnable body, int j)
		{
			body.set.j = j;
			return repeatRow (body);
		}
		
		/**
		 * use loop on s.j; run given body.
		 * if any of body returns true, break loop and return true.
		 * @param body Runnable to execute
		 * @param s NumberSet to use.
		 * @return true if any action returned true, otherwise, false.
		 */
		public static boolean repeatColumn (Runnable body, NumberSet s)
		{
			for (s.j = 0; s.j < SIZE; s.j++)
			{
				if (body.runB ())
				{
					return true;
				}
			}
			return false;
		}
		
		public static boolean repeatColumn (Runnable body)
		{
			return repeatColumn (body, body.set);
		}
		
		public static boolean repeatColumn (Runnable body, int i)
		{
			body.set.i = i;
			return repeatColumn (body);
		}
		
		/**
		 * use loop on s.i and s.j; run given body.
		 * this method runs nested loop in box that contains given location.
		 * if any of body returns true, break loop and return true.
		 * @param body Runnable to execute
		 * @param s NumberSet to use.
		 * @param o NumberSet of reference.
		 * @return true if any action returned true, otherwise, false.
		 */
		public static boolean repeatBox (Runnable body, NumberSet s, NumberSet o)
		{
			for (s.i = NumMgr.getBoxLow (o.i); s.i < NumMgr.getBoxHigh (o.i); s.i++)
			{
				for (s.j = NumMgr.getBoxLow (o.j); s.j < NumMgr.getBoxHigh (o.j); s.j++)
				{
					if (body.runB ())
					{
						return true;
					}
				}
			}
			return false;
		}
		
		public static boolean repeatBox (Runnable body, NumberSet o)
		{
			return repeatBox (body, body.set, o);
		}
	}
	
	
	
	/**
	 * class to carry out all functionality of ActionListeners
	 * @author hasol
	 */
	public static class ActionMgr
	{
		static boolean foundX, foundY;
		public static int x, y, i, j, a, b;
		
		public static void init(Point p)
		{
			// initialize variables
			foundX = foundY = true;
			x = (int)p.getX();
			y = (int)p.getY();
			j = OffsetX;
			i = OffsetY;
			a = b = 0;
		}
		
		public static void highlightClick (Point p)
		{
			init (p);
			
			// set highlight as user input.
			select[3] = 0;
			
			// Scan for X coordinate.
			while (foundX && !(j <= x && x < j + CellSizeX))
			{ // stop loop if not found or in range.
				// if not in range, continue loop, increase range.
				j += CellSizeX;
				b++;
				if (b % 3 == 0)
				{ // for once in three, increase range more.
					j += BoxGap;
				}
				if (b == SIZE)
				{ // if loop is done 9 times. no match. end loop.
					foundX = false;
				}
			}
			// Scan for Y coordinate.
			while (foundY && !(i <= y && y < i + CellSizeY))
			{ // stop loop if not found or in range.
				// if not in range, continue loop, increase range.
				i += CellSizeY;
				a++;
				if (a % 3 == 0)
				{ // for once in three, increase range more.
					i += BoxGap;
				}
				if (a == SIZE)
				{ // if loop is done 9 times. no match. end loop.
					foundY = false;
				}
			}
			// if match is found for X and Y, draw box.
			if (foundX && foundY)
			{
				select[0] = a;
				select[1] = b;
			}
			else
			{ // if not, reset graphic.
				select[0] = select[1] = -1;
			}
		}
		
		public static void action (char c)
		{
			String[] option;
			int k;
			switch (c)
			{
				case 'N': // New
					// prompt to user for new puzzle
					option = new String[] {"All", "Training", "Very Easy", "Easy", "Medium", "Hard", "Very Hard"};
					String str = (String)JOptionPane.showInputDialog (mainFrame, "Select Difficulty", "New Puzzle",
							JOptionPane.PLAIN_MESSAGE, null, option, option[0]);
					if (str != null)
					{
						k = ArrayMgr.linearSearch (option, str) - 1;
						initVar();
						SudokuMgr.initPuzzle (k);
						if (select[4] == 0)
						{ // unless user requests it, fill possible blank.
							ArrayMgr.fill (possible, 0);
						}
						FrameMgr.setLabelText (str + " Puzzle Generated!", 2);
					}
					break;
				case 'R': // Reset
					// prompt to user for reset
					if (JOptionPane.showConfirmDialog (mainFrame, "Reset?", "Confirm", JOptionPane.YES_NO_OPTION)
							== JOptionPane.YES_OPTION)
					{
						initVar();
						FrameMgr.setLabelText ("Puzzle Cleared!", 2);
					}
					break;
				case 'F': // Fill
					SudokuMgr.updatePossible ();
					FrameMgr.setLabelText ("Possible number filled!", 2);
					break;
				case 'T': // Step
					if (!isGenerated)
					{
						original = ArrayMgr.clone (puzzle);
						isGenerated = true;
					}
					// see if puzzle can be solved.
					if (SudokuMgr.solvePuzzle ())
					{
						puzzle = ArrayMgr.clone (temporary);
						if (SudokuMgr.revealPuzzle ())
						{
							FrameMgr.setLabelText ("Step Successful!", 0);
						}
						else
						{
							FrameMgr.setLabelText ("Step Failed!", 0);
						}
					}
					else
					{
						FrameMgr.setLabelText ("Solve Failed!", 0);
					}
					break;
				case 'S': // Solve
					// copy to original only once.
					if (!isGenerated)
					{
						original = ArrayMgr.clone (puzzle);
						isGenerated = true;
					}
					// see if puzzle can be solved.
					if (SudokuMgr.solvePuzzle ())
					{
						FrameMgr.setLabelText ("Solve Successful!", 0);
					}
					else
					{
						FrameMgr.setLabelText ("Solve Failed!", 0);
					}
					break;
				case 'K': // Check
					// see if puzzle is solved
					if (SudokuMgr.checkPuzzle ())
					{
						FrameMgr.setLabelText ("Puzzle is solved.", 1);
					}
					else
					{
						FrameMgr.setLabelText ("Puzzle is not solved.", 1);
					}
					break;
				case 'M': // Mark
					if (select[2] == 0)
					{
						select[2] = 1;
					}
					else
					{
						select[2] = 0;
					}
					break;
				case 'A': // Auto
					if (select[0] >= 0 && select[1] >= 0)
					{
						NumberSet set = new NumberSet(select[0], select[1]);
						// do only if cell is not filled.
						if (puzzle[set.i][set.j] == 0)
						{
							if (NumMgr.getBitCount (possible[set.i][set.j]) == 1)
							{
								if (puzzle[set.i][set.j] <= 0)
								{
									count[0] ++;
									count[2] --;
								}
								set.n = NumMgr.getLogTwo (possible[set.i][set.j]) + 1;
								puzzle[set.i][set.j] = set.n;
								SudokuMgr.refreshPossible (set);
								SudokuMgr.checkPossible ();
							}
							else
							{
								// TODO: choose functionality.
								// auto-fill possible array
								SudokuMgr.updatePossible (set);
								// or toggle possible array
								// or reset to original number
							}
						}
					}
					break;
				case 'C': // Clear
					if (select[0] >= 0 && select[1] >= 0)
					{
						// set puzzle and possible to 0;
						if (puzzle[select[0]][select[1]] > 0)
						{
							count[0] --;
							count[2] ++;
						}
						puzzle[select[0]][select[1]] = 0;
						possible[select[0]][select[1]] = 0;
					}
					break;
				case 'V': // Save/Load
					// initialize option list
					option = new String[] {"Save", "Load", "Cancel"};
					k = JOptionPane.showOptionDialog (mainFrame, "Select Operation", "Choose Option",
							JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, option, option[2]);
					// only if not cancel
					if (k != 2)
					{
						// ask for path and format
						String path = JOptionPane.showInputDialog (mainFrame, "Type Path",
								option[k], JOptionPane.QUESTION_MESSAGE);
						String format = JOptionPane.showInputDialog (mainFrame, "Type Format",
								option[k], JOptionPane.QUESTION_MESSAGE);
						// skip if nothing is entered.
						if (path != null && format != null)
						{
							if (k == 0)
							{ // Save
								IOMgr.puzzleToFile (path, format);
							}
							else if (k == 1)
							{ // Load
								initVar();
								IOMgr.fileToPuzzle (path, format);
							}
						}
					}
					break;
				case 'I': // Info
					JOptionPane.showMessageDialog (mainFrame, "Copyright IHS 2016.\nLisenced to: NYIT - CSCI 185",
							"Information", JOptionPane.INFORMATION_MESSAGE, null);
					break;
				case 'O': // Option
					// initialize option list
					option = new String[] {"Toggle Possible", "Toggle Error", "Cancel"};
					k = JOptionPane.showOptionDialog (mainFrame, "Select Operation", "Choose Option",
							JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, option, option[2]);
					// only if not cancel
					if (k != 2)
					{
						if (k == 0)
						{ // toggle possible
							if (select[4] == 0)
							{
								select[4] = 1;
							}
							else
							{
								select[4] = 0;
							}
						}
						else if (k == 1)
						{ // toggle error
							if (select[5] == 0)
							{
								select[5] = 1;
							}
							else
							{
								select[5] = 0;
							}
						}
					}
					break;
			}
			FrameMgr.redrawFrame();
		}
		
		public static void action (int num)
		{
			// check if selected index is valid.
			if (select[0] >= 0 && select[1] >= 0)
			{
				NumberSet set = new NumberSet (select[0], select[1]);
				set.n = num;
				// check marking state.
				if (select[2] == 0)
				{
					// write only if original cell is not selected.
					if (original[set.i][set.j] <= 0)
					{
						if (puzzle[set.i][set.j] <= 0)
						{
							count[0] ++;
							count[2] --;
						}
						// insert number to puzzle.
						puzzle[set.i][set.j] = set.n;
						SudokuMgr.refreshPossible (set);
						SudokuMgr.checkPossible ();
					}
				}
				else if (puzzle[set.i][set.j] <= 0)
				{
					// or toggle possible array set.
					if ((possible[set.i][set.j] & NumMgr.getTwoPowN (set.n - 1)) > 0)
					{
						possible[set.i][set.j] &= NumMgr.getInverse (NumMgr.getTwoPowN (set.n - 1));
					}
					else
					{
						possible[set.i][set.j] |= NumMgr.getTwoPowN (set.n - 1);
					}
				}
			}
			FrameMgr.redrawFrame();
			// set highlight as user input.
			select[3] = 0;
		}
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		// implemented subclass section.
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		
		/**
		 * MouseAction class that implements java.awt.event.MouseListener.
		 * @author hasol
		 */
		public static class MouseAction implements MouseListener
		{
			public MouseAction ()
			{
				// update graphics.
				mainGraphic = mainFrame.getGraphics ();
			}
			
			@Override
			public void mouseClicked (MouseEvent e)
			{
				// get mouse pointer information.
				Point p = e.getPoint ();
				
				// select box.
				highlightClick (p);
				FrameMgr.redrawFrame ();
			}
			
			@Override
			public void mousePressed (MouseEvent e)
			{
				// No action.
			}
			
			@Override
			public void mouseReleased (MouseEvent e)
			{
				// when user uses mouse, make keyboard to work
				mainFrame.requestFocus ();
			}
			
			@Override
			public void mouseEntered (MouseEvent e)
			{
				// No action.
			}
			
			@Override
			public void mouseExited (MouseEvent e)
			{
				// No action.
			}
		}
		
		/**
		 * KeyboardAction that implements java.awt.event.KeyListener.
		 * @author hasol
		 */
		public static class KeyboardAction implements KeyListener
		{
			@Override
			public void keyTyped (KeyEvent e)
			{
				try
				{ // if key is number,
					action (Integer.parseInt (Character.toString (e.getKeyChar ())));
				}
				catch (NumberFormatException exc)
				{ // if key is a character,
					action (e.getKeyChar ());
				}
			}
			
			@Override
			public void keyPressed (KeyEvent e)
			{
				switch (e.getKeyCode ())
				{
					case KeyEvent.VK_UP:
					case KeyEvent.VK_KP_UP:
						if (select[0] >= 0 && select[0] > 0)
						{
							select[0]--;
							FrameMgr.redrawFrame();
							// set highlight as user input.
							select[3] = 0;
						}
						break;
					case KeyEvent.VK_DOWN:
					case KeyEvent.VK_KP_DOWN:
						if (select[0] >= 0 && select[0] < SIZE-1)
						{
							select[0]++;
							FrameMgr.redrawFrame();
							// set highlight as user input.
							select[3] = 0;
						}
						break;
					case KeyEvent.VK_LEFT:
					case KeyEvent.VK_KP_LEFT:
						if (select[1] >= 0 && select[1] > 0)
						{
							select[1]--;
							FrameMgr.redrawFrame();
							// set highlight as user input.
							select[3] = 0;
						}
						break;
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_KP_RIGHT:
						if (select[1] >= 0 && select[1] < SIZE-1)
						{
							select[1]++;
							FrameMgr.redrawFrame();
							// set highlight as user input.
							select[3] = 0;
						}
						break;
				}
			}
			
			@Override
			public void keyReleased (KeyEvent e)
			{
				// when user uses keyboard, make keyboard to work
				mainFrame.requestFocus ();
			}
		}
		
		
		/**
		 * CommandAction that implements java.awt.event.ActionListener.
		 * @author hasol
		 */
		public static class CommandAction implements ActionListener
		{
			char ActionCode;
			
			public CommandAction (char s)
			{
				// must assign valid command code.
				ActionCode = s;
			}
			
			@Override
			public void actionPerformed (ActionEvent e)
			{
				action (ActionCode);
			}
		}
		
		/**
		 * NumberAction that implements java.awt.event.ActionListener.
		 * @author hasol
		 */
		public static class NumberAction implements ActionListener
		{
			int ButtonNum;
			
			public NumberAction (int n)
			{
				// must assign single digit number.
				ButtonNum = n;
			}
			
			@Override
			public void actionPerformed (ActionEvent e)
			{
				action (ButtonNum);
			}
		}
	}
	
	
	// end of file.
}
