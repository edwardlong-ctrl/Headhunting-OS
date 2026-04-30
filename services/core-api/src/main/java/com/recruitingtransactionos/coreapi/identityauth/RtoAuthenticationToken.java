package com.recruitingtransactionos.coreapi.identityauth;

import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class RtoAuthenticationToken extends AbstractAuthenticationToken {

  private final RtoAuthenticatedPrincipal principal;

  public RtoAuthenticationToken(RtoAuthenticatedPrincipal principal) {
    super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.portalRole().name())));
    this.principal = Objects.requireNonNull(principal, "principal must not be null");
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public RtoAuthenticatedPrincipal getPrincipal() {
    return principal;
  }
}
