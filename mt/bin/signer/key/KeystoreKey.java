package bin.signer.key;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;

public class KeystoreKey extends BaseSignatureKey {
    private byte[] x509_pem;
    private byte[] pk8;

    public KeystoreKey(String file, String password, String alias, String aliasPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(new FileInputStream(file), password.toCharArray());
        Certificate pubkey = keyStore.getCertificate(alias);
        Key key = keyStore.getKey(alias, aliasPassword.toCharArray());
        KeyPair kp = new KeyPair(pubkey.getPublicKey(), (PrivateKey) key);
        x509_pem = ("-----BEGIN CERTIFICATE-----\n" +
                Base64.getEncoder().encodeToString(pubkey.getEncoded()) +
                "\n-----END CERTIFICATE-----").getBytes();
        pk8 = kp.getPrivate().getEncoded();
    }

    @Override
    public InputStream getPublicKey() {
        return new ByteArrayInputStream(x509_pem);
    }

    @Override
    public InputStream getPrivateKey() {
        return new ByteArrayInputStream(pk8);
    }
}
