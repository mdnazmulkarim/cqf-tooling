import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class LiquidNarrativeTest {
    @Test
    public void testTemplate() throws Exception {
        String template = IOUtils.toString(getClass().getResourceAsStream("/library.narrative"), StandardCharsets.UTF_8);

        Patient input = new Patient();
        input.addName().addGiven("FNAME1");
        input.addName().addGiven("FNAME2");

        TemplateNarrativeGenerator gen = new TemplateNarrativeGenerator();
        String output = gen.processLiquid(FhirContext.forDstu3(), template, input);
    }
}
