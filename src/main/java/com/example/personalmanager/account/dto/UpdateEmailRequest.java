package com.example.personalmanager.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新邮箱请求参数。
 */
@Data
public class UpdateEmailRequest {

    // 邮箱地址
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;
}
