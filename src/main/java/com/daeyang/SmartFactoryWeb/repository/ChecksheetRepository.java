package com.daeyang.SmartFactoryWeb.repository;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.daeyang.SmartFactoryWeb.dto.ChecksheetPayload;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetDetailDto;
import com.daeyang.SmartFactoryWeb.dto.ChecksheetSummaryDto;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ChecksheetRepository {
	private final JdbcTemplate pgJdbc;
	private final JdbcTemplate msJdbc;
	private final NamedParameterJdbcTemplate pgNP;
	private final NamedParameterJdbcTemplate msNP;
	private final ObjectMapper objectMapper;

	public ChecksheetRepository(@Qualifier("postgresJdbcTemplate") JdbcTemplate pgJdbc,
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
	
	public Map<String, String> personResult (String empNo) {
		SQL = """
				SELECT person_id, name, Case when P_Depart_Name is null then depart_name else P_Depart_Name end as depart_name, position_name 
        		FROM ga.dbo.Vperson
        		WHERE person_id = ?
			  """;
		
		try {
			Map<String, Object> objResult = msJdbc.queryForMap(SQL, empNo);
			Map<String, String> result = new HashMap<String, String>();
			result.put("name", objResult.get("name").toString());
			result.put("teamName", objResult.get("depart_name").toString());
			
			return result;
    	} catch (EmptyResultDataAccessException e) {
    		return null;
    	}
	}

	public Map<String, Object> selectCehckSheet_equalValue(LocalDate date, String place, LocalTime st, LocalTime et) {
		SQL = "SELECT * FROM checksheet WHERE check_date = ? AND place = ? AND start_time = ? AND end_time = ?";
		
		try {
			return pgJdbc.queryForMap(SQL, Date.valueOf(date), place, Time.valueOf(st), Time.valueOf(et));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	public long insert(ChecksheetPayload p, String payloadJson) {
		SQL = """
				INSERT INTO checksheet (check_date, dept, place, name, start_time, end_time, payload)
				         VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb)) RETURNING id
				 """;

		LocalDate date = LocalDate.parse(p.date); // "yyyy-MM-dd"
		LocalTime st = LocalTime.parse(p.startTime); // "HH:mm"
		LocalTime et = LocalTime.parse(p.endTime);

		Long id = pgJdbc.queryForObject(SQL, Long.class, Date.valueOf(date), p.dept, p.place, p.inspector,
				Time.valueOf(st), Time.valueOf(et), payloadJson);

		return id;
	}

	public List<ChecksheetSummaryDto> findList(LocalDate from, LocalDate to, String approvalStatus, int limit,
			int offset) {

		StringBuilder sql = new StringBuilder("""
				SELECT id,
				check_date,
				dept,
				place,
				"name"      AS inspector,
				start_time,
				end_time,
				e_approval_status
				FROM checksheet
				WHERE 1 = 1
				""");

		Map<String, Object> params = new HashMap<>();

		if (from != null) {
			sql.append(" AND check_date >= :from ");
			params.put("from", Date.valueOf(from));
		}
		if (to != null) {
			sql.append(" AND check_date <= :to ");
			params.put("to", Date.valueOf(to));
		}
		if (approvalStatus != null && !approvalStatus.isBlank()) {
			sql.append(" AND e_approval_status = :status ");
			params.put("status", approvalStatus);
		}

		sql.append("""
				ORDER BY check_date DESC, id DESC
				LIMIT :limit OFFSET :offset
				""");
		params.put("limit", limit);
		params.put("offset", offset);

		return pgNP.query(sql.toString(), params, (rs, rowNum) -> {
			ChecksheetSummaryDto dto = new ChecksheetSummaryDto();
			dto.id = rs.getLong("id");
			dto.checkDate = rs.getDate("check_date").toLocalDate().toString();
			dto.dept = rs.getString("dept");
			dto.place = rs.getString("place");
			dto.inspector = rs.getString("inspector");
			dto.startTime = rs.getTime("start_time").toLocalTime().toString();
			dto.endTime = rs.getTime("end_time").toLocalTime().toString();
			dto.eApprovalStatus = rs.getString("e_approval_status");
			return dto;
		});
	}

//	public List<ChecksheetSummaryDto> findAll(int limit, int offset) {
//		SQL = """
//				SELECT id,
//				       check_date,
//				       dept,
//				       place,
//				       "name"      AS inspector,
//				       start_time,
//				       end_time,
//				       e_approval_status
//				FROM checksheet
//				ORDER BY check_date DESC, id DESC
//				LIMIT ? OFFSET ?
//				""";
//
//		return pgJdbc.query(SQL, (rs, rowNum) -> {
//			ChecksheetSummaryDto dto = new ChecksheetSummaryDto();
//			dto.id = rs.getLong("id");
//			dto.checkDate = rs.getDate("check_date").toLocalDate().toString();
//			dto.dept = rs.getString("dept");
//			dto.place = rs.getString("place");
//			dto.inspector = rs.getString("inspector");
//			dto.startTime = rs.getTime("start_time").toLocalTime().toString();
//			dto.endTime = rs.getTime("end_time").toLocalTime().toString();
//			dto.eApprovalStatus = rs.getString("e_approval_status");
//			return dto;
//		}, limit, offset);
//	}

	public Optional<ChecksheetDetailDto> findDetail(long id) {
		SQL = """
				SELECT id,
				       check_date,
				       dept,
				       place,
				       "name"      AS inspector,
				       start_time,
				       end_time,
				       payload,
				       created_at,
				       e_approval_status
				FROM checksheet
				WHERE id = ?
				""";

		return pgJdbc.query(SQL, rs -> {
			if (!rs.next()) {
				return Optional.empty();
			}
			ChecksheetDetailDto dto = new ChecksheetDetailDto();
			dto.id = rs.getLong("id");
			dto.checkDate = rs.getDate("check_date").toLocalDate().toString();
			dto.dept = rs.getString("dept");
			dto.place = rs.getString("place");
			dto.inspector = rs.getString("inspector");
			dto.startTime = rs.getTime("start_time").toLocalTime().toString();
			dto.endTime = rs.getTime("end_time").toLocalTime().toString();
			dto.createdAt = rs.getTimestamp("created_at").toInstant().toString();
			dto.eApprovalStatus = rs.getString("e_approval_status");

			String payloadJson = rs.getString("payload");
			try {
				ChecksheetPayload payload = objectMapper.readValue(payloadJson, ChecksheetPayload.class);

				// 혹시라도 상단 값이 바뀌었을 때를 대비해서 맞춰줌
				payload.date = dto.checkDate;
				payload.dept = dto.dept;
				payload.place = dto.place;
				payload.inspector = dto.inspector;
				payload.startTime = dto.startTime;
				payload.endTime = dto.endTime;

				dto.payload = payload;
			} catch (Exception e) {
				throw new RuntimeException("payload 파싱 오류", e);
			}

			return Optional.of(dto);
		}, id);
	}
	
	public Map<String, Object> select_empInfo(String empNo) {
		SQL = """
				SELECT person_id, name, Case WHEN P_Depart_Name is null THEN depart_name ELSE P_Depart_Name END AS depart_name, position_name, Bonbu_Site
				FROM ga.dbo.VPerson
				WHERE person_id = ?
			  """;
		
		try {
			return msJdbc.queryForMap(SQL, empNo);
    	} catch (EmptyResultDataAccessException e) {
            return null;
        }
	}
	
	public List<Map<String, Object>> select_placeWho(List<Long> ids) {
		SQL = "SELECT id, place, name FROM checksheet WHERE id IN (:ids) ORDER BY id ASC";
		
		Map<String, Object> params = new HashMap<>();
		params.put("ids", ids);
		
		return pgNP.queryForList(SQL, params);
	}
	
	// 상신 여부 업데이트
	public void updateApprovalStatus(List<Long> ids, String status) {
	    if (ids == null || ids.isEmpty()) {
	        return;
	    }

	    SQL = """
	        UPDATE checksheet
	        SET e_approval_status = :status
	        WHERE id IN (:ids)
	    """;

	    Map<String, Object> params = new HashMap<>();
	    params.put("status", status);
	    params.put("ids", ids);

	    pgNP.update(SQL, params);
	}
	
	// 점검표 삭제
	public int deleteCheckSheet (List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
	        return 0;
	    }

	    SQL = """
	        DELETE
	        FROM checksheet 
	        WHERE id IN (:ids)
	    """;

	    Map<String, Object> params = new HashMap<>();
	    params.put("ids", ids);

	    return pgNP.update(SQL, params);
	}
}
