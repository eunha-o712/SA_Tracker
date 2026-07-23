package com.sa.trk.auth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_users")
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String loginId;

    @Column(nullable = false, length = 20)
    private String displayName;

    @Column(unique = true, length = 254)
    private String email;

    @Column(length = 20)
    private String suddenNickname;

    @Column(unique = true, length = 80)
    private String ouid;

    @Column
    private Boolean clanNone;

    @Column
    private Boolean nicknameVerified;

    @Column(nullable = false)
    private boolean admin;

    @Column
    private Instant verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountStatus accountStatus;

    @Column(length = 500)
    private String sanctionReason;

    @Column
    private Instant sanctionedAt;

    @Column
    private Long sanctionedById;

    @Column(nullable = false, length = 32)
    private String passwordSalt;

    @Column(nullable = false, length = 64)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSuddenNickname() { return suddenNickname; }
    public void setSuddenNickname(String suddenNickname) { this.suddenNickname = suddenNickname; }
    public String getOuid() { return ouid; }
    public void setOuid(String ouid) { this.ouid = ouid; }
    public Boolean getClanNone() { return clanNone; }
    public void setClanNone(Boolean clanNone) { this.clanNone = clanNone; }
    public Boolean getNicknameVerified() { return nicknameVerified; }
    public void setNicknameVerified(Boolean nicknameVerified) { this.nicknameVerified = nicknameVerified; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public String getSanctionReason() { return sanctionReason; }
    public void setSanctionReason(String sanctionReason) { this.sanctionReason = sanctionReason; }
    public Instant getSanctionedAt() { return sanctionedAt; }
    public void setSanctionedAt(Instant sanctionedAt) { this.sanctionedAt = sanctionedAt; }
    public Long getSanctionedById() { return sanctionedById; }
    public void setSanctionedById(Long sanctionedById) { this.sanctionedById = sanctionedById; }
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
