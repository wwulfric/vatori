package dev.vatori.desktop.devtools;

final class ProductionOwnerRuntimeTraceFields {
    boolean ownerV113ManifestObserved;
    boolean ownerV113RequestObserved;
    boolean ownerV113StageObserved;
    boolean ownerV113DomObserved;
    boolean ownerImportV114ManifestObserved;
    boolean ownerImportV114RequestObserved;
    boolean ownerImportV114StageObserved;
    boolean ownerImportV114DomObserved;
    boolean ownerDependenciesV115ManifestObserved;
    boolean ownerDependenciesV115RequestObserved;
    boolean ownerDependenciesV115StageObserved;
    boolean ownerDependenciesV115DomObserved;
    boolean ownerOrderingV116ManifestObserved;
    boolean ownerOrderingV116RequestObserved;
    boolean ownerOrderingV116StageObserved;
    boolean ownerOrderingV116DomObserved;
    boolean resourceSchemeV118ManifestObserved;
    boolean resourceSchemeV118RequestObserved;
    boolean resourceSchemeV118RootStageObserved;
    boolean resourceSchemeV118DependencyStageObserved;
    boolean resourceSchemeV118RootHandlerRequestObserved;
    boolean resourceSchemeV118RootHandlerResponseObserved;
    boolean resourceSchemeV118DependencyHandlerRequestObserved;
    boolean resourceSchemeV118DependencyHandlerResponseObserved;
    boolean resourceSchemeV118ImportObserved;
    boolean resourceSchemeV118DomObserved;
    boolean sideEffectV119ManifestObserved;
    boolean sideEffectV119RequestObserved;
    boolean sideEffectV119StageObserved;
    boolean sideEffectV119DomObserved;
    boolean sideEffectV119ReuseDecisionObserved;
    boolean ownedRuntimeV120ManifestObserved;
    boolean ownedRuntimeV120RequestObserved;
    boolean ownedRuntimeV120StageObserved;
    boolean ownedRuntimeV120DomObserved;
    boolean ownedRuntimeV120ReuseDecisionObserved;
    boolean positiveFixtureV123ManifestObserved;
    boolean positiveFixtureV123RootRequestObserved;
    boolean positiveFixtureV123DependencyRequestObserved;
    boolean positiveFixtureV123RootResponseObserved;
    boolean positiveFixtureV123DependencyResponseObserved;
    boolean positiveFixtureV123RootStageObserved;
    boolean positiveFixtureV123DependencyStageObserved;
    boolean positiveFixtureV123DomObserved;
    boolean positiveFixtureV123RealBlockerObserved;

    boolean hasV1_13Signals() {
        return ownerV113ManifestObserved
                && ownerV113RequestObserved
                && ownerV113StageObserved
                && ownerV113DomObserved;
    }

    boolean hasV1_14Signals() {
        return ownerImportV114ManifestObserved
                && ownerImportV114RequestObserved
                && (ownerImportV114StageObserved || resourceSchemeV118ImportObserved)
                && (ownerImportV114DomObserved || resourceSchemeV118DomObserved);
    }

    boolean hasV1_15Signals() {
        return ownerDependenciesV115ManifestObserved
                && ownerDependenciesV115RequestObserved
                && (ownerDependenciesV115StageObserved || resourceSchemeV118DependencyStageObserved)
                && (ownerDependenciesV115DomObserved
                        || (resourceSchemeV118DependencyHandlerRequestObserved
                                && resourceSchemeV118DependencyHandlerResponseObserved
                                && resourceSchemeV118DomObserved));
    }

    boolean hasV1_16Signals() {
        return ownerOrderingV116ManifestObserved
                && ownerOrderingV116RequestObserved
                && ownerOrderingV116StageObserved
                && ownerOrderingV116DomObserved;
    }

    boolean hasResourceSchemeV1_18Signals() {
        return resourceSchemeV118ManifestObserved
                && resourceSchemeV118RequestObserved
                && resourceSchemeV118RootStageObserved
                && resourceSchemeV118DependencyStageObserved
                && resourceSchemeV118RootHandlerRequestObserved
                && resourceSchemeV118RootHandlerResponseObserved
                && resourceSchemeV118DependencyHandlerRequestObserved
                && resourceSchemeV118DependencyHandlerResponseObserved
                && resourceSchemeV118ImportObserved
                && resourceSchemeV118DomObserved;
    }

    boolean hasSideEffectV1_19Signals() {
        return hasResourceSchemeV1_18Signals()
                && sideEffectV119ManifestObserved
                && sideEffectV119RequestObserved
                && sideEffectV119StageObserved
                && sideEffectV119DomObserved
                && sideEffectV119ReuseDecisionObserved;
    }

    boolean hasOwnedRuntimeV1_20Signals() {
        return ownedRuntimeV120ManifestObserved
                && ownedRuntimeV120RequestObserved
                && ownedRuntimeV120StageObserved
                && ownedRuntimeV120DomObserved
                && ownedRuntimeV120ReuseDecisionObserved;
    }

    boolean hasPositiveFixtureV1_23Signals() {
        return positiveFixtureV123ManifestObserved
                && positiveFixtureV123RootRequestObserved
                && positiveFixtureV123DependencyRequestObserved
                && positiveFixtureV123RootResponseObserved
                && positiveFixtureV123DependencyResponseObserved
                && positiveFixtureV123RootStageObserved
                && positiveFixtureV123DependencyStageObserved
                && positiveFixtureV123DomObserved
                && positiveFixtureV123RealBlockerObserved;
    }
}
