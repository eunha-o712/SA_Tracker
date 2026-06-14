@echo off
chcp 65001>nul

echo ===== SATrk 프로젝트 Backend 폴더 구조 생성 시작 =====
echo.

REM - 공통

mkdir "src\main\java\com\sa\trk\config" 2>nul
mkdir "src\main\java\com\sa\trk\common" 2>nul

REM - NEXON OPEN API

mkdir "src\main\java\com\sa\trk\nexon\client" 2>nul
mkdir "src\main\java\com\sa\trk\nexon\dto" 2>nul

REM - Player

mkdir "src\main\java\com\sa\trk\player\controller" 2>nul
mkdir "src\main\java\com\sa\trk\player\service" 2>nul
mkdir "src\main\java\com\sa\trk\player\dto" 2>nul
mkdir "src\main\java\com\sa\trk\player\entity" 2>nul
mkdir "src\main\java\com\sa\trk\player\repository" 2>nul

REM - Clan

mkdir "src\main\java\com\sa\trk\clan\controller" 2>nul
mkdir "src\main\java\com\sa\trk\clan\service" 2>nul
mkdir "src\main\java\com\sa\trk\clan\dto" 2>nul
mkdir "src\main\java\com\sa\trk\clan\entity" 2>nul
mkdir "src\main\java\com\sa\trk\clan\repository" 2>nul

REM - Match

mkdir "src\main\java\com\sa\trk\match\controller" 2>nul
mkdir "src\main\java\com\sa\trk\match\service" 2>nul
mkdir "src\main\java\com\sa\trk\match\dto" 2>nul
mkdir "src\main\java\com\sa\trk\match\entity" 2>nul
mkdir "src\main\java\com\sa\trk\match\repository" 2>nul

REM - Map

mkdir "src\main\java\com\sa\trk\gameMap\controller" 2>nul
mkdir "src\main\java\com\sa\trk\gameMap\service" 2>nul
mkdir "src\main\java\com\sa\trk\gameMap\dto" 2>nul
mkdir "src\main\java\com\sa\trk\gameMap\entity" 2>nul
mkdir "src\main\java\com\sa\trk\gameMap\repository" 2>nul

REM - Weapon

mkdir "src\main\java\com\sa\trk\weapon\controller" 2>nul
mkdir "src\main\java\com\sa\trk\weapon\service" 2>nul
mkdir "src\main\java\com\sa\trk\weapon\dto" 2>nul
mkdir "src\main\java\com\sa\trk\weapon\entity" 2>nul
mkdir "src\main\java\com\sa\trk\weapon\repository" 2>nul

REM - Stats

mkdir "src\main\java\com\sa\trk\stats\controller" 2>nul
mkdir "src\main\java\com\sa\trk\stats\service" 2>nul
mkdir "src\main\java\com\sa\trk\stats\dto" 2>nul
mkdir "src\main\java\com\sa\trk\stats\entity" 2>nul
mkdir "src\main\java\com\sa\trk\stats\repository" 2>nul

REM - Ranking

mkdir "src\main\java\com\sa\trk\ranking\controller" 2>nul
mkdir "src\main\java\com\sa\trk\ranking\service" 2>nul
mkdir "src\main\java\com\sa\trk\ranking\dto" 2>nul
mkdir "src\main\java\com\sa\trk\ranking\entity" 2>nul
mkdir "src\main\java\com\sa\trk\ranking\repository" 2>nul

REM - Search

mkdir "src\main\java\com\sa\trk\search\controller" 2>nul
mkdir "src\main\java\com\sa\trk\search\service" 2>nul
mkdir "src\main\java\com\sa\trk\search\dto" 2>nul

REM - Favorite

mkdir "src\main\java\com\sa\trk\favorite\controller" 2>nul
mkdir "src\main\java\com\sa\trk\favorite\service" 2>nul
mkdir "src\main\java\com\sa\trk\favorite\dto" 2>nul
mkdir "src\main\java\com\sa\trk\favorite\entity" 2>nul
mkdir "src\main\java\com\sa\trk\favorite\repository" 2>nul

REM - Report

mkdir "src\main\java\com\sa\trk\report\controller" 2>nul
mkdir "src\main\java\com\sa\trk\report\service" 2>nul
mkdir "src\main\java\com\sa\trk\report\dto" 2>nul

REM - Auth

mkdir "src\main\java\com\sa\trk\auth\controller" 2>nul
mkdir "src\main\java\com\sa\trk\auth\service" 2>nul
mkdir "src\main\java\com\sa\trk\auth\dto" 2>nul

echo.
echo ===== 생성완료 =====
echo.
pause