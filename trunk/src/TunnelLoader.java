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

import java.io.IOException;
import java.io.FileReader;
import java.util.Vector;


//
//
// TunnelLoader
//
//


/////////////////////////////////////////////
/////////////////////////////////////////////
class TunnelLoader
{
	TunnelXMLparse txp;
	TunnelXML tunnXML;
	OneTunnel vgsymbols;

	/////////////////////////////////////////////
	void emitError(String mess, IOException e) throws IOException
	{
		TN.emitError(mess);
		throw e;
	}


	/////////////////////////////////////////////
	void LoadSVXdata(OneTunnel tunnel)
	{
		try
		{
			LineInputStream lis = new LineInputStream(tunnel.svxfile, null, null);

			// strip the *begins and *includes
			while (lis.FetchNextLine())
			{
				if (lis.w[0].equalsIgnoreCase("*begin"))
					;
				else if (lis.w[0].equalsIgnoreCase("*end"))
					;
				else if (lis.w[0].equalsIgnoreCase("*include"))
					;
				else
					tunnel.AppendLine(lis.GetLine());
			}

			lis.close();
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
		};
	}


	/////////////////////////////////////////////
	// this rearranges the line into a svx command.
	void LoadPOSdata(OneTunnel tunnel)
	{
		tunnel.vposlegs = new Vector();
		try
		{
			LineInputStream lis = new LineInputStream(tunnel.posfile, null, null);
			while (lis.FetchNextLine())
			{
				// this is a rather poor attempt at dealing with the
				// cases of long numbers not leaving a space between
				// the parenthesis and the first number.
				if (lis.w[0].startsWith("("))
				{
					if ((lis.iwc == 5) && lis.w[1].equals("Easting") && lis.w[2].equals("Northing") && lis.w[3].equals("Altitude"))
						continue;
					int isecnum = 2;
					String sfirstnum = lis.w[1];
					if ((lis.iwc == 5) && !lis.w[0].equals("("))
					{
						sfirstnum = lis.w[0].substring(1);
						isecnum = 1;
					}
					if (isecnum + 4 != lis.iwc)
					{
						System.out.println("Unknown pos-line: " + lis.GetLine());
						continue;
					}
					float px =  Float.valueOf(sfirstnum).floatValue();
					float py =  Float.valueOf(lis.w[isecnum]).floatValue();
					float pz =  Float.valueOf(lis.w[isecnum + 1]).floatValue();
					tunnel.vposlegs.addElement(new OneLeg(lis.w[isecnum + 3], px, py, pz, tunnel, true));
				}
				else if (lis.iwc != 0)
				{
					tunnel.AppendLine(";Unknown pos-line: " + lis.GetLine());
					System.out.println("Unknown pos-line missing(: " + lis.GetLine());
				}
			}

			lis.close();
		}
		catch (IOException ie)
		{
			TN.emitWarning(ie.toString());
		};
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	static boolean FindFilesOfDirectory(OneTunnel tunnel) throws IOException
	{
		Vector fod = tunnel.tundirectory.listFilesDir(true); 

		// here we begin to open XML readers and such like, filling in the different slots.
		boolean bsomethinghere = false;
		for (int i = 0; i < fod.size(); i++)
		{
			FileAbstraction tfile = (FileAbstraction)fod.elementAt(i); 
			assert tfile.isFile(); 

			String suff = TN.getSuffix(tfile.getName());
			if (suff.equals(TN.SUFF_XML))
			{
				int iftype = tfile.GetFileType();

				// fill in the file positions according to what was in this file.
				if (iftype == FileAbstraction.TXML_EXPORTS_FILE)
				{
					assert tunnel.exportfile == null;
					tunnel.exportfile = tfile;
				}
				else if (iftype == FileAbstraction.TXML_MEASUREMENTS_FILE)
				{
					assert tunnel.measurementsfile == null;
					tunnel.measurementsfile = tfile;
				}
				else if (iftype == FileAbstraction.TXML_SKETCH_FILE)
					tunnel.tsketches.addElement(tfile);
				else if (iftype == FileAbstraction.TXML_FONTCOLOURS_FILE)
					tunnel.tfontcolours.addElement(tfile);
				else
				{
					System.out.println(tfile.getName() + " " + iftype);
					assert false;
				}

				bsomethinghere = true;
			}

			else if (suff.equals(TN.SUFF_SVX))
			{
				assert tunnel.svxfile == null;
				tunnel.svxfile = tfile;
				bsomethinghere = true;
			}
			else if (suff.equals(TN.SUFF_POS))
			{
				assert tunnel.posfile == null;
				tunnel.posfile = tfile;
			}

			else if (suff.equals(TN.SUFF_PNG) || suff.equalsIgnoreCase(TN.SUFF_GIF) || suff.equalsIgnoreCase(TN.SUFF_JPG))
				;
			else if (suff.equalsIgnoreCase(TN.SUFF_TXT))
				;
			else
			{
				int j = TN.SUFF_IGNORE.length;
				while (--j >= 0)
					if (suff.equalsIgnoreCase(TN.SUFF_IGNORE[j]))
						break;
				if (j == -1)
					TN.emitMessage("Unknown file type " + tfile.getName());
			}
		}
		return bsomethinghere;
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	boolean FileDirectoryRecurse(OneTunnel tunnel, FileAbstraction loaddirectory) throws IOException
	{
		tunnel.tundirectory = loaddirectory;
		if (!FindFilesOfDirectory(tunnel))  // nothing here
			return false;

		// get the subdirectories and recurse.
		Vector dod = loaddirectory.listFilesDir(false); 
		for (int i = 0; i < dod.size(); i++)
		{
			FileAbstraction sdir = (FileAbstraction)dod.elementAt(i); 
			assert sdir.isDirectory(); 
			String dtname = sdir.getName();
			OneTunnel dtunnel = tunnel.IntroduceSubTunnel(new OneTunnel(dtname, null));
			if (!FileDirectoryRecurse(dtunnel, sdir))
				tunnel.ndowntunnels--; // if there's nothing interesting, take this introduced tunnel back out!
		}
		return true;
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	OneSketch LoadSketchFile(OneTunnel tunnel, int isketchfileindex)
	{
		if (tunnel.tsketches.elementAt(isketchfileindex) instanceof FileAbstraction)
		{
			FileAbstraction tfile = (FileAbstraction)tunnel.tsketches.elementAt(isketchfileindex);
			String fnamess = TN.loseSuffix(tfile.getName());
			txp.SetUp(tunnel, fnamess, FileAbstraction.TXML_SKETCH_FILE);
			OneSketch tsketch = new OneSketch(tfile);
			tsketch.bsketchfilechanged = false;

			if (txp.bSymbolType)
			{
				tsketch.bSymbolType = true;
				tsketch.sketchsymbolname = TN.loseSuffix(tfile.getName());
			}

			tunnel.tsketches.setElementAt(tsketch, isketchfileindex);
			txp.tunnelsketch = tsketch;

			TN.emitMessage("loading sketch file: " + tsketch.sketchfile.getName());
			tunnXML.ParseFile(txp, tfile);
		}
		return (OneSketch)tunnel.tsketches.elementAt(isketchfileindex);
	}


	/////////////////////////////////////////////
	void LoadFilesRecurse(OneTunnel tunnel, boolean bloadsketches) throws IOException
	{
		if (tunnel.svxfile != null)
			LoadSVXdata(tunnel);
		if (tunnel.posfile != null)
			LoadPOSdata(tunnel);
		if (tunnel.exportfile != null)
		{
			txp.SetUp(tunnel, TN.loseSuffix(tunnel.exportfile.getName()), FileAbstraction.TXML_EXPORTS_FILE);
			tunnXML.ParseFile(txp, tunnel.exportfile);
		}
		if (tunnel.measurementsfile != null)
		{
			txp.SetUp(tunnel, TN.loseSuffix(tunnel.measurementsfile.getName()), FileAbstraction.TXML_MEASUREMENTS_FILE);
			tunnXML.ParseFile(txp, tunnel.measurementsfile);
		}

		// load up the font colours found
		for (int i = 0; i < tunnel.tfontcolours.size(); i++)
		{
			FileAbstraction tfile = (FileAbstraction)tunnel.tfontcolours.elementAt(i);
			System.out.println("Loading font colours:" + tfile.getName());
			txp.SetUp(tunnel, TN.loseSuffix(tfile.getName()), FileAbstraction.TXML_FONTCOLOURS_FILE);
			tunnXML.ParseFile(txp, tfile);
		}

		// load up sketches
		if (bloadsketches)
		{
			for (int i = 0; i < tunnel.tsketches.size(); i++)
				LoadSketchFile(tunnel, i);
		}

		// do all the subtunnels
    	for (int i = 0; i < tunnel.ndowntunnels; i++)
			LoadFilesRecurse(tunnel.downtunnels[i], bloadsketches);
	}


	/////////////////////////////////////////////
	/////////////////////////////////////////////
	public TunnelLoader(OneTunnel vgsymbols, SketchLineStyle sketchlinestyle)
	{
		txp = new TunnelXMLparse(vgsymbols);
		txp.bSymbolType = (vgsymbols == null);
		txp.sketchlinestyle = sketchlinestyle; // for loading up the fontcolours
		tunnXML = new TunnelXML();
	}
};
