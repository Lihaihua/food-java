

package net.jjjshop.framework.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具类
 */
@Slf4j
public class PasswordUtil {
    private static final BCryptPasswordEncoder BCRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 密码加盐，再加密
     *
     * @param pwd
     * @param salt
     * @return
     */
    public static String encrypt(String pwd, String salt) {
        String rawPassword = getRawPassword(pwd, salt);
        return BCRYPT_PASSWORD_ENCODER.encode(rawPassword);
    }

    public static boolean matches(String pwd, String salt, String encodedPassword) {
        if (StringUtils.isBlank(encodedPassword)) {
            return false;
        }
        String rawPassword = getRawPassword(pwd, salt);
        if (isBcrypt(encodedPassword)) {
            return BCRYPT_PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
        }
        return DigestUtils.sha256Hex(rawPassword).equals(encodedPassword);
    }

    public static boolean isBcryptHash(String encodedPassword) {
        return StringUtils.isNotBlank(encodedPassword) && isBcrypt(encodedPassword);
    }

    private static String getRawPassword(String pwd, String salt) {
        if (StringUtils.isBlank(pwd)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (StringUtils.isBlank(salt)) {
            throw new IllegalArgumentException("盐值不能为空");
        }
        return pwd + salt;
    }

    private static boolean isBcrypt(String encodedPassword) {
        return encodedPassword.startsWith("$2a$")
                || encodedPassword.startsWith("$2b$")
                || encodedPassword.startsWith("$2y$");
    }

}
