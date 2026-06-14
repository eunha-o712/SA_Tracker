# SATrk Workspace

현재 작업 폴더는 프론트엔드와 백엔드를 분리해서 관리하는 기준 폴더다.

## 폴더 구조

```text
SATrk/
- backend/
- frontend/
```

## backend 에 가져올 것

원본 경로:
`C:\Users\고은종\OneDrive\문서\SA pro\SATrk_work`

가져올 대상:

- `src`
- `build.gradle`
- `settings.gradle`
- `gradlew`
- `gradlew.bat`
- `gradle`
- `.gitignore`
- 필요하면 `README.md`

가져오지 않을 대상:

- `.metadata`
- `.settings`
- `bin`
- `build`
- `.gradle`
- `.classpath`
- `.project`

## frontend 운영 방식

현재 원본 프로젝트의 화면은 React가 아니라 Spring Boot + Thymeleaf 구조다.

참고할 위치:

- `src/main/resources/templates`
- `src/main/resources/static/css`
- `src/main/resources/static/js`

즉, `frontend`는 새 React 프로젝트로 시작하고, 기존 HTML/CSS/JS는 화면과 기능을 옮길 참고본으로 사용한다.

## 추천 진행 순서

1. `backend`에 Spring 프로젝트 파일만 복사한다.
2. STS에서 `backend`를 Gradle 프로젝트로 불러온다.
3. `frontend`에서 React 프로젝트를 새로 만든다.
4. React 개발 서버에서 백엔드 `http://localhost:8085`를 호출하도록 연결한다.
5. 기존 `templates` 화면을 기준으로 React 컴포넌트로 옮긴다.

## 바로 확인할 항목

- 백엔드 포트: `8085`
- DB 연결: MySQL `satrk`
- API 키와 DB 비밀번호는 별도 설정 파일이나 환경 변수로 분리하는 것이 안전하다.
