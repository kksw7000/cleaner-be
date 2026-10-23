package com.cleaner.be.reservation;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Year;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cleaner.be.auth.JwtTokenProvider;
import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;

/** 월별 예약 가능성 API가 연월 범위를 HTTP 경계에서 검증하는지 확인합니다. */
@SpringBootTest
class ReservationAvailabilityValidationTest {

	private MockMvc mockMvc;
	private String accessToken;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	/** 실제 SecurityFilterChain과 Bean Validation이 적용되는 MockMvc 요청을 준비합니다. */
	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
		Member member = memberRepository.save(new Member("availability@example.com", "encoded-password", "availability", "01098765432"));
		accessToken = jwtTokenProvider.createAccessToken(member);
	}

	/** 테스트 회원이 공유 데이터베이스에 남아 다른 테스트와 충돌하지 않게 정리합니다. */
	@AfterEach
	void cleanUp() {
		memberRepository.deleteAll();
	}

	/** 현재 연도 기준 허용 범위의 양 끝을 포함해 기존 월별 조회 흐름이 유지되는지 확인합니다. */
	@Test
	void acceptedYearAndMonthReturnMonthlyAvailability() throws Exception {
		int currentYear = Year.now().getValue();
		assertAcceptedMonth(Integer.toString(currentYear - 3), "1", 31);
		assertAcceptedMonth(Integer.toString(currentYear), "5", 31);
		assertAcceptedMonth(Integer.toString(currentYear + 3), "12", 31);
	}

	/** 현재 연도 기준 ±3년 또는 월 1~12의 허용 범위를 벗어나면 HTTP 400을 반환합니다. */
	@Test
	void outOfRangeYearOrMonthReturnsBadRequest() throws Exception {
		int currentYear = Year.now().getValue();
		assertBadRequest(Integer.toString(currentYear - 4), "5");
		assertBadRequest(Integer.toString(currentYear + 4), "5");
		assertBadRequest(Integer.toString(currentYear), "0");
		assertBadRequest(Integer.toString(currentYear), "13");
	}

	/** 연월 파라미터를 변경해 검증 실패가 400으로 매핑되는지 확인합니다. */
	private void assertBadRequest(String year, String month) throws Exception {
		int currentYear = Year.now().getValue();
		int requestedYear = Integer.parseInt(year);
		String expectedMessage = requestedYear < currentYear - 3 || requestedYear > currentYear + 3
			? "예약 조회 연도는 " + (currentYear - 3) + "년부터 " + (currentYear + 3) + "년까지 선택할 수 있습니다."
			: "요청 파라미터가 허용 범위를 벗어났습니다.";
		mockMvc.perform(get("/api/reservations/availability")
				.param("year", year)
				.param("month", month)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(expectedMessage));
	}

	/** 연월을 요청해 성공 상태와 해당 월 날짜 수가 기존 서비스 응답으로 유지되는지 확인합니다. */
	private void assertAcceptedMonth(String year, String month, int expectedDays) throws Exception {
		mockMvc.perform(get("/api/reservations/availability")
				.param("year", year)
				.param("month", month)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(expectedDays));
	}
}
