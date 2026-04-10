package com.bustech.erp.company.service;

import com.bustech.erp.common.exception.BusinessException;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.company.dto.CreateUserRequest;
import com.bustech.erp.company.dto.UserResponse;
import com.bustech.erp.company.entity.User;
import com.bustech.erp.company.repository.UserRepository;
import com.bustech.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        return UserPrincipal.from(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }

        var company = companyService.findById(request.companyId());

        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(request.role())
            .company(company)
            .build();

        return toResponse(userRepository.save(user));
    }

    public Page<UserResponse> findByCompany(Long companyId, Pageable pageable) {
        return userRepository.findByCompanyId(companyId, pageable)
            .map(this::toResponse);
    }

    public UserResponse findByIdAndCompany(Long id, Long companyId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCompany().getId(),
            user.getCompany().getName(),
            user.getCreatedAt()
        );
    }
}
