package com.daeyang.ReceiptUpload.repository;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ReceiptRepository {
	private final JdbcTemplate msJdbc;
	private final NamedParameterJdbcTemplate msNP;
	private final ObjectMapper objectMapper;

	public ReceiptRepository(@Qualifier("mssqlJdbcTemplate") JdbcTemplate msJdbc,
			@Qualifier("mssqlNamedJdbc") NamedParameterJdbcTemplate msNP, ObjectMapper objectMapper) {
		this.msJdbc = msJdbc;
		this.msNP = msNP;
		this.objectMapper = objectMapper;
	}

	private String SQL = "";
	
	public Map<String, Object> empLogin (String empID, String empPW) {
		SQL = "SELECT Employee_ID FROM bs.dbo.bsUsers WHERE User_Id = ? AND Pass_Word = ?";
		
		try {
			return msJdbc.queryForMap(SQL, empID, empPW);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public int findNextSeq(String employeeId, String regDate) {
		SQL = "SELECT ISNULL(MAX(SEQ), 0) + 1 " +
			  "FROM PLC_RUN.dbo.Account_FileUrl WITH (UPDLOCK, HOLDLOCK) " +
			  "WHERE Employee_ID = ? AND Reg_Date = ?";
		Integer nextSeq = msJdbc.queryForObject(SQL, Integer.class, employeeId, regDate);
		return nextSeq == null ? 1 : nextSeq;
	}

	public int insertAccountFileUrl(String employeeId, String regDate, int seq, String fileUrl, String fileName, String fileExt) {
		SQL = "INSERT INTO PLC_RUN.dbo.Account_FileUrl " +
			  "(Employee_ID, Reg_Date, SEQ, File_Url, File_Name, File_Ext, Electron_Doc_Numb, SAUPJANG, BUSEO, JPNO, GIPYOILJA) " +
			  "VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL)";
		return msJdbc.update(SQL, employeeId, regDate, seq, fileUrl, fileName, fileExt);
	}
}
