package app.bucket;

public class BucketResource {
        private String hash;
        private int peerPort;

        public BucketResource(String hash, int peerPort) {
            this.hash = hash;
            this.peerPort = peerPort;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public int getPeerPort() {
            return peerPort;
        }

        public void setPeerPort(int peerPort) {
            this.peerPort = peerPort;
        }
}
