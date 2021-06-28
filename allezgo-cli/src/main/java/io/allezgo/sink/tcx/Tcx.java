package io.allezgo.sink.tcx;

public record Tcx(String value) {
    public static Tcx of(String value) {
        return new Tcx(value);
    }
}
