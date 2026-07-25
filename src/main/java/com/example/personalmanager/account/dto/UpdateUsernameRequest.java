package com.example.personalmanager.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户名请求参数。
 */
@Data
public class UpdateUsernameRequest {

    // 用户名
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在 4 到 50 之间")
    private String username;
}
