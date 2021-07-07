package io.allezgo.adapters.tcx;

public record Tcx(String value) {
    public static Tcx of(String value) {
        return new Tcx(value);
    }
}
