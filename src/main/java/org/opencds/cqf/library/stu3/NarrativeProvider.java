package org.opencds.cqf.library.stu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.library.BaseNarrativeProvider;

import java.util.Objects;

public class NarrativeProvider extends BaseNarrativeProvider<Narrative> {

    public NarrativeProvider() {
        super(Objects.requireNonNull(NarrativeProvider.class.getClassLoader().getResource("narratives/stu3/narrative.properties")).toString());
    }

    public NarrativeProvider(String pathToPropertiesFile)
    {
        super(pathToPropertiesFile);
    }

    public Narrative getNarrative(FhirContext context, IBaseResource resource) {
        Narrative narrative = new Narrative();
        this.getGenerator().generateNarrative(context, resource, narrative);
        return narrative;
    }
}