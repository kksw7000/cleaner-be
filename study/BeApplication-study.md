# `BeApplication.java` 학습 문서

원본: [`BeApplication.java`](../src/main/java/com/cleaner/be/BeApplication.java)

## 1. 파일의 역할

이 파일은 백엔드 애플리케이션의 시작점입니다. Java 프로그램을 실행할 때 호출되는 `main` 메서드가 Spring Boot를 시작하고, Spring Data JPA의 생성·수정 시각 자동 기록 기능을 켭니다.

- **계층:** 애플리케이션 시작 및 전역 설정
- **필요한 이유:** 실행할 때 Spring이 관리할 객체와 설정을 준비하고 웹 서버를 시작해야 하기 때문입니다.
- **사용되는 상황:** Gradle의 `bootRun`이나 빌드된 애플리케이션을 실행할 때 시작됩니다.

## 2. 전체 코드

아래는 분석한 원본 파일 전체입니다. 문서에는 설명용 주석만 추가했으며 실행 코드는 원본과 같습니다.

```java
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
```

## 3. 코드 한 줄씩 자세히 설명

- `package com.cleaner.be;`: 이 클래스가 `com.cleaner.be` 패키지에 속한다고 선언합니다. 패키지는 관련 Java 파일을 분류하는 폴더 같은 이름 공간입니다.
- `import org.springframework.boot.SpringApplication;`: Spring Boot를 시작하는 `SpringApplication`을 가져옵니다.
- `import ...SpringBootApplication;`: 애플리케이션의 대표 설정을 표시하는 애너테이션을 가져옵니다.
- `import ...EnableJpaAuditing;`: JPA 감사(auditing), 즉 엔티티의 생성·수정 시각을 자동으로 기록하는 기능을 켜는 애너테이션을 가져옵니다.
- `@SpringBootApplication`: Spring Boot의 자동 설정과 컴포넌트 검색을 활성화합니다. 이 클래스의 패키지(`com.cleaner.be`) 아래 구성요소들을 찾아 애플리케이션에 연결하는 기준점입니다.
- `@EnableJpaAuditing`: `@CreatedDate`, `@LastModifiedDate`로 표시한 시각 필드가 자동으로 채워지도록 켭니다. 이 기능은 공통 기반 클래스인 [`BaseTimeEntity`](../src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java)에서 사용됩니다.
- `public class BeApplication`: 다른 코드와 Java 실행 환경이 접근할 수 있는 공개 클래스입니다.
- `public static void main(String[] args)`: Java 프로그램의 시작 메서드입니다. `static`이므로 객체를 먼저 만들지 않고 실행할 수 있고, `void`는 반환값이 없다는 뜻입니다. `args`는 실행 인자를 담는 문자열 배열입니다.
- `SpringApplication.run(BeApplication.class, args);`: Spring Boot가 설정을 읽고 필요한 객체를 연결한 뒤 애플리케이션을 실행합니다. `BeApplication.class`는 시작 설정이 있는 클래스를, `args`는 실행 인자를 전달합니다.

## 4. 메서드별 실행 과정

### `main(String[] args)`

1. **목적:** Spring Boot 애플리케이션을 시작합니다.
2. **입력:** 명령줄 실행 인자를 담은 `String[] args`.
3. **순서:** JVM이 `main`을 호출하고 → `SpringApplication.run`에 현재 애플리케이션 클래스를 전달하고 → Spring Boot가 설정 및 컴포넌트를 준비합니다.
4. **호출 이유:** Spring Boot가 서버와 의존 객체를 설정하는 시작 절차를 대신 수행하게 합니다.
5. **반환:** `void`이므로 값을 반환하지 않습니다. 애플리케이션이 실행 상태를 유지합니다.
6. **예외:** 이 메서드 자체에는 예외 처리 코드가 없습니다. 시작 설정이나 연결 과정의 오류는 Spring Boot 시작 실패로 이어질 수 있습니다. 구체적인 실패 조건은 다른 설정과 실행 환경에 달려 있습니다.

## 5. 실제 실행 흐름

확인 가능한 시작 흐름은 다음과 같습니다.

```text
Java 실행
  → BeApplication.main(args)
  → SpringApplication.run(BeApplication.class, args)
  → Spring Boot 설정·컴포넌트 초기화
  → 웹 애플리케이션 실행
```

애플리케이션이 실행되면 `@SpringBootApplication`이 기준 패키지 아래의 Spring 구성요소를 검색합니다. `@EnableJpaAuditing`도 함께 활성화되어, JPA 엔티티의 생성·수정 시각을 자동으로 채우는 기능을 사용할 수 있습니다.

## 6. 다른 파일과의 관계

- [`BaseTimeEntity.java`](../src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java)는 `@CreatedDate`와 `@LastModifiedDate` 필드를 선언합니다. 이 시작 클래스의 `@EnableJpaAuditing`이 해당 기능을 활성화합니다.
- 다른 `@RestController`, `@Service`, `@Repository` 등의 구성요소는 애플리케이션의 컴포넌트 검색으로 Spring에 연결됩니다. 개별 연결 흐름은 각 기능 문서에서 확인할 수 있습니다.
- Spring Boot의 내부 초기화 세부 순서는 프레임워크 동작이므로 이 파일이 직접 구현하지 않습니다.

## 7. 핵심 개념 정리

- **진입점:** Java 프로그램이 시작되는 `main` 메서드입니다.
- **애너테이션:** 클래스나 코드가 맡은 역할을 프레임워크에 알려주는 표시입니다.
- **자동 설정:** Spring Boot가 의존성 및 설정을 바탕으로 필요한 구성을 준비하는 기능입니다.
- **컴포넌트 검색:** `@Service` 같은 Spring 관리 대상 클래스를 찾아 연결하는 기능입니다.
- **JPA Auditing:** DB에 저장되는 엔티티의 생성·수정 시각을 자동으로 기록하는 Spring Data 기능입니다.

## 8. 예시로 이해하기

터미널에서 `.\gradlew.bat bootRun`을 실행하면 Gradle이 애플리케이션을 실행하고 JVM은 `BeApplication.main`을 호출합니다. 이 메서드는 `SpringApplication.run(...)`에 제어를 넘깁니다. Spring Boot가 설정과 구성요소를 준비한 후 웹 API 요청을 받을 수 있는 상태가 됩니다.

## 9. 핵심 요약

- 이 파일은 백엔드 Java 애플리케이션의 실행 시작점입니다.
- 핵심 코드는 `SpringApplication.run(BeApplication.class, args)`입니다.
- `@SpringBootApplication`은 Spring Boot 애플리케이션의 중심 설정입니다.
- `@EnableJpaAuditing`은 [`BaseTimeEntity`](../src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java)의 생성·수정 시각 자동 기록과 연결됩니다.
