package bin.signer.key;

import java.io.IOException;
import java.io.InputStream;

public abstract class BaseSignatureKey {
    protected String password;

    protected BaseSignatureKey() {

    }

    public void init() throws Exception {
    }

    public void recycle() {
    }

    public boolean needPassword() {
        return false;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public abstract InputStream getPublicKey() throws IOException;

    public abstract InputStream getPrivateKey() throws IOException;

}
