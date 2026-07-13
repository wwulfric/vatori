package dev.vatori.desktop.devtools;

final class StreamResourceTraceFields {
    boolean basicV150ManifestObserved;
    boolean basicV150RequestObserved;
    boolean basicV150ResponseObserved;
    boolean basicV150DomObserved;
    boolean rangeV151ManifestObserved;
    boolean rangeV151RequestObserved;
    boolean rangeV151ResponseObserved;
    boolean rangeV151DomObserved;
    boolean chunksV152ManifestObserved;
    boolean chunksV152RequestObserved;
    boolean chunksV152ResponseObserved;
    boolean chunksV152DomObserved;
    boolean basicBootstrapObserved;
    boolean basicDomObserved;
    boolean rangeBootstrapObserved;
    boolean rangeDomObserved;
    boolean chunksBootstrapObserved;
    boolean chunksDomObserved;

    boolean hasBasicSignals() {
        return basicBootstrapObserved && basicDomObserved;
    }

    boolean hasBasicGateSignals() {
        return basicV150ManifestObserved
                && basicV150RequestObserved
                && basicV150ResponseObserved
                && basicV150DomObserved;
    }

    boolean hasRangeSignals() {
        return rangeBootstrapObserved && rangeDomObserved;
    }

    boolean hasRangeGateSignals() {
        return rangeV151ManifestObserved
                && rangeV151RequestObserved
                && rangeV151ResponseObserved
                && rangeV151DomObserved;
    }

    boolean hasChunksGateSignals() {
        return chunksV152ManifestObserved
                && chunksV152RequestObserved
                && chunksV152ResponseObserved
                && chunksV152DomObserved;
    }

    static boolean containsStreamResourceV150Fields(String line) {
        return StreamResourceBasicTraceFields.containsStreamResourceV150Fields(line);
    }

    static boolean containsStreamResourceV150Response(String line) {
        return StreamResourceBasicTraceFields.containsStreamResourceV150Response(line);
    }

    static boolean containsStreamResourceV150Dom(String line) {
        return StreamResourceBasicTraceFields.containsStreamResourceV150Dom(line);
    }

    static boolean containsStreamResourceRangeV151Fields(String line) {
        return StreamResourceRangeTraceFields.containsStreamResourceRangeV151Fields(line);
    }

    static boolean containsStreamResourceRangeV151Response(String line) {
        return StreamResourceRangeTraceFields.containsStreamResourceRangeV151Response(line);
    }

    static boolean containsStreamResourceRangeV151Dom(String line) {
        return StreamResourceRangeTraceFields.containsStreamResourceRangeV151Dom(line);
    }

    static boolean containsStreamResourceChunksV152Fields(String line) {
        return StreamResourceChunksTraceFields.containsStreamResourceChunksV152Fields(line);
    }

    static boolean containsStreamResourceChunksV152Response(String line) {
        return StreamResourceChunksTraceFields.containsStreamResourceChunksV152Response(line);
    }

    static boolean containsStreamResourceChunksV152Dom(String line) {
        return StreamResourceChunksTraceFields.containsStreamResourceChunksV152Dom(line);
    }
}
