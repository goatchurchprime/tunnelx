////////////////////////////////////////////////////////////////////////////////
// TunnelX -- Cave Drawing Program
// Copyright (C) 2002  Julian Todd.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
////////////////////////////////////////////////////////////////////////////////
package Tunnel;

import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;
import java.io.IOException;
import java.io.File;
import java.lang.StringBuffer;


//
//
// OneTunnel
//
//
// the class which stores the data and connections for one tunnel of data
// this is
class OneTunnel
{
	// tunnel starting point
	String name;
	String fullname;
	String fulleqname;	// same as fullname but with the blank begin names removed.
	int depth;

	// to tell whether its expanded in the tunneltree
	boolean bTunnelTreeExpanded = true;

	// connections up (if present)
	OneTunnel uptunnel = null;

	// the actual data in this tunnel
	StringBuffer TextData = new StringBuffer();
	int starendindex = -1;

	// the leg line format at the start of this text
	LegLineFormat InitialLegLineFormat = TN.defaultleglineformat;

	// the down connections
	OneTunnel[] downtunnels = null;
	int ndowntunnels = 0; // number of down vectors (the rest are there for the delete to be "undone").


	// this is the directory structure (should all be in the same directory).
	File tundirectory = null;
	boolean bsvxfilechanged = false;
	File svxfile = null;
	boolean bexportfilechanged = false;
	File exportfile = null;
	boolean bxmlfilechanged = false;
	File xmlfile = null;

        // output file from survex, retro-fitted for reliable loading.
        File posfile = null;
        Vector vposlegs = null;

	// used to list those on the directory for handy access.
	Vector imgfiles = new Vector();

// this is the compiled data from the TextData
	Vector vlegs = new Vector();		// of type OneLeg

	// attributes
	String svxdate = "*";
	int dateorder = 0; // index into list of dates
	String svxtitle;
	String teamtape;
	String teampics;
	String teaminsts;
	String teamnotes;

	// the station names present in the survey leg data.
	Vector stationnames = new Vector();

	// values read from the TextData
	Vector vstations = new Vector();	// of type OneStation.

	Vec3 LocOffset = new Vec3(); // location offset of the stations (to avoid getting too far from the origin and losing float precision).

	// from the exports file.
	Vector vexports = new Vector(); // of type OneExport.

	// from the exports file.
	Vector vposfixes = new Vector(); // of type OnePosfix.

	boolean bWFtunnactive = false;	// set true if this tunnel is to be highlighted (is a descendent of activetunnel).

	// the cross sections
	Vector vsections = new Vector();
	Vector vtubes = new Vector();

	// the sketch
	Vector tsketches = new Vector(); // of type OneSketch.
	//OneSketch tsketch = null;

	// the back image (recursively taken from the uptunnel).
	File fbackgimg = null;

	// the possible sections
	Vector vposssections = new Vector();

	// the text getting and setting
	String getTextData()
	{
		return(TextData.toString());
	}

	void setTextData(String text)
	{
		TextData.setLength(0);
		TextData.append(text);
		starendindex = -1;
	}

	public String toString()
	{
		return name;
	}


	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los) throws IOException
	{
		los.WriteLine(TNXML.xcomopen(0, TNXML.sMEASUREMENTS, TNXML.sNAME, name));

		int nsets = 0;
		if (svxdate != null)
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_DATE, svxdate));
			nsets++;
		}

		if (svxtitle != null)
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_TITLE, svxtitle));
			nsets++;
		}

		if (teamtape != null)
		{
			los.WriteLine(TNXML.xcomopen(0, TNXML.sSET, TNXML.sSVX_TAPE_PERSON, teamtape));
			nsets++;
		}

//	String teampics;
//	String teaminsts;
//	String teamnotes;


		for (int i = 0; i < vlegs.size(); i++)
			((OneLeg)vlegs.elementAt(i)).WriteXML(los);

		// unroll the sets.
		for (int i = 0; i < nsets; i++)
			los.WriteLine(TNXML.xcomclose(0, TNXML.sSET));


		// write the xsections and tubes
		for (int i = 0; i < vsections.size(); i++)
			((OneSection)vsections.elementAt(i)).WriteXML(los, i);
		for (int i = 0; i < vtubes.size(); i++)
			((OneTube)vtubes.elementAt(i)).WriteXML(los, vsections);

		los.WriteLine(TNXML.xcomclose(0, TNXML.sMEASUREMENTS));
	}

	// extra text
	/////////////////////////////////////////////
	public void Append(String textline)
	{
		if (textline.startsWith("*end"))
			starendindex = TextData.length();
		else
			starendindex = -1;
		TextData.append(textline);
	}

	/////////////////////////////////////////////
	public void AppendLine(String textline)
	{
		Append(textline);
		TextData.append(TN.nl);
	}

	/////////////////////////////////////////////
	// used to put in the *pos_fix,  what a hack.
	public void AppendLineBeforeStarEnd(String textline)
	{
		if (starendindex != -1)
		{
			TextData.insert(starendindex, textline);
			starendindex += textline.length();
			TextData.insert(starendindex, TN.nl);
			starendindex += TN.nl.length();
		}
		else
			AppendLine(textline);
	}


	/////////////////////////////////////////////
	public OneTunnel(String lname, LegLineFormat NewLegLineFormat)
	{
		name = lname.toLowerCase();
		uptunnel = null;
		fullname = name;

		// the eq name tree.
		fulleqname = name;
		depth = 0;

		if (NewLegLineFormat != null)
			InitialLegLineFormat = new LegLineFormat(NewLegLineFormat);
	};

	/////////////////////////////////////////////
	public OneTunnel IntroduceSubTunnel(OneTunnel subtunnel)
	{
		// extend the array.
		if (downtunnels == null)
			downtunnels = new OneTunnel[1];
		else if (ndowntunnels == downtunnels.length)
		{
			OneTunnel[] ldowntunnels = downtunnels;
			downtunnels = new OneTunnel[ndowntunnels * 2];
			for (int j = 0; j < ndowntunnels; j++)
			{
				downtunnels[j] = ldowntunnels[j];
				downtunnels[j + ndowntunnels] = null;
			}
		}

// should check this subtunnel is actually new.


		subtunnel.uptunnel = this;
		subtunnel.fullname = fullname + TN.PathDelimeter + subtunnel.name;

		// the eq name tree.
		subtunnel.fulleqname = fulleqname + TN.PathDelimeter + subtunnel.name;
		if (!subtunnel.fulleqname.equals(subtunnel.fullname))
			TN.emitMessage("eq name is: " + subtunnel.fulleqname + "  for: " + subtunnel.fullname);

		subtunnel.depth = depth + 1;

		downtunnels[ndowntunnels] = subtunnel;
		ndowntunnels++;

		return subtunnel;
	}



	/////////////////////////////////////////////
	void emitMalformedSvxWarning(String mess)
	{
		TN.emitWarning("Malformed svx warning: " + mess);
	}

	/////////////////////////////////////////////
	// pulls stuff into vlegs and vstations.
	private void InterpretSvxText(LineInputStream lis)
	{
		// make working copy (will be from new once the header is right).
		LegLineFormat CurrentLegLineFormat = new LegLineFormat(InitialLegLineFormat);

		while (lis.FetchNextLine())
		{
			if (lis.w[0].equals(""))
				;
			else if (lis.w[0].equalsIgnoreCase("*calibrate"))
				CurrentLegLineFormat.StarCalibrate(lis.w[1], lis.w[2], lis.w[3], lis);
			else if (lis.w[0].equalsIgnoreCase("*units"))
				CurrentLegLineFormat.StarUnits(lis.w[1], lis.w[2], lis);
			else if (lis.w[0].equalsIgnoreCase("*set"))
				CurrentLegLineFormat.StarSet(lis.w[1], lis.w[2], lis);
			else if (lis.w[0].equalsIgnoreCase("*data"))
			{
				if (!CurrentLegLineFormat.StarDataNormal(lis.w, lis.iwc))
					TN.emitWarning("Bad *data line:  " + lis.GetLine());
			}

			else if (lis.w[0].equalsIgnoreCase("*fix") || lis.w[0].equalsIgnoreCase("*pos_fix"))
			{
				OneLeg NewTunnelLeg = CurrentLegLineFormat.ReadFix(lis.w, this, lis.w[0].equalsIgnoreCase("*pos_fix"), lis);
				if (NewTunnelLeg != null)
					vlegs.addElement(NewTunnelLeg);
			}

			else if (lis.w[0].equalsIgnoreCase("*date"))
				svxdate = lis.w[1];
			else if (lis.w[0].equalsIgnoreCase("*title"))
				svxtitle = lis.w[1];
			else if (lis.w[0].equalsIgnoreCase("*flags"))
				; // ignore for now
			else if (lis.w[0].equalsIgnoreCase("*team"))
			{
				if (lis.w[1].equalsIgnoreCase("notes"))
					teamnotes = lis.remainder2.trim();
				else if (lis.w[1].equalsIgnoreCase("tape"))
					teamtape = lis.remainder2.trim();
				else if (lis.w[1].equalsIgnoreCase("insts"))
					teaminsts = lis.remainder2.trim();
				else if (lis.w[1].equalsIgnoreCase("pics"))
					teampics = lis.remainder2.trim();
				else
					; // TN.emitMessage("Unknown *team " + lis.remainder1);
			}

			else if (lis.w[0].equalsIgnoreCase("*begin"))
				TN.emitWarning("word should have been stripped");
			else if (lis.w[0].equalsIgnoreCase("*end"))
				TN.emitWarning("word should have been stripped");
			else if (lis.w[0].equalsIgnoreCase("*include"))
				TN.emitWarning("word should have been stripped");

			else if (lis.w[0].equalsIgnoreCase("*entrance"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*instrument"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*export"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*equate"))
				; // ignore.
			else if (lis.w[0].equalsIgnoreCase("*sd"))
				; // ignore.

			else if (lis.w[0].startsWith("*"))
				TN.emitWarning("Unknown command: " + lis.w[0]);

			else if (lis.iwc >= 2) // used to be ==.  want to use the ignoreall term in the *data normal...
			{
				OneLeg NewTunnelLeg = CurrentLegLineFormat.ReadLeg(lis.w, this, lis);
				if (NewTunnelLeg != null)
					vlegs.addElement(NewTunnelLeg);
			}

			else
			{
				TN.emitWarning("Too few arguments: " + lis.GetLine());
			}
		}
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.
	private void RefreshTunnelRecurse(OneTunnel vgsymbols, Vector vtunnels)
	{
		vtunnels.addElement(this);

		// now scan the data
		LineInputStream lis = new LineInputStream(getTextData(), svxfile);

		vlegs.removeAllElements();

		InterpretSvxText(lis);

		// refresh the symbols
		for (int i = 0; i < tsketches.size(); i++)
		{
			OneSketch os = (OneSketch)tsketches.elementAt(i);
			for (int j = 0; j < os.vssymbols.size(); j++)
				((OneSSymbol)os.vssymbols.elementAt(j)).RefreshSymbol(vgsymbols);
		}

		// apply export names to the stations listed in the legs,
		// and to the stations listed in the xsections

		// the recurse bit
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].RefreshTunnelRecurse(vgsymbols, vtunnels);
	}


	/////////////////////////////////////////////
	class sortdate implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			OneTunnel ot1 = (OneTunnel)o1;
			OneTunnel ot2 = (OneTunnel)o2;
			return ot1.svxdate.compareTo(ot2.svxdate);
		}
	}

	/////////////////////////////////////////////
	int SetOrderdateorder(Vector vtunnels)
	{
		Object[] vts = vtunnels.toArray();
		Arrays.sort(vts, new sortdate());
		for (int i = 0; i < vts.length; i++)
 			((OneTunnel)vts[i]).dateorder = i;
		return vts.length;
	}


	/////////////////////////////////////////////
	// reads the textdata and updates everything from it.
	void RefreshTunnel(OneTunnel vgsymbols)
	{
		Vector vtunnels = new Vector();
		RefreshTunnelRecurse(vgsymbols, vtunnels);
		dateorder = SetOrderdateorder(vtunnels);
System.out.println("dateorder " + dateorder);
	}


	/////////////////////////////////////////////
	void SetWFactiveRecurse(boolean lbWFtunnactive)
	{
		bWFtunnactive = lbWFtunnactive;
		for (int i = 0; i < ndowntunnels; i++)
			downtunnels[i].SetWFactiveRecurse(lbWFtunnactive);
	}


	/////////////////////////////////////////////
	void ResetUniqueBaseStationTunnels()
	{
		// the xsections
		for (int i = 0; i < vsections.size(); i++)
		{
			OneSection oxs = (OneSection)(vsections.elementAt(i));
			oxs.station0ot = this;
			oxs.station0EXS = oxs.station0S;
			oxs.station1ot = this;
			oxs.station1EXS = oxs.station1S;

			oxs.stationforeot = this;
			oxs.stationforeEXS = oxs.orientstationforeS;
			oxs.stationbackot = this;
			oxs.stationbackEXS = oxs.orientstationbackS;
		}
	}
}



