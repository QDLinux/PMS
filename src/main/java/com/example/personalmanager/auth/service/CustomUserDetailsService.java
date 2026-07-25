package com.example.personalmanager.auth.service;

import com.example.personalmanager.auth.entity.SysUser;
import com.example.personalmanager.auth.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义用户详情服务，实现 Spring Security 的 UserDetailsService，
 * 根据用户名从数据库加载用户认证信息。
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository userRepository;

    /**
     * 根据用户名加载用户详情，供 Spring Security 认证使用。
     *
     * @param username 用户名
     * @return 封装用户名、密码及权限的 UserDetails 对象
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return new User(user.getUsername(), user.getPassword(), List.of());
    }
}
