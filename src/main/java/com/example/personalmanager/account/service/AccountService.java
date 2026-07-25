package com.example.personalmanager.account.service;

import com.example.personalmanager.account.dto.ChangePasswordRequest;
import com.example.personalmanager.account.dto.UpdateEmailRequest;
import com.example.personalmanager.account.dto.UpdateNicknameRequest;
import com.example.personalmanager.account.dto.UpdateUsernameRequest;
import com.example.personalmanager.account.dto.UserProfileResponse;
import com.example.personalmanager.auth.entity.SysUser;
import com.example.personalmanager.auth.repository.SysUserRepository;
import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 账户服务，处理当前登录用户的资料查询、昵称/用户名/邮箱修改、头像上传与密码修改等业务。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final CurrentUserService currentUserService;
    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    /**
     * 获取当前登录用户的资料。
     *
     * @return 用户资料
     */
    public UserProfileResponse getProfile() {
        SysUser user = getCurrentUser();
        return map(user);
    }

    /**
     * 更新当前用户昵称。
     *
     * @param request 昵称更新请求
     * @return 更新后的用户资料
     */
    @Transactional
    public UserProfileResponse updateNickname(UpdateNicknameRequest request) {
        SysUser user = getCurrentUser();
        user.setNickname(normalizeRequiredText(request.getNickname(), "昵称不能为空"));
        userRepository.save(user);
        return map(user);
    }

    /**
     * 更新当前用户名，校验用户名是否被其他账号占用。
     *
     * @param request 用户名更新请求
     * @return 更新后的用户资料
     */
    @Transactional
    public UserProfileResponse updateUsername(UpdateUsernameRequest request) {
        SysUser user = getCurrentUser();
        String username = normalizeRequiredText(request.getUsername(), "用户名不能为空");
        if (userRepository.existsByUsernameAndIdNot(username, user.getId())) {
            throw new BusinessException("用户名已存在");
        }
        user.setUsername(username);
        userRepository.save(user);
        return map(user);
    }

    /**
     * 更新当前用户邮箱，校验邮箱是否被其他账号绑定。
     *
     * @param request 邮箱更新请求
     * @return 更新后的用户资料
     */
    @Transactional
    public UserProfileResponse updateEmail(UpdateEmailRequest request) {
        SysUser user = getCurrentUser();
        String email = normalizeRequiredText(request.getEmail(), "邮箱不能为空");
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        user.setEmail(email);
        userRepository.save(user);
        return map(user);
    }

    /**
     * 上传并更新当前用户头像，保存新头像后删除旧头像文件。
     *
     * @param file 头像文件
     * @return 更新后的用户资料
     */
    @Transactional
    public UserProfileResponse uploadAvatar(MultipartFile file) {
        SysUser user = getCurrentUser();
        String oldAvatar = user.getAvatarPath();
        String filename = avatarStorageService.store(file);
        user.setAvatarPath(filename);
        userRepository.save(user);
        avatarStorageService.deleteIfExists(oldAvatar);
        return map(user);
    }

    /**
     * 修改当前用户密码，校验旧密码正确且新旧密码不相同。
     *
     * @param request 修改密码请求
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        SysUser user = getCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // 将用户实体转换为资料响应，昵称为空时回退为用户名，并生成头像访问地址
    private UserProfileResponse map(SysUser user) {
        String nickname = user.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = user.getUsername();
        }
        return UserProfileResponse.builder()
                .username(user.getUsername())
                .nickname(nickname)
                .email(user.getEmail())
                .avatarUrl(user.getAvatarPath() == null ? null : "/api/account/me/avatar")
                .build();
    }

    // 获取当前登录用户
    private SysUser getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    // 校验并规整必填文本，去除首尾空白，为空时抛出业务异常
    private String normalizeRequiredText(String value, String emptyMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(emptyMessage);
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(emptyMessage);
        }
        return normalized;
    }
}
