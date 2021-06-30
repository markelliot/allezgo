package barista;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

public final class Authz {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private Clock clock;

    public Authz(String secret, Clock clock) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.clock = clock;
        this.verifier = JWT.require(algorithm).build();
    }

    public AuthToken newSession(String userId) {
        return AuthTokens.of(
                JWT.create()
                        .withIssuer("shareloyal")
                        .withSubject(userId)
                        .withExpiresAt(Date.from(clock.instant().plus(1, ChronoUnit.DAYS)))
                        .sign(algorithm));
    }

    public Optional<VerifiedAuthToken> check(AuthToken token) {
        try {
            return Optional.of(verifier.verify(token.token()))
                    .map(t -> new VerifiedAuthToken(token, t.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
