package com.example.personalmanager.account.controller;

import com.example.personalmanager.account.dto.ChangePasswordRequest;
import com.example.personalmanager.account.dto.UpdateEmailRequest;
import com.example.personalmanager.account.dto.UpdateNicknameRequest;
import com.example.personalmanager.account.dto.UpdateUsernameRequest;
import com.example.personalmanager.account.dto.UserProfileResponse;
import com.example.personalmanager.account.service.AccountService;
import com.example.personalmanager.account.service.AvatarStorageService;
import com.example.personalmanager.auth.entity.SysUser;
import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 账户控制器，提供当前登录用户的个人资料查询与修改、头像上传与读取、密码修改等接口。
 */
@RestController
@RequestMapping("/api/account/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUserService;
    private final AvatarStorageService avatarStorageService;

    /**
     * 获取当前登录用户的个人资料。
     *
     * @return 用户资料
     */
    @GetMapping
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.ok(accountService.getProfile());
    }

    /**
     * 更新当前用户昵称。
     *
     * @param request 昵称更新请求
     * @return 更新后的用户资料
     */
    @PutMapping("/nickname")
    public ApiResponse<UserProfileResponse> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        return ApiResponse.ok("昵称更新成功", accountService.updateNickname(request));
    }

    /**
     * 更新当前用户名（更新后需重新登录）。
     *
     * @param request 用户名更新请求
     * @return 更新后的用户资料
     */
    @PutMapping("/username")
    public ApiResponse<UserProfileResponse> updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        return ApiResponse.ok("用户名更新成功，请重新登录", accountService.updateUsername(request));
    }

    /**
     * 更新当前用户邮箱。
     *
     * @param request 邮箱更新请求
     * @return 更新后的用户资料
     */
    @PutMapping("/email")
    public ApiResponse<UserProfileResponse> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        return ApiResponse.ok("邮箱更新成功", accountService.updateEmail(request));
    }

    /**
     * 上传并更新当前用户头像。
     *
     * @param file 头像文件
     * @return 更新后的用户资料
     */
    @PostMapping("/avatar")
    public ApiResponse<UserProfileResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("头像上传成功", accountService.uploadAvatar(file));
    }

    /**
     * 读取当前用户头像文件，未设置头像时返回 404。
     *
     * @return 头像文件资源
     */
    @GetMapping("/avatar")
    public ResponseEntity<Resource> avatar() {
        SysUser user = currentUserService.getCurrentUser();
        if (user.getAvatarPath() == null || user.getAvatarPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = avatarStorageService.loadAsResource(user.getAvatarPath());
        String contentType = avatarStorageService.getContentType(user.getAvatarPath());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
    }

    /**
     * 修改当前用户密码。
     *
     * @param request 修改密码请求
     * @return 操作结果
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ApiResponse.ok("密码更新成功", null);
    }
}
