package io.hoyatla.animatruc.core.importer;

public final class ModelImportException extends RuntimeException {
    public ModelImportException(String message) {
        super(message);
    }

    public ModelImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
