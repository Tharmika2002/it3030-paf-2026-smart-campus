package com.sliit.smartcampus.user;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.resource.dto.ApiResponse;
import com.sliit.smartcampus.user.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile fetched successfully",
                userService.getUserById(currentUser.getId())));
    }

    // PATCH /api/v1/users/me/preferences — NEW
    @PatchMapping("/me/preferences")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody NotifPrefs prefs) {
        return ResponseEntity.ok(ApiResponse.success(
                "Preferences updated successfully",
                userService.updatePreferences(currentUser.getId(), prefs)));
    }

    // GET /api/v1/users — ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Users fetched successfully",
                userService.getAllUsers()));
    }

    // PATCH /api/v1/users/{id}/role — ADMIN only
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateRole(
            @PathVariable UUID id,
            @RequestParam Role role) {
        return ResponseEntity.ok(ApiResponse.success(
                "Role updated successfully",
                userService.updateRole(id, role)));
    }
}