# SATrk 운영 배포 준비

이 문서는 SATrk를 실제 서버에 처음 배포할 때 필요한 환경 변수와 데이터베이스 변경 절차를 정리한다.

## 1. 배포 전 확인

- 운영 DB를 먼저 백업한다.
- 비밀번호, API 키, SMTP 앱 비밀번호는 GitHub에 올리지 않는다.
- 백엔드는 `prod` 프로필로 실행한다.
- 프론트엔드와 백엔드의 실제 HTTPS 주소를 확정한다.

## 2. 백엔드 환경 변수

`backend/.env.production.example`을 기준으로 배포 서비스의 환경 변수 화면에 값을 등록한다. 운영 서버에 `.env` 파일을 둘 수는 있지만 저장소에는 커밋하지 않는다.

필수 항목:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `NEXON_API_KEY`, `NEXON_API_BASE_URL`
- `OPENAI_API_KEY`, `OPENAI_MODEL`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `CORS_ALLOWED_ORIGIN_PATTERNS`: 실제 프론트엔드 주소만 지정
- `FRONTEND_BASE_URL`: 이메일 링크가 열 실제 프론트엔드 주소
- 메일 발송에 필요한 `MAIL_*`, `SMTP_*`
- `SECURITY_*`: 로그인·메일·AI·공개 조회 요청 제한

예시:

```properties
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGIN_PATTERNS=https://satrk.example.com
FRONTEND_BASE_URL=https://satrk.example.com
```

운영 환경에서 `CLAN_TEST_ENABLED`는 반드시 `false`로 유지한다.

현재 요청 제한 카운터는 백엔드 한 대의 메모리에 저장된다. 서버를 여러 대로 확장할 때는 Redis 또는 배포 플랫폼의 API Gateway 요청 제한으로 교체한다.

프론트엔드 호스팅 설정에도 `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options`, `Referrer-Policy` 응답 헤더를 등록한다. 백엔드는 API 응답에 보안 헤더를 적용하지만, 실제 React 문서를 보호하는 헤더는 프론트엔드 호스팅 서비스가 전송해야 한다.

프로필 이미지와 게시판 첨부 이미지는 Cloudinary에 영구 저장된다. 게시판 첨부는 `authenticated` 자산으로 업로드되며, 특히 비공개 문의 이미지는 Cloudinary 주소를 브라우저에 직접 전달하지 않는다. 사용자가 SATrk 이미지 API를 요청하면 게시글 열람 권한을 먼저 확인한 뒤 백엔드가 짧게 유효한 서명 주소로 이미지를 받아 전달한다. 기존 `uploads/board` 로컬 파일은 이전 게시글 호환을 위해 계속 읽을 수 있지만, 신규 운영 첨부파일 저장소로 사용하지 않는다.

## 3. 프론트엔드 환경 변수

`frontend/.env.production.example`을 기준으로 빌드 서비스에 실제 API 주소를 등록한다.

```properties
VITE_API_BASE_URL=https://api.satrk.example.com
```

Vite 환경 변수는 빌드 시점에 포함되므로 주소를 바꾸면 프론트엔드를 다시 빌드해야 한다.

## 4. 데이터베이스 마이그레이션

운영 프로필에서는 Hibernate가 DB 구조를 임의로 수정하지 않고 `validate`만 수행한다. 구조 변경은 Flyway SQL 파일로 관리한다.

- 빈 DB: `V1__baseline_schema.sql`이 전체 기본 구조를 생성한다.
- 기존 SATrk DB: 최초 실행 시 현재 DB를 버전 1로 기준 등록하고 기존 테이블과 데이터를 유지한다.
- 이후 변경: `backend/src/main/resources/db/migration/`에 `V2__설명.sql`, `V3__설명.sql` 순서로 추가한다.

한번 운영에 적용한 마이그레이션 파일은 수정하거나 삭제하지 않는다. 변경이 필요하면 다음 번호의 새 파일을 만든다.

첫 운영 실행 전 순서:

1. 운영 DB 전체 백업
2. 배포 환경 변수 확인
3. 백엔드 실행
4. 로그에서 Flyway 적용 성공 확인
5. 회원가입, 로그인, 게시판, 즐겨찾기, 이미지 업로드 확인

문제가 생기면 Flyway `clean`을 사용하지 않는다. 애플리케이션 버전을 되돌리고, 필요한 DB 수정은 새로운 순방향 마이그레이션으로 처리한다.

## 5. 배포 서비스 헬스체크

백엔드는 배포 플랫폼에 종속되지 않는 Spring Boot Actuator 상태 주소를 제공한다.

- 프로세스 생존 확인: `/actuator/health/liveness`
- 요청 처리 준비 확인(DB 연결 포함): `/actuator/health/readiness`
- 전체 기본 상태: `/actuator/health`

배포 서비스의 Health Check Path에는 `/actuator/health/readiness`를 등록한다. 정상 응답은 HTTP `200`과 `{"status":"UP"}`이며 내부 DB 주소나 인증 정보 같은 상세 내용은 노출하지 않는다. SMTP 장애가 서버의 무한 재시작으로 이어지지 않도록 메일 서버는 자동 헬스체크에서 제외하고, 실제 인증 메일 발송은 별도 기능 점검으로 확인한다.

운영 서버 종료 시에는 최대 20초 동안 진행 중인 요청을 마무리하는 graceful shutdown을 사용한다.

## 6. 로컬 검증 명령

```bash
cd backend
./gradlew.bat test --rerun-tasks --console=plain
```

```bash
cd frontend
npm run lint
npm run build
```

로컬 개발은 기본 프로필을 사용하므로 Flyway가 꺼져 있고 Hibernate `update`가 적용된다. 실제 배포에서만 `prod` 프로필과 Flyway가 활성화된다.

## 7. 비밀정보 점검

커밋 전 아래 파일만 예시 파일로 올라가는지 확인한다.

- `backend/.env.local.example`
- `backend/.env.production.example`
- `frontend/.env.production.example`

실제 `backend/.env.local`과 운영 비밀값 파일은 Git 상태에 나타나면 안 된다.
