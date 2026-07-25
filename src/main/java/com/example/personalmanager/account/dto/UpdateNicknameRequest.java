package com.example.personalmanager.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新昵称请求参数。
 */
@Data
public class UpdateNicknameRequest {

    // 昵称
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;
}
