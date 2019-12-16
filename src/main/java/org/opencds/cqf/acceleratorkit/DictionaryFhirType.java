package org.opencds.cqf.acceleratorkit;

import java.util.Arrays;

/**
 * Created by Bryn on 8/18/2019.
 */
public class DictionaryFhirType {
    private String resourceType = "Observation";
    public String getResourceType() {
        return this.resourceType;
    }
    private String resourcePath = "value[x]";
    public String getResourcePath() {
        return this.resourcePath;
    }
    private String resourceTypeAndPath = "Observation.value[x]";
    public String getResourceTypeAndPath() {
        return this.resourceTypeAndPath;
    }
    public void setResource(String resource) {
        this.resourceTypeAndPath = resource;
        if (resource.contains(".")) {
            String[] elements = resource.split("\\.");
            if (elements.length >= 2) {
                switch (elements[0].toLowerCase()) {
                    case "observation": this.resourceType = "Observation"; break;
                    case "encounter": this.resourceType = "Encounter"; break;
                    case "patient": this.resourceType = "Patient"; break;
                    case "coverage": this.resourceType = "Coverage"; break;
                    default: this.resourceType = elements[0]; break;
                }
                this.resourcePath = String.join(".", Arrays.copyOfRange(elements, 1, elements.length));
            }
        }
    }

    private String baseProfile;
    public String getBaseProfile() {
        if (this.baseProfile != null && !this.baseProfile.isEmpty() && this.baseProfile.toLowerCase() != "fhir") {
            return this.baseProfile;
        }
        return String.format("http://hl7.org/fhir/StructureDefinition/%s", this.getResourceType());
    }
    public void setBaseProfile(String baseProfile) {
        this.baseProfile = baseProfile;
    }

    private String version;
    public String getVersion() {
        if (this.version != null && !this.version.isEmpty()) {
            return this.version;
        }
        return "4.0.0";
    }
    public void setVersion(String version) {
        this.version = version;
    }

}
