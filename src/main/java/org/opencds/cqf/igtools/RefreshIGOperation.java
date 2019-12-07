package org.opencds.cqf.igtools;

import org.opencds.cqf.Operation;
import org.opencds.cqf.igtools.IGProcessor;
import org.opencds.cqf.utilities.ArgUtils;

public class RefreshIGOperation extends Operation {

    private String igPath;
    private String igVersion;
    private Boolean includeELM;
    private Boolean includeDependencies;
    private Boolean includeTerminology;
    private Boolean includeTestCases;
    private Boolean versioned;

    public RefreshIGOperation() {    
    } 

    @Override
    public void execute(String[] args) {
        initializeArgs(args);
        IGProcessor.refreshIG(igPath, igVersion, includeELM,  includeDependencies, includeTerminology, includeTestCases, versioned);
    }

    private void initializeArgs(String[] args) {
        ArgUtils.ensure("RefreshIg", args);

        igPath = ArgUtils.getValue("igPath", args, true);
        igVersion = ArgUtils.getValue("igVersion", args);
        if (igVersion.equals("")) {
            igVersion = IGProcessor.getIgVersion(igPath);
        }

        includeELM = ArgUtils.isTrue("includeELM", args);
        includeDependencies = ArgUtils.isTrue("includeDependencies", args);
        includeTerminology = ArgUtils.isTrue("includeTerminology", args);
        includeTestCases = ArgUtils.isTrue("includeTestCases", args);
        versioned = ArgUtils.isTrue("versioned", args);
    }
}
