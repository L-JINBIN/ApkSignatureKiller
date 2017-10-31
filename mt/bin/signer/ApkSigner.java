package bin.signer;

import bin.signer.key.BaseSignatureKey;
import bin.util.StreamUtil;
import bin.zip.ZipEntry;
import bin.zip.ZipFile;
import bin.zip.ZipOutputStream;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class ApkSigner {
    private static class SignatureOutputStream extends OutputStream {
        private int mCount;
        private Signature mSignature;
        private OutputStream out;

        public SignatureOutputStream(OutputStream out, Signature sig) {
            this.out = out;
            mSignature = sig;
            mCount = 0;
        }

        public int size() {
            return mCount;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                mSignature.update(b, off, len);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            out.write(b, off, len);
            mCount += len;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                mSignature.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            out.write(b);
            mCount++;
        }
    }

    private static final String CERT_RSA_NAME = "META-INF/CERT.RSA";

    private static final String CERT_SF_NAME = "META-INF/CERT.SF";

    private static Pattern stripPattern = Pattern
            .compile("^META-INF/(.*)[.](SF|RSA|DSA)$");

    private static Manifest addDigestsToManifest(ZipFile zipFile, ApkSignCallback callback)
            throws IOException, GeneralSecurityException {
        Manifest input = null;
        Manifest output = new Manifest();
        ZipEntry zipEntry = zipFile.getEntry(JarFile.MANIFEST_NAME);
        Attributes main = output.getMainAttributes();
        if (zipEntry != null) {
            input = new Manifest();
            input.read(zipFile.getInputStream(zipEntry));
            main.putAll(input.getMainAttributes());
        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (MT)");
        }

        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] buffer = new byte[4096];
        int num;

        TreeMap<String, ZipEntry> byName = new TreeMap<>();

        int total = 0;
        for (Enumeration<ZipEntry> e = zipFile.getEntries(); e.hasMoreElements(); ) {
            ZipEntry entry = e.nextElement();
            byName.put(entry.getName(), entry);
            total++;
        }

        int progress = 0;
        for (ZipEntry entry : byName.values()) {
            String name = entry.getName();
            if (!entry.isDirectory()
                    && !name.equals(JarFile.MANIFEST_NAME)
                    && !name.equals(ApkSigner.CERT_SF_NAME)
                    && !name.equals(ApkSigner.CERT_RSA_NAME)
                    && (ApkSigner.stripPattern == null || !ApkSigner.stripPattern
                    .matcher(name).matches())) {
                InputStream data = zipFile.getInputStream(entry);
                while ((num = data.read(buffer)) > 0) {
                    md.update(buffer, 0, num);
                }
                Attributes attr = null;
                if (input != null) {
                    attr = input.getAttributes(name);
                }
                attr = attr != null ? new Attributes(attr) : new Attributes();
                attr.putValue("SHA1-Digest", Base64.getEncoder().encodeToString(md.digest()));
                output.getEntries().put(name, attr);
            }
            callback.onProgress(++progress, total);
        }
        return output;
    }

    private static void copyFiles(Manifest manifest, ZipFile in, ZipOutputStream out,
                                  long timestamp, ApkSignCallback callback) throws IOException {
        Map<String, Attributes> entries = manifest.getEntries();
        List<String> names = new ArrayList<>(entries.keySet());
        Collections.sort(names);
        int progress = 0;
        int total = names.size();
        for (String name : names) {
            ZipEntry inEntry = in.getEntry(name);
            inEntry.setTime(timestamp);
            out.copyZipEntry(inEntry, in);
            callback.onProgress(++progress, total);
        }
    }

    private static KeySpec decryptPrivateKey(byte[] encryptedPrivateKey)
            throws GeneralSecurityException {
        EncryptedPrivateKeyInfo epkInfo;
        try {
            epkInfo = new EncryptedPrivateKeyInfo(encryptedPrivateKey);
        } catch (IOException ex) {
            return null;
        }
        SecretKeyFactory skFactory = SecretKeyFactory.getInstance(epkInfo
                .getAlgName());
        Key key = skFactory.generateSecret(new PBEKeySpec("".toCharArray()));
        Cipher cipher = Cipher.getInstance(epkInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, key, epkInfo.getAlgParameters());
        return epkInfo.getKeySpec(cipher);
    }

    private static PrivateKey readPrivateKey(InputStream input)
            throws IOException, GeneralSecurityException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(input.available());
            byte[] bytes = new byte[1024];
            int len;
            while ((len = input.read(bytes)) > 0)
                baos.write(bytes, 0, len);
            bytes = baos.toByteArray();
            baos.close();
            KeySpec spec = ApkSigner.decryptPrivateKey(bytes);
            if (spec == null) {
                spec = new PKCS8EncodedKeySpec(bytes);
            }
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (InvalidKeySpecException ex) {
                return KeyFactory.getInstance("DSA").generatePrivate(spec);
            }
        } finally {
            input.close();
        }
    }

    private static X509Certificate readPublicKey(InputStream input)
            throws IOException, GeneralSecurityException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(input);
        } finally {
            input.close();
        }
    }

    private static final ApkSignCallback SIGN_CALLBACK = new ApkSignCallback() {
        @Override
        public void onStep(Step step) {

        }

        @Override
        public void onProgress(int progress, int total) {

        }
    };

    public static void signApk(File input, File output, BaseSignatureKey key, ApkSignCallback callback) throws Exception {
        ZipFile inputJar = null;
        ZipOutputStream outputJar;
        FileOutputStream outputFile = null;
        if (callback == null)
            callback = ApkSigner.SIGN_CALLBACK;
        try {
            callback.onStep(Step.START);
            //Load key
            X509Certificate publicKey = ApkSigner.readPublicKey(key.getPublicKey());
            PrivateKey privateKey = ApkSigner.readPrivateKey(key.getPrivateKey());
            key.recycle();
            //Calculate SHA1-Digest
            inputJar = new ZipFile(input);
            callback.onStep(Step.SIGN_FILE);
            Manifest manifest = ApkSigner.addDigestsToManifest(inputJar, callback);
            //Write out signature files
            long timestamp = publicKey.getNotBefore().getTime() + 3600L * 1000;
            outputFile = new FileOutputStream(output);
            outputJar = new ZipOutputStream(outputFile);
            outputJar.setZipEncoding(inputJar.getZipEncoding());
            outputJar.setMethod(ZipOutputStream.DEFLATED);
            outputJar.setLevel(9);
            //META-INF/MANIFEST.MF
            ZipEntry je = new ZipEntry(JarFile.MANIFEST_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            manifest.write(outputJar);
            //META-INF/CERT.SF
            je = new ZipEntry(ApkSigner.CERT_SF_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            ApkSigner.writeSignatureFile(manifest, new SignatureOutputStream(
                    outputJar, signature));
            //META-INF/CERT.RSA
            je = new ZipEntry(ApkSigner.CERT_RSA_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            ApkSigner.writeSignatureBlock(signature, publicKey, outputJar);
            //Write out other files
            callback.onStep(Step.OUTPUT);
            ApkSigner.copyFiles(manifest, inputJar, outputJar, timestamp, callback);
            outputJar.close();
            outputFile.flush();
            callback.onStep(Step.FINISH);
        } finally {
            StreamUtil.close(inputJar);
            StreamUtil.close(outputFile);
        }
    }

    public interface ApkSignCallback {

        void onStep(Step step);

        void onProgress(int progress, int total);
    }

    public enum Step {
        START,
        SIGN_FILE,
        OUTPUT,
        FINISH
    }

    private static void writeSignatureBlock(Signature signature,
                                            X509Certificate publicKey, OutputStream out) throws IOException,
            GeneralSecurityException {
        SignerInfo signerInfo = new SignerInfo(new X500Name(publicKey
                .getIssuerX500Principal().getName()),
                publicKey.getSerialNumber(), AlgorithmId.get("SHA1"),
                AlgorithmId.get("RSA"), signature.sign());
        PKCS7 pkcs7 = new PKCS7(new AlgorithmId[]{AlgorithmId.get("SHA1")},
                new ContentInfo(ContentInfo.DATA_OID, null),
                new X509Certificate[]{publicKey},
                new SignerInfo[]{signerInfo});
        pkcs7.encodeSignedData(out);
    }

    private static void writeSignatureFile(Manifest manifest,
                                           SignatureOutputStream out) throws IOException,
            GeneralSecurityException {
        out.write("Signature-Version: 1.0\r\n".getBytes());
        out.write("Created-By: 1.0 (MT_Bin)\r\n".getBytes());

        MessageDigest md = MessageDigest.getInstance("SHA1");
        PrintStream print = new PrintStream(new DigestOutputStream(
                new ByteArrayOutputStream(), md), true, "UTF-8");
        manifest.write(print);
        print.flush();

        out.write(("SHA1-Digest-Manifest: " + Base64.getEncoder().encodeToString(md.digest())
                + "\r\n\r\n").getBytes());
        Map<String, Attributes> entries = manifest.getEntries();
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            // Digest of the manifest stanza for this entry.
            String nameEntry = "Name: " + entry.getKey() + "\r\n";
            print.print(nameEntry);
            for (Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
                print.print(att.getKey() + ": " + att.getValue() + "\r\n");
            }
            print.print("\r\n");
            print.flush();

            out.write(nameEntry.getBytes());

            out.write(("SHA1-Digest: " + Base64.getEncoder().encodeToString(md.digest())
                    + "\r\n\r\n").getBytes());
        }
        if (out.size() % 1024 == 0) {
            out.write('\r');
            out.write('\n');
        }
    }
}