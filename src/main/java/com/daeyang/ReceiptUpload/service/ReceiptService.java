package com.daeyang.ReceiptUpload.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.daeyang.ReceiptUpload.repository.ReceiptRepository;

@Service
public class ReceiptService {
	private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);
	private static final DateTimeFormatter REG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ReceiptRepository repository;
	@Value("${receipt.storage.base-path}")
	private String receiptStorageBasePath;
	
	public ReceiptService (ReceiptRepository repository) {
		this.repository = repository;
	}
	
	public Map<String, Object> empLogin (String empID, String empPW) {
		return repository.empLogin(empID, empPW);
	}

	@Transactional
	public Map<String, Object> uploadReceipts(String empNo, List<MultipartFile> files) throws IOException {
		LocalDate today = LocalDate.now();
		String regDate = today.format(REG_DATE_FORMAT);
		Path storageDir = Paths.get(receiptStorageBasePath);
		if (!Files.exists(storageDir)) {
			Files.createDirectories(storageDir);
		}

		List<Path> savedPaths = new ArrayList<>();
		List<String> savedFileNames = new ArrayList<>();

		try {
			int nextSeq = repository.findNextSeq(empNo, regDate);

			for (MultipartFile file : files) {
				if (file.isEmpty()) {
					continue;
				}

				String contentType = file.getContentType();
				if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
					continue;
				}

				String originalName = sanitizeOriginalFilename(file.getOriginalFilename());
				String fileExt = extractFileExt(originalName);
				String storedFileName = resolveStoredFileName(storageDir, originalName);
				Path targetPath = storageDir.resolve(storedFileName);

				Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
				savedPaths.add(targetPath);
				savedFileNames.add(storedFileName);

				repository.insertAccountFileUrl(empNo, regDate, nextSeq, storedFileName, storedFileName, fileExt);
				nextSeq++;
			}

			if (savedFileNames.isEmpty()) {
				throw new IllegalArgumentException("저장 가능한 이미지 파일이 없습니다.");
			}

			return Map.of("message", "업로드 성공", "savedFileNames", savedFileNames);
		} catch (Exception e) {
			rollbackSavedFiles(savedPaths);
			throw e;
		}
	}

	private void rollbackSavedFiles(List<Path> savedPaths) {
		for (Path savedPath : savedPaths) {
			try {
				Files.deleteIfExists(savedPath);
			} catch (IOException deleteException) {
				log.error("업로드 롤백 파일 삭제 실패 path = {}, message = {}", savedPath, deleteException.getMessage());
			}
		}
	}

	private String sanitizeOriginalFilename(String originalName) {
		if (originalName == null || originalName.isBlank()) {
			return "unknown.jpg";
		}

		String normalized = originalName.replace('\\', '/');
		String sanitized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		return sanitized.isBlank() ? "unknown.jpg" : sanitized;
	}

	private String extractFileExt(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(lastDotIndex);
	}

	private String resolveStoredFileName(Path directory, String originalName) {
		String fileExt = extractFileExt(originalName);
		String baseName = fileExt.isEmpty() ? originalName : originalName.substring(0, originalName.length() - fileExt.length());
		Path candidatePath = directory.resolve(originalName);

		if (!Files.exists(candidatePath)) {
			return originalName;
		}

		int suffix = extractTrailingNumber(baseName);
		String prefix = suffix >= 0 ? trimTrailingNumber(baseName) : baseName + "_";
		int nextSuffix = suffix >= 0 ? suffix + 1 : 1;

		while (true) {
			String candidateName = prefix + nextSuffix + fileExt;
			if (!Files.exists(directory.resolve(candidateName))) {
				return candidateName;
			}
			nextSuffix++;
		}
	}

	private int extractTrailingNumber(String baseName) {
		int underscoreIndex = baseName.lastIndexOf('_');
		if (underscoreIndex < 0 || underscoreIndex == baseName.length() - 1) {
			return -1;
		}

		String trailing = baseName.substring(underscoreIndex + 1);
		for (int i = 0; i < trailing.length(); i++) {
			if (!Character.isDigit(trailing.charAt(i))) {
				return -1;
			}
		}

		try {
			return Integer.parseInt(trailing);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private String trimTrailingNumber(String baseName) {
		int underscoreIndex = baseName.lastIndexOf('_');
		return underscoreIndex < 0 ? baseName : baseName.substring(0, underscoreIndex + 1);
	}

}
