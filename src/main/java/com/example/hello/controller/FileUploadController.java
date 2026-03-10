package com.example.hello.controller;

import com.example.hello.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传Controller
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 上传单张发票图片
     */
    @PostMapping("/invoice")
    public ResponseEntity<Map<String, Object>> uploadInvoice(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String url = fileUploadService.uploadInvoice(file);
            result.put("success", true);
            result.put("url", url);
            result.put("message", "上传成功");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 上传多张发票图片
     */
    @PostMapping("/invoices")
    public ResponseEntity<Map<String, Object>> uploadInvoices(@RequestParam("files") List<MultipartFile> files) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<String> urls = fileUploadService.uploadInvoices(files);
            result.put("success", true);
            result.put("urls", urls);
            result.put("message", "上传成功");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
