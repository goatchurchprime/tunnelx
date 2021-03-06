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

import java.io.StringReader;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;


import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Color;

import java.awt.geom.Line2D;
//import java.awt.geom.Line2D.Float;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Shape;
import java.awt.geom.GeneralPath;


////////////////////////////////////////////////////////////////////////////////
class PathLabelElement
{
	String text;
	float xcelloffset = 0.0F;
	float xcelloffsettop = 0.0F;
	float ycelloffset = 0.0F;
	int yiline;
	boolean bcontinuation = false;

	boolean btextwidthset = false;
	boolean btextheightset = false;
	boolean btextwidthtopset = false;
	boolean bfontmagnifyset = false;

	float ftextjustify = -1.0F; // 0.0 left, 0.5 centre, 1.0 right

	float textwidth;
	float textheight;
	float textwidthtop;
	float fontmagnify = 1.0F;

	float defaultden; // for carrying over between lines
	Rectangle2D textrect = null;
	Shape textshape = null;

	static int HZT_HORIZONTAL_WIDTH = 0; 
	static int HZT_VERTICAL_WIDTH = 1; 
	static int HZT_TOPLINE_WIDTH = 2; 
	static int HZT_FONTMAGNIFY = 3; 

	////////////////////////////////////////////////////////////////////////////////
	PathLabelElement(String ltext, float ldefaultden, float ldefaultftextjustify)
	{
		defaultden = ldefaultden;
		ftextjustify = ldefaultftextjustify;

		// works out if it is a genuine new line
		if (ltext.startsWith(";"))
		{
			bcontinuation = true;
			text = ltext.substring(1).trim();
		}
		else
			text = ltext.trim();

		// extract the width coding of %dd/dddd%
		while (text.indexOf('%') == 0)
		{
			if (text.startsWith("%left%"))
			{
				ftextjustify = 0.0F;
				text = text.substring(6).trim();
				continue;
			}
			if (text.startsWith("%centre%") || text.startsWith("%center%"))
			{
				ftextjustify = 0.5F;
				text = text.substring(8).trim();
				continue;
			}
			if (text.startsWith("%right%"))
			{
				ftextjustify = 1.0F;
				text = text.substring(7).trim();
				continue;
			}
			int islashps = text.indexOf('/', 1);
			int ipercps = text.indexOf('%', 1);
			if ((ipercps == -1) || (islashps == -1) || !(islashps < ipercps))
				break;

			int numstart = 1;

			// h = horizontal width
			// v = vertical width
			// t = top line width (for doing triangles, as in North Arrow)
			// f = make the font bigger
			int ihoriztype = HZT_HORIZONTAL_WIDTH; 
			
			if (text.charAt(numstart) == 'v')
			{
				ihoriztype = HZT_VERTICAL_WIDTH;
				numstart++;
			}
			else if (text.charAt(numstart) == 't')
			{
				ihoriztype = HZT_TOPLINE_WIDTH;
				numstart++;
			}
			else if (text.charAt(numstart) == 'h')
			{
				ihoriztype = HZT_HORIZONTAL_WIDTH;
				numstart++;
			}
			else if (text.charAt(numstart) == 'f')
			{
				ihoriztype = HZT_FONTMAGNIFY;
				numstart++;
			}
			else
				ihoriztype = HZT_HORIZONTAL_WIDTH; // default case

			float textdim = -1.0F;
			String numstr = text.substring(numstart, islashps).trim();
			String denstr = text.substring(islashps + 1, ipercps).trim();

			// extract the numbers
			try
			{
				float num = (float)Double.parseDouble(numstr);  // compilation error with Float
				if (!denstr.equals(""))  // carry the default value over when empty
					defaultden = (float)Double.parseDouble(denstr);
				if ((num < 0.0) || (defaultden <= 0.0))
					break;
				textdim = TN.CENTRELINE_MAGNIFICATION * num / defaultden;
				text = text.substring(ipercps + 1).trim();
			}
			catch (NumberFormatException e)
				{ break; }

			if (ihoriztype == HZT_HORIZONTAL_WIDTH)
			{
				btextwidthset = true;
				textwidth = textdim;
			}
			else if (ihoriztype == HZT_TOPLINE_WIDTH)
			{
				btextwidthtopset = true;
				textwidthtop = textdim;
			}
			else if (ihoriztype == HZT_FONTMAGNIFY)
			{
				bfontmagnifyset = true; 
				fontmagnify = textdim / TN.CENTRELINE_MAGNIFICATION; 
			}
			else
			{
				assert ihoriztype == HZT_VERTICAL_WIDTH;
				btextheightset = true;
				textheight = textdim;
			}
		}

		// then a string of %blackrect% or %whiterect% will make the scalebar pieces rather than write the text
	}

	////////////////////////////////////////////////////////////////////////////////
	void MakeTextRect(float xpos, float ypos)	
	{
		textrect = new Rectangle2D.Float(xpos + xcelloffset, ypos - ycelloffset, textwidth, textheight);
		if ((textwidthtop != textwidth) || (xcelloffset != xcelloffsettop))
		{
			GeneralPath gp = new GeneralPath();
			gp.moveTo(xpos + xcelloffsettop, ypos - ycelloffset);
			gp.lineTo(xpos + xcelloffsettop + textwidthtop, ypos - ycelloffset);
			gp.lineTo(xpos + xcelloffset + textwidth, ypos + textheight - ycelloffset);
			gp.lineTo(xpos + xcelloffset, ypos + textheight - ycelloffset);
			gp.closePath();
			textshape = gp;
		}
		else
			textshape = textrect;
	}
};


////////////////////////////////////////////////////////////////////////////////
class PathLabelDecode
{
	// if it's a set of symbols
	List<String> vlabsymb = new ArrayList<String>();

	// the area symbol
	int iarea_pres_signal = 0; // combobox lookup
	int barea_pres_signal = SketchLineStyle.ASE_KEEPAREA; // 0 normal, 1 dropdown, 2 hole, 3 kill area, 55 sketchframe

	// when barea_pres_signal is ASE_SKETCHFRAME, sketchframe
    SketchFrameDef sketchframedef = null;

	// when barea_pres_signal is ASE_ZSETRELATIVE
	float nodeconnzsetrelative = 0.0F;

	// the label drawing
	String sfontcode = "";
	LabelFontAttr labfontattr = null;
	Color color = null;

// could set a font everywhere with the change of the style
	float fnodeposxrel = -1.0F;
	float fnodeposyrel = -1.0F;
	boolean barrowpresent = false;
	boolean bboxpresent = false;
	String drawlab = "";

	// values used by a centreline
	String centrelinetail = null;
	String centrelinehead = null;
	String centrelineelev = null;


	// linesplitting of the drawlabel (using lazy evaluation)
	List<PathLabelElement> vdrawlablns = new ArrayList<PathLabelElement>();
	int yilines = 0;

	// these could be used for mouse click detection (for dragging of labels)
	private String drawlab_bak = "";
	private Font font_bak = null;
	Font font = null;
	private float fnodeposxrel_bak;
	private float fnodeposyrel_bak;
	private float x_bak;
	private float y_bak;

	float fmdescent;
	float lnspace;
	float drawlabxoff;
	float drawlabyoff;
	float drawlabxwid;
	private float drawlabyhei;

	/*private */float[] arrc = null; // store of the arrow endpoint coords

	Line2D[] arrowdef = null;
	Rectangle2D rectdef = null;

	/////////////////////////////////////////////
	PathLabelDecode()
	{
	}

	/////////////////////////////////////////////
	public String toString()
	{
		assert false;
		return "tail=" + (centrelinetail == null ? "" : centrelinetail) + " head=" + (centrelinehead == null ? "" : centrelinehead);
	}

	/////////////////////////////////////////////
	PathLabelDecode(PathLabelDecode o)
	{
		iarea_pres_signal = o.iarea_pres_signal;
		barea_pres_signal = o.barea_pres_signal;

		if (barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME) // iarea_pres_signal is the index into the combobox, b is the code.
			sketchframedef = new SketchFrameDef(o.sketchframedef);

		nodeconnzsetrelative = o.nodeconnzsetrelative;

		vlabsymb.addAll(o.vlabsymb);
		drawlab = o.drawlab;
		sfontcode = o.sfontcode;
		fnodeposxrel = o.fnodeposxrel;
		fnodeposyrel = o.fnodeposyrel;
		barrowpresent = o.barrowpresent;
		bboxpresent = o.bboxpresent;
		centrelinehead = o.centrelinehead;
		centrelinetail = o.centrelinetail;
		centrelineelev = o.centrelineelev;
	}



	/////////////////////////////////////////////
	// reverse of decoding for saving
	void WriteXML(LineOutputStream los, int indent) throws IOException
	{
		WriteXML(los, indent, true);
	}

	/////////////////////////////////////////////
    boolean IsCentrelineType()
    {
        if (centrelinehead != null)
        {
            assert ((centrelinetail != null)); 
            assert (centrelineelev == null); 
            return true; 
        }
        
		assert (centrelinetail == null); 
        return false; 
    }

	/////////////////////////////////////////////
	void WriteXML(LineOutputStream los, int indent, boolean pathcodes) throws IOException
	{
		if (pathcodes)
			los.WriteLine(TNXML.xcomopen(indent, TNXML.sPATHCODES));
		if (IsCentrelineType())
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sCL_STATIONS, TNXML.sCL_TAIL, centrelinetail, TNXML.sCL_HEAD, centrelinehead));
		if (centrelineelev != null)
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sCL_STATIONS, TNXML.sCL_ELEV, centrelineelev));
		if (drawlab != null)
		{
			if (barrowpresent || bboxpresent)
				los.WriteLine(TNXML.xcomtext(indent + 1, TNXML.sPC_TEXT, TNXML.sLTEXTSTYLE, sfontcode, TNXML.sPC_NODEPOSXREL, String.valueOf(fnodeposxrel), TNXML.sPC_NODEPOSYREL, String.valueOf(fnodeposyrel), TNXML.sPC_ARROWPRES, (barrowpresent ? "1" : "0"), TNXML.sPC_BOXPRES, (bboxpresent ? "1" : "0"), TNXML.xmanglxmltext(drawlab)));
			else
				los.WriteLine(TNXML.xcomtext(indent + 1, TNXML.sPC_TEXT, TNXML.sLTEXTSTYLE, sfontcode, TNXML.sPC_NODEPOSXREL, String.valueOf(fnodeposxrel), TNXML.sPC_NODEPOSYREL, String.valueOf(fnodeposyrel), TNXML.xmanglxmltext(drawlab)));
		}

		// the area signal
		if (iarea_pres_signal != 0)
		{
			if (barea_pres_signal == SketchLineStyle.ASE_SKETCHFRAME) // iarea_pres_signal is the index into the combobox, b is the code.
                sketchframedef.WriteXML(SketchLineStyle.areasignames[iarea_pres_signal], los, indent + 1);
			else if (barea_pres_signal == SketchLineStyle.ASE_ZSETRELATIVE)
				los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, SketchLineStyle.areasignames[iarea_pres_signal], TNXML.sASIG_NODECONN_ZSETRELATIVE, String.valueOf(nodeconnzsetrelative)));
			else
				los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPC_AREA_SIGNAL, TNXML.sAREA_PRESENT, SketchLineStyle.areasignames[iarea_pres_signal]));
		}

		// the symbols
		for (String rname : vlabsymb)
			los.WriteLine(TNXML.xcom(indent + 1, TNXML.sPC_RSYMBOL, TNXML.sLRSYMBOL_NAME, rname));

		if (pathcodes)
			los.WriteLine(TNXML.xcomclose(indent, TNXML.sPATHCODES));
	}

	/////////////////////////////////////////////
	// used for accessing the fontmetrics function
	static BufferedImage fm_image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	static Graphics fm_g = fm_image.getGraphics();
	/////////////////////////////////////////////
	static float arrowheadlength = 5.0F;
	static float arrowheadwidth = 3.0F;
	static float arrowtailstart = 1.5F;


	/////////////////////////////////////////////
	void UpdateLabel(float x, float y, float xend, float yend)
	{
		assert ((drawlab != null) && (drawlab.length() != 0));
		font = (labfontattr == null ? SketchLineStyle.defaultfontlab : labfontattr.fontlab);

		// find what aspects of the text need updating
		boolean blabelchanged = !drawlab.equals(drawlab_bak);
		boolean bfontchanged = (font_bak != font);
		boolean bposchanged = ((fnodeposxrel_bak != fnodeposxrel) || (fnodeposyrel_bak != fnodeposyrel) || (x_bak != x) || (y_bak != y));

		// break up the label string
		if (blabelchanged)
		{
			vdrawlablns.clear();
			int ps = 0;
			float defaultden = -1.0F;
			float defaultftextjustify = 0.0F;
			for (String sple : drawlab.trim().split("\\n"))
			{
				sple = sple.replaceAll("\\t", "    ");  // can't handle tabs properly
				PathLabelElement ple = 	new PathLabelElement(sple, defaultden, defaultftextjustify);
				defaultden = ple.defaultden;
				defaultftextjustify = ple.ftextjustify;
				vdrawlablns.add(ple);
			}
			drawlab_bak = drawlab;

			for (PathLabelElement ple : vdrawlablns)
			{
				if (ple.bcontinuation && (yilines != 0))
					yilines--;
				ple.yiline = yilines;
				yilines++;
			}
		}

		// we break up the string into lines
		FontMetrics fm = (blabelchanged || bfontchanged || bposchanged ? fm_g.getFontMetrics(font) : null); 
		// for using few functions from the given GraphicsAbstraction which may be overwritten but not fully implemented
		if (blabelchanged || bfontchanged)
		{
			lnspace = fm.getAscent() + 0*fm.getLeading();
			drawlabyhei = lnspace * (yilines - 1) + fm.getAscent();
			fmdescent = fm.getDescent();

			drawlabxwid = 0.0F;
			drawlabyhei = 0.0F;
			PathLabelElement pleprev = null;
			for (PathLabelElement ple : vdrawlablns)
			{
				if (!ple.btextwidthset)
					ple.textwidth = fm.stringWidth(ple.text);
				if (!ple.btextwidthtopset)
					ple.textwidthtop = ple.textwidth;
				if (!ple.btextheightset)
				{
					ple.textheight = lnspace;
					if (ple.bfontmagnifyset)
						ple.textheight = lnspace * ple.fontmagnify; // for when we make the font bigger (for titles, etc)
				}
				
				if (ple.bcontinuation && (pleprev != null))
				{
					ple.xcelloffset = pleprev.xcelloffset + pleprev.textwidth;
					ple.xcelloffsettop = pleprev.xcelloffsettop + pleprev.textwidthtop;
					ple.ycelloffset = pleprev.ycelloffset;
				}
				else
				{
					ple.xcelloffset = 0.0F;
					ple.xcelloffsettop = 0.0F;
					ple.ycelloffset = -drawlabyhei;
					drawlabyhei += ple.textheight;
				}
				drawlabxwid = Math.max(drawlabxwid, ple.xcelloffset + ple.textwidth);
				pleprev = ple;

				//System.out.println(":" + i + ":" + ple.textwidth + "~" + ple.textheight + "  " + ple.text);
			}
			font_bak = font;
		}

		if (blabelchanged || bfontchanged || bposchanged)
		{
			// we find the point for the string
			drawlabxoff = -drawlabxwid * (fnodeposxrel + 1) / 2;
			drawlabyoff = drawlabyhei * (fnodeposyrel - 1) / 2;
			PathLabelElement pleprev = null;
			for (PathLabelElement ple : vdrawlablns)
			{
				ple.MakeTextRect(x + drawlabxoff, y + drawlabyoff);
				pleprev = ple;
			}

			// should be made by merging the textrect rectangles
			rectdef = new Rectangle2D.Float(x + drawlabxoff, y + drawlabyoff, drawlabxwid, drawlabyhei);

			fnodeposxrel_bak = fnodeposxrel;
			fnodeposyrel_bak = fnodeposyrel;
			x_bak = x;
			y_bak = y;
		}

		// now relay the positions of the lines
		if (barrowpresent && ((arrc == null) || (arrc[0] != x) || (arrc[1] != xend) || (arrc[2] != y) || (arrc[3] != yend)))
		{
			if (arrc == null)
				arrc = new float[4];
			arrc[0] = x;
			arrc[1] = xend;
			arrc[2] = y;
			arrc[3] = yend;

			if (arrowdef == null)
				arrowdef = new Line2D.Float[3];

			float xv = xend - x;
			float yv = yend - y;
			float ln = (float)Math.sqrt(xv * xv + yv * yv);
			if (ln <= arrowtailstart)
				return;
			float xvu = xv / ln;
			float yvu = yv / ln;
			arrowdef[0] = new Line2D.Float(x + xvu * arrowtailstart, y + yvu * arrowtailstart, xend, yend);
			arrowdef[1] = new Line2D.Float(xend - xvu * arrowheadlength + yvu * arrowheadwidth, yend - yvu * arrowheadlength - xvu * arrowheadwidth, xend, yend);
			arrowdef[2] = new Line2D.Float(xend - xvu * arrowheadlength - yvu * arrowheadwidth, yend - yvu * arrowheadlength + xvu * arrowheadwidth, xend, yend);
		}
	}
};


// fancy spread stuff (to be refactored later)
/*
		String labspread = TNXML.xrawextracttext(plabel, TNXML.sSPREAD);
		if (labspread == null)
		{
			int ps = plabel.indexOf(TNXML.sLRSYMBOL);
			int pe = plabel.indexOf("/>");

			// standard label drawing
			// (this shall take <br> and <font> changes)
			if ((ps == -1) || (pe == -1))
				ga.drawString(plabel, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}
, (plabedl.labfontattr == null ? SketchLineStyle.defaultfontlab : plabedl.labfontattr.fontlab)
		// implements the spread label drawing.
		if ((nlines == 0) || (labspread.length() < 2))
		{
			ga.drawString(labspread, (float)pnstart.pn.getX(), (float)pnstart.pn.getY());
			return;
		}

		// update the label points only when necessary.
		int currlabelcode = (bSplined ? nlines : -nlines);
		if ((currlabelcode != prevlabelcode) || (vlabelpoints == null) || (vlabelpoints.size() != labspread.length()))
		{
			TN.emitMessage("spreading text");
			prevlabelcode = currlabelcode;

			float[] pco = GetCoords(); // not spline for now.

			// measure lengths
			float[] lengp = new float[nlines + 1];
			lengp[0] = 0.0F;
			for (int i = 1; i <= nlines; i++)
			{
				float xv = pco[i * 2] - pco[i * 2 - 2];
				float yv = pco[i * 2 + 1] - pco[i * 2 - 1];
				lengp[i] = lengp[i - 1] + (float)Math.sqrt(xv * xv + yv * yv);
			}

			// make up the labelpoints array.
			if (vlabelpoints == null)
				vlabelpoints = new ArrayList<Point2D>();
			vlabelpoints.setSize(labspread.length());  // appears not to be a function in ArrayList

			// find the locations.
			int il = 1;
			for (int j = 0; j < labspread.length(); j++)
			{
				float lenb = lengp[nlines] * j / (labspread.length() - 1);
				while ((il < nlines) && (lengp[il] < lenb))
					il++;

				// find the lambda along this line.
				float lamden = lengp[il] - lengp[il - 1];
				float lam = (lamden != 0.0F ? (lengp[il] - lenb) / lamden : 0.0F);
				float tx = lam * pco[il * 2 - 2] + (1.0F - lam) * pco[il * 2];
				float ty = lam * pco[il * 2 - 1] + (1.0F - lam) * pco[il * 2 + 1];

				if (vlabelpoints.get(j) == null)
					vlabelpoints.set(j, new Point2D.Float());
				vlabelpoints.get(j).setLocation(tx, ty);
			}
		}

		for (int i = 0; i < labspread.length(); i++)
		{
			Point2D pt = vlabelpoints.get(i);
			ga.drawString(labspread.substring(i, i + 1), (float)pt.getX(), (float)pt.getY());
		}
*/
