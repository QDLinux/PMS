package com.example.personalmanager.security;

import com.example.personalmanager.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类。
 * 配置无状态的 JWT 认证机制、请求授权规则、认证提供者及密码编码器。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthHandlers restAuthHandlers;

    /**
     * 配置安全过滤链，定义认证、授权及异常处理策略。
     *
     * @param http HttpSecurity 配置对象
     * @return 构建完成的安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（采用基于 Token 的无状态认证，无需 CSRF 防护）
                .csrf(csrf -> csrf.disable())
                // 设置会话策略为无状态，不创建 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 自定义未认证与无权限访问的异常处理
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthHandlers)
                        .accessDeniedHandler(restAuthHandlers)
                )
                // 配置请求授权规则：静态资源与认证接口放行，其余请求均需认证
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/dashboard.html", "/app/**", "/assets/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // 在用户名密码认证过滤器之前加入 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置基于数据库用户详情与密码编码器的认证提供者。
     *
     * @return DaoAuthenticationProvider 实例
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * 提供密码编码器，使用委托式编码器以支持多种密码加密格式。
     *
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 暴露认证管理器，供登录认证流程使用。
     *
     * @param configuration 认证配置对象
     * @return AuthenticationManager 实例
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
