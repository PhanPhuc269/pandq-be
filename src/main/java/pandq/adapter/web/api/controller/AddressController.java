package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.AddressDTO;
import pandq.application.services.AddressService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AddressDTO.Response>> getAddressesByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(addressService.getAddressesByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO.Response> getAddressById(@PathVariable UUID id) {
        return ResponseEntity.ok(addressService.getAddressById(id));
    }

    @PostMapping
    public ResponseEntity<AddressDTO.Response> createAddress(@RequestBody AddressDTO.CreateRequest request) {
        return ResponseEntity.ok(addressService.createAddress(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO.Response> updateAddress(@PathVariable UUID id, @RequestBody AddressDTO.UpdateRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressDTO.Response> setDefaultAddress(@PathVariable UUID id) {
        return ResponseEntity.ok(addressService.setDefaultAddress(id));
    }
}
