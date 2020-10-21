package edu.utexas.tacc.tapis.notifications.frontend;

import io.jsonwebtoken.*;

import java.security.PublicKey;
import java.util.Objects;

public class TapisJWTValidator {

    private String encodedJWT;

    // Tapis claim keys.
    private static final String CLAIM_TENANT = "tapis/tenant_id";
    private static final String CLAIM_USERNAME = "tapis/username";
    private static final String CLAIM_TOKEN_TYPE = "tapis/token_type";
    private static final String CLAIM_ACCOUNT_TYPE = "tapis/account_type";
    private static final String CLAIM_SITE = "tapis/target_site";

    //These two are optional, only used when making a service request obo. If
    // tapis/delegation is present, the tapis/delegation_sub claim is required
    private static final String CLAIM_DELEGATION = "tapis/delegation";
    private static final String CLAIM_DELEGATION_SUB = "tapis/delegation_sub";


    public TapisJWTValidator(String encodedJWT) {
        this.encodedJWT = encodedJWT;
    }

    public Jws<Claims> validate(PublicKey publicKey) throws JwtException {
        Jws<Claims> jwts = Jwts.parser()
            .setSigningKey(publicKey)
            .parseClaimsJws(encodedJWT);

        Claims claims = jwts.getBody();
        try {
            Objects.requireNonNull(claims.get(CLAIM_TENANT));
            Objects.requireNonNull(claims.get(CLAIM_USERNAME));
            Objects.requireNonNull(claims.get(CLAIM_TOKEN_TYPE));
            Objects.requireNonNull(claims.get(CLAIM_ACCOUNT_TYPE));
            Objects.requireNonNull(claims.get(CLAIM_SITE));
            if (claims.get(CLAIM_DELEGATION) != null) {
                Objects.requireNonNull(claims.get(CLAIM_DELEGATION_SUB));
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




}
