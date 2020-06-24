package sudokuSolver;


/**
 * StatTest.java
 * CSCI185.sudokuSolver
 * 
 * CSCI 185 M02
 * File ID: 
 * Mar 24, 2016 2:49:03 PM
 * @author Hasol Im
 * Instructor: Professor. Simon Ben-Avi
 * 
 * Auto-Generated Comment Format Ver 1.4
 */
public class StatTest
{
	public static void main (String[] args)
	{
		int success = 0, total;
		double ratio;
		
		for (total = 0; total < 500; total++)
		{
			SudokuMain.initVar ();
			if (true)
			{
				SudokuMain.SudokuMgr.initPuzzle (-1);
				success++;
			}
		}
		ratio = (double)success / total;
		System.out.println (success + " success, " + (total - success) + " fail, " + total + " total, " + ratio + " ratio.");
	}
}
