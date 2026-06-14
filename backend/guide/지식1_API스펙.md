# [지식] NEXON Open API 서든어택 - 전체 API 스펙

## 공통
- Base URL: `https://open.api.nexon.com`
- 인증: 헤더 `x-nxopen-api-key`에 API KEY 전달
- 데이터 제공: 게임 종료 후 평균 10분
- 조회 가능 기간: 2025년 1월 24일 이후
- ouid는 게임 콘텐츠 변경 시 변경될 수 있음

## 에러 응답 (공통)
```json
{ "error": { "name": "string", "message": "string" } }
```
| 코드 | 설명 |
|------|------|
| 400 | Bad Request |
| 403 | Forbidden |
| 429 | Too Many Requests (호출 제한 초과) |
| 500 | Internal Server Error |

---

## 1. 계정 식별자(ouid) 조회
```
GET /suddenattack/v1/id?user_name={닉네임}
```
**응답:**
```json
{ "ouid": "string" }
```

## 2. 기본 정보 조회
```
GET /suddenattack/v1/user/basic?ouid={ouid}
```
**응답:**
```json
{
  "user_name": "string",
  "user_date_create": "2023-12-14T08:28:35Z",
  "title_name": "string",
  "clan_name": "string",
  "manner_grade": "string"
}
```
| 필드 | 설명 |
|------|------|
| user_name | 닉네임 |
| user_date_create | 계정 생성일 (UTC) |
| title_name | 장착 칭호 명 |
| clan_name | 소속 클랜 명 |
| manner_grade | 매너 등급 |

## 3. 계급 정보 조회
```
GET /suddenattack/v1/user/rank?ouid={ouid}
```
- 계급 정보는 게임 접속 종료 시 갱신

**응답:**
```json
{
  "user_name": "string",
  "grade": "string",
  "grade_exp": 0,
  "grade_ranking": 0,
  "season_grade": "string",
  "season_grade_exp": 0,
  "season_grade_ranking": 0
}
```
| 필드 | 설명 |
|------|------|
| grade | 통합 계급 |
| grade_exp | 통합 계급 경험치 |
| grade_ranking | 통합 계급 랭킹 |
| season_grade | 시즌 계급 |
| season_grade_exp | 시즌 계급 경험치 |
| season_grade_ranking | 시즌 계급 랭킹 |

## 4. 티어 정보 조회
```
GET /suddenattack/v1/user/tier?ouid={ouid}
```
- 티어 정보는 게임 접속 종료 시 갱신

**응답:**
```json
{
  "user_name": "string",
  "solo_rank_match_tier": 0,
  "solo_rank_match_score": 0,
  "party_rank_match_tier": 0,
  "party_rank_match_score": 0
}
```
| 필드 | 설명 |
|------|------|
| solo_rank_match_tier | 솔로 랭크전 티어 |
| solo_rank_match_score | 솔로 랭크전 점수 |
| party_rank_match_tier | 파티 랭크전 티어 |
| party_rank_match_score | 파티 랭크전 점수 |

## 5. 최근 동향 정보 조회
```
GET /suddenattack/v1/user/recent-info?ouid={ouid}
```
- 최근 동향 정보는 매치 종료 시 갱신

**응답:**
```json
{
  "user_name": "string",
  "recent_win_rate": 0,
  "recent_kill_death_rate": 0,
  "recent_assault_rate": 0,
  "recent_sniper_rate": 0,
  "recent_special_rate": 0
}
```
| 필드 | 설명 |
|------|------|
| recent_win_rate | 최근 승률 |
| recent_kill_death_rate | 최근 킬데스 |
| recent_assault_rate | 최근 돌격소총 킬데스 |
| recent_sniper_rate | 최근 저격소총 킬데스 |
| recent_special_rate | 최근 특수총 킬데스 |

## 6. 매치 목록 조회
```
GET /suddenattack/v1/match?ouid={ouid}&match_mode={모드}&match_type={유형}
```
**파라미터:**
| 이름 | 위치 | 필수 | 설명 |
|------|------|------|------|
| ouid | query | O | 계정 식별자 |
| match_mode | query | - | 개인전 / 데스매치 / 폭파미션 / 진짜를 모아라 |
| match_type | query | - | 일반전 / 클랜전 / 퀵매치 클랜전 / 클랜 랭크전 / 랭크전 솔로 / 랭크전 파티 / 토너먼트 |

- 최대 1000개 조회 가능

**응답:**
```json
{
  "match": [
    {
      "match_id": "string",
      "match_type": "string",
      "match_mode": "string",
      "date_match": "2023-12-14T08:28:35Z",
      "match_result": "string",
      "kill": 0,
      "death": 0,
      "assist": 0
    }
  ]
}
```
**match_result 규칙:**
- "1": 승리, "2": 패배, "3": 무승부
- 진짜를 모아라 모드는 승패 미기록
- 개인전/진짜를 모아라: 킬 수 높은 순 → 동일 시 데스 적은 순

## 7. 매치 상세 기록 조회
```
GET /suddenattack/v1/match-detail?match_id={매치ID}
```
**응답:**
```json
{
  "match_id": "string",
  "match_type": "string",
  "match_mode": "string",
  "date_match": "2023-12-14T08:28:35Z",
  "match_map": "string",
  "match_detail": [
    {
      "team_id": "string",
      "match_result": "string",
      "user_name": "string",
      "season_grade": "string",
      "clan_name": "string",
      "kill": 0,
      "death": 0,
      "headshot": 0,
      "damage": 0.0,
      "assist": 0
    }
  ]
}
```
- clan_name: 클랜전/퀵매치 클랜전/클랜 랭크전에서만 노출

## 8. 메타데이터 API (파라미터 없음)

### 로고
```
GET /static/suddenattack/meta/logo
→ { "logo_image": "string" }
```

### 통합 계급 목록
```
GET /static/suddenattack/meta/grade
→ [{ "grade": "string", "grade_image": "string" }]
```

### 시즌 계급 목록
```
GET /static/suddenattack/meta/season_grade
→ [{ "season_grade": "string", "season_grade_image": "string" }]
```

### 티어 목록
```
GET /static/suddenattack/meta/tier
→ [{ "tier": "string", "tier_image": "string" }]
```
