package com.example.personalmanager.account.service;

import com.example.personalmanager.common.BusinessException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 头像文件存储服务，负责头像的本地存储、读取、内容类型识别与删除。
 */
@Service
public class AvatarStorageService {

    // 允许的头像文件扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    // 头像文件大小上限（5MB）
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    // 头像存储相对目录
    private static final String RELATIVE_DIR = "src/main/resources/userimage";

    /**
     * 校验并存储头像文件，生成随机文件名后写入本地目录。
     *
     * @param file 头像文件
     * @return 存储后的文件名
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("头像文件大小不能超过 5MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的头像格式");
        }

        // 使用 UUID 生成唯一文件名，避免文件名冲突
        String filename = UUID.randomUUID().toString().replace("-", "") + extension.toLowerCase();
        Path dir = getStorageDir();
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("头像文件保存失败");
        }
        return filename;
    }

    /**
     * 按文件名加载头像为资源对象。
     *
     * @param filename 头像文件名
     * @return 头像文件资源
     */
    public Resource loadAsResource(String filename) {
        Path path = getStorageDir().resolve(filename);
        if (!Files.exists(path)) {
            throw new BusinessException("头像文件不存在");
        }
        try {
            return new ByteArrayResource(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new BusinessException("头像文件读取失败");
        }
    }

    /**
     * 获取头像文件的内容类型，探测失败时按扩展名回退判断。
     *
     * @param filename 头像文件名
     * @return MIME 内容类型
     */
    public String getContentType(String filename) {
        Path path = getStorageDir().resolve(filename);
        try {
            String contentType = Files.probeContentType(path);
            if (StringUtils.hasText(contentType)) {
                return contentType;
            }
        } catch (IOException ignored) {
            // 探测失败时忽略，改用扩展名回退判断
            // Ignore and fallback by extension.
        }
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 删除指定头像文件（若存在），删除失败不抛异常以免阻塞资料更新。
     *
     * @param filename 头像文件名
     */
    public void deleteIfExists(String filename) {
        if (!StringUtils.hasText(filename)) {
            return;
        }
        Path path = getStorageDir().resolve(filename);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 删除失败时忽略，避免阻塞资料更新
            // Ignore deletion failure to avoid blocking profile updates.
        }
    }

    /**
     * 获取头像存储目录的绝对路径（基于应用运行目录拼接相对目录）。
     *
     * @return 存储目录路径
     */
    public Path getStorageDir() {
        return Paths.get(System.getProperty("user.dir")).resolve(RELATIVE_DIR);
    }

    // 从文件名中提取扩展名（含点号），文件名不合法时抛出业务异常
    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException("头像文件名不合法");
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
