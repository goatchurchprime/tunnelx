////////////////////////////////////////////////////////////////////////////////
// Tunnel v2.0 copyright Julian Todd 1999.  
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import javax.swing.JComboBox; 
import javax.swing.JPanel; 
import javax.swing.JButton; 
import javax.swing.BoxLayout; 
import javax.swing.JCheckBox; 
import javax.swing.JToggleButton; 
import javax.swing.JTextField; 

import java.awt.Insets; 
import java.awt.Component; 

import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 

import java.awt.BasicStroke; 
import java.awt.Color; 

//
//
// SketchLineStyle  
//
//

/////////////////////////////////////////////
class SketchLineStyle extends JPanel 
{
	// parallel arrays of wall style info.  
	static String[] linestylenames = { "Centreline", "Wall", "Est. Wall", "Pitch Bound", "Ceiling Bound", "Detail", "Invisible", "Filled" }; 
	static int SLS_CENTRELINE = 0; 

	static int SLS_WALL = 1; 
	static int SLS_ESTWALL = 2; 

	static int SLS_PITCHBOUND = 3; 
	static int SLS_CEILINGBOUND = 4; 

	static int SLS_DETAIL = 5; 
	static int SLS_INVISIBLE = 6; 
	static int SLS_FILLED = 7; 

	static int SLS_SYMBOLOUTLINE = 8; // not a selected style.  

	static float strokew; 
	static Color[] linestylecols = new Color[9]; 
	static BasicStroke[] linestylestrokes = new BasicStroke[9]; 

	static Color linestylecolactive = Color.magenta; 
	static Color linestylecolprint= Color.black; 

	static String[] linestylebuttonnames = { "", "W", "E", "P", "C", "D", "I", "F" }; 

	// (we must prevent the centreline style from being selected --  it's special).  
	JComboBox linestylesel = new JComboBox(linestylenames); 
	//JCheckBox pthsplined = new JCheckBox("Splined"); 
	JToggleButton pthsplined = new JToggleButton("s"); 
	JTextField pthlabel = new JTextField(); 


	/////////////////////////////////////////////
	class LineStyleButton extends JButton implements ActionListener 
	{
		int index; 
		LineStyleButton(String linestylebuttonname, int lindex) 
		{
			super(linestylebuttonname); 
			setMargin(new Insets(2, 2, 2, 2)); 
			index = lindex; 
			addActionListener(this); 
		}

		public void actionPerformed(ActionEvent e) 
		{
			linestylesel.setSelectedIndex(index); 
		}
	}; 

	// this is generates the actions which are read for the changes, 
	// and has the definitive indices.  


	/////////////////////////////////////////////
	static void SetStrokeWidths(float lstrokew) 
	{
		strokew = lstrokew; 
		float[] dash = new float[2]; 

		// centreline 
		linestylestrokes[0] = new BasicStroke(0.5F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew); 
		linestylecols[0] = Color.red; 

		// wall 
		linestylestrokes[1] = new BasicStroke(2.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew); 
		linestylecols[1] = Color.blue; 

		// estimated wall
		dash[0] = 3 * strokew; 
		dash[1] = 3 * strokew; 
		linestylestrokes[2] = new BasicStroke(2.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 1.5F * strokew); 
		linestylecols[2] = Color.blue;  

		// pitch boundary 
		linestylestrokes[3] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 1.5F * strokew); 
		linestylecols[3] = Color.cyan;  

		// ceiling boundary 
		linestylestrokes[4] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew, dash, 1.5F * strokew); 
		linestylecols[4] = Color.cyan;  

		// detail 
		linestylestrokes[5] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew); 
		linestylecols[5] = Color.blue;  

		// invisible 
		linestylestrokes[6] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew); 
		linestylecols[6] = Color.green;  

		// filled 
		linestylestrokes[7] = new BasicStroke(1.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew); 
		linestylecols[7] = Color.black;  

		// symbol paint background.   
		linestylestrokes[8] = new BasicStroke(4.0F * strokew, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5.0F * strokew); 
		linestylecols[8] = Color.white;  // for printing.  
	} 


	/////////////////////////////////////////////
	SketchLineStyle() 
	{
		// do the button panel 
		JPanel buttpanel = new JPanel(); 
		buttpanel.setLayout(new BoxLayout(buttpanel, BoxLayout.X_AXIS)); 
		for (int i = 0; i < linestylebuttonnames.length; i++) 
		{
			if (!linestylebuttonnames[i].equals("")) 
				buttpanel.add(new LineStyleButton(linestylebuttonnames[i], i)); 
		}
		pthsplined.setMargin(new Insets(3, 3, 3, 3)); 
		buttpanel.add(pthsplined); 

		linestylesel.setSelectedIndex(SLS_DETAIL); 

		// do the layout of the main thing.  
		linestylesel.setAlignmentX(Component.LEFT_ALIGNMENT); 
		buttpanel.setAlignmentX(Component.LEFT_ALIGNMENT); 
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 
		add(linestylesel); 
		add(buttpanel); 
		//add(pthsplined); 
		add(pthlabel); 
	}
}; 
