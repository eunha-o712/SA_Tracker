# SA.GG - Sudden Attack Match Search

서든어택 전적 조회 웹사이트 초안입니다. Spring Boot 3.x + MySQL + Thymeleaf + Bootstrap 5 기준으로 구성했습니다.

## 포함된 범위

- 메인 페이지 검색 UI
- MP4 인트로 로딩 화면
- 클랜원 목록 및 클랜원 추가
- 닉네임 기반 ouid 캐싱
- 플레이어 전적 요약 페이지
- 매치 상세 페이지
- Nexon Open API 호출 서비스 계층
- MySQL 초기 스키마 예시

## 실행 전 준비

1. MySQL에 `sa_match` 데이터베이스를 준비합니다.
2. [src/main/resources/db/init.sql](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/resources/db/init.sql)에 있는 스키마를 실행합니다.
3. [src/main/resources/application.properties](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/resources/application.properties)에서 아래 값을 실제 환경으로 바꿉니다.
   - `spring.datasource.username`
   - `spring.datasource.password`
   - `nexon.api.key`

## 주요 파일

- [build.gradle](C:/Users/고은종/OneDrive/문서/SA%20pro/build.gradle)
- [src/main/java/com/sa/match/service/NexonApiService.java](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/java/com/sa/match/service/NexonApiService.java)
- [src/main/resources/templates/index.html](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/resources/templates/index.html)
- [src/main/resources/templates/player.html](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/resources/templates/player.html)
- [src/main/resources/templates/match-detail.html](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/resources/templates/match-detail.html)
- [src/main/resources/static/css/app.css](C:/Users/고은종/OneDrive/문서/SA%20pro/src/main/resources/static/css/app.css)

## 참고

- 현재 작업 환경에는 Gradle 전역 설치와 Gradle Wrapper 파일이 없어 실제 빌드 실행 검증은 아직 못 했습니다.
- 따라서 바로 다음 단계는 Gradle Wrapper 추가 후 애플리케이션 기동, API 실데이터 연결, UI 미세 조정입니다.
