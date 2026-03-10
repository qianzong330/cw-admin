package com.example.hello.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务 - 使用HTTP直接上传到Sealos对象存储
 */
@Service
public class FileUploadService {

    private static final String INVOICE_FOLDER = "invoices/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final String UPLOAD_URL = "http://static-host-z4bn2xr7-hsc-images.sealoshzh.site/";

    /**
     * 上传单张发票图片到Sealos对象存储
     */
    public String uploadInvoice(MultipartFile file) throws IOException {
        validateFile(file);
        
        String fileName = generateFileName(file.getOriginalFilename());
        String key = INVOICE_FOLDER + fileName;
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_URL + key))
                    .header("Content-Type", file.getContentType())
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return UPLOAD_URL + key;
            } else {
                throw new IOException("上传失败，状态码: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new IOException("发票上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传多张发票图片
     */
    public List<String> uploadInvoices(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                urls.add(uploadInvoice(file));
            }
        }
        return urls;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("只允许上传图片文件（JPG、PNG、GIF、WEBP）");
        }
    }

    /**
     * 生成文件名：原文件名_时间戳_随机数.扩展名
     */
    private String generateFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8);
        
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 提取原文件名（不含扩展名）
        String baseName = "invoice";
        if (originalFilename != null && originalFilename.contains(".")) {
            baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            // 限制长度并去除特殊字符
            baseName = baseName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
            if (baseName.length() > 20) {
                baseName = baseName.substring(0, 20);
            }
        }
        
        return baseName + "_" + timestamp + "_" + random + extension;
    }
}
