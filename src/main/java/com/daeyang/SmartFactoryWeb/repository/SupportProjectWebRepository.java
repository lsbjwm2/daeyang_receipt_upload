package com.daeyang.SmartFactoryWeb.repository;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class SupportProjectWebRepository {
	private final JdbcTemplate pgJdbc;
	private final JdbcTemplate msJdbc;
	private final NamedParameterJdbcTemplate pgNP;
	private final NamedParameterJdbcTemplate msNP;
	
	public SupportProjectWebRepository(@Qualifier("postgresJdbcTemplate") JdbcTemplate pgJdbc,
			@Qualifier("mssqlJdbcTemplate") JdbcTemplate msJdbc,
			@Qualifier("postgresNamedJdbc") NamedParameterJdbcTemplate pgNP,
			@Qualifier("mssqlNamedJdbc") NamedParameterJdbcTemplate msNP, ObjectMapper objectMapper) {
		this.pgJdbc = pgJdbc;
		this.msJdbc = msJdbc;
		this.pgNP = pgNP;
		this.msNP = msNP;
	}
	
	private String SQL = "";
	
	public Map<String, Object> login (String ID, String PW) {
		SQL = """
				SELECT name, person_id, CASE WHEN P_Depart_Name is null THEN depart_name ELSE P_Depart_Name end AS depart_name, position_name
				FROM ga.dbo.Vperson
				WHERE person_id = (SELECT Employee_ID FROM bs.dbo.bsUsers WHERE User_Id = ? AND Pass_Word = ?)
			  """;
		
		try {
			return msJdbc.queryForMap(SQL, ID, PW);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
}
