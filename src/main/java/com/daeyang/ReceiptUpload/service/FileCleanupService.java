package com.daeyang.ReceiptUpload.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class FileCleanupService {

	private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);
	private static final long EXPIRATION_DAYS = 30; // 30일

	@Value("${receipt.storage.base-path}")
	private String receiptStorageBasePath;

	// 매일 01시에 실행
	@Scheduled(cron = "0 0 1 * * ?")
	public void cleanupOldReceiptImages() {
		log.info("Starting scheduled cleanup of old receipt images from: {}", receiptStorageBasePath);

		Path storagePath = Paths.get(receiptStorageBasePath);

		if (!Files.exists(storagePath)) {
			log.error("Receipt image storage path does not exist: {}", receiptStorageBasePath);
			return;
		}

		try {
			// 30일 전 시점 계산
			Instant thirtyDaysAgo = Instant.now().minus(EXPIRATION_DAYS, ChronoUnit.DAYS);

			Files.list(storagePath)
			// 일반 파일(디렉토리가 아닌)만 필터링
			.filter(Files::isRegularFile)
			.forEach(file -> {
				try {
					// 파일의 마지막 수정 시간 가져오기
					FileTime lastModifiedTime = Files.getLastModifiedTime(file);
					// 마지막 수정 시간이 30일 전보다 이전인지 확인
					if (lastModifiedTime.toInstant().isBefore(thirtyDaysAgo)) {
						Files.delete(file);
					}
				} catch (IOException e) {
					log.error("Error deleting file {}: {}", file, e.getMessage());
				} catch (SecurityException e) {
					log.error("Security error deleting file {}: {}", file, e.getMessage());
				}
			});
			log.info("Finished scheduled cleanup of old receipt images.");
		} catch (IOException e) {
			log.error("Error listing files in {}: {}", receiptStorageBasePath, e.getMessage());
		}
	}
}
