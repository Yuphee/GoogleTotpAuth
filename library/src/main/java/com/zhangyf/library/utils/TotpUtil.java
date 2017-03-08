package com.zhangyf.library.utils;

import android.content.Context;
import android.util.Log;

import com.zhangyf.library.config.MTokenConstants;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by zhangyf on 2017/3/8.
 */

public class TotpUtil {

    private static final int PIN_LENGTH = 6;
    private static final int REFLECTIVE_PIN_LENGTH = 9;

    public static void init(String seed) {
        SPUtils.put(MTokenConstants.PREFS_USER_KEY, seed);
    }

    public static String getSeed() {
        String seed = SPUtils.getString(MTokenConstants.PREFS_USER_KEY, "");
        if (!seed.equals(""))
            return seed;
        throw new NullPointerException("u should init seed first");
    }

    /**
     * 生成6位数的手机令牌号
     * @return
     */
    public static String generate() {
        try {
            // 加上与服务器的时间差，再计算结果
            return computePin(getSeed(), CountUtils.getValueAtTime(CountUtils
                     .millisToSeconds(CountUtils.currentTimeMillis())), null);
        } catch (OtpSourceException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Computes the one-time PIN given the secret key.
     *
     * @author zhangyf
     *
     * @param secret
     *            the secret key
     * @param otp_state
     *            current token state (counter or time-interval)
     * @param challenge
     *            optional challenge bytes to include when computing passcode.
     * @return the PIN
     */
    private static String computePin(String secret, long otp_state, byte[] challenge)
            throws OtpSourceException {
        if (secret == null || secret.length() == 0) {
            throw new OtpSourceException("Null or empty secret");
        }

        try {
            PasscodeGenerator.Signer signer = getSigningOracle(secret);
            PasscodeGenerator pcg = new PasscodeGenerator(signer,
                    (challenge == null) ? PIN_LENGTH : REFLECTIVE_PIN_LENGTH);

            return (challenge == null) ? pcg.generateResponseCode(otp_state)
                    : pcg.generateResponseCode(otp_state, challenge);
        } catch (GeneralSecurityException e) {
            throw new OtpSourceException("Crypto failure", e);
        }
    }

    private static PasscodeGenerator.Signer getSigningOracle(String secret) {
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            // Create a signer object out of the standard Java MAC
            // implementation.
            return new PasscodeGenerator.Signer() {
                @Override
                public byte[] sign(byte[] data) {
                    return mac.doFinal(data);
                }
            };
        } catch (Base32String.DecodingException error) {
            Log.e("Mlog", error.getMessage());
        } catch (NoSuchAlgorithmException error) {
            Log.e("Mlog", error.getMessage());
        } catch (InvalidKeyException error) {
            Log.e("Mlog", error.getMessage());
        }

        return null;
    }

    private static byte[] decodeKey(String secret) throws Base32String.DecodingException {
        return Base32String.decode(secret);
    }

}
