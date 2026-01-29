package pandq.adapter.web.api.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.UserDTO;
import pandq.application.services.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO.Response>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.Response> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO.Response> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PostMapping
    public ResponseEntity<UserDTO.Response> createUser(@RequestBody UserDTO.CreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO.Response> updateUser(@PathVariable UUID id, @RequestBody UserDTO.UpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@RequestBody UserDTO.FcmTokenRequest request) {
        userService.updateFcmToken(request.getUserId(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fcm-token-by-email")
    public ResponseEntity<Void> updateFcmTokenByEmail(@RequestBody UserDTO.FcmTokenByEmailRequest request) {
        userService.updateFcmTokenByEmail(request.getEmail(), request.getFcmToken(), request.getFirebaseUid());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/close-account")
    public ResponseEntity<Void> closeAccount(@RequestBody UserDTO.CloseAccountRequest request) {
        userService.closeAccount(request.getEmail(), request.getReason());
        return ResponseEntity.ok().build();
    }
}


