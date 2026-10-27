package com.cleaner.be.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cleaner.be.auth.JwtTokenProvider;
import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;

/** 알림 API의 JWT 소유자 제한·홈 세 건·페이지·읽음 처리를 실제 Spring 보안과 저장소로 확인합니다. */
@SpringBootTest
class NotificationControllerIntegrationTest {

	private MockMvc mockMvc;
	private String accessToken;
	private final List<Long> createdNotificationIds = new ArrayList<>();

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationStreamService notificationStreamService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private Member member;
	private Member anotherMember;
	private Long anotherMemberNotificationId;

	/** 별도 회원과 알림을 준비해 실제 인증된 HTTP API 응답을 검증합니다. */
	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
		String suffix = UUID.randomUUID().toString();
		member = memberRepository.save(new Member("n-" + suffix + "@ex.com", "encoded", "알림 회원", "01012345678"));
		anotherMember = memberRepository.save(new Member("o-" + suffix + "@ex.com", "encoded", "다른 회원", "01012345679"));
		accessToken = jwtTokenProvider.createAccessToken(member);
		for (int index = 0; index < 4; index++) {
			Notification saved = notificationRepository.save(new Notification(member, null, NotificationType.RESERVATION_RECEIVED,
				"알림 " + index, LocalDateTime.now().minusMinutes(4 - index)));
			createdNotificationIds.add(saved.getId());
		}
		Notification otherMemberNotification = notificationRepository.save(new Notification(anotherMember, null,
			NotificationType.RESERVATION_APPROVED, "다른 회원 알림", LocalDateTime.now()));
		createdNotificationIds.add(otherMemberNotification.getId());
		anotherMemberNotificationId = otherMemberNotification.getId();
	}

	/** 테스트에서 만든 알림과 회원만 제거해 다른 테스트 데이터에는 영향을 주지 않습니다. */
	@AfterEach
	void cleanUp() {
		if (member != null) {
			notificationRepository.deleteByMemberId(member.getId());
			memberRepository.delete(member);
		}
		if (anotherMember != null) {
			notificationRepository.deleteByMemberId(anotherMember.getId());
			memberRepository.delete(anotherMember);
		}
	}

	/** 홈 API는 최신 세 건만 반환하지만 미읽음 개수는 전체 알림 기준으로 계산합니다. */
	@Test
	void homeReturnsLatestThreeAndOverallUnreadCount() throws Exception {
		mockMvc.perform(get("/api/notifications/home").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notifications.length()").value(3))
			.andExpect(jsonPath("$.notifications[0].message").value("알림 3"))
			.andExpect(jsonPath("$.unreadCount").value(4));
	}

	/** 보관 API는 요청한 크기의 페이지와 페이지 메타데이터를 반환합니다. */
	@Test
	void historyReturnsRequestedPage() throws Exception {
		mockMvc.perform(get("/api/notifications").param("page", "0").param("size", "2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.notifications.length()").value(2))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(4));
	}

	/** 다른 회원의 알림 ID를 읽음 처리하지 못하게 하며 본인 알림은 읽음 시각을 기록합니다. */
	@Test
	void readEndpointEnforcesOwnerAndUpdatesUnreadCount() throws Exception {
		mockMvc.perform(patch("/api/notifications/{id}/read", anotherMemberNotificationId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/notifications/{id}/read", createdNotificationIds.get(0))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.readAt").isNotEmpty());

		mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value(3));

		mockMvc.perform(patch("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value(3));
		mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value(0));
	}

	/** 인증된 SSE 연결에 트랜잭션 커밋 후 본인 신규 알림 이벤트를 전달합니다. */
	@Test
	void streamPushesCommittedNotificationToAuthenticatedMember() throws Exception {
		MvcResult stream = mockMvc.perform(MockMvcRequestBuilders.get("/api/notifications/stream")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(request().asyncStarted())
			.andReturn();

		notificationService.create(member, null, NotificationType.RESERVATION_APPROVED, "예약이 확정되었습니다.");
		notificationStreamService.closeAll();

		String eventStream = stream.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(eventStream).contains("event:notification", "예약이 확정되었습니다.");
	}
}
