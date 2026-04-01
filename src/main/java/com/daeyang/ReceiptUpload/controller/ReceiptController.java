package com.daeyang.ReceiptUpload.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.daeyang.ReceiptUpload.service.ReceiptService;

@RestController
@RequestMapping("/api/receipt")
public class ReceiptController {
	private static final Logger log = LoggerFactory.getLogger(ReceiptController.class);
	private final ReceiptService service;
	private static final Path BASE_DIR = Paths.get("D:\\receiptIMG");
	
	public ReceiptController (ReceiptService service) {
		this.service = service;
	}
	
	@PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> requestBody) {
    	if (requestBody == null || requestBody.isEmpty()) {
    		return ResponseEntity.badRequest().build();
    	}
    	
    	String empID = requestBody.get("empID").toString();
    	String empPW = requestBody.get("empPW").toString();

    	Map<String, Object> loginResult = service.empLogin(empID, empPW);

        // DB에 저장된 해시와 비교
        if (loginResult == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else {
        	String empNo = loginResult.get("Employee_ID").toString();
        	return ResponseEntity.ok(Map.of("empNo", empNo));
        }
    }

	// 영수증 사진 저장
	@PostMapping("/upload")
    public ResponseEntity<?> uploadReceipts(@RequestParam("empNo") String empNo, @RequestParam("receiptImages") List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "업로드할 파일이 없습니다."));
        }
		
        try {        	
        	List<String> savedFileNames = new ArrayList<>();

            // 사용자별 폴더 생성
//            Path userDir = Paths.get(BASE_DIR, empNo);
//            if (!Files.exists(userDir)) {
//                Files.createDirectories(userDir);
//            }

        	if (!Files.exists(BASE_DIR)) {
                Files.createDirectories(BASE_DIR);
            }
        	
            for (MultipartFile file : files) {

                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                
                // 이미지 파일만 허용
                if (!file.getContentType().startsWith("image/")) continue;
                
                // null 방어
                if (originalName == null || originalName.isBlank()) {
                	log.error("originalName == null || originalName.isBlank() = " + empNo);
                    originalName = "unknown.jpg";
                }
                
                Path targetPath = BASE_DIR.resolve(originalName);

                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                savedFileNames.add(originalName);
            }

            return ResponseEntity.ok(Map.of("message", "업로드 성공"));

        } catch (Exception e) {
            log.error(empNo + " 업로드 중 오류 발생 e = " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "업로드 중 오류 발생"));
        }
    }
}
