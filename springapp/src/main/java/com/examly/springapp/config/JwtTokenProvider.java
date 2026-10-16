package com.examly.springapp.config;

import com.examly.springapp.model.OrganizationMembership;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtTokenProvider extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain)
            throws ServletException, IOException {
        
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.extractUsername(jwt);
                User user = userRepository.findByUsername(username);
                
                if (user != null) {
                    Set<GrantedAuthority> authorities = new HashSet<>();

                    // 1. Tenant membership authority derived from live database status
                    List<OrganizationMembership> activeMemberships = membershipRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
                    if (activeMemberships.size() == 1) {
                        OrganizationMembership mem = activeMemberships.get(0);
                        if (mem.getOrganization() != null && "ACTIVE".equalsIgnoreCase(mem.getOrganization().getStatus())) {
                            String memRole = mem.getRole();
                            if (memRole != null && !memRole.isBlank()) {
                                String authName = memRole.startsWith("ROLE_") ? memRole.toUpperCase() : "ROLE_" + memRole.toUpperCase();
                                authorities.add(new SimpleGrantedAuthority(authName));
                            }
                        }
                    }

                    // 2. Platform Admin authority strictly for explicit platform admins without tenant membership
                    // OR if user.getRole() is specifically PLATFORM_ADMIN / ROLE_PLATFORM_ADMIN
                    boolean isExplicitPlatformAdmin = user.getRole() != null && (
                            user.getRole().equalsIgnoreCase("PLATFORM_ADMIN") ||
                            user.getRole().equalsIgnoreCase("ROLE_PLATFORM_ADMIN")
                    );
                    boolean isLegacyAdmin = activeMemberships.isEmpty() && user.getRole() != null && (
                            user.getRole().equalsIgnoreCase("admin") ||
                            user.getRole().equalsIgnoreCase("ROLE_ADMIN")
                    );
                    if (isExplicitPlatformAdmin) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
                    }
                    if (isLegacyAdmin) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }

                    // 3. Fallback to base user role if no authorities assigned
                    if (authorities.isEmpty()) {
                        String baseRole = user.getRole() != null ? user.getRole().toUpperCase() : "EMPLOYEE";
                        String authName = baseRole.startsWith("ROLE_") ? baseRole : "ROLE_" + baseRole;
                        authorities.add(new SimpleGrantedAuthority(authName));
                    }

                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            username, 
                            null, 
                            authorities
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}