package com.sa.trk.clan.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.clan.dto.ClanMemberResponseDto;
import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.player.service.PlayerService;

@Service
public class ClanMemberService {

    private final ClanMemberRepository clanMemberRepository;
    private final PlayerService playerService;
    private final AuthUserRepository authUserRepository;
    private final ClanMemberMetricsService clanMemberMetricsService;

    public ClanMemberService(
            ClanMemberRepository clanMemberRepository,
            PlayerService playerService,
            AuthUserRepository authUserRepository,
            ClanMemberMetricsService clanMemberMetricsService) {
        this.clanMemberRepository = clanMemberRepository;
        this.playerService = playerService;
        this.authUserRepository = authUserRepository;
        this.clanMemberMetricsService = clanMemberMetricsService;
    }

    @Transactional
    public ClanMemberResponseDto addMember(Long ownerId, String userName) {
        validateOwnerId(ownerId);
        String normalizedUserName = normalizeUserName(userName);
        return clanMemberRepository.findByOwnerIdAndUserNameIgnoreCase(ownerId, normalizedUserName)
                .map(this::toDto)
                .orElseGet(() -> createMember(ownerId, normalizedUserName));
    }

    @Transactional(readOnly = true)
    public List<ClanMemberResponseDto> getMembers(Long ownerId) {
        validateOwnerId(ownerId);
        return clanMemberRepository.findAllByOwnerIdOrderByUserNameAsc(ownerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteMember(Long ownerId, Long id) {
        validateOwnerId(ownerId);
        if (id == null || id < 1) throw memberNotFound();
        ClanMember member = clanMemberRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(this::memberNotFound);
        clanMemberRepository.delete(member);
    }

    private ClanMemberResponseDto createMember(Long ownerId, String userName) {
        String ouid = resolveOuid(ownerId, userName);
        if (ouid.isBlank()) {
            throw new IllegalArgumentException("플레이어 정보를 찾을 수 없습니다.");
        }

        UserBasicDto basic = playerService.getUserBasic(ouid);
        String clanName = basic == null ? "" : normalizeText(basic.getClan_name());
        if (clanName.isBlank()) {
            throw new IllegalArgumentException("소속 클랜이 없는 플레이어는 등록할 수 없습니다.");
        }

        ClanMember member = new ClanMember();
        member.setOwner(authUserRepository.getReferenceById(ownerId));
        member.setUserName(normalizeText(basic.getUser_name()).isBlank()
                ? userName
                : normalizeText(basic.getUser_name()));
        member.setClanName(clanName);
        member.setOuid(ouid);
        ClanMember savedMember = clanMemberRepository.save(member);
        return toDto(clanMemberMetricsService.refreshMember(savedMember));
    }

    private String resolveOuid(Long ownerId, String userName) {
        String ownerOuid = authUserRepository.findById(ownerId)
                .filter(owner -> sameText(owner.getSuddenNickname(), userName))
                .map(AuthUser::getOuid)
                .map(this::normalizeText)
                .orElse("");
        if (!ownerOuid.isBlank()) {
            return ownerOuid;
        }

        OuidResponseDto ouidResponse = playerService.getOuid(userName);
        return ouidResponse == null ? "" : normalizeText(ouidResponse.getOuid());
    }

    private void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId < 1) {
            throw new IllegalArgumentException("로그인 회원 정보를 확인해주세요.");
        }
    }

    private IllegalArgumentException memberNotFound() {
        return new IllegalArgumentException("삭제할 클랜원을 찾을 수 없습니다.");
    }

    private ClanMemberResponseDto toDto(ClanMember member) {
        ClanMemberResponseDto response = new ClanMemberResponseDto();
        response.setId(member.getId());
        response.setUserName(member.getUserName());
        response.setClanName(member.getClanName());
        response.setCreatedAt(member.getCreatedAt());
        return response;
    }

    private String normalizeUserName(String userName) {
        String normalized = normalizeText(userName);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean sameText(String left, String right) {
        return normalizeText(left).equalsIgnoreCase(normalizeText(right));
    }
}
