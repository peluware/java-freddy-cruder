package com.peluware.freddy.cruder.export;

/**
 * {@link Export} base that fixes {@link #filename()} and {@link #mediaType()} to whatever was
 * resolved when this export was created, leaving only {@link #writeTo} to implement.
 */
abstract class AbstractExport implements Export {

    private final String filename;
    private final String mediaType;

    protected AbstractExport(String filename, String mediaType) {
        this.filename = filename;
        this.mediaType = mediaType;
    }

    @Override
    public String filename() {
        return filename;
    }

    @Override
    public String mediaType() {
        return mediaType;
    }
}
