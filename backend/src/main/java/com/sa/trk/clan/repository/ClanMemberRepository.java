package com.sa.trk.clan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.trk.clan.entity.ClanMember;

public interface ClanMemberRepository extends JpaRepository<ClanMember, Long> {

    Optional<ClanMember> findByOwnerIdAndUserNameIgnoreCase(Long ownerId, String userName);

    Optional<ClanMember> findByIdAndOwnerId(Long id, Long ownerId);

    List<ClanMember> findAllByOwnerIdOrderByUserNameAsc(Long ownerId);

    List<ClanMember> findAllByOwnerIdAndClanNameIgnoreCaseOrderByUserNameAsc(Long ownerId, String clanName);

    List<ClanMember> findAllByOwnerIdAndIdIn(Long ownerId, List<Long> ids);
}
