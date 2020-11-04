package edu.utexas.tacc.tapis.notifications.websockets;

import io.jsonwebtoken.*;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

public class TapisJWTValidator {

    private final String encodedJWT;

    public TapisJWTValidator(String encodedJWT) {
        this.encodedJWT = encodedJWT;
    }

    private PublicKey decodePublicKeyString(String plainTextPublicKey) throws Exception {
        String trimmedKey = trimPublicKey(plainTextPublicKey);
        byte[] publicBytes = Base64.getDecoder().decode(trimmedKey.getBytes());
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    public Jws<Claims> validate(String publicKey) throws Exception {
       PublicKey key = decodePublicKeyString(publicKey);
       return validate(key);
    }

    public Jws<Claims> validate(PublicKey publicKey) throws JwtException {
        Jws<Claims> jwts = Jwts.parser()
            .setSigningKey(publicKey)
            .parseClaimsJws(encodedJWT);

        Claims claims = jwts.getBody();
        try {
            Objects.requireNonNull(claims.get(TapisClaims.TAPIS_TENANT));
            Objects.requireNonNull(claims.get(TapisClaims.TAPIS_USERNAME));
            Objects.requireNonNull(claims.get(TapisClaims.TAPIS_TOKEN_TYPE));
            Objects.requireNonNull(claims.get(TapisClaims.TAPIS_ACCOUNT_TYPE));
//            Objects.requireNonNull(claims.get(TapisClaims.TAPIS_TARGET_SITE));
            if (claims.get(TapisClaims.TAPIS_DELEGATION) != null) {
                Objects.requireNonNull(claims.get(TapisClaims.TAPIS_DELEGATION_SUB));
            }
        } catch (NullPointerException ex) {
            throw new JwtException("Claims are not valid");
        }
        return jwts;

    }

    public Claims getClaimsNoValidation() {
        int i = encodedJWT.lastIndexOf('.');
        String withoutSignature = encodedJWT.substring(0, i+1);
        Jwt<?,Claims> untrusted = Jwts.parser()
            .parseClaimsJwt(withoutSignature);
        return untrusted.getBody();
    }

    private String trimPublicKey(String encodedPublicKey) {
        // This should never happen.
        if (encodedPublicKey == null) return "";

        // Remove prologue and epilogue if they exist.  The Tapis Tenants service
        // often stores keys with the following PEM prologue and epilogue (see
        // https://tools.ietf.org/html/rfc1421 for the specification):
        //
        //      "-----BEGIN PUBLIC KEY-----\n"
        //      "\n-----END PUBLIC KEY-----"
        //
        // In general, different messages can appear after the BEGIN and END text,
        // so stripping out the prologue and epilogue requires some care.  The
        // approach below handles only unix-style line endings.
        //
        // Check for unix style prologue.
        int index = encodedPublicKey.indexOf("-\n");
        if (index > 0) encodedPublicKey = encodedPublicKey.substring(index + 2);

        // Check for unix style epilogue.
        index = encodedPublicKey.lastIndexOf("\n-");
        if (index > 0) encodedPublicKey = encodedPublicKey.substring(0, index);

        return encodedPublicKey;
    }



}
