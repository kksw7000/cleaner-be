package com.cleaner.be;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Spring Boot를 시작하고 엔티티 생성·수정 시각 자동 기록을 활성화합니다. */
@SpringBootApplication
@EnableJpaAuditing
public class BeApplication {

	/** 예약 날짜 정책이 서버 시간대와 관계없이 대한민국 날짜를 기준으로 동작하게 합니다. */
	@Bean
	Clock koreaClock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}

	/** 설정과 컴포넌트를 로드해 백엔드 애플리케이션을 실행합니다. */
	public static void main(String[] args) {
		SpringApplication.run(BeApplication.class, args);
	}
}
