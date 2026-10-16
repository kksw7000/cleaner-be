package com.cleaner.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Spring Boot를 시작하고 엔티티 생성·수정 시각 자동 기록을 활성화합니다. */
@SpringBootApplication
@EnableJpaAuditing
public class BeApplication {

	/** 설정과 컴포넌트를 로드해 백엔드 애플리케이션을 실행합니다. */
	public static void main(String[] args) {
		SpringApplication.run(BeApplication.class, args);
	}
}
