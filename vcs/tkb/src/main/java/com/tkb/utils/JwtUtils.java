package com.tkb.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtils {

    private static String singkey = "morerich";
    // Token 過期時間為1天 (ms) 1000 * 60 = 1分鐘
    // 1分鐘 * 60 = 1小時 *24 = 1天
    static long expire = 1000 * 60 * 60 * 24 ;

    public static String generateJWT(Map<String, Object> claims) {

        String jwt = Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, singkey) // 簽名算法
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .compact();
        return jwt;
    }

    public static Claims parseJWT(String jwt) {

        Claims claims = Jwts.parser()
                .setSigningKey(singkey)
                .parseClaimsJws(jwt)
                .getBody();

        return claims;
    }
}
