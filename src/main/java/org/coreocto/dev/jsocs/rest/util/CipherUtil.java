package org.coreocto.dev.jsocs.rest.util;

import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class CipherUtil {

    @Autowired
    private AppConfig appConfig;

    public Cipher getCipher(int mode, byte[] ivBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] keyBytes = appConfig.APP_ENCRYPT_KEY.getBytes();
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        SecretKeySpec m_keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher m_cipher = javax.crypto.Cipher.getInstance("AES/CTR/NoPadding");
        m_cipher.init(mode, m_keySpec, iv);
        return m_cipher;
    }
}
