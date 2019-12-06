package org.opencds.cqf.library;
import ca.uhn.fhir.context.FhirContext;
import org.cqframework.cql.cql2elm.*;
import org.cqframework.cql.elm.tracking.TrackBack;
import org.hl7.elm.r1.ValueSetDef;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.Operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.cqframework.cql.cql2elm.CqlTranslator;

public abstract class BaseLibraryGenerator<L extends IBaseResource, T extends BaseNarrativeProvider> extends Operation {

    T narrativeProvider;
    FhirContext fhirContext;

    String operationName;
    String encoding = "json";
    private File cqlContentDir;
    private File[] cqlFiles;

    private String pathToCQLContent;
    private String pathToCqlContentDir;
    private ModelManager modelManager;
    private LibraryManager libraryManager;
    private LibrarySourceProvider sourceProvider;

    String pathToLibrary;
    Map<String, CqlTranslator> translatorMap = new HashMap<>();
    Map<String, String> cqlMap = new HashMap<>();
    Map<String, String> elmMap = new HashMap<>();
    Map<String, L> libraryMap = new HashMap<>();

    @Override
    public void execute(String[] args) {
        buildArgs(args);
        setRelevantCqlFiles();
        
        modelManager = new ModelManager();
        sourceProvider = new GenericLibrarySourceProvider(pathToCqlContentDir);
        libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().registerProvider(sourceProvider);

        translateCqlFiles();

        for (Map.Entry<String, CqlTranslator> entry : translatorMap.entrySet()) {
            if (!libraryMap.containsKey(entry.getKey())) {
                processLibrary(entry.getKey(), entry.getValue());
            }
        }

        output();
    }

    private void buildArgs(String[] args) {
        for (String arg : args) {
            if (arg.equals(operationName)) {
                continue;
            }

            String[] flagAndValue = arg.split("=");
            String flag = flagAndValue[0];
            String value = flagAndValue.length < 2 ? null : flagAndValue[1];

            if (flag.equals("-pathtocqlcontent") || flag.equals("-ptcql")) {
                pathToCQLContent = value;
            }
            else if (flag.equals("-pathtolibrary") || flag.equals("-ptl")) {
                pathToLibrary = value;
            }
            else if (flag.equals("-encoding") || flag.equals("-e")) {
                encoding = value == null ? "json" : value.toLowerCase();
            }
            else if (flag.equals("-outputpath") || flag.equals("-op")) {
                setOutputPath(value);
            }
        }

        if(pathToCQLContent == null)
        {
            throw new IllegalArgumentException("The path to the CQL Content is required");
        }  
    }

    private void setRelevantCqlFiles() {
        File cqlContent = new File(pathToCQLContent);
        cqlContentDir = cqlContent.getParentFile();
        pathToCqlContentDir = cqlContentDir.getPath();
        if (!cqlContentDir.isDirectory()) {
            throw new IllegalArgumentException("The specified path to library files is not a directory");
        }
        String cql = getCql(cqlContent);
        ArrayList<String> dependencyLibraries = getIncludedLibraries(cql);
        File[] allCqlContentFiles = cqlContentDir.listFiles();
        if (allCqlContentFiles == null) {
            return;
        }
        else if (allCqlContentFiles.length == 0) {
            return;
        }
        ArrayList<File> dependencyLibrarieFiles = new ArrayList<File>();
        dependencyLibrarieFiles.add(cqlContent);
        for (File cqlFile : allCqlContentFiles) {
            if (dependencyLibraries.contains(cqlFile.getName().replace(".cql", ""))) {
                dependencyLibrarieFiles.add(cqlFile);
            }

        }
        cqlFiles = dependencyLibrarieFiles.toArray(new File[0]);
        if (cqlFiles == null) {
            return;
        }
        else if (cqlFiles.length == 0) {
            return;
        }

    }

    private void translateCqlFiles() {
        CqlTranslator translator;
        for (File cqlFile : cqlFiles) {
            if (!cqlFile.getName().endsWith(".cql")) continue;
            translator = translate(cqlFile);
            translatorMap.put(translator.toELM().getIdentifier().getId(), translator);
            cqlMap.put(translator.toELM().getIdentifier().getId(), getCql(cqlFile));
            if (encoding.equals("json")) {
                elmMap.put(translator.toELM().getIdentifier().getId(), translator.toJson());
            }
            else {
                elmMap.put(translator.toELM().getIdentifier().getId(), translator.toXml());
            }
        }
    }

    //instead of processLibrary this would be refreshLibrary or refreshMeasure
    public abstract void processLibrary(String id, CqlTranslator translator);
    
    public abstract void output();

    private String getCql(File file) {
        StringBuilder cql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                cql.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Error reading CQL file: " + file.getName());
        }
        return cql.toString();
    }

    private CqlTranslator translate(File cqlFile) {
        try {
            ArrayList<CqlTranslator.Options> options = new ArrayList<>();
            options.add(CqlTranslator.Options.EnableDateRangeOptimization);

            CqlTranslator translator =
                    CqlTranslator.fromFile(
                            cqlFile,
                            modelManager,
                            libraryManager,
                            options.toArray(new CqlTranslator.Options[options.size()])
                    );

            if (translator.getErrors().size() > 0) {
                System.err.println("Translation failed due to errors:");
                ArrayList<String> errors = new ArrayList<>();
                for (CqlTranslatorException error : translator.getErrors()) {
                    TrackBack tb = error.getLocator();
                    String lines = tb == null ? "[n/a]" : String.format("[%d:%d, %d:%d]",
                            tb.getStartLine(), tb.getStartChar(), tb.getEndLine(), tb.getEndChar());
                    System.err.printf("%s %s%n", lines, error.getMessage());
                    errors.add(lines + error.getMessage());
                }
                throw new IllegalArgumentException(errors.toString());
            }

            return translator;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Error encountered during CQL translation: " + e.getMessage());
        }
    }

    String getValueSetId(String valueSetName) {
        for (CqlTranslator translator : translatorMap.values()) {
            org.hl7.elm.r1.Library.ValueSets valueSets = translator.toELM().getValueSets();
            if (valueSets != null) {
                for (ValueSetDef def : valueSets.getDef()) {
                    if (def.getName().equals(valueSetName)) {
                        return def.getId();
                    }
                }
            }
        }
        return valueSetName;
    }

    private ArrayList<String> getIncludedLibraries(String cql) {
        int includeDefinitionIndex = cql.indexOf("include");
        String[] includedDefsAndBelow = cql.substring(includeDefinitionIndex).split("\\n");

        int index = 0; 
        ArrayList<String> relatedArtifacts = new ArrayList<String>();
        while (includedDefsAndBelow[index].startsWith("include")) {
            String includedLibraryName = includedDefsAndBelow[index].replace("include ", "").split(" version ")[0];
            String includedLibraryVersion = includedDefsAndBelow[index].replace("include ", "").split(" version ")[1].replaceAll("\'", "").split(" called")[0];
            String includedLibraryId = includedLibraryName + "-" + includedLibraryVersion;
            relatedArtifacts.add(includedLibraryId);
            index++;
        }
        return relatedArtifacts;
    }
    private String getIdFromSource(String cql) {
        if (cql.startsWith("library")) {
            return getNameFromSource(cql);
        }

        throw new RuntimeException("This tool requires cql libraries to include a named/versioned identifier");
    }

    private String getNameFromSource(String cql) {
        return cql.replaceFirst("library ", "").split(" version")[0].replaceAll("\"", "");
    }
    private String getVersionFromSource(String cql) {
        return cql.split("version")[1].split("'")[1];
    }
}
