@echo off
chcp 65001>nul

echo ===== SATrk Frontend 폴더 구조 생성 시작 =====
echo.

REM - public 정적 파일
mkdir "public\video" 2>nul

REM - API 호출 모음
mkdir "src\api" 2>nul

REM - 이미지 / 아이콘 같은 에셋
mkdir "src\assets\images" 2>nul
mkdir "src\assets\icons" 2>nul

REM - 공통 컴포넌트
mkdir "src\components\LoadingIntro" 2>nul
mkdir "src\components\SearchBar" 2>nul
mkdir "src\components\Header" 2>nul
mkdir "src\components\Footer" 2>nul
mkdir "src\components\PlayerProfile" 2>nul
mkdir "src\components\StatsSummary" 2>nul
mkdir "src\components\MatchCard" 2>nul
mkdir "src\components\MatchFilter" 2>nul
mkdir "src\components\TeamTable" 2>nul

REM - 페이지 단위 컴포넌트
mkdir "src\pages\HomePage" 2>nul
mkdir "src\pages\PlayerPage" 2>nul
mkdir "src\pages\MatchDetailPage" 2>nul

REM - 공통 스타일
mkdir "src\styles" 2>nul

REM - 날짜 포맷, 공통 함수 등 유틸
mkdir "src\utils" 2>nul

echo.
echo ===== 생성 완료 =====
echo.
pause