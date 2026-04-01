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
	private final JdbcTemplate pgJdbc;
	private final JdbcTemplate msJdbc;
	private final NamedParameterJdbcTemplate pgNP;
	private final NamedParameterJdbcTemplate msNP;
	private final ObjectMapper objectMapper;

	public ReceiptRepository(@Qualifier("postgresJdbcTemplate") JdbcTemplate pgJdbc,
			@Qualifier("mssqlJdbcTemplate") JdbcTemplate msJdbc,
			@Qualifier("postgresNamedJdbc") NamedParameterJdbcTemplate pgNP,
			@Qualifier("mssqlNamedJdbc") NamedParameterJdbcTemplate msNP, ObjectMapper objectMapper) {
		this.pgJdbc = pgJdbc;
		this.msJdbc = msJdbc;
		this.pgNP = pgNP;
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
}
