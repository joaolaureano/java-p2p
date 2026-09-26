package p2p.peer;

/**
 * Thrown when a file download cannot be completed (missing file, timeout, size or checksum
 * mismatch, ...).
 */
public class DownloadException extends Exception {

    private static final long serialVersionUID = 1L;

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
