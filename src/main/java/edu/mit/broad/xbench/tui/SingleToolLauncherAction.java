/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import java.awt.Dimension;

import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

/**
 * @author Aravind Subramanian
 */
public class SingleToolLauncherAction extends WidgetAction {
    public final static Dimension DEFAULT_DIM = new Dimension(550, 380);

    private static final String getToolName(final Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }
        return ParseUtils.getLastToken(tool.getClass().getName(), ".");
    }

    private Tool fTool;
    private ParamSet fPSet;
    private String fTitle;

    public SingleToolLauncherAction(Tool tool, ParamSet pset, String titleOpt) {
        super("SingleToolLauncherAction", getToolName(tool), 
                "Set Parameters and Launch Analysis Tools", ToolLauncher.ICON);

        if (pset == null) {
            throw new IllegalArgumentException("Param pset cannot be null");
        }

        this.fTool = tool;
        this.fPSet = pset;
        this.fTitle = titleOpt;
    }

    public Widget getWidget() {
        // IMP dont place this in the class init are -- cause the app to recursively loop
        setSize(DEFAULT_DIM.width, DEFAULT_DIM.height, true);
        return new SingleToolLauncher(fTool, fPSet, true, true, true, fTitle, SingleToolLauncher.ICON);
    }
}    // End ToolAction
