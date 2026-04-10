package com.bustech.erp.common.util;

import com.bustech.erp.common.exception.AccessDeniedException;
import com.bustech.erp.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Usuário não autenticado.");
    }

    public static Long getCurrentCompanyId() {
        return getCurrentUser().getCompanyId();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static void assertCompanyAccess(Long companyId) {
        if (!getCurrentCompanyId().equals(companyId)) {
            throw new AccessDeniedException();
        }
    }
}
