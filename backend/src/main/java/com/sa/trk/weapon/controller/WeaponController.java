package com.sa.trk.weapon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.weapon.dto.WeaponStatsResponseDto;
import com.sa.trk.weapon.service.WeaponService;

@RestController
public class WeaponController {

    private final WeaponService weaponService;

    public WeaponController(WeaponService weaponService) {
        this.weaponService = weaponService;
    }

    @GetMapping("/api/weapon")
    public WeaponStatsResponseDto getWeaponStats(@RequestParam("userName") String userName) {
        return weaponService.getWeaponStats(userName);
    }
}