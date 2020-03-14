package com.kj.repo.tt.crypt;

import java.security.Key;
import java.security.KeyPair;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kj.repo.infra.crypt.CryptSign;
import com.kj.repo.infra.crypt.algoritm.AlgoritmCipher;
import com.kj.repo.infra.crypt.key.CryptKey;

/**
 * @author kj
 */
public class TeCrypt {

    public static Logger logger = LoggerFactory.getLogger(TeCrypt.class);

    public static void main(String[] args) throws Exception {
        Key key = CryptKey.generateKey(AlgoritmCipher.DESede.getName(), AlgoritmCipher.DESede.getKeysize());
        KeyPair keyPair = CryptKey
                .generateKeyPair(AlgoritmCipher.RSA_2048.getName(), AlgoritmCipher.RSA_2048.getKeysize());
        System.out.println(Base64.getEncoder().encodeToString(key.getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        CryptSign.Sign sign = CryptSign.sign(AlgoritmCipher.RSA_2048.getName(), keyPair.getPrivate());
        CryptSign.Verify verify = CryptSign.verify(AlgoritmCipher.RSA_2048.getName(), keyPair.getPublic());
        System.out.println(
                verify.verify("kj".getBytes(), sign.sign("kj".getBytes())));
    }
}
