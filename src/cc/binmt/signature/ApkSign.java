package cc.binmt.signature;

import java.io.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ApkSign {

//    public static void main(String[] args) throws Exception {
//        File file = new File("signature.dat");
//        File apk = new File("src.apk");
//        byte[] signData = getApkSignData(apk);
//        System.out.println("data");
//        System.out.println(Base64.getEncoder().encodeToString(signData));
//    }

    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            InputStream is = jarFile.getInputStream(je);
            //noinspection StatementWithEmptyBody
            while (is.read(readBuffer, 0, readBuffer.length) != -1) ;
            is.close();
            return je.getCertificates();
        } catch (Exception ignored) {
        }
        return null;
    }

    public static byte[] getApkSignData(File apkFile) throws Exception {
        byte[] readBuffer = new byte[8192];
        Certificate[] certs = null;
        JarFile jarFile = new JarFile(apkFile);
        Enumeration<?> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = (JarEntry) entries.nextElement();
            if (je.isDirectory()) {
                continue;
            }
            if (je.getName().startsWith("META-INF/")) {
                continue;
            }
            Certificate[] localCerts = loadCertificates(jarFile, je, readBuffer);
            if (localCerts != null) {
                if (certs == null) {
                    certs = localCerts;
                } else {
                    for (Certificate cert : certs) {
                        boolean found = false;
                        for (Certificate localCert : localCerts) {
                            if (cert != null && cert.equals(localCert)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found || certs.length != localCerts.length) {
                            jarFile.close();
                            throw new Exception("Certificates ars different.");
                        }
                    }
                }
            }
        }
        jarFile.close();
        if (certs == null)
            throw new Exception("Certificates ars null.");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write(certs.length);
        for (int i = 0; i < certs.length; i++) {
            byte[] data = certs[i].getEncoded();
            System.out.printf("  --SignatureHash[%d]: %08x\n", i, Arrays.hashCode(data));
            dos.writeInt(data.length);
            dos.write(data);
        }
        return baos.toByteArray();
    }

}