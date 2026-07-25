package com.example.personalmanager.account.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户个人资料响应数据。
 */
@Data
@Builder
public class UserProfileResponse {

    // 用户名
    private String username;
    // 昵称
    private String nickname;
    // 邮箱
    private String email;
    // 头像访问地址
    private String avatarUrl;
}
