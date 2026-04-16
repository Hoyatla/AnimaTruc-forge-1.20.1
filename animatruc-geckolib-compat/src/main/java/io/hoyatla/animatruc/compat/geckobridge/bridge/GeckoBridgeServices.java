package io.hoyatla.animatruc.compat.geckobridge.bridge;

final class GeckoBridgeServices {
    private static volatile GeckoBridgeRuntime runtime;

    private GeckoBridgeServices() {
    }

    static void bind(GeckoBridgeRuntime value) {
        runtime = value;
    }

    static GeckoBridgeRuntime runtime() {
        return runtime;
    }
}
