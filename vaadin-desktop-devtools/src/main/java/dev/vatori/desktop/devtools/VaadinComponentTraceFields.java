package dev.vatori.desktop.devtools;

final class VaadinComponentTraceFields {
    boolean webComponentResourceManifestObserved;
    boolean webComponentResourceRequestObserved;
    boolean webComponentResourceResponseObserved;
    boolean webComponentResourceDomObserved;
    boolean webComponentResourceExecutedDomObserved;
    boolean webComponentFailureClassificationObserved;
    boolean webComponentEventRpcObserved;
    boolean webComponentPatchObserved;
    boolean webComponentDomObserved;
    boolean textFieldEventRpcObserved;
    boolean textFieldPatchObserved;
    boolean textFieldDomObserved;
    boolean resolverV13ManifestObserved;
    boolean resolverV13DomObserved;
    boolean secondWebComponentResourceRequestObserved;
    boolean secondWebComponentResourceResponseObserved;
    boolean secondWebComponentDevBundleResourceObserved;
    boolean secondWebComponentEventRpcObserved;
    boolean secondWebComponentPatchObserved;
    boolean secondWebComponentDomObserved;

    boolean hasV1_2Signals() {
        return webComponentResourceManifestObserved
                && webComponentResourceRequestObserved
                && webComponentResourceResponseObserved
                && webComponentResourceDomObserved
                && webComponentResourceExecutedDomObserved
                && webComponentFailureClassificationObserved
                && webComponentEventRpcObserved
                && webComponentPatchObserved
                && webComponentDomObserved;
    }

    boolean hasV1_3Signals() {
        return resolverV13ManifestObserved
                && resolverV13DomObserved
                && secondWebComponentResourceRequestObserved
                && secondWebComponentResourceResponseObserved
                && secondWebComponentEventRpcObserved
                && secondWebComponentPatchObserved
                && secondWebComponentDomObserved;
    }

    boolean hasFrameworkBaseSignals() {
        return resolverV13ManifestObserved
                && resolverV13DomObserved
                && webComponentResourceRequestObserved
                && webComponentResourceResponseObserved
                && secondWebComponentResourceRequestObserved
                && secondWebComponentResourceResponseObserved
                && secondWebComponentDevBundleResourceObserved;
    }

    boolean hasTextFieldRuntimeSignals() {
        return textFieldEventRpcObserved
                && textFieldPatchObserved
                && textFieldDomObserved;
    }
}
