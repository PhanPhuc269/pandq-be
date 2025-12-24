package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.AddressDTO;
import pandq.application.port.repositories.AddressRepository;
import pandq.domain.models.user.Address;
import pandq.domain.models.user.User;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final JpaUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressDTO.Response> getAddressesByUserId(UUID userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressDTO.Response getAddressById(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        return mapToResponse(address);
    }

    @Transactional
    public AddressDTO.Response createAddress(AddressDTO.CreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaultAddresses(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .receiverName(request.getReceiverName())
                .phone(request.getPhone())
                .detailAddress(request.getDetailAddress())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        Address savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    @Transactional
    public AddressDTO.Response updateAddress(UUID id, AddressDTO.UpdateRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            unsetOtherDefaultAddresses(address.getUser().getId());
        }

        address.setReceiverName(request.getReceiverName());
        address.setPhone(request.getPhone());
        address.setDetailAddress(request.getDetailAddress());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        Address savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    private void unsetOtherDefaultAddresses(UUID userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    @Transactional
    public void deleteAddress(UUID id) {
        addressRepository.deleteById(id);
    }

    /**
     * Set an address as the default address for checkout
     * Unsets any previous default address for the same user
     */
    @Transactional
    public AddressDTO.Response setDefaultAddress(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Unset other default addresses for this user
        unsetOtherDefaultAddresses(address.getUser().getId());
        
        // Set this address as default
        address.setIsDefault(true);
        Address savedAddress = addressRepository.save(address);
        
        return mapToResponse(savedAddress);
    }

    private AddressDTO.Response mapToResponse(Address address) {
        AddressDTO.Response response = new AddressDTO.Response();
        response.setId(address.getId());
        response.setUserId(address.getUser().getId());
        response.setReceiverName(address.getReceiverName());
        response.setPhone(address.getPhone());
        response.setDetailAddress(address.getDetailAddress());
        response.setWard(address.getWard());
        response.setDistrict(address.getDistrict());
        response.setCity(address.getCity());
        response.setIsDefault(address.getIsDefault());
        return response;
    }
}
