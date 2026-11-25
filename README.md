# 백엔드 실행 안내

## 사전 준비

- Java 17
- PostgreSQL 데이터베이스
- Google OAuth 클라이언트 정보

백엔드 폴더(`be`)에서 `.env` 파일을 만들고 아래 항목을 환경에 맞게 설정합니다. `.env`는 Git에서 제외되므로 실제 비밀번호나 키를 저장해도 저장소에 포함되지 않습니다.

```properties
DB_URL=jdbc:postgresql://localhost:5432/데이터베이스명
DB_USERNAME=PostgreSQL_사용자명
DB_PASSWORD=PostgreSQL_비밀번호
JWT_SECRET=Base64로_인코딩한_32바이트_이상의_비밀키
JWT_REFRESH_COOKIE_SECURE=false
JWT_REFRESH_COOKIE_SAME_SITE=Lax
GOOGLE_CLIENT_ID=Google_OAuth_클라이언트_ID
GOOGLE_CLIENT_SECRET=Google_OAuth_클라이언트_시크릿
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

`JWT_SECRET`은 Base64 형식이어야 하며 디코딩한 값이 최소 32바이트여야 합니다. 로컬 HTTP 환경에서는 refresh 쿠키의 `JWT_REFRESH_COOKIE_SECURE`를 `false`로 설정하고, HTTPS 환경에서는 `true`로 설정합니다. Google OAuth 클라이언트 정보는 Google Cloud Console에서 발급한 값을 사용합니다.

`CORS_ALLOWED_ORIGINS`에는 프론트엔드 origin을 쉼표로 구분해 입력합니다. 배포 환경에서는 실제 프론트엔드 주소로 바꾸며, credentials를 사용하는 요청을 위해 와일드카드(`*`) 대신 정확한 origin을 지정합니다. CORS 허용 헤더에는 JWT `Authorization`과 비로그인 방문자 중복 조회 방지용 `X-Visitor-Id`가 포함됩니다.

## 백엔드 실행

터미널에서 백엔드 폴더로 이동한 뒤 Gradle Wrapper로 Spring Boot를 실행합니다.

```
powershell

.\gradlew.bat bootRun
```

macOS 또는 Linux에서는 다음 명령을 사용합니다.

```
bash

./gradlew bootRun
```

Gradle Wrapper가 필요한 Gradle 버전을 자동으로 준비하므로 별도로 Gradle을 설치할 필요는 없습니다. 서버를 종료하려면 실행 중인 터미널에서 `Ctrl+C`를 누릅니다.

## 빌드 및 JAR 실행

Windows에서는 다음 명령으로 테스트를 포함해 빌드하고 JAR 파일을 실행할 수 있습니다.

```
powershell

.\gradlew.bat clean build
java -jar .\build\libs\be-0.0.1-SNAPSHOT.jar
```

macOS 또는 Linux에서는 다음과 같이 실행합니다.

```
bash

./gradlew clean build
java -jar build/libs/be-0.0.1-SNAPSHOT.jar
```
